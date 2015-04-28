package pse;

import java.util.ArrayList;
import java.util.Arrays;

public final class PSEVMMult_CPU implements PSEMult, Releaseable
{
    public PSEVMMult_CPU(PSEVMCreateData_CSR data)
    {
		this.stCnt = data.stCnt;
		this.totalIterationCnt = 0;

		this.enabledMatNP = data.enabledMatNP;
		this.enabledMatIO = data.enabledMatIO;
		this.enabledMatI = data.enabledMatI;
		this.enabledMatO = data.enabledMatO;

		this.enabledMatP = enabledMatI || enabledMatO || enabledMatIO;

        this.matIOValZip = data.matIOValZip;
        this.matIOSrc = data.matIOSrc;
        this.matIOTrgBeg = data.matIOTrgBeg;

		this.matODiagVal = data.matODiagVal;
        this.matIValZip = data.matIValZip;
        this.matISrc = data.matISrc;
        this.matITrgBeg = data.matITrgBeg;

        this.matNPVal = data.matNPVal;
        this.matNPSrc = data.matNPSrc;
        this.matNPTrgBeg = data.matNPTrgBeg;

		if (enabledMatP) {
			this.sum = new double[stCnt * 2];
			this.solRes = new double[stCnt * 2];
			this.sol = new double[stCnt * 2];
		} else {
			this.sum = new double[stCnt];
			this.solRes = new double[stCnt];
			this.sol = new double[stCnt];
		}
    }

	@Override
	final public void setWeight(double[] weight, double weightDef, int weightOff)
	{
		this.weight = weight;
		this.weightDef = weightDef;
		this.weightOff = weightOff;
	}

	final public void update(PSEVMCreateData_CSR data)
	{
		totalIterationCnt = 0;

		this.matODiagVal = data.matODiagVal;

		if (enabledMatNP) {
			this.matNPVal = data.matNPVal;
		}

		if (enabledMatIO) {
			this.matIOValZip = data.matIOValZip;
		}

		if (enabledMatI) {
			this.matIValZip = data.matIValZip;
		}

		Arrays.fill(sum, 0);
	}

	@Override
	final public void release()
	{
	}

	@Override
	final public void getSum(final double[] sumMin, final double[] sumMax)
	{
		if (enabledMatP) {
			for (int i = 0; i < stCnt; ++i) {
				sumMin[i] = sum[i * 2];
				sumMax[i] = sum[i * 2 + 1];
			}
		}
		else {
			System.arraycopy(this.sum, 0, sumMin, 0, sumMin.length);
			System.arraycopy(this.sum, 0, sumMax, 0, sumMax.length);
		}
	}

	@Override
	final public void setSum(final double[] sumMin, final double[] sumMax)
	{
		if (enabledMatP) {
			for (int i = 0; i < stCnt; ++i) {
				sum[i * 2] = sumMin[i];
				sum[i * 2 + 1] = sumMax[i];
			}
		} else {
			System.arraycopy(sumMin, 0, this.sum, 0, sumMin.length);
		}
	}

	@Override
	final public void getMult(final double[] resMin, final double[] resMax)
	{
		if (enabledMatP) {
			for (int i = 0; i < stCnt; ++i) {
				resMin[i] = sol[i * 2];
				resMax[i] = sol[i * 2 + 1];
			}
		}
		else {
			System.arraycopy(this.sol, 0, resMin, 0, resMin.length);
			System.arraycopy(this.sol, 0, resMax, 0, resMax.length);
		}
	}

	@Override
	final public void setMult(final double[] min, final double[] max)
	{
		if (enabledMatP) {
			for (int i = 0; i < stCnt; ++i) {
				sol[i * 2] = min[i];
				sol[i * 2 + 1] = max[i];
			}
		} else {
			System.arraycopy(min, 0, this.sol, 0, min.length);
		}
	}

	@Override
	final public void mult(int iterationCnt)
	{
		// This if could be jut around the sum code, but we want the loop as tight as possible.
		if (enabledMatI || enabledMatIO) {
			for (int i = 0; i < iterationCnt; ++i) {
				if (enabledMatNP) {
					PSE_VM_NP_BOTH(stCnt, matODiagVal, matNPVal, matNPSrc, matNPTrgBeg, sol, solRes);
				} else {
					PSE_VM_DIAG_BOTH(stCnt, matODiagVal, sol, solRes);
				}

				if (enabledMatIO) {
					PSE_VM_IO_BOTH(stCnt, matIOValZip, matIOSrc, matIOTrgBeg, sol, solRes);
				}

				if (enabledMatI) {
					PSE_VM_I_BOTH(stCnt, matIValZip, matISrc, matITrgBeg, sol, solRes);
				}

				++totalIterationCnt;
				PSEMultUtility.weightedSumTo(stCnt * 2, getSumWeight(), solRes, sum);

				swapSolMem();
			}
		} else {
			for (int i = 0; i < iterationCnt; ++i) {
				if (enabledMatNP) {
					PSE_VM_NP(stCnt, matODiagVal, matNPVal, matNPSrc, matNPTrgBeg, sol, solRes);
				} else {
					PSE_VM_DIAG(stCnt, matODiagVal, sol, solRes);
				}

				++totalIterationCnt;
				PSEMultUtility.weightedSumTo(stCnt, getSumWeight(), solRes, sum);

				swapSolMem();
			}
		}
	}

	final private void swapSolMem()
	{
		final double[] tmp1 = solRes;
		solRes = sol;
		sol = tmp1;
	}

