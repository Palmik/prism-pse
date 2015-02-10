package pse;

/**
 * Created by palmik on 24/12/14.
 */
public interface PSEMult
{
	public void setMult(final double min[], final double max[]);
	public void getMult(final double resMin[], final double resMax[]);
	public void mult(final int iterationCnt);
	public void mult(final double min[], final double resMin[], final double max[], final double resMax[], final int iterationCnt);
	public void getSum(final double[] sumMin, final double[] sumMax);
	public void setSum(final double[] sumMin, final double[] sumMax);
}
