package pse;

import org.jocl.*;
import static org.jocl.CL.*;

public final class PSEModelForMV_GPU
{
	public PSEModelForMV_GPU
		( int stCnt

		, double[] matIOLowerVal
		, double[] matIOUpperVal
		, int[] matIOCol
		, int[] matIORow
		, int[] matIORowBeg
		, int matIORowCnt

		, double[] matNPVal
		, int[] matNPCol
		, int[] matNPRow
		, int[] matNPRowBeg
		, int matNPRowCnt

		, double[] weight
		, double   weightDef
		, int      weightOff
		)
	{
		this.stCnt = stCnt;

		this.oclProgram = new OCLProgram();
		this.clCommandQueueMin = oclProgram.createCommandQueue();
		this.clCommandQueueMax = oclProgram.createCommandQueue();

		setExceptionsEnabled(true);

		this.matIORowCnt = matIORowCnt;
		this.matNPRowCnt = matNPRowCnt;
		this.enabledMatIO = matIORowCnt > 0 && matIORowBeg[matIORowCnt] > 0;
		this.enabledMatNP = matNPRowCnt > 0 && matNPRowBeg[matNPRowCnt] > 0;

		this.weight = weight;
		this.weightDef = weightDef;
		this.weightOff = weightOff;
		this.totalIterationCnt = 0;

		clKernelMatIOMin = oclProgram.createKernel("PSE_MV_IO");
		clKernelMatIOMax = oclProgram.createKernel("PSE_MV_IO");
		clKernelMatNPMin = oclProgram.createKernel("PSE_MV_NP");
		clKernelMatNPMax = oclProgram.createKernel("PSE_MV_NP");
		clKernelSumMin = oclProgram.createKernel("WeightedSumTo");
		clKernelSumMax = oclProgram.createKernel("WeightedSumTo");

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
			final Pointer matIOCol_ = Pointer.to(matIOCol);
			final Pointer matIORow_ = Pointer.to(matIORow);
			final Pointer matIORowBeg_ = Pointer.to(matIORowBeg);

			this.matIOLowerVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * matIORowBeg[matIORowCnt], matIOLowerVal_, null);
			this.matIOUpperVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * matIORowBeg[matIORowCnt], matIOUpperVal_, null);
			this.matIOCol = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * matIORowBeg[matIORowCnt], matIOCol_, null);
			this.matIORow = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * matIORowCnt, matIORow_, null);
			this.matIORowBeg = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * (matIORowCnt + 1), matIORowBeg_, null);

			clSetKernelArg(clKernelMatIOMin, 0, Sizeof.cl_uint, Pointer.to(new int[]{matIORowCnt}));
			clSetKernelArg(clKernelMatIOMin, 1, Sizeof.cl_mem, Pointer.to(this.matIOLowerVal));
			clSetKernelArg(clKernelMatIOMin, 2, Sizeof.cl_mem, Pointer.to(this.matIOUpperVal));
			clSetKernelArg(clKernelMatIOMin, 3, Sizeof.cl_mem, Pointer.to(this.matIOCol));
			clSetKernelArg(clKernelMatIOMin, 4, Sizeof.cl_mem, Pointer.to(this.matIORow));
			clSetKernelArg(clKernelMatIOMin, 5, Sizeof.cl_mem, Pointer.to(this.matIORowBeg));

			clSetKernelArg(clKernelMatIOMax, 0, Sizeof.cl_uint, Pointer.to(new int[]{matIORowCnt}));
			clSetKernelArg(clKernelMatIOMax, 1, Sizeof.cl_mem, Pointer.to(this.matIOUpperVal));
			clSetKernelArg(clKernelMatIOMax, 2, Sizeof.cl_mem, Pointer.to(this.matIOLowerVal));
			clSetKernelArg(clKernelMatIOMax, 3, Sizeof.cl_mem, Pointer.to(this.matIOCol));
			clSetKernelArg(clKernelMatIOMax, 4, Sizeof.cl_mem, Pointer.to(this.matIORow));
			clSetKernelArg(clKernelMatIOMax, 5, Sizeof.cl_mem, Pointer.to(this.matIORowBeg));
		}

		if (enabledMatNP) {
			// Setup mat
			final Pointer matNPVal_ = Pointer.to(matNPVal);
			final Pointer matNPCol_ = Pointer.to(matNPCol);
			final Pointer matNPRow_ = Pointer.to(matNPRow);
			final Pointer matNPRowBeg_ = Pointer.to(matNPRowBeg);

			this.matNPVal = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * matNPRowBeg[matNPRowCnt], matNPVal_, null);
			this.matNPCol = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * matNPRowBeg[matNPRowCnt], matNPCol_, null);
			this.matNPRow = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * matNPRowCnt, matNPRow_, null);
			this.matNPRowBeg = clCreateBuffer(clContext(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * (matNPRowCnt + 1), matNPRowBeg_, null);

			clSetKernelArg(clKernelMatNPMin, 0, Sizeof.cl_uint, Pointer.to(new int[]{matNPRowCnt}));
			clSetKernelArg(clKernelMatNPMin, 1, Sizeof.cl_mem, Pointer.to(this.matNPVal));
			clSetKernelArg(clKernelMatNPMin, 2, Sizeof.cl_mem, Pointer.to(this.matNPCol));
			clSetKernelArg(clKernelMatNPMin, 3, Sizeof.cl_mem, Pointer.to(this.matNPRow));
			clSetKernelArg(clKernelMatNPMin, 4, Sizeof.cl_mem, Pointer.to(this.matNPRowBeg));

			clSetKernelArg(clKernelMatNPMax, 0, Sizeof.cl_uint, Pointer.to(new int[]{matNPRowCnt}));
			clSetKernelArg(clKernelMatNPMax, 1, Sizeof.cl_mem, Pointer.to(this.matNPVal));
			clSetKernelArg(clKernelMatNPMax, 2, Sizeof.cl_mem, Pointer.to(this.matNPCol));
			clSetKernelArg(clKernelMatNPMax, 3, Sizeof.cl_mem, Pointer.to(this.matNPRow));
			clSetKernelArg(clKernelMatNPMax, 4, Sizeof.cl_mem, Pointer.to(this.matNPRowBeg));
		}

		minMem1 = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, Sizeof.cl_double * stCnt, null, null);
		minMem2 = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, Sizeof.cl_double * stCnt, null, null);
		maxMem1 = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, Sizeof.cl_double * stCnt, null, null);
		maxMem2 = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, Sizeof.cl_double * stCnt, null, null);
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

		final long[] lws = new long[]{oclProgram.clLocalWorkSize(1024)};
		final long[] gwsIO = new long[]{leastGreaterMultiple(matIORowCnt, lws[0])};
		final long[] gwsNP = new long[]{leastGreaterMultiple(matNPRowCnt, lws[0])};
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

			if (sumWeight() > 0 || sumWeight() < 0)
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
		clReleaseMemObject(matIOCol);
		clReleaseMemObject(matIORow);
		clReleaseMemObject(matIORowBeg);
		clReleaseMemObject(matNPVal);
		clReleaseMemObject(matNPCol);
		clReleaseMemObject(matNPRow);
		clReleaseMemObject(matNPRowBeg);
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
		oclProgram.release();
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
	final private cl_context clContext() { return oclProgram.getContext(); }

	final private int stCnt;

	final private boolean enabledMatIO;
	final private boolean enabledMatNP;

	final private OCLProgram oclProgram;
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

	private final int matIORowCnt;
	private cl_mem matIOLowerVal;
	private cl_mem matIOUpperVal;
	private cl_mem matIOCol;
	private cl_mem matIORow;
	private cl_mem matIORowBeg;

	private final int matNPRowCnt;
	private cl_mem matNPVal;
	private cl_mem matNPCol;
	private cl_mem matNPRow;
	private cl_mem matNPRowBeg;

	final private cl_mem minMem1;
	final private cl_mem minMem2;
	final private cl_mem maxMem1;
	final private cl_mem maxMem2;
}
