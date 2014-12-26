package pse;

import org.jocl.*;
import static org.jocl.CL.*;

public final class PSEMVMult_OCL implements PSEMVMult
{
	public PSEMVMult_OCL
		( int stCnt

		, double[] matIOLowerVal
		, double[] matIOUpperVal

		, double[] matNPVal

		, double[] weight
		, double weightDef
		, int weightOff

		, PSEMVMultTopology_OCL topo
		, PSEMVMultSettings_OCL opts
		)
	{
		this.stCnt = stCnt;
		this.topo = topo;
		this.clContext = opts.clContext;

		this.clCommandQueueMin = clCreateCommandQueue(clContext, opts.clDeviceIdMin, 0, null);
		this.clCommandQueueMax = clCreateCommandQueue(clContext, opts.clDeviceIdMax, 0, null);

		//int maxSubs[] = new int[]{0};
		//clGetDeviceInfo(oclProgram.getDeviceId(), CL_DEVICE_PARTITION_MAX_SUB_DEVICES, Sizeof.cl_uint, Pointer.to(maxSubs), null);
		//System.out.printf("MAX SUBS %s\n", maxSubs[0]);

		setExceptionsEnabled(true);

		this.enabledMatIO = topo.matIORowCnt > 0 && topo.matIORowBegHost[topo.matIORowCnt] > 0;
		this.enabledMatNP = topo.matNPRowCnt > 0 && topo.matNPRowBegHost[topo.matNPRowCnt] > 0;

		this.weight = weight;
		this.weightDef = weightDef;
		this.weightOff = weightOff;
		this.totalIterationCnt = 0;

		clProgram = OCLProgram.createProgram(OCLProgram.SOURCE, clContext);
		clKernelMatIOMin = OCLProgram.createKernel("PSE_MV_IO_CSR_SCALAR", clProgram);
		clKernelMatIOMax = OCLProgram.createKernel("PSE_MV_IO_CSR_SCALAR", clProgram);
		clKernelMatNPMin = OCLProgram.createKernel("PSE_MV_NP_CSR_SCALAR", clProgram);
		clKernelMatNPMax = OCLProgram.createKernel("PSE_MV_NP_CSR_SCALAR", clProgram);
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

		if (enabledMatIO) {
			// Setup mat
			final Pointer matIOLowerVal_ = Pointer.to(matIOLowerVal);
			final Pointer matIOUpperVal_ = Pointer.to(matIOUpperVal);

			this.matIOLowerVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * topo.matIORowBegHost[topo.matIORowCnt], matIOLowerVal_, null);
			this.matIOUpperVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * topo.matIORowBegHost[topo.matIORowCnt], matIOUpperVal_, null);

			clSetKernelArg(clKernelMatIOMin, 0, Sizeof.cl_uint, Pointer.to(new int[]{topo.matIORowCnt}));
			clSetKernelArg(clKernelMatIOMin, 1, Sizeof.cl_mem, Pointer.to(this.matIOLowerVal));
			clSetKernelArg(clKernelMatIOMin, 2, Sizeof.cl_mem, Pointer.to(this.matIOUpperVal));
			clSetKernelArg(clKernelMatIOMin, 3, Sizeof.cl_mem, Pointer.to(topo.matIOCol));
			clSetKernelArg(clKernelMatIOMin, 4, Sizeof.cl_mem, Pointer.to(topo.matIORow));
			clSetKernelArg(clKernelMatIOMin, 5, Sizeof.cl_mem, Pointer.to(topo.matIORowBeg));

