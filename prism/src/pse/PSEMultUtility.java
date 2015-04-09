package pse;

final public class PSEMultUtility
{
	public static PSEMultOptions getOptions()
	{
		return new PSEMultOptions(getOptOcl(), getOptPara(), getOptMany());
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
		return Integer.parseUnsignedInt(envPARA);
	}

	private static int getOptMany()
	{
		String envMANY = System.getenv("PSE_MANY");
		if (envMANY == null) {
			return 0;
		}
		return Integer.parseUnsignedInt(envMANY);
	}

}
