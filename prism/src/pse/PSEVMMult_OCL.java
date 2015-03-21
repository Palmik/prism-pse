package pse;

import org.jocl.*;

import static org.jocl.CL.*;

public class PSEVMMult_OCL implements PSEMult, Releaseable
{
	public PSEVMMult_OCL(PSEVMMultSettings_OCL opts, PSEVMMultTopology_OCL topo, PSEVMCreateData_CSR data, final
	double[] weight, final double weightDef, final int weightOff)
	{
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
				clKernelMatNP = OCLProgram.createKernel("PSE_VM_NP_BOTH_", clProgram);
			} else {
				clKernelMatNP = OCLProgram.createKernel("PSE_VM_NP_", clProgram);
			}
		} else {
			if (enabledMatI || enabledMatIO) {
				clKernelDiag = OCLProgram.createKernel("PSE_VM_DIAG_BOTH_", clProgram);
			} else {
				clKernelDiag = OCLProgram.createKernel("PSE_VM_DIAG_", clProgram);
			}
		}

		clKernelMatIO = OCLProgram.createKernel("PSE_VM_IO_", clProgram);
		clKernelMatI = OCLProgram.createKernel("PSE_VM_I_", clProgram);

		{
			final double[] zeroes = new double[stCnt];
			final Pointer zeroes_ = Pointer.to(zeroes);
			this.sumMin = clCreateBuffer(clContext(), CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * stCnt, zeroes_, null);
			this.sumMax = clCreateBuffer(clContext(), CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * stCnt, zeroes_, null);
		}

		if (enabledMatI || enabledMatIO) {
			clKernelSum = OCLProgram.createKernel("WeightedSumToBoth", clProgram);
			clSetKernelArg(clKernelSum, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
			clSetKernelArg(clKernelSum, 4, Sizeof.cl_mem, Pointer.to(this.sumMin));
			clSetKernelArg(clKernelSum, 5, Sizeof.cl_mem, Pointer.to(this.sumMax));

		} else {
			clKernelSum = OCLProgram.createKernel("WeightedSumTo", clProgram);
			clSetKernelArg(clKernelSum, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
			clSetKernelArg(clKernelSum, 3, Sizeof.cl_mem, Pointer.to(this.sumMin));
		}

		{
			final Pointer matMinDiagVal_ = Pointer.to(data.matOMinDiagVal);
			final Pointer matMaxDiagVal_ = Pointer.to(data.matOMaxDiagVal);

			this.matOMinDiagVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * stCnt, matMinDiagVal_, null);
			this.matOMaxDiagVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * stCnt, matMaxDiagVal_, null);

			if (enabledMatNP) {
				// Setup mat
				final Pointer matVal_ = Pointer.to(data.matNPVal);

				this.matNPVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_double * topo.matNPTrgBegHost[stCnt], matVal_, null);

				if (enabledMatI || enabledMatIO) {
					clSetKernelArg(clKernelMatNP, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
					clSetKernelArg(clKernelMatNP, 1, Sizeof.cl_mem, Pointer.to(this.matOMinDiagVal));
					clSetKernelArg(clKernelMatNP, 2, Sizeof.cl_mem, Pointer.to(this.matOMaxDiagVal));
					clSetKernelArg(clKernelMatNP, 3, Sizeof.cl_mem, Pointer.to(this.matNPVal));
					clSetKernelArg(clKernelMatNP, 4, Sizeof.cl_mem, Pointer.to(topo.matNPSrc));
					clSetKernelArg(clKernelMatNP, 5, Sizeof.cl_mem, Pointer.to(topo.matNPTrgBeg));
				} else {
					clSetKernelArg(clKernelMatNP, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
					clSetKernelArg(clKernelMatNP, 1, Sizeof.cl_mem, Pointer.to(this.matOMinDiagVal));
					clSetKernelArg(clKernelMatNP, 2, Sizeof.cl_mem, Pointer.to(this.matNPVal));
					clSetKernelArg(clKernelMatNP, 3, Sizeof.cl_mem, Pointer.to(topo.matNPSrc));
					clSetKernelArg(clKernelMatNP, 4, Sizeof.cl_mem, Pointer.to(topo.matNPTrgBeg));
				}
			} else {
				if (enabledMatI || enabledMatIO) {
					clSetKernelArg(clKernelDiag, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
					clSetKernelArg(clKernelDiag, 1, Sizeof.cl_mem, Pointer.to(this.matOMinDiagVal));
					clSetKernelArg(clKernelDiag, 2, Sizeof.cl_mem, Pointer.to(this.matOMaxDiagVal));
				} else {
					clSetKernelArg(clKernelDiag, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
					clSetKernelArg(clKernelDiag, 1, Sizeof.cl_mem, Pointer.to(this.matOMinDiagVal));
				}
			}
		}

		if (enabledMatI) {
			// Setup mat min
			final Pointer matIMinVal_ = Pointer.to(data.matIMinVal);
			final Pointer matIMaxVal_ = Pointer.to(data.matIMaxVal);

			this.matIMaxVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * topo.matITrgBegHost[stCnt], matIMaxVal_, null);
			this.matIMinVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * topo.matITrgBegHost[stCnt], matIMinVal_, null);

			clSetKernelArg(clKernelMatI, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
			clSetKernelArg(clKernelMatI, 1, Sizeof.cl_mem, Pointer.to(this.matIMinVal));
			clSetKernelArg(clKernelMatI, 2, Sizeof.cl_mem, Pointer.to(this.matIMaxVal));
			clSetKernelArg(clKernelMatI, 3, Sizeof.cl_mem, Pointer.to(topo.matISrc));
			clSetKernelArg(clKernelMatI, 4, Sizeof.cl_mem, Pointer.to(topo.matITrgBeg));
		}

		if (enabledMatIO) {
			// Setup mat IO
			final Pointer matIOLowerVal0_ = Pointer.to(data.matIOLowerVal0);
			final Pointer matIOLowerVal1_ = Pointer.to(data.matIOLowerVal1);
			final Pointer matIOUpperVal0_ = Pointer.to(data.matIOUpperVal0);
			final Pointer matIOUpperVal1_ = Pointer.to(data.matIOUpperVal1);

			this.matIOLowerVal0 = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * topo.matIOTrgBegHost[stCnt], matIOLowerVal0_, null);
			this.matIOLowerVal1 = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * topo.matIOTrgBegHost[stCnt], matIOLowerVal1_, null);
			this.matIOUpperVal0 = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * topo.matIOTrgBegHost[stCnt], matIOUpperVal0_, null);
			this.matIOUpperVal1 = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * topo.matIOTrgBegHost[stCnt], matIOUpperVal1_, null);

			clSetKernelArg(clKernelMatIO, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
			clSetKernelArg(clKernelMatIO, 1, Sizeof.cl_mem, Pointer.to(this.matIOLowerVal0));
			clSetKernelArg(clKernelMatIO, 2, Sizeof.cl_mem, Pointer.to(this.matIOLowerVal1));
			clSetKernelArg(clKernelMatIO, 3, Sizeof.cl_mem, Pointer.to(this.matIOUpperVal0));
			clSetKernelArg(clKernelMatIO, 4, Sizeof.cl_mem, Pointer.to(this.matIOUpperVal1));
			clSetKernelArg(clKernelMatIO, 5, Sizeof.cl_mem, Pointer.to(topo.matIOSrc));
			clSetKernelArg(clKernelMatIO, 6, Sizeof.cl_mem, Pointer.to(topo.matIOTrgBeg));
		}

		minMem = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, Sizeof.cl_double * stCnt, null, null);
		resMinMem = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, Sizeof.cl_double * stCnt, null, null);
		maxMem = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, Sizeof.cl_double * stCnt, null, null);
		resMaxMem = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, Sizeof.cl_double * stCnt, null, null);
	}

	public void update(PSEVMCreateData_CSR data)
	{
		totalIterationCnt = 0;

		{
			final double[] zeroes = new double[stCnt];
			final Pointer zeroes_ = Pointer.to(zeroes);
			clEnqueueWriteBuffer(clCommandQueueMin(), sumMin, false, 0, Sizeof.cl_double * stCnt, zeroes_
				, 0, null, null);
			clEnqueueWriteBuffer(clCommandQueueMax(), sumMax, false, 0, Sizeof.cl_double * stCnt, zeroes_
				, 0, null, null);
		}

		{
			final Pointer matOMinDiagVal_ = Pointer.to(data.matOMinDiagVal);
			final Pointer matOMaxDiagVal_ = Pointer.to(data.matOMaxDiagVal);
			clEnqueueWriteBuffer(clCommandQueueMin(), matOMinDiagVal, false, 0, Sizeof.cl_double * stCnt, matOMinDiagVal_, 0, null, null);
			clEnqueueWriteBuffer(clCommandQueueMax(), matOMaxDiagVal, false, 0, Sizeof.cl_double * stCnt, matOMaxDiagVal_, 0, null, null);
		}

		if (enabledMatNP) {
			final Pointer matNPVal_ = Pointer.to(data.matNPVal);
			clEnqueueWriteBuffer(clCommandQueueMin(), matNPVal, false, 0, Sizeof.cl_double * topo.matNPTrgBegHost[stCnt], matNPVal_,
					0, null, null);
		}

		if (enabledMatI) {
			// Setup mat min
			final Pointer matIMinVal_ = Pointer.to(data.matIMinVal);
			final Pointer matIMaxVal_ = Pointer.to(data.matIMaxVal);

			clEnqueueWriteBuffer(clCommandQueueMin(), matIMinVal, false, 0, Sizeof.cl_double * topo.matITrgBegHost[stCnt], matIMinVal_,
				0, null, null);
			clEnqueueWriteBuffer(clCommandQueueMax(), matIMaxVal, false, 0, Sizeof.cl_double * topo.matITrgBegHost[stCnt], matIMaxVal_,
				0, null, null);

		}

		if (enabledMatIO) {
			// Setup mat IO
			final Pointer matIOLowerVal0_ = Pointer.to(data.matIOLowerVal0);
			final Pointer matIOLowerVal1_ = Pointer.to(data.matIOLowerVal1);
			final Pointer matIOUpperVal0_ = Pointer.to(data.matIOUpperVal0);
			final Pointer matIOUpperVal1_ = Pointer.to(data.matIOUpperVal1);

			clEnqueueWriteBuffer(clCommandQueueMin(), matIOLowerVal0, false, 0, Sizeof.cl_double * topo.matIOTrgBegHost[stCnt], matIOLowerVal0_,
				0, null, null);
			clEnqueueWriteBuffer(clCommandQueueMax(), matIOLowerVal1, false, 0, Sizeof.cl_double * topo.matIOTrgBegHost[stCnt], matIOLowerVal1_,
				0, null, null);
			clEnqueueWriteBuffer(clCommandQueueMin(), matIOUpperVal0, false, 0, Sizeof.cl_double * topo.matIOTrgBegHost[stCnt], matIOUpperVal0_,
				0, null, null);
			clEnqueueWriteBuffer(clCommandQueueMax(), matIOUpperVal1, false, 0, Sizeof.cl_double * topo.matIOTrgBegHost[stCnt], matIOUpperVal1_,
				0, null, null);
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
	final public void setMult(final double min[], final double max[])
	{
		clEnqueueWriteBuffer(clCommandQueueMin(), minMem, true, 0, Sizeof.cl_double * stCnt, Pointer.to(min)
				, 0, null, null);
		clEnqueueWriteBuffer(clCommandQueueMax(), maxMem, true, 0, Sizeof.cl_double * stCnt, Pointer.to(max)
				, 0, null, null);
	}

	@Override
	final public void getMult(final double resMin[], final double resMax[])
	{
		if (enabledMatI || enabledMatIO) {
			clEnqueueReadBuffer(clCommandQueueMin(), minMem, true, 0, Sizeof.cl_double * stCnt, Pointer.to(resMin), 0, null, null);
			clEnqueueReadBuffer(clCommandQueueMax(), maxMem, true, 0, Sizeof.cl_double * stCnt, Pointer.to(resMax), 0, null, null);
		}
		else {
			clEnqueueReadBuffer(clCommandQueueMin(), minMem, true, 0, Sizeof.cl_double * stCnt, Pointer.to(resMin), 0, null, null);
			clEnqueueReadBuffer(clCommandQueueMax(), minMem, true, 0, Sizeof.cl_double * stCnt, Pointer.to(resMax), 0, null, null);
		}
	}

	@Override
	final public void mult(int iterationCnt)
	{
		final long[] lws = new long[]{OCLProgram.localWorkSize(64)};
		final long[] gws = new long[]{leastGreaterMultiple(stCnt, lws[0])};

		for (int i = 0; i < iterationCnt; ++i) {
			if (enabledMatNP) {
				if (enabledMatI || enabledMatIO) {
					clSetKernelArg(clKernelMatNP, 6, Sizeof.cl_mem, Pointer.to(minMem));
					clSetKernelArg(clKernelMatNP, 7, Sizeof.cl_mem, Pointer.to(maxMem));
					clSetKernelArg(clKernelMatNP, 8, Sizeof.cl_mem, Pointer.to(resMinMem));
					clSetKernelArg(clKernelMatNP, 9, Sizeof.cl_mem, Pointer.to(resMaxMem));
				} else {
					clSetKernelArg(clKernelMatNP, 5, Sizeof.cl_mem, Pointer.to(minMem));
					clSetKernelArg(clKernelMatNP, 6, Sizeof.cl_mem, Pointer.to(resMinMem));
				}
				clEnqueueNDRangeKernel(clCommandQueueMin(), clKernelMatNP, 1, null, gws, lws, 0, null, null);
			} else {
				if (enabledMatI || enabledMatIO) {
					clSetKernelArg(clKernelDiag, 3, Sizeof.cl_mem, Pointer.to(minMem));
					clSetKernelArg(clKernelDiag, 4, Sizeof.cl_mem, Pointer.to(maxMem));
					clSetKernelArg(clKernelDiag, 5, Sizeof.cl_mem, Pointer.to(resMinMem));
					clSetKernelArg(clKernelDiag, 6, Sizeof.cl_mem, Pointer.to(resMaxMem));
				} else {
					clSetKernelArg(clKernelDiag, 2, Sizeof.cl_mem, Pointer.to(maxMem));
					clSetKernelArg(clKernelDiag, 3, Sizeof.cl_mem, Pointer.to(resMaxMem));
				}
				clEnqueueNDRangeKernel(clCommandQueueMax(), clKernelDiag, 1, null, gws, lws, 0, null, null);
			}

			if (enabledMatIO) {
				clSetKernelArg(clKernelMatIO, 7, Sizeof.cl_mem, Pointer.to(minMem));
				clSetKernelArg(clKernelMatIO, 8, Sizeof.cl_mem, Pointer.to(maxMem));
				clSetKernelArg(clKernelMatIO, 9, Sizeof.cl_mem, Pointer.to(resMinMem));
				clSetKernelArg(clKernelMatIO, 10, Sizeof.cl_mem, Pointer.to(resMaxMem));
				clEnqueueNDRangeKernel(clCommandQueueMin(), clKernelMatIO, 1, null, gws, lws, 0, null, null);
			}

			if (enabledMatI) {
				clSetKernelArg(clKernelMatI, 5, Sizeof.cl_mem, Pointer.to(minMem));
				clSetKernelArg(clKernelMatI, 6, Sizeof.cl_mem, Pointer.to(maxMem));
				clSetKernelArg(clKernelMatI, 7, Sizeof.cl_mem, Pointer.to(resMinMem));
				clSetKernelArg(clKernelMatI, 8, Sizeof.cl_mem, Pointer.to(resMaxMem));
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
	final public void mult(double min[], double resMin[], double max[], double resMax[], int iterationCnt)
	{
		setMult(min, max); mult(iterationCnt); getMult(resMin, resMax);
	}

	@Override
	final public void getSum(final double[] sumMin, final double[] sumMax)
	{
		if (enabledMatI || enabledMatIO) {
			clEnqueueReadBuffer(clCommandQueueMin(), this.sumMin, true, 0, Sizeof.cl_double * stCnt, Pointer.to(sumMin), 0, null, null);
			clEnqueueReadBuffer(clCommandQueueMax(), this.sumMax, true, 0, Sizeof.cl_double * stCnt, Pointer.to(sumMax), 0, null, null);
		}
		else {
			clEnqueueReadBuffer(clCommandQueueMin(), this.sumMin, true, 0, Sizeof.cl_double * stCnt, Pointer.to(sumMin), 0, null, null);
			clEnqueueReadBuffer(clCommandQueueMax(), this.sumMin, true, 0, Sizeof.cl_double * stCnt, Pointer.to(sumMax), 0, null, null);
		}
		clFinish(clCommandQueueMin());
		clFinish(clCommandQueueMax());
	}

	@Override
	final public void setSum(final double[] sumMin, final double[] sumMax)
	{
		clEnqueueWriteBuffer(clCommandQueueMin(), this.sumMin, false, 0, Sizeof.cl_double * stCnt, Pointer.to(sumMin), 0, null, null);
		clEnqueueWriteBuffer(clCommandQueueMax(), this.sumMax, false, 0, Sizeof.cl_double * stCnt, Pointer.to(sumMax), 0, null, null);
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

	private cl_mem minMem;
	private cl_mem maxMem;
	private cl_mem resMinMem;
	private cl_mem resMaxMem;
}