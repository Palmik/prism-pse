package pse;

import org.jocl.*;

import static org.jocl.CL.*;

public class PSEVMMult_OCL implements PSEMult, Releaseable
{
	public PSEVMMult_OCL(PSEVMMultSettings_OCL opts, PSEVMMultTopology_OCL topo, PSEVMCreateData_CSR data)
	{
		this.stCnt = topo.stCnt;
		this.opts = opts;
		this.topo = topo;

		this.clCommandQueue = clCreateCommandQueue(clContext(), opts.clDeviceIdMin, 0, null);

		setExceptionsEnabled(true);

		this.enabledMatNP = topo.enabledMatNP;
		this.enabledMatIO = topo.enabledMatIO;
		this.enabledMatI = topo.enabledMatI;
		this.enabledMatO = topo.enabledMatO;
		this.enabledMatP = enabledMatI || enabledMatO || enabledMatIO;

		this.totalIterationCnt = 0;

		clProgram = OCLProgram.createProgram(OCLProgram.SOURCE, clContext());

		{
			final int len = (Sizeof.cl_double * stCnt) * (enabledMatP ? 2 : 1);
			this.matODiagVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY, len, null, null);
		}
		matNPInitialized = false;
		if (enabledMatNP) {
			final int len = Sizeof.cl_double * topo.matNPTrgBegHost[stCnt];
			this.matNPVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY, len, null, null);
			if (enabledMatP) {
				clKernelMatNP = OCLProgram.createKernel("PSE_VM_NP_CSR_BOTH", clProgram);
			} else {
				clKernelMatNP = OCLProgram.createKernel("PSE_VM_NP_CSR", clProgram);
			}
			clSetKernelArg(clKernelMatNP, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
			clSetKernelArg(clKernelMatNP, 1, Sizeof.cl_mem, Pointer.to(this.matODiagVal));
			clSetKernelArg(clKernelMatNP, 2, Sizeof.cl_mem, Pointer.to(this.matNPVal));
			clSetKernelArg(clKernelMatNP, 3, Sizeof.cl_mem, Pointer.to(topo.matNPSrc));
			clSetKernelArg(clKernelMatNP, 4, Sizeof.cl_mem, Pointer.to(topo.matNPTrgBeg));
		} else {
			if (enabledMatP) {
				clKernelDiag = OCLProgram.createKernel("PSE_VM_DIAG_BOTH", clProgram);
			} else {
				clKernelDiag = OCLProgram.createKernel("PSE_VM_DIAG", clProgram);
			}
			clSetKernelArg(clKernelDiag, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
			clSetKernelArg(clKernelDiag, 1, Sizeof.cl_mem, Pointer.to(this.matODiagVal));
		}

		if (enabledMatIO) {
			clKernelMatIO = OCLProgram.createKernel("PSE_VM_IO_CSR_BOTH", clProgram);

			final int len = Sizeof.cl_double * topo.matIOTrgBegHost[stCnt] * 4;
			this.matIOValZip = clCreateBuffer(clContext(), CL_MEM_READ_ONLY, len, null, null);

			clSetKernelArg(clKernelMatIO, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
			clSetKernelArg(clKernelMatIO, 1, Sizeof.cl_mem, Pointer.to(this.matIOValZip));
			clSetKernelArg(clKernelMatIO, 2, Sizeof.cl_mem, Pointer.to(topo.matIOSrc));
			clSetKernelArg(clKernelMatIO, 3, Sizeof.cl_mem, Pointer.to(topo.matIOTrgBeg));
		}

		if (enabledMatI) {
			clKernelMatI = OCLProgram.createKernel("PSE_VM_I_CSR_BOTH", clProgram);

			final int len = Sizeof.cl_double * topo.matITrgBegHost[stCnt] * 2;
			this.matIValZip = clCreateBuffer(clContext(), CL_MEM_READ_ONLY, len, null, null);

			clSetKernelArg(clKernelMatI, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
			clSetKernelArg(clKernelMatI, 1, Sizeof.cl_mem, Pointer.to(this.matIValZip));
			clSetKernelArg(clKernelMatI, 2, Sizeof.cl_mem, Pointer.to(topo.matISrc));
			clSetKernelArg(clKernelMatI, 3, Sizeof.cl_mem, Pointer.to(topo.matITrgBeg));
		}

		{
			final int len = Sizeof.cl_double * stCnt * (enabledMatP ? 2 : 1);
			this.sum = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, len, null, null);
			clKernelSum = OCLProgram.createKernel("WeightedSumTo", clProgram);
			clSetKernelArg(clKernelSum, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt * (enabledMatP ? 2 : 1)}));
			clSetKernelArg(clKernelSum, 3, Sizeof.cl_mem, Pointer.to(this.sum));
		}

