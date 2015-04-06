package pse;

public interface PSEMult
{
	public void setMult(final double min[], final double max[]);
	public void getMult(final double resMin[], final double resMax[]);
	public void mult(final int iterationCnt);
	public void getSum(final double[] sumMin, final double[] sumMax);
	public void setSum(final double[] sumMin, final double[] sumMax);
}
