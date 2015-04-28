package pse;

import prism.Pair;
import prism.PrismLog;

import java.util.BitSet;

final public class PSEMultUtility
{
	public static void zipArray(double[] a, double[] b, double[] z)
	{
		assert a.length == b.length;
		assert z.length == a.length * 2;
		for (int i = 0; i < a.length; ++i) {
			z[i * 2] = a[i];
			z[i * 2 + 1] = b[i];
		}
	}

	public static void unzipArray(double[] z, double[] a, double[] b)
	{
		assert a.length == b.length;
		assert z.length == a.length * 2;
		for (int i = 0; i < a.length; ++i) {
			a[i] = z[i * 2];
			b[i] = z[i * 2 + 1];
		}
	}

	public static PSEMultOptions getOptions()
	{
		return new PSEMultOptions(getOptOcl(), getOptPara(), getOptMany(), getOptAdaptiveFoxGlynn());
	}

	public static void weightedSumToBoth(
		final int n, final double w,
		final double[] inMin, final double[] inMax,
		final double[] sumMin, final double[] sumMax)
	{
		if (w != 0) {
			for (int j = 0; j < n; ++j) {
				sumMin[j] += w * inMin[j];
				sumMax[j] += w * inMax[j];
			}
		}
	}

	public static void weightedSumTo(final int n, final double w, final double[] in, final double[] sum)
	{
		if (w != 0) {
			for (int j = 0; j < n; ++j) {
				sum[j] += w * in[j];
			}
		}
	}

	private static boolean getOptOcl()
	{
		String envOCL = System.getenv("PSE_OCL");
		return (envOCL != null && envOCL.equals("1"));
	}

	private static int getOptPara()
	{
		String envPARA = System.getenv("PSE_PARA");
		if (envPARA == null) {
			return 0;
		}
		return Integer.parseInt(envPARA);
	}

	private static int getOptMany()
	{
		String envMANY = System.getenv("PSE_MANY");
		if (envMANY == null) {
			return 0;
		}
		return Integer.parseInt(envMANY);
	}

	private static boolean getOptAdaptiveFoxGlynn()
	{
		String envOCL = System.getenv("PSE_ADAPTIVE_FOX_GLYNN");
		return (envOCL != null && envOCL.equals("1"));
	}
}
