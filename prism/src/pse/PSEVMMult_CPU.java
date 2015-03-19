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
				final double sumWeight = getSumWeight();
				if (sumWeight != 0.0) {
					for (int j = 0; j < stCnt; ++j) {
						sumMin[j] += sumWeight * resMin[j];
						sumMax[j] += sumWeight * resMax[j];
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
				final double sumWeight = getSumWeight();
				if (sumWeight != 0.0) {
					for (int j = 0; j < stCnt; ++j) {
						sumMin[j] += sumWeight * resMin[j];
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
			if (enabledMatI || enabledMatIO) {
				PSE_VM_NP_BOTH(stCnt, matOMinDiagVal, matOMaxDiagVal, matNPVal, matNPSrc, matNPTrgBeg, min, max, resMin, resMax);
			} else {
				PSE_VM_NP(stCnt, matOMinDiagVal, matNPVal, matNPSrc, matNPTrgBeg, min, resMin);
			}
		} else {
			if (enabledMatI || enabledMatIO) {
				PSE_VM_DIAG_BOTH(stCnt, matOMinDiagVal, matOMaxDiagVal, min, max, resMin, resMax);
			} else {
				PSE_VM_DIAG(stCnt, matOMinDiagVal, min, resMin);
			}
		}

	    if (enabledMatIO) {
		    PSE_VM_IO(stCnt
			    , matIOLowerVal0, matIOLowerVal1
			    , matIOUpperVal0, matIOUpperVal1
		        , matIOSrc
			    , matIOTrgBeg
			    , min
				, max
			    , resMin
				, resMax
		        );
	    }

	    if (enabledMatI) {
		   PSE_VM_I(stCnt, matIMinVal, matIMaxVal, matISrc, matITrgBeg, min, max, resMin, resMax);
	    }
    }

	final private void PSE_VM_I
		( final int matRowCnt
		, final double[] matVal1
		, final double[] matVal2
		, final int[] matCol
		, final int[] matRowBeg

		, final double[] in1
		, final double[] in2
		, final double[] out1
		, final double[] out2
		)
	{
		for (int v0 = 0; v0 < matRowCnt; ++v0) {
			int cb = matRowBeg[v0];
			int ce = matRowBeg[v0 + 1];
			double dot1 = out1[v0];
			double dot2 = out2[v0];
			for (int i = cb; i < ce; ++i) {
				dot1 += matVal1[i] * in1[matCol[i]];
				dot2 += matVal2[i] * in2[matCol[i]];
			}
			out1[v0] = dot1;
			out2[v0] = dot2;
		}
	}

	final private void PSE_VM_DIAG_BOTH
		( final int matRowCnt
		, final double[] matDiaVal1
		, final double[] matDiaVal2
		, final double[] in1
		, final double[] in2
		, final double[] out1
		, final double[] out2
		)
	{
		for (int v0 = 0; v0 < matRowCnt; ++v0) {
			out1[v0] = in1[v0] * matDiaVal1[v0];
			out2[v0] = in2[v0] * matDiaVal2[v0];
		}
	}

	final private void PSE_VM_DIAG
		( final int matRowCnt
		, final double[] matDiaVal1
		, final double[] in1
		, final double[] out1
		)
	{
		for (int v0 = 0; v0 < matRowCnt; ++v0) {
			out1[v0] = in1[v0] * matDiaVal1[v0];
		}
	}

	final private void PSE_VM_NP_BOTH
		( final int matRowCnt
		, final double[] matDiaVal1
		, final double[] matDiaVal2
		, final double[] matVal
		, final int[] matCol
		, final int[] matRowBeg

		, final double[] in1
		, final double[] in2
		, final double[] out1
		, final double[] out2
		)
	{
		for (int v0 = 0; v0 < matRowCnt; ++v0) {
			double dot1 = in1[v0] * matDiaVal1[v0]; //out[v0] + in[v0] * matDiaVal[v0];
			double dot2 = in2[v0] * matDiaVal2[v0]; //out[v0] + in[v0] * matDiaVal[v0];

			int cb = matRowBeg[v0];
			int ce = matRowBeg[v0 + 1];
			for (int i = cb; i < ce; ++i) {
				dot1 += matVal[i] * in1[matCol[i]];
				dot2 += matVal[i] * in2[matCol[i]];
			}
			out1[v0] = dot1;
			out2[v0] = dot2;
		}
	}

	final private void PSE_VM_NP
		( final int matRowCnt
			, final double[] matDiaVal1
			, final double[] matVal
			, final int[] matCol
			, final int[] matRowBeg

			, final double[] in1
			, final double[] out1
		)
	{
		for (int v0 = 0; v0 < matRowCnt; ++v0) {
			double dot1 = in1[v0] * matDiaVal1[v0]; //out[v0] + in[v0] * matDiaVal[v0];

			int cb = matRowBeg[v0];
			int ce = matRowBeg[v0 + 1];
			for (int i = cb; i < ce; ++i) {
				dot1 += matVal[i] * in1[matCol[i]];
			}
			out1[v0] = dot1;
		}
	}

	final private void PSE_VM_IO
		( final int matRowCnt
		, final double[] matLowerVal0
		, final double[] matLowerVal1
		, final double[] matUpperVal0
		, final double[] matUpperVal1
		, final int[] matCol
		, final int[] matRowBeg

		, final double[] in1
		, final double[] in2
		, final double[] out1
		, final double[] out2
		)
	{
		for (int v1 = 0; v1 < matRowCnt; ++v1) {
			double dot1 = out1[v1];
			double dot2 = out2[v1];

			int cb = matRowBeg[v1];
			int ce = matRowBeg[v1 + 1];

			for (int i = cb; i < ce; ++i) {
				final int v0 = matCol[i];
				final double diff1 = (matLowerVal0[i] * in1[v0] - matLowerVal1[i] * in1[v1]);
				final double diff2 = (matUpperVal0[i] * in2[v0] - matUpperVal1[i] * in2[v1]);
				if (diff1 > 0.0) {
					dot1 += diff1;
				} else {
					dot1 += (matUpperVal0[i] * in1[v0] - matUpperVal1[i] * in1[v1]);
				}
				if (diff2 > 0.0) {
					dot2 += diff2;
				} else {
					dot2 += (matLowerVal0[i] * in2[v0] - matLowerVal1[i] * in2[v1]);
				}
			}
			out1[v1] = dot1;
			out2[v1] = dot2;
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