			clSetKernelArg(clKernelMatIOMax, 0, Sizeof.cl_uint, Pointer.to(new int[]{topo.matIORowCnt}));
			clSetKernelArg(clKernelMatIOMax, 1, Sizeof.cl_mem, Pointer.to(this.matIOUpperVal));
			clSetKernelArg(clKernelMatIOMax, 2, Sizeof.cl_mem, Pointer.to(this.matIOLowerVal));
			clSetKernelArg(clKernelMatIOMax, 3, Sizeof.cl_mem, Pointer.to(topo.matIOCol));
			clSetKernelArg(clKernelMatIOMax, 4, Sizeof.cl_mem, Pointer.to(topo.matIORow));
			clSetKernelArg(clKernelMatIOMax, 5, Sizeof.cl_mem, Pointer.to(topo.matIORowBeg));
		}

		if (enabledMatNP) {
			// Setup mat
			final Pointer matNPVal_ = Pointer.to(matNPVal);

			this.matNPVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * topo.matNPRowBegHost[topo.matNPRowCnt], matNPVal_, null);

			clSetKernelArg(clKernelMatNPMin, 0, Sizeof.cl_uint, Pointer.to(new int[]{topo.matNPRowCnt}));
			clSetKernelArg(clKernelMatNPMin, 1, Sizeof.cl_mem, Pointer.to(this.matNPVal));
			clSetKernelArg(clKernelMatNPMin, 2, Sizeof.cl_mem, Pointer.to(topo.matNPCol));
			clSetKernelArg(clKernelMatNPMin, 3, Sizeof.cl_mem, Pointer.to(topo.matNPRow));
			clSetKernelArg(clKernelMatNPMin, 4, Sizeof.cl_mem, Pointer.to(topo.matNPRowBeg));

			clSetKernelArg(clKernelMatNPMax, 0, Sizeof.cl_uint, Pointer.to(new int[]{topo.matNPRowCnt}));
			clSetKernelArg(clKernelMatNPMax, 1, Sizeof.cl_mem, Pointer.to(this.matNPVal));
			clSetKernelArg(clKernelMatNPMax, 2, Sizeof.cl_mem, Pointer.to(topo.matNPCol));
			clSetKernelArg(clKernelMatNPMax, 3, Sizeof.cl_mem, Pointer.to(topo.matNPRow));
			clSetKernelArg(clKernelMatNPMax, 4, Sizeof.cl_mem, Pointer.to(topo.matNPRowBeg));
		}

		minMem1 = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, Sizeof.cl_double * stCnt, null, null);
		minMem2 = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, Sizeof.cl_double * stCnt, null, null);
		maxMem1 = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, Sizeof.cl_double * stCnt, null, null);
		maxMem2 = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, Sizeof.cl_double * stCnt, null, null);
	}

	final public void update
		( double[] matIOLowerVal
		, double[] matIOUpperVal
		, double[] matNPVal
		)
	{
		if (enabledMatIO) {
			clReleaseMemObject(this.matIOLowerVal);
			clReleaseMemObject(this.matIOUpperVal);
			// Setup mat
			final Pointer matIOLowerVal_ = Pointer.to(matIOLowerVal);
			final Pointer matIOUpperVal_ = Pointer.to(matIOUpperVal);

			this.matIOLowerVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * this.topo.matIORowBegHost[this.topo.matIORowCnt], matIOLowerVal_, null);
			this.matIOUpperVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * this.topo.matIORowBegHost[this.topo.matIORowCnt], matIOUpperVal_, null);

			clSetKernelArg(clKernelMatIOMin, 1, Sizeof.cl_mem, Pointer.to(this.matIOLowerVal));
			clSetKernelArg(clKernelMatIOMin, 2, Sizeof.cl_mem, Pointer.to(this.matIOUpperVal));

			clSetKernelArg(clKernelMatIOMax, 1, Sizeof.cl_mem, Pointer.to(this.matIOUpperVal));
			clSetKernelArg(clKernelMatIOMax, 2, Sizeof.cl_mem, Pointer.to(this.matIOLowerVal));
		}

		if (enabledMatNP) {
			clReleaseMemObject(this.matNPVal);

			final Pointer matNPVal_ = Pointer.to(matNPVal);

			this.matNPVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * this.topo.matNPRowBegHost[this.topo.matNPRowCnt], matNPVal_, null);

			clSetKernelArg(clKernelMatNPMin, 1, Sizeof.cl_mem, Pointer.to(this.matNPVal));

			clSetKernelArg(clKernelMatNPMax, 1, Sizeof.cl_mem, Pointer.to(this.matNPVal));
		}
	}

	final public void mvMult
		( final double min[], final double resMin[]
		, final double max[], final double resMax[]
		, final int iterationCnt
		)
	{
		cl_mem minMem = minMem1;
		cl_mem maxMem = maxMem1;
		cl_mem resMinMem = minMem2;
		cl_mem resMaxMem = maxMem2;

		clEnqueueWriteBuffer(clCommandQueueMin(), minMem, true, 0, Sizeof.cl_double * stCnt, Pointer.to(min)
			, 0, null, null);
		clEnqueueWriteBuffer(clCommandQueueMax(), maxMem, true, 0, Sizeof.cl_double * stCnt, Pointer.to(max)
			, 0, null, null);

		final long[] lws = new long[]{OCLProgram.localWorkSize(1024)};
		final long[] gwsIO = new long[]{leastGreaterMultiple(this.topo.matIORowCnt, lws[0])};
		final long[] gwsNP = new long[]{leastGreaterMultiple(this.topo.matNPRowCnt, lws[0])};
		final long[] gwsSum = new long[]{leastGreaterMultiple(stCnt, lws[0])};

		for (int i = 0; i < iterationCnt; ++i) {
			clEnqueueCopyBuffer(clCommandQueueMin(), minMem, resMinMem, 0, 0, Sizeof.cl_double * stCnt
				, 0, null, null);
			clEnqueueCopyBuffer(clCommandQueueMax(), maxMem, resMaxMem, 0, 0, Sizeof.cl_double * stCnt
				, 0, null, null);

			if (enabledMatIO) {
				clSetKernelArg(clKernelMatIOMin, 6, Sizeof.cl_mem, Pointer.to(minMem));
				clSetKernelArg(clKernelMatIOMin, 7, Sizeof.cl_mem, Pointer.to(resMinMem));
				clSetKernelArg(clKernelMatIOMax, 6, Sizeof.cl_mem, Pointer.to(maxMem));
				clSetKernelArg(clKernelMatIOMax, 7, Sizeof.cl_mem, Pointer.to(resMaxMem));

				clEnqueueNDRangeKernel(clCommandQueueMin(), clKernelMatIOMin, 1, null
					, gwsIO, lws, 0, null, null);
				clEnqueueNDRangeKernel(clCommandQueueMax(), clKernelMatIOMax, 1, null
					, gwsIO, lws, 0, null, null);
			}

			if (enabledMatNP) {
				clSetKernelArg(clKernelMatNPMin, 5, Sizeof.cl_mem, Pointer.to(minMem));
				clSetKernelArg(clKernelMatNPMin, 6, Sizeof.cl_mem, Pointer.to(resMinMem));
				clSetKernelArg(clKernelMatNPMax, 5, Sizeof.cl_mem, Pointer.to(maxMem));
				clSetKernelArg(clKernelMatNPMax, 6, Sizeof.cl_mem, Pointer.to(resMaxMem));

				clEnqueueNDRangeKernel(clCommandQueueMin(), clKernelMatNPMin, 1, null
					, gwsNP, lws, 0, null, null);
				clEnqueueNDRangeKernel(clCommandQueueMax(), clKernelMatNPMax, 1, null
					, gwsNP, lws, 0, null, null);
			}

			++totalIterationCnt;

			if (sumWeight() != 0)
			{
				clSetKernelArg(clKernelSumMin, 1, Sizeof.cl_double, Pointer.to(new double[]{sumWeight()}));
				clSetKernelArg(clKernelSumMin, 2, Sizeof.cl_mem, Pointer.to(resMinMem));
				clEnqueueNDRangeKernel(clCommandQueueMin(), clKernelSumMin, 1, null
					, gwsSum, lws
					, 0, null, null);

				clSetKernelArg(clKernelSumMax, 1, Sizeof.cl_double, Pointer.to(new double[]{sumWeight()}));
				clSetKernelArg(clKernelSumMax, 2, Sizeof.cl_mem, Pointer.to(resMaxMem));
				clEnqueueNDRangeKernel(clCommandQueueMax(), clKernelSumMax, 1, null
					, gwsSum, lws
					, 0, null, null);
			}

			// Swap
			final cl_mem tmpMin = minMem;
			final cl_mem tmpMax = maxMem;
			minMem = resMinMem;
			maxMem = resMaxMem;
			resMinMem = tmpMin;
			resMaxMem = tmpMax;
		}
		clEnqueueReadBuffer(clCommandQueueMin(), minMem, true, 0, Sizeof.cl_double * stCnt, Pointer.to(resMin), 0, null, null);
		clEnqueueReadBuffer(clCommandQueueMax(), maxMem, true, 0, Sizeof.cl_double * stCnt, Pointer.to(resMax), 0, null, null);
		clFinish(clCommandQueueMin());
		clFinish(clCommandQueueMax());
	}

	final public void getSum(final double[] sumMin, final double[] sumMax)
	{
		clEnqueueReadBuffer(clCommandQueueMin(), this.sumMin, true, 0, Sizeof.cl_double * stCnt, Pointer.to(sumMin), 0, null, null);
		clEnqueueReadBuffer(clCommandQueueMin(), this.sumMax, true, 0, Sizeof.cl_double * stCnt, Pointer.to(sumMax), 0, null, null);
		clFinish(clCommandQueueMin());
	}

	final public void release()
	{
		clReleaseMemObject(matIOLowerVal);
		clReleaseMemObject(matIOUpperVal);
		clReleaseMemObject(matNPVal);
		clReleaseMemObject(minMem1);
		clReleaseMemObject(minMem2);
		clReleaseMemObject(maxMem1);
		clReleaseMemObject(maxMem2);
		clReleaseMemObject(sumMin);
		clReleaseMemObject(sumMax);
		clReleaseKernel(clKernelMatIOMin);
		clReleaseKernel(clKernelMatIOMax);
		clReleaseKernel(clKernelMatNPMin);
		clReleaseKernel(clKernelMatNPMax);
		clReleaseKernel(clKernelSumMin);
		clReleaseKernel(clKernelSumMax);
		clReleaseCommandQueue(clCommandQueueMin);
		clReleaseCommandQueue(clCommandQueueMax);
		clReleaseContext(clContext);
		clReleaseProgram(clProgram);
	}

	private static long leastGreaterMultiple(long x, long z)
	{
		return x + (z - x % z) % z;
	}

	final private double sumWeight()
	{
		if (totalIterationCnt >= weightOff)
		{
			return weight[totalIterationCnt - weightOff];
		}
		return weightDef;
	}

	final private cl_command_queue clCommandQueueMin() { return clCommandQueueMin; }
	final private cl_command_queue clCommandQueueMax() { return clCommandQueueMax; }
	final private cl_context clContext() { return clContext; }

	final private int stCnt;
	final private PSEMVMultTopology_OCL topo;
	final private boolean enabledMatIO;
	final private boolean enabledMatNP;

	final private cl_program clProgram;
	final private cl_context clContext;
	final private cl_command_queue clCommandQueueMin;
	final private cl_command_queue clCommandQueueMax;

	final private cl_kernel clKernelMatIOMin;
	final private cl_kernel clKernelMatIOMax;
	final private cl_kernel clKernelMatNPMin;
	final private cl_kernel clKernelMatNPMax;
	final private cl_kernel clKernelSumMin;
	final private cl_kernel clKernelSumMax;

	private int totalIterationCnt;
	final private cl_mem sumMin;
	final private cl_mem sumMax;
	final private double[] weight;
	final private double   weightDef;
	final private int      weightOff;

	private cl_mem matIOLowerVal;
	private cl_mem matIOUpperVal;

	private cl_mem matNPVal;

	final private cl_mem minMem1;
	final private cl_mem minMem2;
	final private cl_mem maxMem1;
	final private cl_mem maxMem2;
}
