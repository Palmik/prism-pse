package pse;

import org.jocl.*;

import static org.jocl.CL.*;

public final class PSEMVMultMany_OCL implements PSEMultMany, Releaseable
{
	public PSEMVMultMany_OCL(PSEMVMultSettings_OCL opts, PSEMVMultTopology_OCL topo, int matCnt,
		final double[] weight, final double weightDef, final int weightOff)
	{
		this.matCnt = matCnt;
		this.stCnt = topo.stCnt;
		this.topo = topo;
		this.opts = opts;

		this.clCommandQueue = clCreateCommandQueue(clContext(), opts.clDeviceIdMax, 0, null);

		setExceptionsEnabled(true);

		this.enabledMatP = topo.matPRowCnt > 0 && topo.matPRowBegHost[topo.matPRowCnt] > 0;
		this.enabledMatNP = topo.matNPRowCnt > 0 && topo.matNPRowBegHost[topo.matNPRowCnt] > 0;

		this.weight = weight;
		this.weightDef = weightDef;
		this.weightOff = weightOff;
		this.totalIterationCnt = 0;

		clProgram = OCLProgram.createProgram(OCLProgram.SOURCE, clContext());

		if (enabledMatNP) {
			matNPInitialized = false;
			if (enabledMatP) {
				clKernelMatNP = OCLProgram.createKernel("PSE_MV_NP_CSR_BOTH_MANY", clProgram);
			} else {
				clKernelMatNP = OCLProgram.createKernel("PSE_MV_NP_CSR_MANY", clProgram);
			}

			final int len = Sizeof.cl_double * topo.matNPRowBegHost[topo.matNPRowCnt];
			matNPVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY, len, null, null);

			clSetKernelArg(clKernelMatNP, 1, Sizeof.cl_uint, Pointer.to(new int[]{topo.stCnt}));
			clSetKernelArg(clKernelMatNP, 2, Sizeof.cl_uint, Pointer.to(new int[]{topo.matNPRowCnt}));
			clSetKernelArg(clKernelMatNP, 3, Sizeof.cl_mem, Pointer.to(this.matNPVal));
			clSetKernelArg(clKernelMatNP, 4, Sizeof.cl_mem, Pointer.to(topo.matNPCol));
			clSetKernelArg(clKernelMatNP, 5, Sizeof.cl_mem, Pointer.to(topo.matNPRow));
			clSetKernelArg(clKernelMatNP, 6, Sizeof.cl_mem, Pointer.to(topo.matNPRowBeg));
		}

