package pse;

import org.jocl.*;
import static org.jocl.CL.*;

final public class PSEMVMultTopology_OCL
{
	public PSEMVMultTopology_OCL
		( int[] matIOCol
		, int[] matIORow
		, int[] matIORowBeg
		, int matIORowCnt

		, int[] matNPCol
		, int[] matNPRow
		, int[] matNPRowBeg
		, int matNPRowCnt

		, cl_context clContext
		)
	{
		this.matIOColHost = matIOCol;
		this.matIORowHost = matIORow;
		this.matIORowBegHost = matIORowBeg;
		this.matIORowCnt = matIORowCnt;

		this.matNPColHost = matNPCol;
		this.matNPRowHost = matNPRow;
		this.matNPRowBegHost = matNPRowBeg;
		this.matNPRowCnt = matNPRowCnt;

		this.enabledMatIO = matIORowCnt > 0 && matIORowBeg[matIORowCnt] > 0;
		this.enabledMatNP = matNPRowCnt > 0 && matNPRowBeg[matNPRowCnt] > 0;

		if (enabledMatIO) {
			final Pointer matIOCol_ = Pointer.to(matIOCol);
			final Pointer matIORow_ = Pointer.to(matIORow);
			final Pointer matIORowBeg_ = Pointer.to(matIORowBeg);

			this.matIOCol = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * matIORowBeg[matIORowCnt], matIOCol_, null);
			this.matIORow = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * matIORowCnt, matIORow_, null);
			this.matIORowBeg = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * (matIORowCnt + 1), matIORowBeg_, null);
		}

		if (enabledMatNP) {
			final Pointer matNPCol_ = Pointer.to(matNPCol);
			final Pointer matNPRow_ = Pointer.to(matNPRow);
			final Pointer matNPRowBeg_ = Pointer.to(matNPRowBeg);

			this.matNPCol = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * matNPRowBeg[matNPRowCnt], matNPCol_, null);
			this.matNPRow = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * matNPRowCnt, matNPRow_, null);
			this.matNPRowBeg = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_uint * (matNPRowCnt + 1), matNPRowBeg_, null);
		}
	}

	public final int[] matIOColHost;
	public final int[] matIORowHost;
	public final int[] matIORowBegHost;

	public final int[] matNPColHost;
	public final int[] matNPRowHost;
	public final int[] matNPRowBegHost;

	public final int matIORowCnt;
	public cl_mem matIOCol;
	public cl_mem matIORow;
	public cl_mem matIORowBeg;
	public final boolean enabledMatIO;

	public final int matNPRowCnt;
	public cl_mem matNPCol;
	public cl_mem matNPRow;
	public cl_mem matNPRowBeg;
	public final boolean enabledMatNP;
}
