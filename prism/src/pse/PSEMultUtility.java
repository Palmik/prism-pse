package pse;

import prism.Pair;
import prism.PrismLog;

import java.util.BitSet;

final public class PSEMultUtility
{
	public final static Pair<PSEFoxGlynn, Releaseable> getFoxGlynnVM(PSEMultOptions options,
		PSEModel model, int iterStep, PrismLog prismLog)
	{
		if (options.getMany() > 0) {
			PSEMultManyManager manager = getMultManyManagerVM(options, model);
			return new Pair<PSEFoxGlynn, Releaseable>(new PSEFoxGlynnMany(options, model, manager, iterStep, prismLog), manager);
		} else if (options.getPara() > 0) {
			PSEMultManager manager = getMultManagerVM(options, model);
			return new Pair<PSEFoxGlynn, Releaseable>(new PSEFoxGlynnParallel(options, model, manager, iterStep, prismLog), manager);
		} else {
			PSEMultManager manager = getMultManagerVM(options, model);
			return new Pair<PSEFoxGlynn, Releaseable>(new PSEFoxGlynnSimple(options, model, manager, iterStep, prismLog), manager);
		}
	}

	public final static Pair<PSEFoxGlynn, Releaseable> getFoxGlynnMV(PSEMultOptions options,
		PSEModel model, BitSet modelSubset, boolean modelSubsetComplement, int iterStep, PrismLog prismLog)
	{
		if (options.getMany() > 0) {
			PSEMultManyManager manager = getMultManyManagerMV(options, model, modelSubset, modelSubsetComplement);
			return new Pair<PSEFoxGlynn, Releaseable>(new PSEFoxGlynnMany(options, model, manager, iterStep, prismLog), manager);
		} else if (options.getPara() > 0) {
			PSEMultManager manager = getMultManagerMV(options, model, modelSubset, modelSubsetComplement);
			return new Pair<PSEFoxGlynn, Releaseable>(new PSEFoxGlynnParallel(options, model, manager, iterStep, prismLog), manager);
		} else {
			PSEMultManager manager = getMultManagerMV(options, model, modelSubset, modelSubsetComplement);
			return new Pair<PSEFoxGlynn, Releaseable>(new PSEFoxGlynnSimple(options, model, manager, iterStep, prismLog), manager);
		}
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
