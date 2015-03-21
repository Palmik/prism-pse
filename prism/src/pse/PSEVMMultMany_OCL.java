package pse;

import org.jocl.*;

import static org.jocl.CL.*;

public class PSEVMMultMany_OCL implements PSEMultMany, Releaseable
{
	public PSEVMMultMany_OCL(PSEVMMultSettings_OCL opts, PSEVMMultTopology_OCL topo, int matCnt, final
	double[] weight, final double weightDef, final int weightOff)
	{
		this.matCnt = matCnt;
		this.stCnt = topo.stCnt;
		this.opts = opts;
		this.topo = topo;

		this.clCommandQueueMin = clCreateCommandQueue(clContext(), opts.clDeviceIdMin, 0, null);
		this.clCommandQueueMax = clCreateCommandQueue(clContext(), opts.clDeviceIdMax, 0, null);

		setExceptionsEnabled(true);

		this.enabledMatNP = topo.enabledMatNP;
		this.enabledMatIO = topo.enabledMatIO;
		this.enabledMatI = topo.enabledMatI;

		this.weight = weight;
		this.weightDef = weightDef;
		this.weightOff = weightOff;
		this.totalIterationCnt = 0;

		clProgram = OCLProgram.createProgram(OCLProgram.SOURCE, clContext());
		if (enabledMatNP) {
			if (enabledMatI || enabledMatIO) {
				clKernelMatNP = OCLProgram.createKernel("PSE_VM_NP_BOTH_MANY", clProgram);
			} else {
				clKernelMatNP = OCLProgram.createKernel("PSE_VM_NP_MANY", clProgram);
			}
		} else {
			if (enabledMatI || enabledMatIO) {
				clKernelDiag = OCLProgram.createKernel("PSE_VM_DIAG_BOTH_MANY", clProgram);
			} else {
				clKernelDiag = OCLProgram.createKernel("PSE_VM_DIAG_MANY", clProgram);
			}
		}

		clKernelMatIO = OCLProgram.createKernel("PSE_VM_IO_MANY", clProgram);
		clKernelMatI = OCLProgram.createKernel("PSE_VM_I_MANY", clProgram);

		{
			final int len = Sizeof.cl_double * stCnt * matCnt;
			this.sumMin = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, len, null, null);
			this.sumMax = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, len, null, null);
		}

		if (enabledMatI || enabledMatIO) {
			clKernelSum = OCLProgram.createKernel("WeightedSumToBoth", clProgram);
			clSetKernelArg(clKernelSum, 4, Sizeof.cl_mem, Pointer.to(this.sumMin));
			clSetKernelArg(clKernelSum, 5, Sizeof.cl_mem, Pointer.to(this.sumMax));

		} else {
			clKernelSum = OCLProgram.createKernel("WeightedSumTo", clProgram);
			clSetKernelArg(clKernelSum, 3, Sizeof.cl_mem, Pointer.to(this.sumMin));
		}

		{
			final int len = Sizeof.cl_double * stCnt * matCnt;
			this.matOMinDiagVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY, len, null, null);
			this.matOMaxDiagVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY, len, null, null);
		}

		{
			if (enabledMatNP) {
				// Setup mat
				// NOTE: We setup the NP matrix in here because it is not updated by update (for good reasons).
				final int len = Sizeof.cl_double * topo.matNPTrgBegHost[stCnt];
				this.matNPVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY, len, null, null);
				this.matNPValFilled = false;

				if (enabledMatI || enabledMatIO) {
					clSetKernelArg(clKernelMatNP, 1, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
					clSetKernelArg(clKernelMatNP, 2, Sizeof.cl_mem, Pointer.to(this.matOMinDiagVal));
					clSetKernelArg(clKernelMatNP, 3, Sizeof.cl_mem, Pointer.to(this.matOMaxDiagVal));
					clSetKernelArg(clKernelMatNP, 4, Sizeof.cl_mem, Pointer.to(this.matNPVal));
					clSetKernelArg(clKernelMatNP, 5, Sizeof.cl_mem, Pointer.to(topo.matNPSrc));
					clSetKernelArg(clKernelMatNP, 6, Sizeof.cl_mem, Pointer.to(topo.matNPTrgBeg));
				} else {
					clSetKernelArg(clKernelMatNP, 1, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
					clSetKernelArg(clKernelMatNP, 2, Sizeof.cl_mem, Pointer.to(this.matOMinDiagVal));
					clSetKernelArg(clKernelMatNP, 3, Sizeof.cl_mem, Pointer.to(this.matNPVal));
					clSetKernelArg(clKernelMatNP, 4, Sizeof.cl_mem, Pointer.to(topo.matNPSrc));
					clSetKernelArg(clKernelMatNP, 5, Sizeof.cl_mem, Pointer.to(topo.matNPTrgBeg));
				}
			} else {
				if (enabledMatI || enabledMatIO) {
					clSetKernelArg(clKernelDiag, 1, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
					clSetKernelArg(clKernelDiag, 2, Sizeof.cl_mem, Pointer.to(this.matOMinDiagVal));
					clSetKernelArg(clKernelDiag, 3, Sizeof.cl_mem, Pointer.to(this.matOMaxDiagVal));
				} else {
					clSetKernelArg(clKernelDiag, 1, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
					clSetKernelArg(clKernelDiag, 2, Sizeof.cl_mem, Pointer.to(this.matOMinDiagVal));
				}
			}
		}

		if (enabledMatI) {
			// Setup mat min
			final int len = Sizeof.cl_double * topo.matITrgBegHost[stCnt] * matCnt;

			this.matIMaxVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY, len, null, null);
			this.matIMinVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY, len, null, null);

			clSetKernelArg(clKernelMatI, 1, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
			clSetKernelArg(clKernelMatI, 2, Sizeof.cl_mem, Pointer.to(this.matIMinVal));
			clSetKernelArg(clKernelMatI, 3, Sizeof.cl_mem, Pointer.to(this.matIMaxVal));
			clSetKernelArg(clKernelMatI, 4, Sizeof.cl_mem, Pointer.to(topo.matISrc));
			clSetKernelArg(clKernelMatI, 5, Sizeof.cl_mem, Pointer.to(topo.matITrgBeg));
		}

		if (enabledMatIO) {
			// Setup mat IO
			final int len = Sizeof.cl_double * topo.matIOTrgBegHost[stCnt] * matCnt;

			this.matIOLowerVal0 = clCreateBuffer(clContext(), CL_MEM_READ_ONLY,	len, null, null);
			this.matIOLowerVal1 = clCreateBuffer(clContext(), CL_MEM_READ_ONLY,	len, null, null);
			this.matIOUpperVal0 = clCreateBuffer(clContext(), CL_MEM_READ_ONLY,	len, null, null);
			this.matIOUpperVal1 = clCreateBuffer(clContext(), CL_MEM_READ_ONLY, len, null, null);

			clSetKernelArg(clKernelMatIO, 1, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
			clSetKernelArg(clKernelMatIO, 2, Sizeof.cl_mem, Pointer.to(this.matIOLowerVal0));
			clSetKernelArg(clKernelMatIO, 3, Sizeof.cl_mem, Pointer.to(this.matIOLowerVal1));
			clSetKernelArg(clKernelMatIO, 4, Sizeof.cl_mem, Pointer.to(this.matIOUpperVal0));
			clSetKernelArg(clKernelMatIO, 5, Sizeof.cl_mem, Pointer.to(this.matIOUpperVal1));
			clSetKernelArg(clKernelMatIO, 6, Sizeof.cl_mem, Pointer.to(topo.matIOSrc));
			clSetKernelArg(clKernelMatIO, 7, Sizeof.cl_mem, Pointer.to(topo.matIOTrgBeg));
		}

		{
			final int len = Sizeof.cl_double * stCnt * matCnt;
			minMem = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, len, null, null);
			resMinMem = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, len, null, null);
			maxMem = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, len, null, null);
			resMaxMem = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, len, null, null);
		}
	}

	public void update(int matId, PSEVMCreateData_CSR data)
	{
		totalIterationCnt = 0;

		{
			final int len = Sizeof.cl_double * stCnt;
			final int off = len * matId;

			final double[] zeroes = new double[stCnt];
			final Pointer zeroes_ = Pointer.to(zeroes);
			clEnqueueWriteBuffer(clCommandQueueMin(), sumMin, false, off, len, zeroes_, 0, null, null);
			if (enabledMatI || enabledMatIO) {
				clEnqueueWriteBuffer(clCommandQueueMax(), sumMax, false, off, len, zeroes_, 0, null, null);
			}
		}

		{
			final int len = Sizeof.cl_double * stCnt;
			final int off = len * matId;

			final Pointer matOMinDiagVal_ = Pointer.to(data.matOMinDiagVal);
			final Pointer matOMaxDiagVal_ = Pointer.to(data.matOMaxDiagVal);
			clEnqueueWriteBuffer(clCommandQueueMin(), matOMinDiagVal, false, off, len,
				matOMinDiagVal_, 0, null, null);
			if (enabledMatI || enabledMatIO) {
				clEnqueueWriteBuffer(clCommandQueueMax(), matOMaxDiagVal, false, off, len, matOMaxDiagVal_, 0, null, null);
			}
		}

		if (enabledMatNP && !matNPValFilled) {
			final int len = Sizeof.cl_double * topo.matNPTrgBegHost[stCnt];
			clEnqueueWriteBuffer(clCommandQueueMin(), matNPVal, false, 0, len,
				Pointer.to(data.matNPVal), 0, null, null);
			matNPValFilled = true;
		}

		if (enabledMatI) {
			// Setup mat min
			final int len = Sizeof.cl_double * topo.matITrgBegHost[stCnt];
			final int off = len * matId;

			final Pointer matIMinVal_ = Pointer.to(data.matIMinVal);
			final Pointer matIMaxVal_ = Pointer.to(data.matIMaxVal);

			clEnqueueWriteBuffer(clCommandQueueMin(), matIMinVal, false, off, len,
				matIMinVal_, 0, null, null);
			clEnqueueWriteBuffer(clCommandQueueMax(), matIMaxVal, false, off, len,
				matIMaxVal_, 0, null, null);

		}

		if (enabledMatIO) {
			// Setup mat IO
			final int len = Sizeof.cl_double * topo.matIOTrgBegHost[stCnt];
			final int off = len * matId;

			final Pointer matIOLowerVal0_ = Pointer.to(data.matIOLowerVal0);
			final Pointer matIOLowerVal1_ = Pointer.to(data.matIOLowerVal1);
			final Pointer matIOUpperVal0_ = Pointer.to(data.matIOUpperVal0);
			final Pointer matIOUpperVal1_ = Pointer.to(data.matIOUpperVal1);

			clEnqueueWriteBuffer(clCommandQueueMin(), matIOLowerVal0, false, off, len,
				matIOLowerVal0_, 0, null, null);
			clEnqueueWriteBuffer(clCommandQueueMax(), matIOLowerVal1, false, off, len,
				matIOLowerVal1_, 0, null, null);
			clEnqueueWriteBuffer(clCommandQueueMin(), matIOUpperVal0, false, off, len,
				matIOUpperVal0_, 0, null, null);
			clEnqueueWriteBuffer(clCommandQueueMax(), matIOUpperVal1, false, off, len,
				matIOUpperVal1_, 0, null, null);
		}

		clFinish(clCommandQueueMin());
		clFinish(clCommandQueueMax());
	}

	@Override
	public void release()
	{
		clReleaseMemObject(minMem);
		clReleaseMemObject(maxMem);
		clReleaseMemObject(resMinMem);
		clReleaseMemObject(resMaxMem);

		if (enabledMatIO) {
			clReleaseMemObject(matIOLowerVal0);
			clReleaseMemObject(matIOLowerVal1);
			clReleaseMemObject(matIOUpperVal0);
			clReleaseMemObject(matIOUpperVal1);
		}
		clReleaseKernel(clKernelMatIO);

		if (topo.enabledMatI) {
			clReleaseMemObject(matIMinVal);
			clReleaseMemObject(matIMaxVal);
		}
		clReleaseKernel(clKernelMatI);

		clReleaseMemObject(matOMinDiagVal);
		clReleaseMemObject(matOMaxDiagVal);
		if (topo.enabledMatNP) {
			clReleaseMemObject(matNPVal);
			clReleaseKernel(clKernelMatNP);
		} else {
			clReleaseKernel(clKernelDiag);
		}

		clReleaseCommandQueue(clCommandQueueMin);
		clReleaseCommandQueue(clCommandQueueMax);
	}

	@Override
	final public void setMult(int matId, final double min[], final double max[])
	{
		final int len = Sizeof.cl_double * stCnt;
		final int off = len * matId;
		clEnqueueWriteBuffer(clCommandQueueMin(), minMem, true, off, len, Pointer.to(min), 0, null, null);
		clEnqueueWriteBuffer(clCommandQueueMax(), maxMem, true, off, len, Pointer.to(max), 0, null, null);
	}

	@Override
	final public void getMult(int matId, final double resMin[], final double resMax[])
	{
		final int len = Sizeof.cl_double * stCnt;
		final int off = len * matId;
		if (enabledMatI || enabledMatIO) {
			clEnqueueReadBuffer(clCommandQueueMin(), minMem, true, off, len, Pointer.to(resMin), 0, null, null);
			clEnqueueReadBuffer(clCommandQueueMax(), maxMem, true, off, len, Pointer.to(resMax), 0, null, null);
		}
		else {
			clEnqueueReadBuffer(clCommandQueueMin(), minMem, true, off, len, Pointer.to(resMin), 0, null, null);
			clEnqueueReadBuffer(clCommandQueueMax(), minMem, true, off, len, Pointer.to(resMax), 0, null, null);
		}
	}

	@Override
	final public void mult(int matCnt, int iterationCnt)
	{
		assert (this.matCnt >= matCnt);

		final long[] lws = new long[]{OCLProgram.localWorkSize(64)};
		final long[] gws = new long[]{leastGreaterMultiple(stCnt * matCnt, lws[0])};

		if (enabledMatNP) {
			clSetKernelArg(clKernelMatNP, 0, Sizeof.cl_uint, Pointer.to(new int[]{matCnt}));
		} else {
			clSetKernelArg(clKernelDiag, 0, Sizeof.cl_uint, Pointer.to(new int[]{matCnt}));
		}
		clSetKernelArg(clKernelMatIO, 0, Sizeof.cl_uint, Pointer.to(new int[]{matCnt}));
		clSetKernelArg(clKernelMatI, 0, Sizeof.cl_uint, Pointer.to(new int[]{matCnt}));
		clSetKernelArg(clKernelSum, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt * matCnt}));
		for (int i = 0; i < iterationCnt; ++i) {
			if (enabledMatNP) {
				if (enabledMatI || enabledMatIO) {
					clSetKernelArg(clKernelMatNP, 7, Sizeof.cl_mem, Pointer.to(minMem));
					clSetKernelArg(clKernelMatNP, 8, Sizeof.cl_mem, Pointer.to(maxMem));
					clSetKernelArg(clKernelMatNP, 9, Sizeof.cl_mem, Pointer.to(resMinMem));
					clSetKernelArg(clKernelMatNP, 10, Sizeof.cl_mem, Pointer.to(resMaxMem));
				} else {
					clSetKernelArg(clKernelMatNP, 6, Sizeof.cl_mem, Pointer.to(minMem));
					clSetKernelArg(clKernelMatNP, 7, Sizeof.cl_mem, Pointer.to(resMinMem));
				}
				clEnqueueNDRangeKernel(clCommandQueueMin(), clKernelMatNP, 1, null, gws, lws, 0, null, null);
			} else {
				if (enabledMatI || enabledMatIO) {
					clSetKernelArg(clKernelDiag, 4, Sizeof.cl_mem, Pointer.to(minMem));
					clSetKernelArg(clKernelDiag, 5, Sizeof.cl_mem, Pointer.to(maxMem));
					clSetKernelArg(clKernelDiag, 6, Sizeof.cl_mem, Pointer.to(resMinMem));
					clSetKernelArg(clKernelDiag, 7, Sizeof.cl_mem, Pointer.to(resMaxMem));
				} else {
					clSetKernelArg(clKernelDiag, 3, Sizeof.cl_mem, Pointer.to(maxMem));
					clSetKernelArg(clKernelDiag, 4, Sizeof.cl_mem, Pointer.to(resMaxMem));
				}
				clEnqueueNDRangeKernel(clCommandQueueMax(), clKernelDiag, 1, null, gws, lws, 0, null, null);
			}

			if (enabledMatIO) {
				clSetKernelArg(clKernelMatIO, 8, Sizeof.cl_mem, Pointer.to(minMem));
				clSetKernelArg(clKernelMatIO, 9, Sizeof.cl_mem, Pointer.to(maxMem));
				clSetKernelArg(clKernelMatIO, 10, Sizeof.cl_mem, Pointer.to(resMinMem));
				clSetKernelArg(clKernelMatIO, 11, Sizeof.cl_mem, Pointer.to(resMaxMem));
				clEnqueueNDRangeKernel(clCommandQueueMin(), clKernelMatIO, 1, null, gws, lws, 0, null, null);
			}


			if (enabledMatI) {
				clSetKernelArg(clKernelMatI, 6, Sizeof.cl_mem, Pointer.to(minMem));
				clSetKernelArg(clKernelMatI, 7, Sizeof.cl_mem, Pointer.to(maxMem));
				clSetKernelArg(clKernelMatI, 8, Sizeof.cl_mem, Pointer.to(resMinMem));
				clSetKernelArg(clKernelMatI, 9, Sizeof.cl_mem, Pointer.to(resMaxMem));
				clEnqueueNDRangeKernel(clCommandQueueMin(), clKernelMatI, 1, null, gws, lws, 0, null, null);
			}

			++totalIterationCnt;

			if (getSumWeight() != 0) {
				if (enabledMatI || enabledMatIO) {
					clSetKernelArg(clKernelSum, 1, Sizeof.cl_double, Pointer.to(new double[]{getSumWeight()}));
					clSetKernelArg(clKernelSum, 2, Sizeof.cl_mem, Pointer.to(resMinMem));
					clSetKernelArg(clKernelSum, 3, Sizeof.cl_mem, Pointer.to(resMaxMem));
				} else {
					clSetKernelArg(clKernelSum, 1, Sizeof.cl_double, Pointer.to(new double[]{getSumWeight()}));
					clSetKernelArg(clKernelSum, 2, Sizeof.cl_mem, Pointer.to(resMinMem));
				}
				clEnqueueNDRangeKernel(clCommandQueueMin(), clKernelSum, 1, null, gws, lws, 0, null, null);
			}

			// Swap
			final cl_mem tmpMin = minMem;
			final cl_mem tmpMax = maxMem;
			minMem = resMinMem;
			maxMem = resMaxMem;
			resMinMem = tmpMin;
			resMaxMem = tmpMax;
		}
		clFinish(clCommandQueueMin());
		clFinish(clCommandQueueMax());
	}

	@Override
	final public void getSum(int matId, final double[] sumMin, final double[] sumMax)
	{
		final int len = Sizeof.cl_double * stCnt;
		final int off = len * matId;
		if (enabledMatI || enabledMatIO) {
			clEnqueueReadBuffer(clCommandQueueMin(), this.sumMin, true, off, len, Pointer.to(sumMin), 0, null, null);
			clEnqueueReadBuffer(clCommandQueueMax(), this.sumMax, true, off, len, Pointer.to(sumMax), 0, null, null);
		}
		else {
			clEnqueueReadBuffer(clCommandQueueMin(), this.sumMin, true, off, len, Pointer.to(sumMin), 0, null, null);
			clEnqueueReadBuffer(clCommandQueueMax(), this.sumMin, true, off, len, Pointer.to(sumMax), 0, null, null);
		}
		clFinish(clCommandQueueMin());
		clFinish(clCommandQueueMax());
	}

	@Override
	final public void setSum(int matId, final double[] sumMin, final double[] sumMax)
	{
		final int len = Sizeof.cl_double * stCnt;
		final int off = len * matId;
		clEnqueueWriteBuffer(clCommandQueueMin(), this.sumMin, false, off, len, Pointer.to(sumMin), 0, null, null);
		clEnqueueWriteBuffer(clCommandQueueMax(), this.sumMax, false, off, len, Pointer.to(sumMax), 0, null, null);
		clFinish(clCommandQueueMin());
		clFinish(clCommandQueueMax());
	}

	private static long leastGreaterMultiple(long x, long z)
	{
		return x + (z - x % z) % z;
	}

	final private double getSumWeight()
	{
		if (totalIterationCnt >= weightOff)
		{
			return weight[totalIterationCnt - weightOff];
		}
		return weightDef;
	}

	final private cl_command_queue clCommandQueueMin() { return clCommandQueueMin; }
	final private cl_command_queue clCommandQueueMax() { return clCommandQueueMax; }
	final private cl_context clContext() { return opts.clContext; }

	final private int matCnt;
	final private int stCnt;
	final private PSEVMMultTopology_OCL topo;
	final private PSEVMMultSettings_OCL opts;

	final private boolean enabledMatNP;
	final private boolean enabledMatIO;
	final private boolean enabledMatI;

	final private cl_program clProgram;
	final private cl_command_queue clCommandQueueMin;
	final private cl_command_queue clCommandQueueMax;

	private cl_kernel clKernelMatIO;
	private cl_kernel clKernelMatI;
	private cl_kernel clKernelMatNP;
	private cl_kernel clKernelDiag;
	final private cl_kernel clKernelSum;

	private int totalIterationCnt;
	final private cl_mem sumMin;
	final private cl_mem sumMax;
	final private double[] weight;
	final private double   weightDef;
	final private int      weightOff;

	private cl_mem matIOLowerVal0;
	private cl_mem matIOLowerVal1;
	private cl_mem matIOUpperVal0;
	private cl_mem matIOUpperVal1;

	private cl_mem matIMaxVal;
	private cl_mem matIMinVal;

	private cl_mem matOMinDiagVal;
	private cl_mem matOMaxDiagVal;
	private cl_mem matNPVal;
	private boolean matNPValFilled;

	private cl_mem minMem;
	private cl_mem maxMem;
	private cl_mem resMinMem;
	private cl_mem resMaxMem;
}