		{
			final int len = (Sizeof.cl_double * stCnt) * (enabledMatP ? 2 : 1);
			sol = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, len, null, null);
			solRes = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, len, null, null);
		}

		if (enabledMatP) {
			doubleBuff = new double[stCnt * 2];
		} else {
			doubleBuff = null;
		}
	}

	@Override
	final public void setWeight(double[] weight, double weightDef, int weightOff)
	{
		this.weight = weight;
		this.weightDef = weightDef;
		this.weightOff = weightOff;
	}

	public void update(PSEVMCreateData_CSR data)
	{
		totalIterationCnt = 0;

		{
			final int len = (Sizeof.cl_double * stCnt) * (enabledMatP ? 2 : 1);
			final Pointer matODiagVal_ = Pointer.to(data.matODiagVal);
			clEnqueueWriteBuffer(clCommandQueue(), matODiagVal, false, 0, len, matODiagVal_, 0, null, null);
		}
		if (enabledMatNP && !matNPInitialized) {
			final int len = Sizeof.cl_double * topo.matNPTrgBegHost[stCnt];
			final Pointer matNPVal_ = Pointer.to(data.matNPVal);
			clEnqueueWriteBuffer(clCommandQueue(), matNPVal, false, 0, len, matNPVal_, 0, null, null);
			matNPInitialized = true;
		}

		if (enabledMatIO) {
			final int len = Sizeof.cl_double * topo.matIOTrgBegHost[stCnt] * 4;
			final Pointer matIOValZip_ = Pointer.to(data.matIOValZip);
			clEnqueueWriteBuffer(clCommandQueue(), matIOValZip, false, 0, len, matIOValZip_, 0, null, null);
		}

		if (enabledMatI) {
			final int len = Sizeof.cl_double * topo.matITrgBegHost[stCnt] * 2;
			final Pointer matIValZip_ = Pointer.to(data.matIValZip);
			clEnqueueWriteBuffer(clCommandQueue(), matIValZip, false, 0, len, matIValZip_, 0, null, null);
		}

		{
			final int len = (Sizeof.cl_double * stCnt) * (enabledMatP ? 2 : 1);
			final double[] zeroes = new double[len];
			final Pointer zeroes_ = Pointer.to(zeroes);
			clEnqueueWriteBuffer(clCommandQueue(), sum, false, 0, len, zeroes_, 0, null, null);
		}

		clFinish(clCommandQueue());
	}

	@Override
	public void release()
	{
		clReleaseCommandQueue(clCommandQueue);

		// MAT NP
		clReleaseMemObject(matODiagVal);
		if (enabledMatNP) {
			clReleaseKernel(clKernelMatNP);
			clReleaseMemObject(matNPVal);
		} else {
			clReleaseKernel(clKernelDiag);
		}

		// MAT IO
		if (enabledMatIO) {
			clReleaseKernel(clKernelMatIO);
			clReleaseMemObject(matIOValZip);
		}

		// MAT I
		if (enabledMatI) {
			clReleaseKernel(clKernelMatI);
			clReleaseMemObject(matIValZip);
		}

		clReleaseMemObject(sum);
		clReleaseMemObject(sol);
		clReleaseMemObject(solRes);
	}

	@Override
	final public void getSum(final double[] sumMin, final double[] sumMax)
	{
		if (enabledMatP) {
			final int len = Sizeof.cl_double * stCnt * 2;
			clEnqueueReadBuffer(clCommandQueue(), this.sum, true, 0, len, Pointer.to(doubleBuff), 0, null, null);
			PSEMultUtility.unzipArray(doubleBuff, sumMin, sumMax);
		}
		else {
			final int len = Sizeof.cl_double * stCnt;
			clEnqueueReadBuffer(clCommandQueue(), this.sum, true, 0, len, Pointer.to(sumMin), 0, null, null);
			clEnqueueReadBuffer(clCommandQueue(), this.sum, true, 0, len, Pointer.to(sumMax), 0, null, null);
		}
		clFinish(clCommandQueue());
	}

	@Override
	final public void setSum(final double[] sumMin, final double[] sumMax)
	{
		if (enabledMatP) {
			final int len = Sizeof.cl_double * stCnt * 2;
			PSEMultUtility.zipArray(sumMin, sumMax, doubleBuff);
			clEnqueueWriteBuffer(clCommandQueue(), this.sum, false, 0, len, Pointer.to(doubleBuff), 0, null, null);
		} else {
			final int len = Sizeof.cl_double * stCnt;
			clEnqueueWriteBuffer(clCommandQueue(), this.sum, false, 0, len, Pointer.to(sumMin), 0, null, null);
		}
	}

	@Override
	final public void setMult(final double solMin[], final double solMax[])
	{
		if (enabledMatP) {
			final int len = Sizeof.cl_double * stCnt * 2;
			PSEMultUtility.zipArray(solMin, solMax, doubleBuff);
			clEnqueueWriteBuffer(clCommandQueue(), this.sol, false, 0, len, Pointer.to(doubleBuff), 0, null, null);
		} else {
			final int len = Sizeof.cl_double * stCnt;
			clEnqueueWriteBuffer(clCommandQueue(), this.sol, false, 0, len, Pointer.to(solMin), 0, null, null);
		}
	}

	@Override
	final public void getMult(final double solMin[], final double solMax[])
	{
		if (enabledMatP) {
			final int len = Sizeof.cl_double * stCnt * 2;
			clEnqueueReadBuffer(clCommandQueue(), this.sol, true, 0, len, Pointer.to(doubleBuff), 0, null, null);
			PSEMultUtility.unzipArray(doubleBuff, solMin, solMax);
		}
		else {
			final int len = Sizeof.cl_double * stCnt;
			clEnqueueReadBuffer(clCommandQueue(), this.sol, true, 0, len, Pointer.to(solMin), 0, null, null);
			clEnqueueReadBuffer(clCommandQueue(), this.sol, true, 0, len, Pointer.to(solMax), 0, null, null);
		}
		clFinish(clCommandQueue());
	}

	@Override
	final public void mult(int iterationCnt)
	{
		final long[] lws = new long[]{OCLProgram.localWorkSize(64)};
		final long[] gws = new long[]{leastGreaterMultiple(stCnt, lws[0])};
		final long[] gwsSum = new long[]{leastGreaterMultiple(stCnt * (enabledMatP ? 2 : 1), lws[0])};

		if (enabledMatI || enabledMatIO) {
			for (int i = 0; i < iterationCnt; ++i) {
				if (enabledMatNP) {
					clSetKernelArg(clKernelMatNP, 5, Sizeof.cl_mem, Pointer.to(sol));
					clSetKernelArg(clKernelMatNP, 6, Sizeof.cl_mem, Pointer.to(solRes));
					clEnqueueNDRangeKernel(clCommandQueue(), clKernelMatNP, 1, null, gws, lws, 0, null, null);
				} else {
					clSetKernelArg(clKernelDiag, 2, Sizeof.cl_mem, Pointer.to(sol));
					clSetKernelArg(clKernelDiag, 3, Sizeof.cl_mem, Pointer.to(solRes));
					clEnqueueNDRangeKernel(clCommandQueue(), clKernelDiag, 1, null, gws, lws, 0, null, null);
				}

				if (enabledMatIO) {
					clSetKernelArg(clKernelMatIO, 4, Sizeof.cl_mem, Pointer.to(sol));
					clSetKernelArg(clKernelMatIO, 5, Sizeof.cl_mem, Pointer.to(solRes));
					clEnqueueNDRangeKernel(clCommandQueue(), clKernelMatIO, 1, null, gws, lws, 0, null, null);
				}

				if (enabledMatI) {
					clSetKernelArg(clKernelMatI, 4, Sizeof.cl_mem, Pointer.to(sol));
					clSetKernelArg(clKernelMatI, 5, Sizeof.cl_mem, Pointer.to(solRes));
					clEnqueueNDRangeKernel(clCommandQueue(), clKernelMatI, 1, null, gws, lws, 0, null, null);
				}

				++totalIterationCnt;

				if (getSumWeight() != 0) {
					clSetKernelArg(clKernelSum, 1, Sizeof.cl_double, Pointer.to(new double[]{getSumWeight()}));
					clSetKernelArg(clKernelSum, 2, Sizeof.cl_mem, Pointer.to(solRes));
					clEnqueueNDRangeKernel(clCommandQueue(), clKernelSum, 1, null, gwsSum, lws, 0, null, null);
				}

				swapSolMem();
			}
		} else {
			for (int i = 0; i < iterationCnt; ++i) {
				if (enabledMatNP) {
					clSetKernelArg(clKernelMatNP, 5, Sizeof.cl_mem, Pointer.to(sol));
					clSetKernelArg(clKernelMatNP, 6, Sizeof.cl_mem, Pointer.to(solRes));
					clEnqueueNDRangeKernel(clCommandQueue(), clKernelMatNP, 1, null, gws, lws, 0, null, null);
				} else {
					clSetKernelArg(clKernelDiag, 2, Sizeof.cl_mem, Pointer.to(sol));
					clSetKernelArg(clKernelDiag, 3, Sizeof.cl_mem, Pointer.to(solRes));
					clEnqueueNDRangeKernel(clCommandQueue(), clKernelDiag, 1, null, gws, lws, 0, null, null);
				}

				++totalIterationCnt;

				if (getSumWeight() != 0) {
					clSetKernelArg(clKernelSum, 1, Sizeof.cl_double, Pointer.to(new double[]{getSumWeight()}));
					clSetKernelArg(clKernelSum, 2, Sizeof.cl_mem, Pointer.to(solRes));
					clEnqueueNDRangeKernel(clCommandQueue(), clKernelSum, 1, null, gwsSum, lws, 0, null, null);
				}

				swapSolMem();
			}
		}
	}

	final private void swapSolMem()
	{
		final cl_mem tmp = sol;
		sol = solRes;
		solRes = tmp;
	}

	final private double getSumWeight()
	{
		if (totalIterationCnt >= weightOff) {
			return weight[totalIterationCnt - weightOff];
		}
		return weightDef;
	}

	private static long leastGreaterMultiple(long x, long z)
	{
		return x + (z - x % z) % z;
	}

	final private cl_command_queue clCommandQueue() { return clCommandQueue; }
	final private cl_context clContext() { return opts.clContext; }

	final private int stCnt;
	final private PSEVMMultTopology_OCL topo;
	final private PSEVMMultSettings_OCL opts;

	final private boolean enabledMatNP;
	final private boolean enabledMatIO;
	final private boolean enabledMatI;
	final private boolean enabledMatO;
	final private boolean enabledMatP;

	final private cl_program clProgram;
	final private cl_command_queue clCommandQueue;

	private cl_kernel clKernelMatIO;
	private cl_kernel clKernelMatI;
	private cl_kernel clKernelMatNP;
	private cl_kernel clKernelDiag;
	final private cl_kernel clKernelSum;

	private int totalIterationCnt;
	private double[] weight;
	private double weightDef;
	private int weightOff;

	private cl_mem matIOValZip;
	private cl_mem matIValZip;
	private cl_mem matODiagVal;
	private cl_mem matNPVal;
	private boolean matNPInitialized;

	private cl_mem sum;
	private cl_mem sol;
	private cl_mem solRes;

	final private double[] doubleBuff;
}