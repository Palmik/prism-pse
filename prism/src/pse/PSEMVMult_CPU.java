package pse;

public final class PSEMVMult_CPU implements PSEMVMult
{
	public PSEMVMult_CPU
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

		, double[] weight
		, double weightDef
		, int weightOff
		)
	{
		this.totalIterationCnt = 0;

		this.stCnt = stCnt;
		this.matIOLowerVal = matIOLowerVal;
		this.matIOUpperVal = matIOUpperVal;
		this.matIOCol = matIOCol;
		this.matIORow = matIORow;
		this.matIORowBeg = matIORowBeg;
		this.matIORowCnt = matIORowCnt;
		this.hasIOMat = matIORowCnt > 0 && matIORowBeg[matIORowCnt] > 0;

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

	@Override
	final public void mvMult
		( double min[], double resMin[]
		, double max[], double resMax[]
		, int iterationCnt
		)
	{
		for (int i = 0; i < iterationCnt; ++i) {
			mvMult(min, resMin, max, resMax);
			++totalIterationCnt;
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

	@Override
	final public void getSum(final double[] sumMin, final double[] sumMax)
	{
		System.arraycopy(this.sumMin, 0, sumMin, 0, sumMin.length);
		System.arraycopy(this.sumMax, 0, sumMax, 0, sumMax.length);
	}


	@Override
	public void update(final double[] matIOLowerVal, final double[] matIOUpperVal, final double[] matNPVal)
	{
		this.matIOLowerVal = matIOLowerVal;
		this.matIOUpperVal = matIOUpperVal;
		this.matNPVal = matNPVal;
	}

	final private void mvMult
		( final double min[], final double resMin[]
		, final double max[], final double resMax[]
		)
	{
		System.arraycopy(min, 0, resMin, 0, resMin.length);
		System.arraycopy(max, 0, resMax, 0, resMax.length);

		if (hasIOMat) {
			for (int ii = 0; ii < matIORowCnt; ++ii) {
				final int v0 = matIORow[ii];
				final int tb = matIORowBeg[ii];
				final int te = matIORowBeg[ii + 1];
				for (int jj = tb; jj < te; ++jj) {
					final int v1 = matIOCol[jj];
					if (min[v1] > min[v0]) {
						resMin[v0] += matIOLowerVal[jj] * (min[v1] - min[v0]);
					} else {
						resMin[v0] += matIOUpperVal[jj] * (min[v1] - min[v0]);
					}
					if (max[v1] > max[v0]) {
						resMax[v0] += matIOUpperVal[jj] * (max[v1] - max[v0]);
					} else {
						resMax[v0] += matIOLowerVal[jj] * (max[v1] - max[v0]);
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

	private double[] matIOLowerVal;
	private double[] matIOUpperVal;
	final private int[] matIOCol;
	final private int[] matIORow;
	final private int[] matIORowBeg;
    final private int matIORowCnt;
	final private boolean hasIOMat;

	private double[] matNPVal;
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