	final static private void PSE_VM_DIAG_BOTH(final int matRowCnt,
		final double[] matDiaValZip,
		final double[] in,
		final double[] out)
	{
		for (int v0 = 0; v0 < matRowCnt; ++v0) {
			out[v0 * 2] = in[v0 * 2] * matDiaValZip[v0 * 2];
			out[v0 * 2 + 1] = in[v0 * 2 + 1] * matDiaValZip[v0 * 2 + 1];
		}
	}

	final static private void PSE_VM_DIAG(final int matRowCnt,
		final double[] matDiaVal,
		final double[] in, final double[] out)
	{
		for (int v0 = 0; v0 < matRowCnt; ++v0) {
			out[v0] = in[v0] * matDiaVal[v0];
		}
	}

	final static private void PSE_VM_NP_BOTH(final int matRowCnt,
		final double[] matDiaValZip, final double[] matVal,
		final int[] matCol, final int[] matRowBeg,
		final double[] in,
		final double[] out)
	{
		for (int v0 = 0; v0 < matRowCnt; ++v0) {
			double dotMin = in[v0 * 2] * matDiaValZip[v0 * 2]; //out[v0] + in[v0] * matDiaVal[v0];
			double dotMax = in[v0 * 2 + 1] * matDiaValZip[v0 * 2 + 1]; //out[v0] + in[v0] * matDiaVal[v0];

			final int cb = matRowBeg[v0];
			final int ce = matRowBeg[v0 + 1];
			for (int i = cb; i < ce; ++i) {
				dotMin += matVal[i] * in[matCol[i] * 2];
				dotMax += matVal[i] * in[matCol[i] * 2 + 1];
			}
			out[v0 * 2] = dotMin;
			out[v0 * 2 + 1] = dotMax;
		}
	}

	final static private void PSE_VM_NP(final int matRowCnt,
		final double[] matDiaVal, final double[] matVal,
		final int[] matCol, final int[] matRowBeg,
		final double[] in, final double[] out)
	{
		for (int v0 = 0; v0 < matRowCnt; ++v0) {
			double dot = in[v0] * matDiaVal[v0]; //out[v0] + in[v0] * matDiaVal[v0];

			final int cb = matRowBeg[v0];
			final int ce = matRowBeg[v0 + 1];
			for (int i = cb; i < ce; ++i) {
				dot += matVal[i] * in[matCol[i]];
			}
			out[v0] = dot;
		}
	}

	final static private void PSE_VM_IO_BOTH(final int matRowCnt,
		final double[] matValZip,
		final int[] matCol, final int[] matRowBeg,
		final double[] in,
		final double[] out)
	{
		for (int v1 = 0; v1 < matRowCnt; ++v1) {
			double dotMin = out[v1 * 2];
			double dotMax = out[v1 * 2 + 1];

			int cb = matRowBeg[v1];
			int ce = matRowBeg[v1 + 1];

			for (int i = cb; i < ce; ++i) {
				final int v0 = matCol[i];
				final double diff1 = (matValZip[i * 4] * in[v0 * 2] - matValZip[i * 4 + 1] * in[v1 * 2]);
				final double diff2 = (matValZip[i * 4 + 2] * in[v0 * 2 + 1] - matValZip[i * 4 + 3] * in[v1 * 2 + 1]);
				if (diff1 > 0.0) {
					dotMin += diff1;
				} else {
					dotMin += (matValZip[i * 4 + 2] * in[v0 * 2] - matValZip[i * 4 + 3] * in[v1 * 2]);
				}
				if (diff2 > 0.0) {
					dotMax += diff2;
				} else {
					dotMax += (matValZip[i * 4] * in[v0 * 2 + 1] - matValZip[i * 4 + 1] * in[v1 * 2 + 1]);
				}
			}
			out[v1 * 2] = dotMin;
			out[v1 * 2 + 1] = dotMax;
		}
	}

	final static private void PSE_VM_I_BOTH(final int matRowCnt,
		final double[] matValZip,
		final int[] matCol, final int[] matRowBeg,
		final double[] in,
		final double[] out)
	{
		for (int v0 = 0; v0 < matRowCnt; ++v0) {
			int cb = matRowBeg[v0];
			int ce = matRowBeg[v0 + 1];
			double dotMin = out[v0 * 2];
			double dotMax = out[v0 * 2 + 1];
			for (int i = cb; i < ce; ++i) {
				dotMin += matValZip[i * 2] * in[matCol[i] * 2];
				dotMax += matValZip[i * 2 + 1] * in[matCol[i] * 2 + 1];
			}
			out[v0 * 2] = dotMin;
			out[v0 * 2 + 1] = dotMax;
		}
	}

	final private double getSumWeight()
	{
		if (totalIterationCnt >= weightOff) {
			return weight[totalIterationCnt - weightOff];
		}
		return weightDef;
	}

	final private int stCnt;

	private double[] matIOValZip;

	private int[] matIOSrc;
	private int[] matIOTrgBeg;

	private double[] matIValZip;
    final private int[] matISrc;
    final private int[] matITrgBeg;

	private double[] matODiagVal;

	private double[] matNPVal;
    final private int[] matNPSrc;
    final private int[] matNPTrgBeg;

	private int totalIterationCnt;
	private double[] weight;
	private double weightDef;
	private int weightOff;
	final private double[] sum;
	private double[] sol;
	private double[] solRes;

	final private boolean enabledMatNP;
	final private boolean enabledMatIO;
	final private boolean enabledMatI;
	final private boolean enabledMatO;
	final private boolean enabledMatP;
}