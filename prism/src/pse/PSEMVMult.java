package pse;

/**
 * Created by palmik on 24/12/14.
 */
public interface PSEMVMult
{
	public void mvMult(final double min[], final double resMin[], final double max[], final double resMax[], final int iterationCnt);
	public void update(double[] matIOLowerVal, double[] matIOUpperVal, double[] matNPVal);
	public void getSum(final double[] sumMin, final double[] sumMax);
}
