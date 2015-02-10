package pse;

import org.jocl.*;

import static org.jocl.CL.*;

public final class PSEMVMult_OCL implements PSEMult, Releaseable
{
	public PSEMVMult_OCL
		( PSEMVMultSettings_OCL opts, PSEMVMultTopology_OCL topo, PSEMVCreateData_CSR data
		, final double[] weight
		, final double weightDef
		, final int weightOff
		)
	{
		this.stCnt = topo.stCnt;
		this.topo = topo;
		this.opts = opts;

		this.clCommandQueueMin = clCreateCommandQueue(clContext(), opts.clDeviceIdMin, 0, null);
		this.clCommandQueueMax = clCreateCommandQueue(clContext(), opts.clDeviceIdMax, 0, null);

		setExceptionsEnabled(true);

		this.enabledMatIO = topo.matIORowCnt > 0 && topo.matIORowBegHost[topo.matIORowCnt] > 0;
		this.enabledMatNP = topo.matNPRowCnt > 0 && topo.matNPRowBegHost[topo.matNPRowCnt] > 0;

		this.weight = weight;
		this.weightDef = weightDef;
		this.weightOff = weightOff;
		this.totalIterationCnt = 0;

		clProgram = OCLProgram.createProgram(OCLProgram.SOURCE, clContext());
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
			final Pointer matIOLowerVal_ = Pointer.to(data.matIOLowerVal);
			final Pointer matIOUpperVal_ = Pointer.to(data.matIOUpperVal);

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
			final Pointer matNPVal_ = Pointer.to(data.matNPVal);

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

		minMem = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, Sizeof.cl_double * stCnt, null, null);
		resMinMem = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, Sizeof.cl_double * stCnt, null, null);
		maxMem = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, Sizeof.cl_double * stCnt, null, null);
		resMaxMem = clCreateBuffer(clContext(), CL_MEM_READ_WRITE, Sizeof.cl_double * stCnt, null, null);
	}

	/* Updates the matrix values (assumes that values that were zero are zero as well). Resets sums to zero.
	 */
	final public void update(PSEMVCreateData_CSR data)
	{
		totalIterationCnt = 0;

		{
			final double[] zeroes = new double[stCnt];
			final Pointer zeroes_ = Pointer.to(zeroes);
			clEnqueueWriteBuffer(clCommandQueueMin(), sumMin, false, 0, Sizeof.cl_double * stCnt, zeroes_,
					0, null, null);
			clEnqueueWriteBuffer(clCommandQueueMax(), sumMax, false, 0, Sizeof.cl_double * stCnt, zeroes_,
					0, null, null);
		}

		if (enabledMatIO) {
			final Pointer matIOLowerVal_ = Pointer.to(data.matIOLowerVal);
			final Pointer matIOUpperVal_ = Pointer.to(data.matIOUpperVal);

			clEnqueueWriteBuffer(clCommandQueueMin(), matIOLowerVal, false, 0, Sizeof.cl_double * topo.matIORowBegHost[topo.matIORowCnt], matIOLowerVal_,
					0, null, null);
			clEnqueueWriteBuffer(clCommandQueueMax(), matIOUpperVal, false, 0, Sizeof.cl_double * topo.matIORowBegHost[topo.matIORowCnt], matIOUpperVal_,
					0, null, null);
		}

		if (enabledMatNP) {
			final Pointer matNPVal_ = Pointer.to(data.matNPVal);
			clEnqueueWriteBuffer(clCommandQueueMin(), matNPVal, false, 0, Sizeof.cl_double * topo.matNPRowBegHost[topo.matNPRowCnt], matNPVal_,
					0, null, null);
		}

		clFinish(clCommandQueueMin());
		clFinish(clCommandQueueMax());
	}

	@Override
	final public void release()
	{
		if (enabledMatIO) {
			clReleaseMemObject(matIOLowerVal);
			clReleaseMemObject(matIOUpperVal);
		}
		if (enabledMatNP) {
			clReleaseMemObject(matNPVal);
		}
		clReleaseMemObject(minMem);
		clReleaseMemObject(resMinMem);
		clReleaseMemObject(maxMem);
		clReleaseMemObject(resMaxMem);
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
		clReleaseProgram(clProgram);
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
		clEnqueueReadBuffer(clCommandQueueMin(), minMem, true, 0, Sizeof.cl_double * stCnt, Pointer.to(resMin), 0, null, null);
		clEnqueueReadBuffer(clCommandQueueMax(), maxMem, true, 0, Sizeof.cl_double * stCnt, Pointer.to(resMax), 0, null, null);
	}

	@Override
	final public void mult(int iterationCnt)
	{
		final long[] lws = new long[]{OCLProgram.localWorkSize(1024)};
		final long[] gwsIO = new long[]{Utility.leastGreaterMultiple(this.topo.matIORowCnt, lws[0])};
		final long[] gwsNP = new long[]{Utility.leastGreaterMultiple(this.topo.matNPRowCnt, lws[0])};
		final long[] gwsSum = new long[]{Utility.leastGreaterMultiple(stCnt, lws[0])};

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

			if (getSumWeight() != 0)
			{
				clSetKernelArg(clKernelSumMin, 1, Sizeof.cl_double, Pointer.to(new double[]{getSumWeight()}));
				clSetKernelArg(clKernelSumMin, 2, Sizeof.cl_mem, Pointer.to(resMinMem));
				clEnqueueNDRangeKernel(clCommandQueueMin(), clKernelSumMin, 1, null
						, gwsSum, lws
						, 0, null, null);

				clSetKernelArg(clKernelSumMax, 1, Sizeof.cl_double, Pointer.to(new double[]{getSumWeight()}));
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
		clFinish(clCommandQueueMin());
		clFinish(clCommandQueueMax());
	}

	@Override
	final public void mult
		( final double min[], final double resMin[]
		, final double max[], final double resMax[]
		, final int iterationCnt
		)
	{
		setMult(min, max); mult(iterationCnt); getMult(resMin, resMax);
	}

	@Override
	final public void getSum(final double[] sumMin, final double[] sumMax)
	{
		clEnqueueReadBuffer(clCommandQueueMin(), this.sumMin, true, 0, Sizeof.cl_double * stCnt, Pointer.to(sumMin), 0, null, null);
		clEnqueueReadBuffer(clCommandQueueMax(), this.sumMax, true, 0, Sizeof.cl_double * stCnt, Pointer.to(sumMax), 0, null, null);
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
	final private PSEMVMultTopology_OCL topo;
	final private PSEMVMultSettings_OCL opts;
	final private boolean enabledMatIO;
	final private boolean enabledMatNP;

	final private cl_program clProgram;
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

	private cl_mem minMem;
	private cl_mem resMinMem;
	private cl_mem maxMem;
	private cl_mem resMaxMem;
}
