package pse;

import org.jocl.*;

import static org.jocl.CL.*;

public class OCLProgram
{
	public OCLProgram()
	{
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
		clDeviceId = devices[deviceIndex];

		// Create a context for the selected device
		clContext = clCreateContext(
			contextProperties, 1, new cl_device_id[]{clDeviceId},
			null, null, null);

		// Create a command-queue
		clCommandQueue = clCreateCommandQueue(clContext, clDeviceId, 0, null);

		// Create the program from the source code
		clProgram = clCreateProgramWithSource(clContext,
			1, new String[]{clProgramSource}, null, null);

		// Build the program
		clBuildProgram(clProgram, 0, null, null, null, null);
	}

	public cl_command_queue createCommandQueue()
	{
		return clCreateCommandQueue(clContext, clDeviceId, 0, null);
	}

	public final void release()
	{
		clReleaseProgram(clProgram);
		clReleaseCommandQueue(clCommandQueue);
		clReleaseContext(clContext);
	}

	public final cl_context getContext() {
		return clContext;
	}

	public final cl_command_queue getCommandQueue() {
		return clCommandQueue;
	}

	public final cl_program getProgram() {
		return clProgram;
	}

	private final cl_context clContext;
	private final cl_command_queue clCommandQueue;
	private final cl_program clProgram;
	private final cl_device_id clDeviceId;

	private static String clProgramSource =
		"#pragma OPENCL EXTENSION cl_khr_fp64 : enable\n#pragma OPENCL EXTENSION cl_amd_printf : enable\n\ntypedef double real;\n\n\n__kernel void SpMV1_CS\n  ( const uint stCnt\n  , __global real const* val\n  , __global uint const* trg\n  , __global uint const* srcBeg\n\n  , __global real const* vi\n  , __global real* vo\n  )\n{\n  int v0 = get_global_id(0);\n\n  real prod = vo[v0];\n  if (v0 < stCnt)\n  {\n    uint cb = srcBeg[v0];\n    uint ce = srcBeg[v0 + 1];\n\n    for (uint i = cb; i < ce; ++i)\n    {\n      prod += val[i] * vi[trg[i]];\n    }\n    vo[v0] = prod;\n  }\n}\n\n__kernel void PSE_MV_IO\n  ( const uint matRowCnt\n  , __global real const* matLowerVal\n  , __global real const* matUpperVal\n  , __global uint const* matCol\n  , __global uint const* matRow\n  , __global uint const* matRowBeg\n\n  , __global real const* in\n  , __global real* out\n  )\n{\n    uint ii = get_global_id(0);\n    if (ii < matRowCnt)\n    {\n        const uint v0 = matRow[ii];\n        const uint tb = matRowBeg[ii];\n        const uint te = matRowBeg[ii + 1];\n        real dot = out[v0];\n        for (int jj = tb; jj < te; ++jj) {\n            const uint v1 = matCol[jj];\n\n            const real diff = in[v1] - in[v0];\n            if (diff > 0) {\n                dot += matLowerVal[jj] * diff;\n            } else {\n                dot += matUpperVal[jj] * diff;\n            }\n        }\n        out[v0] = dot;\n    }\n}\n\n__kernel void PSE_MV_NP\n  ( const uint matRowCnt\n  , __global real const* matVal\n  , __global uint const* matCol\n  , __global uint const* matRow\n  , __global uint const* matRowBeg\n\n  , __global real const* in\n  , __global real* out\n  )\n{\n    uint ii = get_global_id(0);\n    if (ii < matRowCnt)\n    {\n        const uint v0 = matRow[ii];\n        const uint tb = matRowBeg[ii];\n        const uint te = matRowBeg[ii + 1];\n        real dot = out[v0];\n        for (uint jj = tb; jj < te; ++jj)\n        {\n            const uint v1 = matCol[jj];\n            const real rate = matVal[jj];\n            dot += rate * (in[v1] - in[v0]);\n        }\n        out[v0] = dot;\n\t}\n}\n\n__kernel void SpMV2_CS\n  ( const uint stCnt\n  , __global real const* diagVal1\n  , __global real const* diagVal2\n  , __global real const* val\n  , __global uint const* trg\n  , __global uint const* srcBeg\n\n  , __global real const* vi1\n  , __global real const* vi2\n  , __global real* vo1\n  , __global real* vo2\n  )\n{\n  int v0 = get_global_id(0);\n\n  if (v0 < stCnt)\n  {\n    real prod1 = vi1[v0] * diagVal1[v0]; //vo1[v0] + vi1[v0] * diagVal1[v0];\n    real prod2 = vi2[v0] * diagVal2[v0]; //vo2[v0] + vi2[v0] * diagVal2[v0];\n\n    uint cb = srcBeg[v0];\n    uint ce = srcBeg[v0 + 1];\n\n    for (uint i = cb; i < ce; ++i)\n    {\n      prod1 += val[i] * vi1[trg[i]];\n      prod2 += val[i] * vi2[trg[i]];\n    }\n    vo1[v0] = prod1;\n    vo2[v0] = prod2;\n  }\n}\n\n__kernel void SpMVIO_CS\n  ( const uint stCnt\n  , __global real const* lowerVal0\n  , __global real const* lowerVal1\n  , __global real const* upperVal0\n  , __global real const* upperVal1\n  , __global uint const* src\n  , __global uint const* trgBeg\n\n  , __global real const* min0\n  , __global real const* max0\n  , __global real* min1\n  , __global real* max1\n  )\n{\n  int v1 = get_global_id(0);\n\n  if (v1 < stCnt)\n  {\n    real prod1 = min1[v1];\n    real prod2 = max1[v1];\n\n    uint cb = trgBeg[v1];\n    uint ce = trgBeg[v1 + 1];\n\n    for (uint i = cb; i < ce; ++i)\n    {\n      const uint v0 = src[i];\n      const real rlowerMin = (lowerVal0[i] * min0[v0] - lowerVal1[i] * min0[v1]);\n      const real rupperMax = (upperVal0[i] * max0[v0] - upperVal1[i] * max0[v1]);\n      if (rlowerMin > 0.0)\n      {\n        prod1 += rlowerMin;\n      }\n      else\n      {\n        prod1 += (upperVal0[i] * min0[v0] - upperVal1[i] * min0[v1]);\n      }\n      if (rupperMax > 0.0)\n      {\n        prod2 += rupperMax;\n      }\n      else\n      {\n        prod2 += (lowerVal0[i] * max0[v0] - lowerVal1[i] * max0[v1]);\n      }\n    }\n    min1[v1] = prod1;\n    max1[v1] = prod2;\n  }\n}\n"
		;
}
