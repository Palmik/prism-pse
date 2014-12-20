package pse;

import org.jocl.*;

import java.math.BigDecimal;

import static org.jocl.CL.*;

public class PSEVMMult_OCL {
	public PSEVMMult_OCL
		(int stCnt, int trCnt
			, double[] matIOLowerVal0
			, double[] matIOLowerVal1
			, double[] matIOUpperVal0
			, double[] matIOUpperVal1
			, int[] matIOSrc
			, int[] matIOTrgBeg

			, double[] matIMinVal
			, double[] matIMaxVal
			, int[] matISrc
			, int[] matITrgBeg

			, double[] matOMinDiagVal
			, double[] matOMaxDiagVal
			, double[] matNPVal
			, int[] matNPSrc
			, int[] matNPTrgBeg

			, double[] weight
			, double weightDef
			, int weightOff
		) {
		this.diag = matOMinDiagVal;
		this.stCnt = stCnt;
		this.trCnt = trCnt;

		this.oclProgram = new OCLProgram();
		this.clCommandQueueMin = oclProgram.createCommandQueue();
		this.clCommandQueueMax = oclProgram.createCommandQueue();

		setExceptionsEnabled(true);

		this.enabledMatNP = matNPTrgBeg[stCnt] > 0;
		this.enabledMatIO = matIOTrgBeg[stCnt] > 0;
		this.enabledMatI = matITrgBeg[stCnt] > 0;

		if (enabledMatI || enabledMatIO) {
			clKernelMatNPMin = oclProgram.createKernel("PSE_VM_NP");
			clKernelMatNPMax = oclProgram.createKernel("PSE_VM_NP");
		}
		else {
			clKernelMatNPMin = oclProgram.createKernel("PSE_VM_NP");
		}
		clKernelMatIOMin = oclProgram.createKernel("PSE_VM_IO");
		clKernelMatIOMax = oclProgram.createKernel("PSE_VM_IO");
		clKernelMatIMin = oclProgram.createKernel("PSE_VM_I");
		clKernelMatIMax = oclProgram.createKernel("PSE_VM_I");
		clKernelSumMin = oclProgram.createKernel("WeightedSumTo");
		clKernelSumMax = oclProgram.createKernel("WeightedSumTo");

		this.weight = weight;
		this.weightDef = weightDef;
		this.weightOff = weightOff;
		this.totalIterationCnt = 0;

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

		if (enabledMatNP) {
			// Setup mat
			final Pointer matVal_ = Pointer.to(matNPVal);
			final Pointer matSrc_ = Pointer.to(matNPSrc);
			final Pointer matTrgBeg_ = Pointer.to(matNPTrgBeg);
			final Pointer matMinDiagVal_ = Pointer.to(matOMinDiagVal);
			final Pointer matMaxDiagVal_ = Pointer.to(matOMaxDiagVal);

			this.matNPVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * matNPTrgBeg[stCnt], matVal_, null);
			this.matNPSrc = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * matNPTrgBeg[stCnt], matSrc_, null);
			this.matNPTrgBeg = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * (stCnt + 1), matTrgBeg_, null);
			this.matOMinDiagVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * stCnt, matMinDiagVal_, null);
			this.matOMaxDiagVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * stCnt, matMaxDiagVal_, null);

			clSetKernelArg(clKernelMatNPMin, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
			clSetKernelArg(clKernelMatNPMin, 1, Sizeof.cl_mem, Pointer.to(this.matOMinDiagVal));
			clSetKernelArg(clKernelMatNPMin, 2, Sizeof.cl_mem, Pointer.to(this.matNPVal));
			clSetKernelArg(clKernelMatNPMin, 3, Sizeof.cl_mem, Pointer.to(this.matNPSrc));
			clSetKernelArg(clKernelMatNPMin, 4, Sizeof.cl_mem, Pointer.to(this.matNPTrgBeg));

			if (enabledMatI || enabledMatIO) {
				clSetKernelArg(clKernelMatNPMax, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
				clSetKernelArg(clKernelMatNPMax, 1, Sizeof.cl_mem, Pointer.to(this.matOMaxDiagVal));
				clSetKernelArg(clKernelMatNPMax, 2, Sizeof.cl_mem, Pointer.to(this.matNPVal));
				clSetKernelArg(clKernelMatNPMax, 3, Sizeof.cl_mem, Pointer.to(this.matNPSrc));
				clSetKernelArg(clKernelMatNPMax, 4, Sizeof.cl_mem, Pointer.to(this.matNPTrgBeg));
			}
		}

		if (enabledMatI) {
			// Setup mat min
			final Pointer matIMinVal_ = Pointer.to(matIMinVal);
			final Pointer matIMaxVal_ = Pointer.to(matIMaxVal);
			final Pointer matISrc_ = Pointer.to(matISrc);
			final Pointer matITrgBeg_ = Pointer.to(matITrgBeg);

			this.matIMaxVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * matITrgBeg[stCnt], matIMaxVal_, null);
			this.matIMinVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * matITrgBeg[stCnt], matIMinVal_, null);
			this.matISrc = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * matITrgBeg[stCnt], matISrc_, null);
			this.matITrgBeg = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * (stCnt + 1), matITrgBeg_, null);

			clSetKernelArg(clKernelMatIMin, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
			clSetKernelArg(clKernelMatIMin, 1, Sizeof.cl_mem, Pointer.to(this.matIMinVal));
			clSetKernelArg(clKernelMatIMin, 2, Sizeof.cl_mem, Pointer.to(this.matISrc));
			clSetKernelArg(clKernelMatIMin, 3, Sizeof.cl_mem, Pointer.to(this.matITrgBeg));

			clSetKernelArg(clKernelMatIMax, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
			clSetKernelArg(clKernelMatIMax, 1, Sizeof.cl_mem, Pointer.to(this.matIMaxVal));
			clSetKernelArg(clKernelMatIMax, 2, Sizeof.cl_mem, Pointer.to(this.matISrc));
			clSetKernelArg(clKernelMatIMax, 3, Sizeof.cl_mem, Pointer.to(this.matITrgBeg));
		}

		if (enabledMatIO) {
			// Setup mat IO
			final Pointer matIOLowerVal0_ = Pointer.to(matIOLowerVal0);
			final Pointer matIOLowerVal1_ = Pointer.to(matIOLowerVal1);
			final Pointer matIOUpperVal0_ = Pointer.to(matIOUpperVal0);
			final Pointer matIOUpperVal1_ = Pointer.to(matIOUpperVal1);
			final Pointer matIOSrc_ = Pointer.to(matIOSrc);
			final Pointer matIOTrgBeg_ = Pointer.to(matIOTrgBeg);

			this.matIOLowerVal0 = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * matIOTrgBeg[stCnt], matIOLowerVal0_, null);
			this.matIOLowerVal1 = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * matIOTrgBeg[stCnt], matIOLowerVal1_, null);
			this.matIOUpperVal0 = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * matIOTrgBeg[stCnt], matIOUpperVal0_, null);
			this.matIOUpperVal1 = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * matIOTrgBeg[stCnt], matIOUpperVal1_, null);
			this.matIOSrc = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * matIOTrgBeg[stCnt], matIOSrc_, null);
			this.matIOTrgBeg = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * (stCnt + 1), matIOTrgBeg_, null);

			clSetKernelArg(clKernelMatIOMin, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
			clSetKernelArg(clKernelMatIOMin, 1, Sizeof.cl_mem, Pointer.to(this.matIOLowerVal0));
			clSetKernelArg(clKernelMatIOMin, 2, Sizeof.cl_mem, Pointer.to(this.matIOLowerVal1));
			clSetKernelArg(clKernelMatIOMin, 3, Sizeof.cl_mem, Pointer.to(this.matIOUpperVal0));
			clSetKernelArg(clKernelMatIOMin, 4, Sizeof.cl_mem, Pointer.to(this.matIOUpperVal1));
			clSetKernelArg(clKernelMatIOMin, 5, Sizeof.cl_mem, Pointer.to(this.matIOSrc));
			clSetKernelArg(clKernelMatIOMin, 6, Sizeof.cl_mem, Pointer.to(this.matIOTrgBeg));

			clSetKernelArg(clKernelMatIOMax, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
			clSetKernelArg(clKernelMatIOMax, 1, Sizeof.cl_mem, Pointer.to(this.matIOUpperVal0));
			clSetKernelArg(clKernelMatIOMax, 2, Sizeof.cl_mem, Pointer.to(this.matIOUpperVal1));
			clSetKernelArg(clKernelMatIOMax, 3, Sizeof.cl_mem, Pointer.to(this.matIOLowerVal0));
			clSetKernelArg(clKernelMatIOMax, 4, Sizeof.cl_mem, Pointer.to(this.matIOLowerVal1));
			clSetKernelArg(clKernelMatIOMax, 5, Sizeof.cl_mem, Pointer.to(this.matIOSrc));
			clSetKernelArg(clKernelMatIOMax, 6, Sizeof.cl_mem, Pointer.to(this.matIOTrgBeg));
		}

		minMem1 = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, Sizeof.cl_double * stCnt, null, null);
		minMem2 = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, Sizeof.cl_double * stCnt, null, null);
		maxMem1 = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, Sizeof.cl_double * stCnt, null, null);
		maxMem2 = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, Sizeof.cl_double * stCnt, null, null);

	}

	public void vmMult(double min[], double resMin[], double max[], double resMax[], int iterationCnt) {
		cl_mem minMem = minMem1;
		cl_mem maxMem = maxMem1;
		cl_mem resMinMem = minMem2;
		cl_mem resMaxMem = maxMem2;

		clEnqueueWriteBuffer(clCommandQueueMin(), minMem, true, 0, Sizeof.cl_double * stCnt, Pointer.to(min)
			, 0, null, null);
		clEnqueueWriteBuffer(clCommandQueueMax(), maxMem, true, 0, Sizeof.cl_double * stCnt, Pointer.to(max)
			, 0, null, null);

		final long[] lws = new long[]{oclProgram.clLocalWorkSize(1024)};
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
		if (enabledMatI || enabledMatIO) {
			clEnqueueReadBuffer(clCommandQueueMin(), minMem, true, 0, Sizeof.cl_double * stCnt, Pointer.to(resMin), 0, null, null);
			clEnqueueReadBuffer(clCommandQueueMax(), maxMem, true, 0, Sizeof.cl_double * stCnt, Pointer.to(resMax), 0, null, null);
			clFinish(clCommandQueueMin());
			clFinish(clCommandQueueMax());
		}
		else {
			clEnqueueReadBuffer(clCommandQueueMin(), minMem, true, 0, Sizeof.cl_double * stCnt, Pointer.to(resMin), 0, null, null);
			clEnqueueReadBuffer(clCommandQueueMax(), minMem, true, 0, Sizeof.cl_double * stCnt, Pointer.to(resMax), 0, null, null);
			clFinish(clCommandQueueMin());
			clFinish(clCommandQueueMax());
		}
	}

	final public void getSum(final double[] sumMin, final double[] sumMax)
	{
		if (enabledMatI || enabledMatIO) {
			clEnqueueReadBuffer(clCommandQueueMin(), this.sumMin, true, 0, Sizeof.cl_double * stCnt, Pointer.to(sumMin), 0, null, null);
			clEnqueueReadBuffer(clCommandQueueMax(), this.sumMax, true, 0, Sizeof.cl_double * stCnt, Pointer.to(sumMax), 0, null, null);
			clFinish(clCommandQueueMin());
			clFinish(clCommandQueueMax());
		}
		else {
			clEnqueueReadBuffer(clCommandQueueMin(), this.sumMin, true, 0, Sizeof.cl_double * stCnt, Pointer.to(sumMin), 0, null, null);
			clEnqueueReadBuffer(clCommandQueueMax(), this.sumMin, true, 0, Sizeof.cl_double * stCnt, Pointer.to(sumMax), 0, null, null);
			clFinish(clCommandQueueMin());
			clFinish(clCommandQueueMax());
		}
	}

	public void release() {
		clReleaseMemObject(minMem1);
		clReleaseMemObject(minMem2);
		clReleaseMemObject(matIOLowerVal0);
		clReleaseMemObject(matIOLowerVal1);
		clReleaseMemObject(matIOUpperVal0);
		clReleaseMemObject(matIOUpperVal1);
		clReleaseMemObject(matIOSrc);
		clReleaseMemObject(matIOTrgBeg);
		clReleaseMemObject(matIMinVal);
		clReleaseMemObject(matISrc);
		clReleaseMemObject(matITrgBeg);
		clReleaseMemObject(matIMaxVal);
		clReleaseMemObject(matOMinDiagVal);
		clReleaseMemObject(matOMaxDiagVal);
		clReleaseMemObject(matNPVal);
		clReleaseMemObject(matNPSrc);
		clReleaseMemObject(matNPTrgBeg);
		clReleaseKernel(clKernelMatNPMin);
		clReleaseKernel(clKernelMatNPMax);
		clReleaseKernel(clKernelMatIOMin);
		clReleaseKernel(clKernelMatIOMax);
		clReleaseKernel(clKernelMatIMin);
		clReleaseKernel(clKernelMatIMax);
		clReleaseCommandQueue(clCommandQueueMin);
		oclProgram.release();
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
	final private cl_context clContext() { return oclProgram.getContext(); }

	final private int stCnt;
	final private int trCnt;

	final private boolean enabledMatNP;
	final private boolean enabledMatIO;
	final private boolean enabledMatI;

	final private OCLProgram oclProgram;
	final private cl_command_queue clCommandQueueMin;
	final private cl_command_queue clCommandQueueMax;

	private cl_kernel clKernelMatIOMin;
	private cl_kernel clKernelMatIOMax;
	private cl_kernel clKernelMatIMin;
	private cl_kernel clKernelMatIMax;
	private cl_kernel clKernelMatNPMin;
	private cl_kernel clKernelMatNPMax;
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
	private cl_mem matIOSrc;
	private cl_mem matIOTrgBeg;

	private cl_mem matIMaxVal;
	private cl_mem matIMinVal;
	private cl_mem matISrc;
	private cl_mem matITrgBeg;

	private cl_mem matOMinDiagVal;
	private cl_mem matOMaxDiagVal;
	private cl_mem matNPVal;
	private cl_mem matNPSrc;
	private cl_mem matNPTrgBeg;

	final private cl_mem minMem1;
	final private cl_mem maxMem1;
	final private cl_mem minMem2;
	final private cl_mem maxMem2;
	double[] diag;
}