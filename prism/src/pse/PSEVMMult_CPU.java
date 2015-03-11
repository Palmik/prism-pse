package pse;

import java.util.Arrays;

public final class PSEVMMult_CPU implements PSEMult, Releaseable
{
    public PSEVMMult_CPU
		( PSEVMCreateData_CSR data

        , double[] weight
        , double weightDef
        , int weightOff
        )
    {
	    this.totalIterationCnt = 0;

        this.stCnt = data.stCnt;

        this.matIOLowerVal0 = data.matIOLowerVal0;
	    this.matIOLowerVal1 = data.matIOLowerVal1;
	    this.matIOUpperVal0 = data.matIOUpperVal0;
	    this.matIOUpperVal1 = data.matIOUpperVal1;
        this.matIOSrc = data.matIOSrc;
        this.matIOTrgBeg = data.matIOTrgBeg;

        this.matOMinDiagVal = data.matOMinDiagVal;
	    this.matOMaxDiagVal = data.matOMaxDiagVal;
        this.matIMinVal = data.matIMinVal;
	    this.matIMaxVal = data.matIMaxVal;
        this.matISrc = data.matISrc;
        this.matITrgBeg = data.matITrgBeg;

        this.matNPVal = data.matNPVal;
        this.matNPSrc = data.matNPSrc;
        this.matNPTrgBeg = data.matNPTrgBeg;

	    this.weight = weight;
	    this.weightDef = weightDef;
	    this.weightOff = weightOff;
	    this.sumMin = new double[stCnt];
	    this.sumMax = new double[stCnt];
		this.min = new double[stCnt];
		this.max = new double[stCnt];
		this.resMin = new double[stCnt];
		this.resMax = new double[stCnt];

	    this.enabledMatNP = matNPTrgBeg[stCnt] > 0;
	    this.enabledMatIO = matIOTrgBeg[stCnt] > 0;
	    this.enabledMatI = matITrgBeg[stCnt] > 0;
    }

	final public void update(PSEVMCreateData_CSR data)
	{
		totalIterationCnt = 0;

		Arrays.fill(sumMin, 0);
		Arrays.fill(sumMax, 0);

		this.matIOLowerVal0 = data.matIOLowerVal0;
		this.matIOLowerVal1 = data.matIOLowerVal1;
		this.matIOUpperVal0 = data.matIOUpperVal0;
		this.matIOUpperVal1 = data.matIOUpperVal1;

		this.matOMinDiagVal = data.matOMinDiagVal;
		this.matOMaxDiagVal = data.matOMaxDiagVal;
		this.matIMinVal = data.matIMinVal;
		this.matIMaxVal = data.matIMaxVal;

		this.matNPVal = data.matNPVal;
	}

	@Override
	final public void release()
	{

	}

	@Override
	final public void getSum(final double[] sumMin, final double[] sumMax)
	{
		if (enabledMatI || enabledMatIO) {
			System.arraycopy(this.sumMin, 0, sumMin, 0, sumMin.length);
			System.arraycopy(this.sumMax, 0, sumMax, 0, sumMax.length);
		}
		else {
			System.arraycopy(this.sumMin, 0, sumMin, 0, sumMin.length);
			System.arraycopy(this.sumMin, 0, sumMax, 0, sumMax.length);
		}
	}

	@Override
	final public void setSum(final double[] sumMin, final double[] sumMax)
	{
		System.arraycopy(sumMin, 0, this.sumMin, 0, sumMin.length);
		System.arraycopy(sumMax, 0, this.sumMax, 0, sumMax.length);
	}

	@Override
	final public void setMult(final double[] min, final double[] max)
	{
		System.arraycopy(min, 0, this.min, 0, min.length);
		System.arraycopy(max, 0, this.max, 0, max.length);
	}

	@Override
	final public void getMult(final double[] resMin, final double[] resMax)
	{
		if (enabledMatI || enabledMatIO) {
			System.arraycopy(this.resMin, 0, resMin, 0, resMin.length);
			System.arraycopy(this.resMax, 0, resMax, 0, resMax.length);
		} else {
			System.arraycopy(this.resMin, 0, resMin, 0, resMin.length);
			System.arraycopy(this.resMin, 0, resMax, 0, resMax.length);
		}
	}

	@Override
	final public void mult(int iterationCnt)
	{
		// This if could be jut around the sum code, but we want the loop as tight as possible.
		if (enabledMatI || enabledMatIO) {
			for (int i = 0; i < iterationCnt; ++i) {
				mult(min, resMin, max, resMax);
				++totalIterationCnt;
				if (getSumWeight() != 0) {
					for (int j = 0; j < stCnt; ++j) {
						sumMin[j] += getSumWeight() * resMin[j];
						sumMax[j] += getSumWeight() * resMax[j];
					}
				}
				final double[] tmp1 = resMin;
				final double[] tmp2 = resMax;
				resMin = min;
				resMax = max;
				min = tmp1;
				max = tmp2;
			}
		}
		else {
			for (int i = 0; i < iterationCnt; ++i) {
				mult(min, resMin, max, resMax);
				++totalIterationCnt;
				if (getSumWeight() != 0) {
					for (int j = 0; j < stCnt; ++j) {
						sumMin[j] += getSumWeight() * resMin[j];
					}
				}
				final double[] tmp1 = resMin;
				final double[] tmp2 = resMax;
				resMin = min;
				resMax = max;
				min = tmp1;
				max = tmp2;
			}
		}
	}

