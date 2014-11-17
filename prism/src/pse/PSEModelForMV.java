package pse;

public final class PSEModelForMV
{
	public PSEModelForMV
	( int stCnt

	, double[] matLowerVal
	, double[] matUpperVal
	, int[] matCol
	, int[] matRow
	, int[] matRowBeg
	, int matRowCnt

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
		this.totalIterationCnt = 0;

		this.stCnt = stCnt;
		this.matLowerVal = matLowerVal;
		this.matUpperVal = matUpperVal;
		this.matCol = matCol;
		this.matRow = matRow;
		this.matRowBeg = matRowBeg;
		this.matRowCnt = matRowCnt;
		this.hasMat = matRowCnt > 0 && matRowBeg[matRowCnt] > 0;

		this.matNPVal = matNPVal;
		this.matNPCol = matNPCol;
		this.matNPRow = matNPRow;
		this.matNPRowBeg = matNPRowBeg;
		this.matNPRowCnt = matNPRowCnt;
		this.hasNPMat = matNPRowCnt > 0 && matNPRowBeg[matNPRowCnt] > 0;

		this.weight = weight;
		this.weightDef = weightDef;
		this.weightOff = weightOff;
		this.sumMin = new double[stCnt];
		this.sumMax = new double[stCnt];
	}

	final public void mvMult
		( double min[], double resMin[]
		, double max[], double resMax[]
		, int iterationCnt
		)
	{
		for (int i = 0; i < iterationCnt; ++i) {
			mvMult(min, resMin, max, resMax);
			for (int j = 0; j < stCnt; ++j)
			{
				sumMin[j] += getSumWeight() * resMin[j];
				sumMax[j] += getSumWeight() * resMax[j];
			}

			final double[] tmp1 = resMin;
			final double[] tmp2 = resMax;
			resMin = min;
			resMax = max;
			min = tmp1;
			max = tmp2;
		}
	}

	final public void getSum(final double[] sumMin, final double[] sumMax)
	{
		System.arraycopy(this.sumMin, 0, sumMin, 0, sumMin.length);
		System.arraycopy(this.sumMax, 0, sumMax, 0, sumMax.length);
	}

	final private void mvMult
		( final double min[], final double resMin[]
		, final double max[], final double resMax[]
		)
	{
		System.arraycopy(min, 0, resMin, 0, resMin.length);
		System.arraycopy(max, 0, resMax, 0, resMax.length);

		if (hasMat) {
			for (int ii = 0; ii < matRowCnt; ++ii) {
				final int v0 = matRow[ii];
				final int tb = matRowBeg[ii];
				final int te = matRowBeg[ii + 1];
				for (int jj = tb; jj < te; ++jj) {
					final int v1 = matCol[jj];
					if (min[v1] > min[v0]) {
						resMin[v0] += matLowerVal[jj] * (min[v1] - min[v0]);
					} else {
						resMin[v0] += matUpperVal[jj] * (min[v1] - min[v0]);
					}
					if (max[v1] > max[v0]) {
						resMax[v0] += matUpperVal[jj] * (max[v1] - max[v0]);
					} else {
						resMax[v0] += matLowerVal[jj] * (max[v1] - max[v0]);
					}
				}
			}
		}

		if (hasNPMat) {
			for (int ii = 0; ii < matNPRowCnt; ++ii) {
				final int v0 = matNPRow[ii];
				final int tb = matNPRowBeg[ii];
				final int te = matNPRowBeg[ii + 1];
				for (int jj = tb; jj < te; ++jj) {
					final int v1 = matNPCol[jj];
					final double rate = matNPVal[jj];
					resMin[v0] += rate * (min[v1] - min[v0]);
					resMax[v0] += rate * (max[v1] - max[v0]);
				}
			}
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

	private int stCnt;

	final private double[] matLowerVal;
	final private double[] matUpperVal;
	final private int[] matCol;
	final private int[] matRow;
	final private int[] matRowBeg;
    final private int matRowCnt;
	final private boolean hasMat;

	final private double[] matNPVal;
	final private int[] matNPCol;
	final private int[] matNPRow;
	final private int[] matNPRowBeg;
	final private int matNPRowCnt;
	final private boolean hasNPMat;

	private int totalIterationCnt;
	final private double[] weight;
	final private double weightDef;
	final private int weightOff;
	final private double[] sumMin;
	final private double[] sumMax;
}
