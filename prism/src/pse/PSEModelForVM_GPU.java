package pse;

import org.jocl.*;

import static org.jocl.CL.*;

public class PSEModelForVM_GPU {
	public PSEModelForVM_GPU
		( int stCnt, int trCnt
		, double[] matIOLowerVal0
		, double[] matIOLowerVal1
		, double[] matIOUpperVal0
		, double[] matIOUpperVal1
		, int[] matIOSrc
		, int[] matIOTrgBeg

		, double[] matMinVal
		, int[] matMinSrc
		, int[] matMinTrgBeg

		, double[] matMaxVal
		, int[] matMaxSrc
		, int[] matMaxTrgBeg

		, double[] matMinDiagVal
		, double[] matMaxDiagVal
		, double[] matVal
		, int[] matSrc
		, int[] matTrgBeg
		) {
		this.stCnt = stCnt;
		this.trCnt = trCnt;

		acquireOpenCL(clProgramSource);
		setExceptionsEnabled(true);

		this.enabledMat = matTrgBeg[stCnt] > 0;
		this.enabledMatIO = matIOTrgBeg[stCnt] > 0;
		this.enabledMatMinMax = matMinTrgBeg[stCnt] > 0;

		clKernelMat = clCreateKernel(clProgram, "SpMV2_CS", null);
		clKernelMatIO = clCreateKernel(clProgram, "SpMVIO_CS", null);
		clKernelMatMax = clCreateKernel(clProgram, "SpMV1_CS", null);
		clKernelMatMin = clCreateKernel(clProgram, "SpMV1_CS", null);

		if (enabledMat) {
			// Setup mat
			final Pointer matVal_ = Pointer.to(matVal);
			final Pointer matSrc_ = Pointer.to(matSrc);
			final Pointer matTrgBeg_ = Pointer.to(matTrgBeg);
			final Pointer matMinDiagVal_ = Pointer.to(matMinDiagVal);
			final Pointer matMaxDiagVal_ = Pointer.to(matMaxDiagVal);

			this.matVal = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * matTrgBeg[stCnt], matVal_, null);
			this.matSrc = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * matTrgBeg[stCnt], matSrc_, null);
			this.matTrgBeg = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * (stCnt + 1), matTrgBeg_, null);
			this.matMinDiagVal = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * stCnt, matMinDiagVal_, null);
			this.matMaxDiagVal = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * stCnt, matMaxDiagVal_, null);

			clSetKernelArg(clKernelMat, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
			clSetKernelArg(clKernelMat, 1, Sizeof.cl_mem, Pointer.to(this.matMinDiagVal));
			clSetKernelArg(clKernelMat, 2, Sizeof.cl_mem, Pointer.to(this.matMaxDiagVal));
			clSetKernelArg(clKernelMat, 3, Sizeof.cl_mem, Pointer.to(this.matVal));
			clSetKernelArg(clKernelMat, 4, Sizeof.cl_mem, Pointer.to(this.matSrc));
			clSetKernelArg(clKernelMat, 5, Sizeof.cl_mem, Pointer.to(this.matTrgBeg));
		}

		if (enabledMatMinMax) {
			// Setup mat min
			final Pointer matMinVal_ = Pointer.to(matMinVal);
			final Pointer matMinSrc_ = Pointer.to(matMinSrc);
			final Pointer matMinTrgBeg_ = Pointer.to(matMinTrgBeg);

			this.matMinVal = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * matMinTrgBeg[stCnt], matMinVal_, null);
			this.matMinSrc = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * matMinTrgBeg[stCnt], matMinSrc_, null);
			this.matMinTrgBeg = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * (stCnt + 1), matMinTrgBeg_, null);

			clSetKernelArg(clKernelMatMin, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
			clSetKernelArg(clKernelMatMin, 1, Sizeof.cl_mem, Pointer.to(this.matMinVal));
			clSetKernelArg(clKernelMatMin, 2, Sizeof.cl_mem, Pointer.to(this.matMinSrc));
			clSetKernelArg(clKernelMatMin, 3, Sizeof.cl_mem, Pointer.to(this.matMinTrgBeg));

			// Setup mat max
			final Pointer matMaxVal_ = Pointer.to(matMaxVal);
			final Pointer matMaxSrc_ = Pointer.to(matMaxSrc);
			final Pointer matMaxTrgBeg_ = Pointer.to(matMaxTrgBeg);

			this.matMaxVal = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * matMaxTrgBeg[stCnt], matMaxVal_, null);
			this.matMaxSrc = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * matMaxTrgBeg[stCnt], matMaxSrc_, null);
			this.matMaxTrgBeg = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * (stCnt + 1), matMaxTrgBeg_, null);

			clSetKernelArg(clKernelMatMax, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
			clSetKernelArg(clKernelMatMax, 1, Sizeof.cl_mem, Pointer.to(this.matMaxVal));
			clSetKernelArg(clKernelMatMax, 2, Sizeof.cl_mem, Pointer.to(this.matMaxSrc));
			clSetKernelArg(clKernelMatMax, 3, Sizeof.cl_mem, Pointer.to(this.matMaxTrgBeg));
		}

		if (enabledMatIO) {
			// Setup mat IO
			final Pointer matIOLowerVal0_ = Pointer.to(matIOLowerVal0);
			final Pointer matIOLowerVal1_ = Pointer.to(matIOLowerVal1);
			final Pointer matIOUpperVal0_ = Pointer.to(matIOUpperVal0);
			final Pointer matIOUpperVal1_ = Pointer.to(matIOUpperVal1);
			final Pointer matIOSrc_ = Pointer.to(matIOSrc);
			final Pointer matIOTrgBeg_ = Pointer.to(matIOTrgBeg);

			this.matIOLowerVal0 = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * matIOTrgBeg[stCnt], matIOLowerVal0_, null);
			this.matIOLowerVal1 = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * matIOTrgBeg[stCnt], matIOLowerVal1_, null);
			this.matIOUpperVal0 = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * matIOTrgBeg[stCnt], matIOUpperVal0_, null);
			this.matIOUpperVal1 = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_double * matIOTrgBeg[stCnt], matIOUpperVal1_, null);
			this.matIOSrc = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * matTrgBeg[stCnt], matIOSrc_, null);
			this.matIOTrgBeg = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * (stCnt + 1), matIOTrgBeg_, null);

			clSetKernelArg(clKernelMatIO, 0, Sizeof.cl_uint, Pointer.to(new int[]{stCnt}));
			clSetKernelArg(clKernelMatIO, 1, Sizeof.cl_mem, Pointer.to(this.matIOLowerVal0));
			clSetKernelArg(clKernelMatIO, 2, Sizeof.cl_mem, Pointer.to(this.matIOLowerVal1));
			clSetKernelArg(clKernelMatIO, 3, Sizeof.cl_mem, Pointer.to(this.matIOUpperVal0));
			clSetKernelArg(clKernelMatIO, 4, Sizeof.cl_mem, Pointer.to(this.matIOUpperVal1));
			clSetKernelArg(clKernelMatIO, 5, Sizeof.cl_mem, Pointer.to(this.matIOSrc));
			clSetKernelArg(clKernelMatIO, 6, Sizeof.cl_mem, Pointer.to(this.matIOTrgBeg));
		}

		minMem1 = clCreateBuffer(clContext, CL_MEM_READ_WRITE, Sizeof.cl_double * stCnt, null, null);
		minMem2 = clCreateBuffer(clContext, CL_MEM_READ_WRITE, Sizeof.cl_double * stCnt, null, null);
		maxMem1 = clCreateBuffer(clContext, CL_MEM_READ_WRITE, Sizeof.cl_double * stCnt, null, null);
		maxMem2 = clCreateBuffer(clContext, CL_MEM_READ_WRITE, Sizeof.cl_double * stCnt, null, null);

	}

	public void vmMult(double min[], double resMin[], double max[], double resMax[], int iterationCnt) {
		cl_mem minMem = minMem1;
		cl_mem maxMem = maxMem1;
		cl_mem resMinMem = minMem2;
		cl_mem resMaxMem = maxMem2;

		final cl_event evWrite[] = new cl_event[]{new cl_event(), new cl_event()};
		clEnqueueWriteBuffer(clCommandQueue, minMem, true, 0, Sizeof.cl_double * stCnt, Pointer.to(min), 0, null, evWrite[0]);
		clEnqueueWriteBuffer(clCommandQueue, maxMem, true, 0, Sizeof.cl_double * stCnt, Pointer.to(max), 0, null, evWrite[1]);
		clWaitForEvents(2, evWrite);
		clReleaseEvent(evWrite[0]);
		clReleaseEvent(evWrite[1]);

		final long[] lws = new long[]{1024};
		final long[] gws = new long[]{leastGreaterMultiple(stCnt, lws[0])};

		final cl_event[] evMat = new cl_event[]{new cl_event()};
		final cl_event[] evMatIO = new cl_event[]{new cl_event()};
		final cl_event[] evMatMinMax = new cl_event[]{new cl_event(), new cl_event()};
		for (int i = 0; i < iterationCnt; ++i) {
			if (enabledMat) {
				clSetKernelArg(clKernelMat, 6, Sizeof.cl_mem, Pointer.to(minMem));
				clSetKernelArg(clKernelMat, 7, Sizeof.cl_mem, Pointer.to(maxMem));
				clSetKernelArg(clKernelMat, 8, Sizeof.cl_mem, Pointer.to(resMinMem));
				clSetKernelArg(clKernelMat, 9, Sizeof.cl_mem, Pointer.to(resMaxMem));
			}

			if (enabledMatIO) {
				clSetKernelArg(clKernelMatIO, 7, Sizeof.cl_mem, Pointer.to(minMem));
				clSetKernelArg(clKernelMatIO, 8, Sizeof.cl_mem, Pointer.to(maxMem));
				clSetKernelArg(clKernelMatIO, 9, Sizeof.cl_mem, Pointer.to(resMinMem));
				clSetKernelArg(clKernelMatIO, 10, Sizeof.cl_mem, Pointer.to(resMaxMem));
			}

			if (enabledMatMinMax) {
				clSetKernelArg(clKernelMatMin, 4, Sizeof.cl_mem, Pointer.to(minMem));
				clSetKernelArg(clKernelMatMin, 5, Sizeof.cl_mem, Pointer.to(resMinMem));

				clSetKernelArg(clKernelMatMax, 4, Sizeof.cl_mem, Pointer.to(maxMem));
				clSetKernelArg(clKernelMatMax, 5, Sizeof.cl_mem, Pointer.to(resMaxMem));
			}

			if (enabledMat) {
				clEnqueueNDRangeKernel(clCommandQueue, clKernelMat, 1, null
					, gws, lws
					, (i == 0) ? 0 : evMatMinMax.length, (i == 0) ? null : evMatMinMax, evMat[0]);
			} else {
				clEnqueueMarkerWithWaitList(clCommandQueue, (i == 0) ? 0 : evMatMinMax.length, (i == 0) ? null : evMatMinMax, evMat[0]);
			}

			if (enabledMatIO) {
				clEnqueueNDRangeKernel(clCommandQueue, clKernelMatIO, 1, null
					, gws, lws
					, evMat.length, evMat, evMatIO[0]);
			} else {
				clEnqueueMarkerWithWaitList(clCommandQueue, evMat.length, evMat, evMatIO[0]);
			}

			if (enabledMatMinMax) {
				clEnqueueNDRangeKernel(clCommandQueue, clKernelMatMin, 1, null
					, gws, lws
					, evMatIO.length, evMatIO, evMatMinMax[0]);

				clEnqueueNDRangeKernel(clCommandQueue, clKernelMatMax, 1, null
					, gws, lws
					, evMatIO.length, evMatIO, evMatMinMax[1]);
			} else {
				clEnqueueMarkerWithWaitList(clCommandQueue, evMatIO.length, evMatIO, evMatMinMax[0]);
				clEnqueueMarkerWithWaitList(clCommandQueue, evMatIO.length, evMatIO, evMatMinMax[1]);
			}

			// Swap
			final cl_mem tmpMin = minMem;
			final cl_mem tmpMax = maxMem;
			minMem = resMinMem;
			maxMem = resMaxMem;
			resMinMem = tmpMin;
			resMaxMem = tmpMax;
		}
		clFinish(clCommandQueue);
		clEnqueueReadBuffer(clCommandQueue, minMem, true, 0, Sizeof.cl_double * stCnt, Pointer.to(resMin), 0, null, null);
		clEnqueueReadBuffer(clCommandQueue, maxMem, true, 0, Sizeof.cl_double * stCnt, Pointer.to(resMax), 0, null, null);
		clFinish(clCommandQueue);
	}

	public void releaseOpenCL() {
		clReleaseMemObject(minMem1);
		clReleaseMemObject(minMem2);
		clReleaseMemObject(matIOLowerVal0);
		clReleaseMemObject(matIOLowerVal1);
		clReleaseMemObject(matIOUpperVal0);
		clReleaseMemObject(matIOUpperVal1);
		clReleaseMemObject(matIOSrc);
		clReleaseMemObject(matIOTrgBeg);
		clReleaseMemObject(matMinVal);
		clReleaseMemObject(matMinSrc);
		clReleaseMemObject(matMinTrgBeg);
		clReleaseMemObject(matMaxVal);
		clReleaseMemObject(matMaxSrc);
		clReleaseMemObject(matMaxTrgBeg);
		clReleaseMemObject(matMinDiagVal);
		clReleaseMemObject(matMaxDiagVal);
		clReleaseMemObject(matVal);
		clReleaseMemObject(matSrc);
		clReleaseMemObject(matTrgBeg);
		clReleaseKernel(clKernelMat);
		clReleaseKernel(clKernelMatIO);
		clReleaseKernel(clKernelMatMin);
		clReleaseKernel(clKernelMatMax);
		clReleaseProgram(clProgram);
		clReleaseCommandQueue(clCommandQueue);
		clReleaseContext(clContext);
	}

	private void acquireOpenCL(String programSource) {
		// The platform, device type and device number
		// that will be used
		final int platformIndex = 0;
		final long deviceType = CL_DEVICE_TYPE_ALL;
		final int deviceIndex = 0;

		// Enable exceptions and subsequently omit error checks in this sample
		CL.setExceptionsEnabled(true);

		// Obtain the number of platforms
		int numPlatformsArray[] = new int[1];
		clGetPlatformIDs(0, null, numPlatformsArray);
		int numPlatforms = numPlatformsArray[0];

		// Obtain a platform ID
		cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
		clGetPlatformIDs(platforms.length, platforms, null);
		cl_platform_id platform = platforms[platformIndex];

		// Initialize the context properties
		cl_context_properties contextProperties = new cl_context_properties();
		contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

		// Obtain the number of devices for the platform
		int numDevicesArray[] = new int[1];
		clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
		int numDevices = numDevicesArray[0];

		// Obtain a device ID
		cl_device_id devices[] = new cl_device_id[numDevices];
		clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
		cl_device_id device = devices[deviceIndex];

		// Create a context for the selected device
		clContext = clCreateContext(
			contextProperties, 1, new cl_device_id[]{device},
			null, null, null);

		// Create a command-queue
		clCommandQueue = clCreateCommandQueue(clContext, devices[0], 0, null);

		// Create the program from the source code
		clProgram = clCreateProgramWithSource(clContext,
			1, new String[]{programSource}, null, null);

		// Build the program
		clBuildProgram(clProgram, 0, null, null, null, null);

	        /*
	        // Create the kernel
	        return clCreateKernel(clProgram, kernelName, null);
	        */
	}

	private static long leastGreaterMultiple(long x, long z) {
		return x + (z - x % z) % z;
	}

	private cl_context clContext;
	private cl_command_queue clCommandQueue;
	private cl_program clProgram;
	private cl_kernel clKernelMatIO;
	private cl_kernel clKernelMatMin;
	private cl_kernel clKernelMatMax;
	private cl_kernel clKernelMat;

	final private int stCnt;
	final private int trCnt;

	final private boolean enabledMat;
	final private boolean enabledMatIO;
	final private boolean enabledMatMinMax;

	final private cl_mem minMem1;
	final private cl_mem maxMem1;
	final private cl_mem minMem2;
	final private cl_mem maxMem2;

	private cl_mem matIOLowerVal0;
	private cl_mem matIOLowerVal1;
	private cl_mem matIOUpperVal0;
	private cl_mem matIOUpperVal1;
	private cl_mem matIOSrc;
	private cl_mem matIOTrgBeg;

	private cl_mem matMinVal;
	private cl_mem matMinSrc;
	private cl_mem matMinTrgBeg;

	private cl_mem matMaxVal;
	private cl_mem matMaxSrc;
	private cl_mem matMaxTrgBeg;

	private cl_mem matMinDiagVal;
	private cl_mem matMaxDiagVal;
	private cl_mem matVal;
	private cl_mem matSrc;
	private cl_mem matTrgBeg;

	private static String clProgramSource =
		"#pragma OPENCL EXTENSION cl_khr_fp64 : enable\n#pragma OPENCL EXTENSION cl_amd_printf : enable\n\ntypedef double real;\n\n__kernel void SpMV1_CS\n  ( const uint stCnt\n  , __global real const* val\n  , __global uint const* trg\n  , __global uint const* srcBeg\n\n  , __global real const* vi\n  , __global real* vo\n  )\n{\n  int v0 = get_global_id(0);\n\n  real prod = vo[v0];\n  if (v0 < stCnt)\n  {\n    uint cb = srcBeg[v0];\n    uint ce = srcBeg[v0 + 1];\n\n    for (uint i = cb; i < ce; ++i)\n    {\n      prod += val[i] * vi[trg[i]];\n    }\n    vo[v0] = prod;\n  }\n}\n\n__kernel void SpMV2_CS\n  ( const uint stCnt\n  , __global real const* diagVal1\n  , __global real const* diagVal2\n  , __global real const* val\n  , __global uint const* trg\n  , __global uint const* srcBeg\n\n  , __global real const* vi1\n  , __global real const* vi2\n  , __global real* vo1\n  , __global real* vo2\n  )\n{\n  int v0 = get_global_id(0);\n\n  if (v0 < stCnt)\n  {\n    real prod1 = vi1[v0] * diagVal1[v0]; //vo1[v0] + vi1[v0] * diagVal1[v0];\n    real prod2 = vi2[v0] * diagVal2[v0]; //vo2[v0] + vi2[v0] * diagVal2[v0];\n\n    uint cb = srcBeg[v0];\n    uint ce = srcBeg[v0 + 1];\n\n    for (uint i = cb; i < ce; ++i)\n    {\n      prod1 += val[i] * vi1[trg[i]];\n      prod2 += val[i] * vi2[trg[i]];\n    }\n    vo1[v0] = prod1;\n    vo2[v0] = prod2;\n  }\n}\n\n__kernel void SpMVIO_CS\n  ( const uint stCnt\n  , __global real const* lowerVal0\n  , __global real const* lowerVal1\n  , __global real const* upperVal0\n  , __global real const* upperVal1\n  , __global uint const* src\n  , __global uint const* trgBeg\n\n  , __global real const* min0\n  , __global real const* max0\n  , __global real* min1\n  , __global real* max1\n  )\n{\n  int v1 = get_global_id(0);\n\n  if (v1 < stCnt)\n  {\n    real prod1 = min1[v1];\n    real prod2 = max1[v1];\n\n    uint cb = trgBeg[v1];\n    uint ce = trgBeg[v1 + 1];\n\n    for (uint i = cb; i < ce; ++i)\n    {\n      const uint v0 = src[i];\n      const real rlowerMin = (lowerVal0[i] * min0[v0] - lowerVal1[i] * min0[v1]);\n      const real rupperMax = (upperVal0[i] * max0[v0] - upperVal1[i] * max0[v1]);\n      if (rlowerMin > 0.0)\n      {\n        prod1 += rlowerMin;\n      }\n      else\n      {\n        prod1 += (upperVal0[i] * min0[v0] - upperVal1[i] * min0[v1]);\n      }\n      if (rupperMax > 0.0)\n      {\n        prod2 += rupperMax;\n      }\n      else\n      {\n        prod2 += (lowerVal0[i] * max0[v0] - lowerVal1[i] * max0[v1]);\n      }\n    }\n    min1[v1] = prod1;\n    max1[v1] = prod2;\n  }\n}\n";
}