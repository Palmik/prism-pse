package pse;

import org.jocl.*;

import static org.jocl.CL.*;

final public class OCLProgram
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

	final public cl_command_queue createCommandQueue()
	{
		return createCommandQueue(0);
	}

	final public cl_command_queue createCommandQueue(int params)
	{
		return clCreateCommandQueue(clContext, clDeviceId, params, null);
	}

	final public cl_kernel createKernel(String name)
	{
		return clCreateKernel(clProgram, name, null);
	}

	final public void release()
	{
		clReleaseProgram(clProgram);
		clReleaseCommandQueue(clCommandQueue);
		clReleaseContext(clContext);
	}

	// The local worksize, can be overwritten by the onv variable OCL_LWS
	int clLocalWorkSize(int def)
	{
		String envOCL_LWS = System.getenv("OCL_LWS");
		int res = def;
		if (envOCL_LWS != null)
		{
			res = Integer.parseInt(envOCL_LWS);
		}
		return res;
	}

	public final cl_context getContext() {
		return clContext;
	}

	public final cl_program getProgram() {
		return clProgram;
	}

	private final cl_context clContext;
	private final cl_command_queue clCommandQueue;
	private final cl_program clProgram;
	private final cl_device_id clDeviceId;

	private static String clProgramSource =
		"#pragma OPENCL EXTENSION cl_khr_fp64 : enable\n#pragma OPENCL EXTENSION cl_amd_printf : enable\n\n#define WARP_SIZE 32\n\n//#define MAD(A, B, C) (mad(A,B,C))\n#define MAD(A, B, C) ((A) * (B) + (C))\n\ntypedef double real;\n\n__kernel void WeightedSumTo\n  ( const uint n\n  , const real w\n  , __global real const* restrict vec\n  , __global real* restrict sum\n  )\n{\n    uint ii = get_global_id(0);\n    if (ii < n)\n    {\n        sum[ii] = MAD(vec[ii], w, sum[ii]);\n    }\n}\n\n__kernel void PSE_MV_IO_CSR_SCALAR\n  ( const uint matRowCnt\n  , __global real const* restrict matLowerVal\n  , __global real const* restrict matUpperVal\n  , __global uint const* restrict matCol\n  , __global uint const* restrict matRow\n  , __global uint const* restrict matRowBeg\n\n  , __global real const* restrict in\n  , __global real* restrict out\n  )\n{\n    uint ii = get_global_id(0);\n    if (ii < matRowCnt)\n    {\n        const uint v0 = matRow[ii];\n        const uint tb = matRowBeg[ii];\n        const uint te = matRowBeg[ii + 1];\n        real dot = out[v0];\n        for (int jj = tb; jj < te; ++jj) {\n            const uint v1 = matCol[jj];\n\n            const real diff = in[v1] - in[v0];\n            if (diff > 0) {\n                dot = MAD(matLowerVal[jj], diff, dot);\n            } else {\n                dot = MAD(matUpperVal[jj], diff, dot);\n            }\n        }\n        out[v0] = dot;\n    }\n}\n\n__kernel void PSE_MV_NP_CSR_SCALAR\n  ( const uint matRowCnt\n  , __global real const* restrict matVal\n  , __global uint const* restrict matCol\n  , __global uint const* restrict matRow\n  , __global uint const* restrict matRowBeg\n\n  , __global real const* restrict in\n  , __global real* restrict out\n  )\n{\n    uint ii = get_global_id(0);\n    if (ii < matRowCnt)\n    {\n        const uint v0 = matRow[ii];\n        const uint tb = matRowBeg[ii];\n        const uint te = matRowBeg[ii + 1];\n        real dot = out[v0];\n        for (uint jj = tb; jj < te; ++jj)\n        {\n            const uint v1 = matCol[jj];\n            const real rate = matVal[jj];\n            dot = MAD(rate, in[v1] - in[v0], dot);\n        }\n        out[v0] = dot;\n\t}\n}\n\n__kernel void PSE_MV_NP_CSR_VECTOR\n  ( const uint matRowCnt\n  , __global real const* restrict matVal\n  , __global uint const* restrict matCol\n  , __global uint const* restrict matRow\n  , __global uint const* restrict matRowBeg\n\n  , __global real const* restrict in\n  , __global real* restrict out\n\n  , __local volatile real* sdata\n  )\n{\n    uint id = get_global_id(0);\n    uint warp_id = id / WARP_SIZE; // == The warp of this worker\n    uint warp_pos = id & (WARP_SIZE - 1); // == id % WARP_SIZE == The position of this worker within the warp\n    uint warp_cnt = get_global_size(0) / WARP_SIZE;\n    for (uint ii = warp_id; ii < matRowCnt; ii += warp_cnt)\n    {\n        const uint v0 = matRow[ii];\n        const uint tb = matRowBeg[ii];\n        const uint te = matRowBeg[ii + 1];\n        real dot = out[v0];\n        for (uint jj = tb + warp_pos; jj < te; jj += WARP_SIZE)\n        {\n            const uint v1 = matCol[jj];\n            const real rate = matVal[jj];\n            dot = MAD(rate, in[v1] - in[v0], dot);\n        }\n\n        const int i = get_local_id(0);\n        sdata[i] = dot;\n        sdata[i] = dot += sdata[i + 16];\n        sdata[i] = dot += sdata[i + 8];\n        sdata[i] = dot += sdata[i + 4];\n        sdata[i] = dot += sdata[i + 2];\n        sdata[i] = dot += sdata[i + 1];\n\n        if (warp_pos == 0)\n        {\n            out[v0] = dot;\n        }\n\t}\n}\n\n__kernel void PSE_VM_I\n  ( const uint matRowCnt\n  , __global real const* restrict matVal\n  , __global uint const* restrict matCol\n  , __global uint const* restrict matRowBeg\n\n  , __global real const* restrict in\n  , __global real* restrict out\n  )\n{\n  int v0 = get_global_id(0);\n\n  if (v0 < matRowCnt)\n  {\n    uint cb = matRowBeg[v0];\n    uint ce = matRowBeg[v0 + 1];\n    real dot = out[v0];\n    for (uint i = cb; i < ce; ++i)\n    {\n      dot = MAD(matVal[i], in[matCol[i]], dot);\n    }\n    out[v0] = dot;\n  }\n}\n\n__kernel void PSE_VM_NP\n  ( const uint matRowCnt\n  , __global real const* restrict matDiaVal\n  , __global real const* restrict matVal\n  , __global uint const* restrict matCol\n  , __global uint const* restrict matRowBeg\n\n  , __global real const* restrict in\n  , __global real* restrict out\n  )\n{\n  int v0 = get_global_id(0);\n\n  if (v0 < matRowCnt)\n  {\n    real dot = in[v0] * matDiaVal[v0]; //out[v0] + in[v0] * matDiaVal[v0];\n\n    uint cb = matRowBeg[v0];\n    uint ce = matRowBeg[v0 + 1];\n    for (uint i = cb; i < ce; ++i)\n    {\n      dot = MAD(matVal[i], in[matCol[i]], dot);\n    }\n    out[v0] = dot;\n  }\n}\n\n__kernel void PSE_VM_IO\n  ( const uint matRowCnt\n  , __global real const* restrict matLowerVal0\n  , __global real const* restrict matLowerVal1\n  , __global real const* restrict matUpperVal0\n  , __global real const* restrict matUpperVal1\n  , __global uint const* restrict matCol\n  , __global uint const* restrict matRowBeg\n\n  , __global real const* restrict in\n  , __global real* restrict out\n  )\n{\n  int v1 = get_global_id(0);\n\n  if (v1 < matRowCnt)\n  {\n    real dot = out[v1];\n\n    uint cb = matRowBeg[v1];\n    uint ce = matRowBeg[v1 + 1];\n\n    for (uint i = cb; i < ce; ++i)\n    {\n      const uint v0 = matCol[i];\n      const real rlower = (matLowerVal0[i] * in[v0] - matLowerVal1[i] * in[v1]);\n      if (rlower > 0.0)\n      {\n        dot += rlower;\n      }\n      else\n      {\n        dot += (matUpperVal0[i] * in[v0] - matUpperVal1[i] * in[v1]);\n      }\n    }\n    out[v1] = dot;\n  }\n}\n"
		;
}
