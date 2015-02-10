package pse;

import org.jocl.*;
import static org.jocl.CL.*;

final public class PSEMVMultTopology_OCL implements Releaseable
{
	public PSEMVMultTopology_OCL(PSEMVCreateData_CSR data, cl_context clContext)
	{
		this.stCnt = data.stCnt;
		this.matIOColHost = data.matIOCol;
		this.matIORowHost = data.matIORow;
		this.matIORowBegHost = data.matIORowBeg;
		this.matIORowCnt = data.matIORowCnt;

		this.matNPColHost = data.matNPCol;
		this.matNPRowHost = data.matNPRow;
		this.matNPRowBegHost = data.matNPRowBeg;
		this.matNPRowCnt = data.matNPRowCnt;

		this.enabledMatIO = matIORowCnt > 0 && matIORowBegHost[matIORowCnt] > 0;
		this.enabledMatNP = matNPRowCnt > 0 && matNPRowBegHost[matNPRowCnt] > 0;

		if (enabledMatIO) {
			final Pointer matIOCol_ = Pointer.to(data.matIOCol);
			final Pointer matIORow_ = Pointer.to(data.matIORow);
			final Pointer matIORowBeg_ = Pointer.to(data.matIORowBeg);

			this.matIOCol = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * matIORowBegHost[matIORowCnt], matIOCol_, null);
			this.matIORow = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * matIORowCnt, matIORow_, null);
			this.matIORowBeg = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * (matIORowCnt + 1), matIORowBeg_, null);
		}

		if (enabledMatNP) {
			final Pointer matNPCol_ = Pointer.to(data.matNPCol);
			final Pointer matNPRow_ = Pointer.to(data.matNPRow);
			final Pointer matNPRowBeg_ = Pointer.to(data.matNPRowBeg);

			this.matNPCol = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * matNPRowBegHost[matNPRowCnt], matNPCol_, null);
			this.matNPRow = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * matNPRowCnt, matNPRow_, null);
			this.matNPRowBeg = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * (matNPRowCnt + 1), matNPRowBeg_, null);
		}
	}

	final public void release()
	{
		if (enabledMatNP) {
			clReleaseMemObject(matNPRowBeg);
			clReleaseMemObject(matNPRow);
			clReleaseMemObject(matNPCol);
		}
		if (enabledMatIO) {
			clReleaseMemObject(matIORowBeg);
			clReleaseMemObject(matIORow);
			clReleaseMemObject(matIOCol);
		}
	}

	final public int stCnt;

	final public int[] matIOColHost;
	final public int[] matIORowHost;
	final public int[] matIORowBegHost;

	final public int[] matNPColHost;
	final public int[] matNPRowHost;
	final public int[] matNPRowBegHost;

	final public int matIORowCnt;
	public cl_mem matIOCol;
	public cl_mem matIORow;
	public cl_mem matIORowBeg;
	final public boolean enabledMatIO;

	final public int matNPRowCnt;
	public cl_mem matNPCol;
	public cl_mem matNPRow;
	public cl_mem matNPRowBeg;
	final public boolean enabledMatNP;
}