	@Override
    final public void mult
        ( double min[], double resMin[]
        , double max[], double resMax[]
        , int iterationCnt
        )
    {
		setMult(min, max); mult(iterationCnt); getMult(resMin, resMax);
    }

	final private void mult
		( final double min[], final double resMin[]
		, final double max[], final double resMax[]
		)
    {
	    if (enabledMatNP) {
	        PSE_VM_NP(stCnt, matOMinDiagVal, matNPVal, matNPSrc, matNPTrgBeg, min, resMin);
		    if (enabledMatI || enabledMatIO) {
			    PSE_VM_NP(stCnt, matOMaxDiagVal, matNPVal, matNPSrc, matNPTrgBeg, max, resMax);
		    }
		} else {
			PSE_VM_DIAG(stCnt, matOMinDiagVal, min, resMin);
			if (enabledMatI || enabledMatIO) {
				PSE_VM_DIAG(stCnt, matOMaxDiagVal, max, resMax);
			}
		}

	    if (enabledMatIO) {
		    PSE_VM_IO(stCnt
			    , matIOLowerVal0, matIOLowerVal1
			    , matIOUpperVal0, matIOUpperVal1
		        , matIOSrc
			    , matIOTrgBeg
			    , min
			    , resMin
		        );
		    PSE_VM_IO(stCnt
			    , matIOUpperVal0, matIOUpperVal1
			    , matIOLowerVal0, matIOLowerVal1
			    , matIOSrc
			    , matIOTrgBeg
			    , max
			    , resMax
		    );
	    }
	    if (enabledMatI) {
		   PSE_VM_I(stCnt, matIMinVal, matISrc, matITrgBeg, min, resMin);
		   PSE_VM_I(stCnt, matIMaxVal, matISrc, matITrgBeg, max, resMax);
	    }
    }

	private void PSE_VM_DIAG
		( final int matRowCnt
		, final double[] matDiaVal
		, final double[] in
		, final double[] out
		)
	{
		for (int v0 = 0; v0 < matRowCnt; ++v0) {
			out[v0] = in[v0] * matDiaVal[v0];
		}
	}

	private void PSE_VM_I
		( final int matRowCnt
		, final double[] matVal
		, final int[] matCol
		, final int[] matRowBeg

		, final double[] in
		, final double[] out
		)
	{
		for (int v0 = 0; v0 < matRowCnt; ++v0) {
			int cb = matRowBeg[v0];
			int ce = matRowBeg[v0 + 1];
			double dot = out[v0];
			for (int i = cb; i < ce; ++i) {
				dot += matVal[i] * in[matCol[i]];
			}
			out[v0] = dot;
		}
	}

	private void PSE_VM_NP
		( final int matRowCnt
		, final double[] matDiaVal
		, final double[] matVal
		, final int[] matCol
		, final int[] matRowBeg

		, final double[] in
		, final double[] out
		)
	{
		for (int v0 = 0; v0 < matRowCnt; ++v0) {
			double dot = in[v0] * matDiaVal[v0]; //out[v0] + in[v0] * matDiaVal[v0];

			int cb = matRowBeg[v0];
			int ce = matRowBeg[v0 + 1];
			for (int i = cb; i < ce; ++i) {
				dot += matVal[i] * in[matCol[i]];
			}
			out[v0] = dot;
		}
	}

	private void PSE_VM_IO
		( final int matRowCnt
		, final double[] matLowerVal0
		, final double[] matLowerVal1
		, final double[] matUpperVal0
		, final double[] matUpperVal1
		, final int[] matCol
		, final int[] matRowBeg

		, final double[] in
		, final double[] out
		)
	{
		for (int v1 = 0; v1 < matRowCnt; ++v1) {
			double dot = out[v1];

			int cb = matRowBeg[v1];
			int ce = matRowBeg[v1 + 1];

			for (int i = cb; i < ce; ++i) {
				final int v0 = matCol[i];
				final double rlower = (matLowerVal0[i] * in[v0] - matLowerVal1[i] * in[v1]);
				if (rlower > 0.0) {
					dot += rlower;
				} else {
					dot += (matUpperVal0[i] * in[v0] - matUpperVal1[i] * in[v1]);
				}
			}
			out[v1] = dot;
		}
	}

	final private double getSumWeight()
	{
		if (totalIterationCnt >= weightOff)
		{
			return weight[totalIterationCnt - weightOff];
		}
		return weightDef;
	}

	final private int stCnt;

    private double[] matIOLowerVal0;
	private double[] matIOLowerVal1;
	private double[] matIOUpperVal0;
	private double[] matIOUpperVal1;

	private int[] matIOSrc;
	private int[] matIOTrgBeg;

    private double[] matOMinDiagVal;
	private double[] matOMaxDiagVal;
    private double[] matIMinVal;
	private double[] matIMaxVal;
    final private int[] matISrc;
    final private int[] matITrgBeg;

    private double[] matNPVal;
    final private int[] matNPSrc;
    final private int[] matNPTrgBeg;

	private int totalIterationCnt;
	final private double[] weight;
	final private double weightDef;
	final private int weightOff;
	final private double[] sumMin;
	final private double[] sumMax;
	private double[] min;
	private double[] max;
	private double[] resMin;
	private double[] resMax;

	final private boolean enabledMatNP;
	final private boolean enabledMatIO;
	final private boolean enabledMatI;
}