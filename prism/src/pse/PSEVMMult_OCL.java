package pse;

import org.jocl.*;
import static org.jocl.CL.*;

public class PSEVMMult_OCL implements PSEMult, Releaseable
{
	public PSEVMMult_OCL
		( PSEVMMultSettings_OCL opts
        , PSEVMMultTopology_OCL topo
		, PSEVMCreateData_CSR data
				, final double[] weight
				, final double weightDef
				, final int weightOff
		)
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
				clKernelMatNPMin = OCLProgram.createKernel("PSE_VM_NP", clProgram);
				clKernelMatNPMax = OCLProgram.createKernel("PSE_VM_NP", clProgram);
			} else {
				clKernelMatNPMin = OCLProgram.createKernel("PSE_VM_NP", clProgram);
			}
		} else {
			if (enabledMatI || enabledMatIO) {
				clKernelDiagMin = OCLProgram.createKernel("PSE_VM_DIAG", clProgram);
				clKernelDiagMax = OCLProgram.createKernel("PSE_VM_DIAG", clProgram);
			} else {
				clKernelDiagMin = OCLProgram.createKernel("PSE_VM_DIAG", clProgram);
			}
		}

		clKernelMatIOMin = OCLProgram.createKernel("PSE_VM_IO", clProgram);
		clKernelMatIOMax = OCLProgram.createKernel("PSE_VM_IO", clProgram);
		clKernelMatIMin = OCLProgram.createKernel("PSE_VM_I", clProgram);
		clKernelMatIMax = OCLProgram.createKernel("PSE_VM_I", clProgram);
		clKernelSumMin = OCLProgram.createKernel("WeightedSumTo", clProgram);
		clKernelSumMax = OCLProgram.createKernel("WeightedSumTo", clProgram);

		{
			final double[] zeroes = new double[stCnt];
			final Pointer zeroes_ = Pointer.to(zeroes);
			this.sumMin = clCreateBuffer(clContext(), CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * stCnt, zeroes_, null);
			this.sumMax = clCreateBuffer(clContext(), CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * stCnt, zeroes_, null);
		}

		clSetKernelArg(clKernelSumMin, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
		clSetKernelArg(clKernelSumMin, 3, Sizeof.cl_mem, Pointer.to(this.sumMin));

		clSetKernelArg(clKernelSumMax, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
		clSetKernelArg(clKernelSumMax, 3, Sizeof.cl_mem, Pointer.to(this.sumMax));

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

				clSetKernelArg(clKernelMatNPMin, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
				clSetKernelArg(clKernelMatNPMin, 1, Sizeof.cl_mem, Pointer.to(this.matOMinDiagVal));
				clSetKernelArg(clKernelMatNPMin, 2, Sizeof.cl_mem, Pointer.to(this.matNPVal));
				clSetKernelArg(clKernelMatNPMin, 3, Sizeof.cl_mem, Pointer.to(topo.matNPSrc));
				clSetKernelArg(clKernelMatNPMin, 4, Sizeof.cl_mem, Pointer.to(topo.matNPTrgBeg));

				if (enabledMatI || enabledMatIO) {
					clSetKernelArg(clKernelMatNPMax, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
					clSetKernelArg(clKernelMatNPMax, 1, Sizeof.cl_mem, Pointer.to(this.matOMaxDiagVal));
					clSetKernelArg(clKernelMatNPMax, 2, Sizeof.cl_mem, Pointer.to(this.matNPVal));
					clSetKernelArg(clKernelMatNPMax, 3, Sizeof.cl_mem, Pointer.to(topo.matNPSrc));
					clSetKernelArg(clKernelMatNPMax, 4, Sizeof.cl_mem, Pointer.to(topo.matNPTrgBeg));
				}
			} else {
				clSetKernelArg(clKernelDiagMin, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
				clSetKernelArg(clKernelDiagMin, 1, Sizeof.cl_mem, Pointer.to(this.matOMinDiagVal));

				if (enabledMatI || enabledMatIO) {
					clSetKernelArg(clKernelDiagMax, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
					clSetKernelArg(clKernelDiagMax, 1, Sizeof.cl_mem, Pointer.to(this.matOMaxDiagVal));
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

			clSetKernelArg(clKernelMatIMin, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
			clSetKernelArg(clKernelMatIMin, 1, Sizeof.cl_mem, Pointer.to(this.matIMinVal));
			clSetKernelArg(clKernelMatIMin, 2, Sizeof.cl_mem, Pointer.to(topo.matISrc));
			clSetKernelArg(clKernelMatIMin, 3, Sizeof.cl_mem, Pointer.to(topo.matITrgBeg));

			clSetKernelArg(clKernelMatIMax, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
			clSetKernelArg(clKernelMatIMax, 1, Sizeof.cl_mem, Pointer.to(this.matIMaxVal));
			clSetKernelArg(clKernelMatIMax, 2, Sizeof.cl_mem, Pointer.to(topo.matISrc));
			clSetKernelArg(clKernelMatIMax, 3, Sizeof.cl_mem, Pointer.to(topo.matITrgBeg));
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

			clSetKernelArg(clKernelMatIOMin, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
			clSetKernelArg(clKernelMatIOMin, 1, Sizeof.cl_mem, Pointer.to(this.matIOLowerVal0));
			clSetKernelArg(clKernelMatIOMin, 2, Sizeof.cl_mem, Pointer.to(this.matIOLowerVal1));
			clSetKernelArg(clKernelMatIOMin, 3, Sizeof.cl_mem, Pointer.to(this.matIOUpperVal0));
			clSetKernelArg(clKernelMatIOMin, 4, Sizeof.cl_mem, Pointer.to(this.matIOUpperVal1));
			clSetKernelArg(clKernelMatIOMin, 5, Sizeof.cl_mem, Pointer.to(topo.matIOSrc));
			clSetKernelArg(clKernelMatIOMin, 6, Sizeof.cl_mem, Pointer.to(topo.matIOTrgBeg));

			clSetKernelArg(clKernelMatIOMax, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
			clSetKernelArg(clKernelMatIOMax, 1, Sizeof.cl_mem, Pointer.to(this.matIOUpperVal0));
			clSetKernelArg(clKernelMatIOMax, 2, Sizeof.cl_mem, Pointer.to(this.matIOUpperVal1));
			clSetKernelArg(clKernelMatIOMax, 3, Sizeof.cl_mem, Pointer.to(this.matIOLowerVal0));
			clSetKernelArg(clKernelMatIOMax, 4, Sizeof.cl_mem, Pointer.to(this.matIOLowerVal1));
			clSetKernelArg(clKernelMatIOMax, 5, Sizeof.cl_mem, Pointer.to(topo.matIOSrc));
			clSetKernelArg(clKernelMatIOMax, 6, Sizeof.cl_mem, Pointer.to(topo.matIOTrgBeg));
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

		if (enabledMatNP) {
			final Pointer matNPVal_ = Pointer.to(data.matNPVal);
			clEnqueueWriteBuffer(clCommandQueueMin(), matNPVal, false, 0, Sizeof.cl_double * topo.matNPTrgBegHost[stCnt], matNPVal_,
					0, null, null);
		}

		{
			final Pointer matOMinDiagVal_ = Pointer.to(data.matOMinDiagVal);
			final Pointer matOMaxDiagVal_ = Pointer.to(data.matOMaxDiagVal);
			clEnqueueWriteBuffer(clCommandQueueMin(), matOMinDiagVal, false, 0, Sizeof.cl_double * stCnt, matOMinDiagVal_, 0, null, null);
			clEnqueueWriteBuffer(clCommandQueueMax(), matOMaxDiagVal, false, 0, Sizeof.cl_double * stCnt, matOMaxDiagVal_, 0, null, null);
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
		clReleaseMemObject(resMinMem);

		if (enabledMatIO) {
			clReleaseMemObject(matIOLowerVal0);
			clReleaseMemObject(matIOLowerVal1);
			clReleaseMemObject(matIOUpperVal0);
			clReleaseMemObject(matIOUpperVal1);
		}
		if (topo.enabledMatI) {
			clReleaseMemObject(matIMinVal);
			clReleaseMemObject(matIMaxVal);
		}

		if (topo.enabledMatNP) {
			clReleaseMemObject(matNPVal);
			clReleaseMemObject(matOMinDiagVal);
			clReleaseMemObject(matOMaxDiagVal);

			clReleaseKernel(clKernelMatNPMin);
			if (enabledMatI || enabledMatIO) {
				clReleaseKernel(clKernelMatNPMax);
			}
		} else {
			clReleaseKernel(clKernelDiagMin);
			if (enabledMatI || enabledMatIO) {
				clReleaseKernel(clKernelDiagMax);
			}
		}

		clReleaseKernel(clKernelMatIOMin);
		clReleaseKernel(clKernelMatIOMax);
		clReleaseKernel(clKernelMatIMin);
		clReleaseKernel(clKernelMatIMax);
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
		cl_mem minMem = this.minMem;
		cl_mem maxMem = this.maxMem;
		cl_mem resMinMem = this.resMinMem;
		cl_mem resMaxMem = this.resMaxMem;

		final long[] lws = new long[]{OCLProgram.localWorkSize(1024)};
		final long[] gws = new long[]{leastGreaterMultiple(stCnt, lws[0])};

		for (int i = 0; i < iterationCnt; ++i) {
			if (enabledMatNP) {
				clSetKernelArg(clKernelMatNPMin, 5, Sizeof.cl_mem, Pointer.to(minMem));
				clSetKernelArg(clKernelMatNPMin, 6, Sizeof.cl_mem, Pointer.to(resMinMem));
				clEnqueueNDRangeKernel(clCommandQueueMin(), clKernelMatNPMin, 1, null, gws, lws, 0, null, null);
				if (enabledMatI || enabledMatIO) {
					clSetKernelArg(clKernelMatNPMax, 5, Sizeof.cl_mem, Pointer.to(maxMem));
					clSetKernelArg(clKernelMatNPMax, 6, Sizeof.cl_mem, Pointer.to(resMaxMem));
					clEnqueueNDRangeKernel(clCommandQueueMax(), clKernelMatNPMax, 1, null, gws, lws, 0, null, null);
				}
			} else {
				clSetKernelArg(clKernelDiagMin, 2, Sizeof.cl_mem, Pointer.to(minMem));
				clSetKernelArg(clKernelDiagMin, 3, Sizeof.cl_mem, Pointer.to(resMinMem));
				clEnqueueNDRangeKernel(clCommandQueueMin(), clKernelDiagMin, 1, null, gws, lws, 0, null, null);
				if (enabledMatI || enabledMatIO) {
					clSetKernelArg(clKernelDiagMax, 5, Sizeof.cl_mem, Pointer.to(maxMem));
					clSetKernelArg(clKernelDiagMax, 6, Sizeof.cl_mem, Pointer.to(resMaxMem));
					clEnqueueNDRangeKernel(clCommandQueueMax(), clKernelDiagMax, 1, null, gws, lws, 0, null, null);
				}
			}

			if (enabledMatIO) {
				clSetKernelArg(clKernelMatIOMin, 7, Sizeof.cl_mem, Pointer.to(minMem));
				clSetKernelArg(clKernelMatIOMin, 8, Sizeof.cl_mem, Pointer.to(resMinMem));
				clSetKernelArg(clKernelMatIOMax, 7, Sizeof.cl_mem, Pointer.to(maxMem));
				clSetKernelArg(clKernelMatIOMax, 8, Sizeof.cl_mem, Pointer.to(resMaxMem));

				clEnqueueNDRangeKernel(clCommandQueueMin(), clKernelMatIOMin, 1, null, gws, lws, 0, null, null);
				clEnqueueNDRangeKernel(clCommandQueueMax(), clKernelMatIOMax, 1, null, gws, lws, 0, null, null);
			}

			if (enabledMatI) {
				clSetKernelArg(clKernelMatIMin, 4, Sizeof.cl_mem, Pointer.to(minMem));
				clSetKernelArg(clKernelMatIMin, 5, Sizeof.cl_mem, Pointer.to(resMinMem));
				clSetKernelArg(clKernelMatIMax, 4, Sizeof.cl_mem, Pointer.to(maxMem));
				clSetKernelArg(clKernelMatIMax, 5, Sizeof.cl_mem, Pointer.to(resMaxMem));

				clEnqueueNDRangeKernel(clCommandQueueMin(), clKernelMatIMin, 1, null
					, gws, lws, 0, null, null);

				clEnqueueNDRangeKernel(clCommandQueueMax(), clKernelMatIMax, 1, null
					, gws, lws, 0, null, null);
			}

			++totalIterationCnt;

			if (getSumWeight() != 0) {
				clSetKernelArg(clKernelSumMin, 1, Sizeof.cl_double, Pointer.to(new double[]{getSumWeight()}));
				clSetKernelArg(clKernelSumMin, 2, Sizeof.cl_mem, Pointer.to(resMinMem));
				clEnqueueNDRangeKernel(clCommandQueueMin(), clKernelSumMin, 1, null
					, gws, lws
					, 0, null, null);
				if (enabledMatI || enabledMatIO) {
					clSetKernelArg(clKernelSumMax, 1, Sizeof.cl_double, Pointer.to(new double[]{getSumWeight()}));
					clSetKernelArg(clKernelSumMax, 2, Sizeof.cl_mem, Pointer.to(resMaxMem));
					clEnqueueNDRangeKernel(clCommandQueueMax(), clKernelSumMax, 1, null
						, gws, lws
						, 0, null, null);
				}
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

	private cl_kernel clKernelMatIOMin;
	private cl_kernel clKernelMatIOMax;
	private cl_kernel clKernelMatIMin;
	private cl_kernel clKernelMatIMax;
	private cl_kernel clKernelMatNPMin;
	private cl_kernel clKernelMatNPMax;
	private cl_kernel clKernelDiagMin;
	private cl_kernel clKernelDiagMax;
	final private cl_kernel clKernelSumMin;
	final private cl_kernel clKernelSumMax;

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

	final private cl_mem minMem;
	final private cl_mem maxMem;
	final private cl_mem resMinMem;
	final private cl_mem resMaxMem;
}