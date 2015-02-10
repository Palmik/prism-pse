package pse;

import java.util.Arrays;

public final class PSEMVMult_CPU implements PSEMult, Releaseable
{
	public PSEMVMult_CPU
		( PSEMVCreateData_CSR data

		, double[] weight
		, double weightDef
		, int weightOff
		)
	{
		this.totalIterationCnt = 0;

		this.stCnt = data.stCnt;
		this.matIOLowerVal = data.matIOLowerVal;
		this.matIOUpperVal = data.matIOUpperVal;
		this.matIOCol = data.matIOCol;
		this.matIORow = data.matIORow;
		this.matIORowBeg = data.matIORowBeg;
		this.matIORowCnt = data.matIORowCnt;

		this.matNPVal = data.matNPVal;
		this.matNPCol = data.matNPCol;
		this.matNPRow = data.matNPRow;
		this.matNPRowBeg = data.matNPRowBeg;
		this.matNPRowCnt = data.matNPRowCnt;

		this.enabledMatIO = data.matIORowCnt > 0 && data.matIORowBeg[data.matIORowCnt] > 0;
		this.enabledMatNP = data.matNPRowCnt > 0 && data.matNPRowBeg[data.matNPRowCnt] > 0;

		this.weight = weight;
		this.weightDef = weightDef;
		this.weightOff = weightOff;
		this.sumMin = new double[stCnt];
		this.sumMax = new double[stCnt];
		this.min = new double[stCnt];
		this.max = new double[stCnt];
		this.resMin = new double[stCnt];
		this.resMax = new double[stCnt];
	}

	@Override
	final public void getMult(final double[] resMin, final double[] resMax)
	{
		System.arraycopy(this.resMin, 0, resMin, 0, resMin.length);
		System.arraycopy(this.resMax, 0, resMax, 0, resMax.length);
	}

	@Override
	final public void setMult(final double[] min, final double[] max)
	{
		System.arraycopy(min, 0, this.min, 0, min.length);
		System.arraycopy(max, 0, this.max, 0, max.length);
	}

	@Override
	final public void mult(int iterationCnt)
	{
		for (int i = 0; i < iterationCnt; ++i) {
			System.arraycopy(min, 0, resMin, 0, resMin.length);
			System.arraycopy(max, 0, resMax, 0, resMax.length);

			if (enabledMatIO) {
				for (int ii = 0; ii < matIORowCnt; ++ii) {
					final int v0 = matIORow[ii];
					final int tb = matIORowBeg[ii];
					final int te = matIORowBeg[ii + 1];
					for (int jj = tb; jj < te; ++jj) {
						final int v1 = matIOCol[jj];
						final double diffMin = min[v1] - min[v0];
						final double diffMax = max[v1] - max[v0];
						if (diffMin > 0) {
							resMin[v0] += matIOLowerVal[jj] * diffMin;
						} else {
							resMin[v0] += matIOUpperVal[jj] * diffMin;
						}
						if (diffMax > 0) {
							resMax[v0] += matIOUpperVal[jj] * diffMax;
						} else {
							resMax[v0] += matIOLowerVal[jj] * diffMax;
						}
					}
				}
			}

			if (enabledMatNP) {
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

			++totalIterationCnt;

			final double sumWeight = getSumWeight();
			if (sumWeight != 0) {
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

	@Override
	final public void mult
		( double min[], double resMin[]
		, double max[], double resMax[]
		, int iterationCnt
		)
	{
		setMult(min, max); mult(iterationCnt); getMult(resMin, resMax);
	}

	@Override
	final public void getSum(final double[] sumMin, final double[] sumMax)
	{
		System.arraycopy(this.sumMin, 0, sumMin, 0, sumMin.length);
		System.arraycopy(this.sumMax, 0, sumMax, 0, sumMax.length);
	}

	@Override
	final public void setSum(final double[] sumMin, final double[] sumMax)
	{
		System.arraycopy(sumMin, 0, this.sumMin, 0, sumMin.length);
		System.arraycopy(sumMax, 0, this.sumMax, 0, sumMax.length);
	}

	public void update(PSEMVCreateData_CSR data)
	{
		this.totalIterationCnt = 0;
		Arrays.fill(sumMin, 0);
		Arrays.fill(sumMax, 0);
		this.matIOLowerVal = data.matIOLowerVal;
		this.matIOUpperVal = data.matIOUpperVal;
		this.matNPVal = data.matNPVal;
	}

	@Override
	public void release()
	{
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
	final private boolean enabledMatIO;

	private double[] matNPVal;
	final private int[] matNPCol;
	final private int[] matNPRow;
	final private int[] matNPRowBeg;
	final private int matNPRowCnt;
	final private boolean enabledMatNP;

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
}
