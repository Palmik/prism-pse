package pse;

import java.util.BitSet;

final public class PSEMultUtility
{
	public final static PSEFoxGlynn getFoxGlynnVM(PSEModel model)
	{
		return null;
	}

	public final static PSEFoxGlynn getFoxGlynnMV(PSEModel model, BitSet modelSubset, boolean modelSubsetComplement)
	{
		return null;
	}

	private final static PSEMultManager getMultManagerVM(PSEMultOptions options, PSEModel model)
	{
		if (options.getOcl()) {
			return new PSEVMMultManager_OCL(model);
		} else {
			return new PSEVMMultManager_CPU(model);
		}
	}

	private final static PSEMultManager getMultManagerMV(PSEMultOptions options, PSEModel model, BitSet modelSubset, boolean modelSubsetComplement)
	{
		if (options.getOcl()) {
			return new PSEMVMultManager_OCL(model, modelSubset, modelSubsetComplement);
		} else {
			return new PSEMVMultManager_CPU(model, modelSubset, modelSubsetComplement);
		}
	}

	private final static PSEMultManyManager getMultManyManagerVM(PSEMultOptions options, PSEModel model)
	{
		if (options.getOcl()) {
			return new PSEVMMultManyManager_OCL(model);
		}
		return null;
	}

	private final static PSEMultManyManager getMultManyManagerMV(PSEMultOptions options, PSEModel model, BitSet modelSubset, boolean modelSubsetComplement)
	{
		if (options.getOcl()) {
			return new PSEMVMultManyManager_OCL(model, modelSubset, modelSubsetComplement);
		}
		return null;
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

	private static boolean getOptAdaptiveFoxGlynn()
	{
		String envOCL = System.getenv("PSE_ADAPTIVE_FOX_GLYNN");
		return (envOCL != null && envOCL.equals("1"));
	}
}