		if (enabledMatP) {
			clKernelMatP = OCLProgram.createKernel("PSE_MV_P_CSR_BOTH_MANY", clProgram);

			final int len = Sizeof.cl_double * topo.matPRowBegHost[topo.matPRowCnt] * matCnt;
			this.matPLowerVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY, len, null, null);
			this.matPUpperVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY, len, null, null);

			clSetKernelArg(clKernelMatP, 1, Sizeof.cl_uint, Pointer.to(new int[]{topo.stCnt}));
			clSetKernelArg(clKernelMatP, 2, Sizeof.cl_uint, Pointer.to(new int[]{topo.matPRowCnt}));
			clSetKernelArg(clKernelMatP, 3, Sizeof.cl_mem, Pointer.to(this.matPLowerVal));
			clSetKernelArg(clKernelMatP, 4, Sizeof.cl_mem, Pointer.to(this.matPUpperVal));
			clSetKernelArg(clKernelMatP, 5, Sizeof.cl_mem, Pointer.to(topo.matPCol));
			clSetKernelArg(clKernelMatP, 6, Sizeof.cl_mem, Pointer.to(topo.matPRow));
			clSetKernelArg(clKernelMatP, 7, Sizeof.cl_mem, Pointer.to(topo.matPRowBeg));
		}

		{
			int len = Sizeof.cl_double * stCnt * matCnt;
			if (enabledMatP) {
				clKernelSum = OCLProgram.createKernel("WeightedSumToBoth", clProgram);
				this.sumMin = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, len, null, null);
				this.sumMax = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, len, null, null);
				clSetKernelArg(clKernelSum, 4, Sizeof.cl_mem, Pointer.to(this.sumMin));
				clSetKernelArg(clKernelSum, 5, Sizeof.cl_mem, Pointer.to(this.sumMax));
			} else {
				clKernelSum = OCLProgram.createKernel("WeightedSumTo", clProgram);
				this.sumMin = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, len, null, null);
				clSetKernelArg(clKernelSum, 3, Sizeof.cl_mem, Pointer.to(this.sumMin));
			}
			clSetKernelArg(clKernelSum, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
		}

		{
			final int len = Sizeof.cl_double * stCnt * matCnt;
			minMem = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, len, null, null);
			resMinMem = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, len, null, null);
			if (enabledMatP) {
				maxMem = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, len, null, null);
				resMaxMem = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, len, null, null);
			}
		}
	}

	/* Updates the matrix values (assumes that values that were zero are zero as well). Resets sums to zero.
	 */
	final public void update(int matId, PSEMVCreateData_CSR data)
	{
		assert(matId < matCnt);

		totalIterationCnt = 0;

		if (enabledMatNP && !matNPInitialized) {
			final int len = Sizeof.cl_double * topo.matNPRowBegHost[topo.matNPRowCnt];
			clEnqueueWriteBuffer(clCommandQueue(), matNPVal, false, 0, len, Pointer.to(data.matNPVal), 0, null, null);
			matNPInitialized = true;
		}

		if (enabledMatP) {
			final int len = Sizeof.cl_double * topo.matPRowBegHost[topo.matPRowCnt];
			final int off = matId * len;

			final Pointer matPLowerVal_ = Pointer.to(data.matPLowerVal);
			final Pointer matPUpperVal_ = Pointer.to(data.matPUpperVal);
			clEnqueueWriteBuffer(clCommandQueue(), matPLowerVal, false, off, len, matPLowerVal_, 0, null, null);
			clEnqueueWriteBuffer(clCommandQueue(), matPUpperVal, false, off, len, matPUpperVal_, 0, null, null);
		}

		{
			final int len = Sizeof.cl_double * stCnt;
			final int off = matId * len;

			final double[] zeroes = new double[stCnt];
			final Pointer zeroes_ = Pointer.to(zeroes);
			clEnqueueWriteBuffer(clCommandQueue(), sumMin, false, off, len, zeroes_, 0, null, null);
			if (enabledMatP) {
				clEnqueueWriteBuffer(clCommandQueue(), sumMax, false, off, len, zeroes_, 0, null, null);
			}
		}
		clFinish(clCommandQueue());
	}

	@Override
	final public void release()
	{
		if (enabledMatP) {
			clReleaseKernel(clKernelMatP);
			clReleaseMemObject(matPLowerVal);
			clReleaseMemObject(matPUpperVal);
			clReleaseMemObject(maxMem);
			clReleaseMemObject(resMaxMem);
			clReleaseMemObject(sumMax);
		}
		if (enabledMatNP) {
			clReleaseKernel(clKernelMatNP);
			clReleaseMemObject(matNPVal);
		}
		clReleaseMemObject(minMem);
		clReleaseMemObject(resMinMem);
		clReleaseMemObject(sumMin);
		clReleaseKernel(clKernelSum);
		clReleaseCommandQueue(clCommandQueue);
		clReleaseProgram(clProgram);
	}

	@Override
	final public void getSum(int matId, final double[] sumMin, final double[] sumMax)
	{
		final int len = Sizeof.cl_double * stCnt;
		final int off = matId * len;
		if (enabledMatP) {
			clEnqueueReadBuffer(clCommandQueue(), this.sumMin, true, off, len, Pointer.to(sumMin), 0, null, null);
			clEnqueueReadBuffer(clCommandQueue(), this.sumMax, true, off, len, Pointer.to(sumMax), 0, null, null);
		} else {
			clEnqueueReadBuffer(clCommandQueue(), this.sumMin, true, off, len, Pointer.to(sumMin), 0, null, null);
			clEnqueueReadBuffer(clCommandQueue(), this.sumMin, true, off, len, Pointer.to(sumMax), 0, null, null);
		}
		clFinish(clCommandQueue());
	}

	@Override
	final public void setSum(int matId, final double[] sumMin, final double[] sumMax)
	{
		final int len = Sizeof.cl_double * stCnt;
		final int off = matId * len;
		clEnqueueWriteBuffer(clCommandQueue(), this.sumMin, false, off, len, Pointer.to(sumMin), 0, null, null);
		if (enabledMatP) {
			clEnqueueWriteBuffer(clCommandQueue(), this.sumMax, false, off, len, Pointer.to(sumMax), 0, null, null);
		}
		clFinish(clCommandQueue());
	}

	@Override
	final public void getMult(int matId, final double resMin[], final double resMax[])
	{
		final int len = Sizeof.cl_double * stCnt;
		final int off = matId * len;
		if (enabledMatP) {
			clEnqueueReadBuffer(clCommandQueue(), minMem, true, off, len, Pointer.to(resMin), 0, null, null);
			clEnqueueReadBuffer(clCommandQueue(), maxMem, true, off, len, Pointer.to(resMax), 0, null, null);
		} else {
			clEnqueueReadBuffer(clCommandQueue(), minMem, true, off, len, Pointer.to(resMin), 0, null, null);
			clEnqueueReadBuffer(clCommandQueue(), minMem, true, off, len, Pointer.to(resMax), 0, null, null);
		}
		clFinish(clCommandQueue());
	}

	@Override
	final public void setMult(int matId, final double min[], final double max[])
	{
		final int len = Sizeof.cl_double * stCnt;
		final int off = matId * len;
		clEnqueueWriteBuffer(clCommandQueue(), minMem, true, off, len, Pointer.to(min), 0, null, null);
		if (enabledMatP) {
			clEnqueueWriteBuffer(clCommandQueue(), maxMem, true, off, len, Pointer.to(max), 0, null, null);
		}
		clFinish(clCommandQueue());
	}

	@Override
	final public void mult(int matCnt, int iterationCnt)
	{
		assert(matCnt <= this.matCnt);

		final long[] lws = new long[]{OCLProgram.localWorkSize(64)};
		final long[] gwsP = new long[]{Utility.leastGreaterMultiple(this.topo.matPRowCnt * matCnt, lws[0])};
		final long[] gwsNP = new long[]{Utility.leastGreaterMultiple(this.topo.matNPRowCnt * matCnt, lws[0])};
		final long[] gwsSum = new long[]{Utility.leastGreaterMultiple(stCnt * matCnt, lws[0])};

		if (enabledMatP) {
			clSetKernelArg(clKernelMatP, 0, Sizeof.cl_uint, Pointer.to(new int[]{matCnt}));
		}
		if (enabledMatNP) {
			clSetKernelArg(clKernelMatNP, 0, Sizeof.cl_uint, Pointer.to(new int[]{matCnt}));
		}
		clSetKernelArg(clKernelSum, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt * matCnt}));
		if (enabledMatP) {
			for (int i = 0; i < iterationCnt; ++i) {
				final int len = Sizeof.cl_double * stCnt * matCnt;
				clEnqueueCopyBuffer(clCommandQueue(), minMem, resMinMem, 0, 0, len, 0, null, null);
				clEnqueueCopyBuffer(clCommandQueue(), maxMem, resMaxMem, 0, 0, len, 0, null, null);

				// MAT P
				clSetKernelArg(clKernelMatP, 8, Sizeof.cl_mem, Pointer.to(minMem));
				clSetKernelArg(clKernelMatP, 9, Sizeof.cl_mem, Pointer.to(maxMem));
				clSetKernelArg(clKernelMatP, 10, Sizeof.cl_mem, Pointer.to(resMinMem));
				clSetKernelArg(clKernelMatP, 11, Sizeof.cl_mem, Pointer.to(resMaxMem));
				clEnqueueNDRangeKernel(clCommandQueue(), clKernelMatP, 1, null, gwsP, lws, 0, null, null);

				// MAT NP
				if (enabledMatNP) {
					clSetKernelArg(clKernelMatNP, 7, Sizeof.cl_mem, Pointer.to(minMem));
					clSetKernelArg(clKernelMatNP, 8, Sizeof.cl_mem, Pointer.to(maxMem));
					clSetKernelArg(clKernelMatNP, 9, Sizeof.cl_mem, Pointer.to(resMinMem));
					clSetKernelArg(clKernelMatNP, 10, Sizeof.cl_mem, Pointer.to(resMaxMem));
					clEnqueueNDRangeKernel(clCommandQueue(), clKernelMatNP, 1, null, gwsNP, lws, 0, null, null);
				}

				++totalIterationCnt;

				// SUM
				if (getSumWeight() != 0) {
					clSetKernelArg(clKernelSum, 1, Sizeof.cl_double, Pointer.to(new double[]{getSumWeight()}));
					clSetKernelArg(clKernelSum, 2, Sizeof.cl_mem, Pointer.to(resMinMem));
					clSetKernelArg(clKernelSum, 3, Sizeof.cl_mem, Pointer.to(resMaxMem));
					clEnqueueNDRangeKernel(clCommandQueue(), clKernelSum, 1, null, gwsSum, lws, 0, null, null);
				}

				swapSolMem();
			}
		} else {
			for (int i = 0; i < iterationCnt; ++i) {
				clEnqueueCopyBuffer(clCommandQueue(), minMem, resMinMem, 0, 0, Sizeof.cl_double * stCnt, 0, null, null);

				// MAT NP
				if (enabledMatNP) {
					clSetKernelArg(clKernelMatNP, 7, Sizeof.cl_mem, Pointer.to(minMem));
					clSetKernelArg(clKernelMatNP, 8, Sizeof.cl_mem, Pointer.to(resMinMem));
					clEnqueueNDRangeKernel(clCommandQueue(), clKernelMatNP, 1, null, gwsNP, lws, 0, null, null);
				}

				++totalIterationCnt;

				// SUM
				if (getSumWeight() != 0) {
					clSetKernelArg(clKernelSum, 1, Sizeof.cl_double, Pointer.to(new double[]{getSumWeight()}));
					clSetKernelArg(clKernelSum, 2, Sizeof.cl_mem, Pointer.to(resMinMem));
					clEnqueueNDRangeKernel(clCommandQueue(), clKernelSum, 1, null, gwsSum, lws, 0, null, null);
				}

				swapSolMem();
			}
		}
		clFinish(clCommandQueue());
	}

	final private void swapSolMem()
	{
		final cl_mem tmpMin = minMem;
		final cl_mem tmpMax = maxMem;
		minMem = resMinMem;
		maxMem = resMaxMem;
		resMinMem = tmpMin;
		resMaxMem = tmpMax;
	}

	final private double getSumWeight()
	{
		if (totalIterationCnt >= weightOff) {
			return weight[totalIterationCnt - weightOff];
		}
		return weightDef;
	}

	final private cl_command_queue clCommandQueue() { return clCommandQueue; }
	final private cl_context clContext() { return opts.clContext; }

	final private int matCnt;
	final private int stCnt;
	final private PSEMVMultTopology_OCL topo;
	final private PSEMVMultSettings_OCL opts;
	final private boolean enabledMatP;
	final private boolean enabledMatNP;

	final private cl_program clProgram;
	final private cl_command_queue clCommandQueue;

	private cl_kernel clKernelMatP;
	private cl_kernel clKernelMatNP;
	private cl_kernel clKernelSum;

	private int totalIterationCnt;
	private cl_mem sumMin;
	private cl_mem sumMax;
	final private double[] weight;
	final private double   weightDef;
	final private int      weightOff;

	private cl_mem matPLowerVal;
	private cl_mem matPUpperVal;

	private boolean matNPInitialized;
	private cl_mem matNPVal;

	private cl_mem minMem;
	private cl_mem resMinMem;
	private cl_mem maxMem;
	private cl_mem resMaxMem;
}
