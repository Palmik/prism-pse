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

		clKernelMatIOMin = clCreateKernel(clProgram(), "PSE_MV_IO", null);
		clKernelMatIOMax = clCreateKernel(clProgram(), "PSE_MV_IO", null);
		clKernelMatNPMin = clCreateKernel(clProgram(), "PSE_MV_NP", null);
		clKernelMatNPMax = clCreateKernel(clProgram(), "PSE_MV_NP", null);

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
		( double min[], double resMin[]
		, double max[], double resMax[]
		, int iterationCnt
		)
	{
		cl_mem minMem = minMem1;
		cl_mem maxMem = maxMem1;
		cl_mem resMinMem = minMem2;
		cl_mem resMaxMem = maxMem2;

		final cl_event evWrite[] = new cl_event[]{new cl_event(), new cl_event()};
		clEnqueueWriteBuffer(clCommandQueueMin(), minMem, true, 0, Sizeof.cl_double * stCnt, Pointer.to(min), 0, null, evWrite[0]);
		clEnqueueWriteBuffer(clCommandQueueMax(), maxMem, true, 0, Sizeof.cl_double * stCnt, Pointer.to(max), 0, null, evWrite[1]);
		clWaitForEvents(2, evWrite);
		for (cl_event e : evWrite) clReleaseEvent(e);

		final long[] lws = new long[]{1024};
		final long[] gwsIO = new long[]{leastGreaterMultiple(matIORowCnt, lws[0])};
		final long[] gwsNP = new long[]{leastGreaterMultiple(matNPRowCnt, lws[0])};

		final cl_event[] evMatIOMin = new cl_event[]{new cl_event()};
		final cl_event[] evMatIOMax = new cl_event[]{new cl_event()};
		final cl_event[] evMatNPMin = new cl_event[]{new cl_event()};
		final cl_event[] evMatNPMax = new cl_event[]{new cl_event()};
		final cl_event[] evCopyMin = new cl_event[]{new cl_event()};
		final cl_event[] evCopyMax = new cl_event[]{new cl_event()};
		for (int i = 0; i < iterationCnt; ++i) {
			clEnqueueCopyBuffer(clCommandQueueMin(), minMem, resMinMem, 0, 0, Sizeof.cl_double * stCnt
				, (i == 0) ? 0 : 1, (i == 0) ? null : evMatNPMin, evCopyMin[0]);
			clEnqueueCopyBuffer(clCommandQueueMax(), maxMem, resMaxMem, 0, 0, Sizeof.cl_double * stCnt
				, (i == 0) ? 0 : 1, (i == 0) ? null : evMatNPMax, evCopyMax[0]);

			if (enabledMatIO) {
				clSetKernelArg(clKernelMatIOMin, 6, Sizeof.cl_mem, Pointer.to(minMem));
				clSetKernelArg(clKernelMatIOMin, 7, Sizeof.cl_mem, Pointer.to(resMinMem));
				clSetKernelArg(clKernelMatIOMax, 6, Sizeof.cl_mem, Pointer.to(maxMem));
				clSetKernelArg(clKernelMatIOMax, 7, Sizeof.cl_mem, Pointer.to(resMaxMem));
			}

			if (enabledMatNP) {
				clSetKernelArg(clKernelMatNPMin, 5, Sizeof.cl_mem, Pointer.to(minMem));
				clSetKernelArg(clKernelMatNPMin, 6, Sizeof.cl_mem, Pointer.to(resMinMem));
				clSetKernelArg(clKernelMatNPMax, 5, Sizeof.cl_mem, Pointer.to(maxMem));
				clSetKernelArg(clKernelMatNPMax, 6, Sizeof.cl_mem, Pointer.to(resMaxMem));
			}

			if (enabledMatIO) {
				clEnqueueNDRangeKernel(clCommandQueueMin(), clKernelMatIOMin, 1, null
					, gwsIO, lws
					, 1, evCopyMin, evMatIOMin[0]);
				clEnqueueNDRangeKernel(clCommandQueueMax(), clKernelMatIOMax, 1, null
					, gwsIO, lws
					, 1, evCopyMax, evMatIOMax[0]);
			} else {
				clEnqueueMarkerWithWaitList(clCommandQueueMin()
					, 1, evCopyMin, evMatIOMin[0]);
				clEnqueueMarkerWithWaitList(clCommandQueueMax()
					, 1, evCopyMax, evMatIOMax[0]);
			}

			if (enabledMatNP) {
				clEnqueueNDRangeKernel(clCommandQueueMin(), clKernelMatNPMin, 1, null
					, gwsNP, lws
					, 1, evMatIOMin, evMatNPMin[0]);
				clEnqueueNDRangeKernel(clCommandQueueMax(), clKernelMatNPMax, 1, null
					, gwsNP, lws
					, 1, evMatIOMax, evMatNPMax[0]);
			} else {
				clEnqueueMarkerWithWaitList(clCommandQueueMin()
					, 1, evMatIOMin, evMatNPMin[0]);
				clEnqueueMarkerWithWaitList(clCommandQueueMax()
					, 1, evMatIOMax, evMatNPMax[0]);
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
		clReleaseEvent(evMatIOMin[0]);
		clReleaseEvent(evMatIOMax[0]);
		clReleaseEvent(evMatNPMin[0]);
		clReleaseEvent(evMatNPMax[0]);
		clReleaseEvent(evCopyMin[0]);
		clReleaseEvent(evCopyMax[0]);
		clEnqueueReadBuffer(clCommandQueueMin(), minMem, true, 0, Sizeof.cl_double * stCnt, Pointer.to(resMin), 0, null, null);
		clEnqueueReadBuffer(clCommandQueueMin(), maxMem, true, 0, Sizeof.cl_double * stCnt, Pointer.to(resMax), 0, null, null);
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
		clReleaseKernel(clKernelMatIOMin);
		clReleaseKernel(clKernelMatIOMax);
		clReleaseKernel(clKernelMatNPMin);
		clReleaseKernel(clKernelMatNPMax);
		clReleaseCommandQueue(clCommandQueueMin);
		clReleaseCommandQueue(clCommandQueueMax);
		oclProgram.release();
	}

	private static long leastGreaterMultiple(long x, long z)
	{
		return x + (z - x % z) % z;
	}

	final private cl_program clProgram() { return oclProgram.getProgram(); }
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
