package pse;

import prism.PrismLog;

import java.util.BitSet;

public class Utility
{
    public static long leastGreaterMultiple(long x, long z)
    {
        return x + (z - x % z) % z;
    }
    public static int leastGreaterMultiple(int x, int z)
    {
        return x + (z - x % z) % z;
    }

    public static BitSet makeSubset(BitSet subset, boolean complement, int numStates)
    {
        if (subset == null) {
            subset = new BitSet(numStates);
            subset.set(complement ? 1 : 0, numStates);
            return subset;
        }

        subset = (BitSet)subset.clone();
        if (complement) {
            subset.flip(0, numStates);
        }
        return subset;
    }

    public static PSEMultManager makeMVMultManager(PSEModel model, BitSet modelSubset, boolean modelSubsetComplement)
    {
        String envOCL = System.getenv("OCL");
        boolean useOCL = envOCL != null && envOCL.equals("1");
        if (useOCL) {
            System.err.printf("OCL=1\n");
            return new PSEMVMultManager_OCL(model, modelSubset, modelSubsetComplement);
        } else {
            System.err.printf("OCL=0\n");
            return new PSEMVMultManager_CPU(model, modelSubset, modelSubsetComplement);
        }
    }

    public static PSEMultManager makeVMMultManager(PSEModel model)
    {
        String envOCL = System.getenv("OCL");
        boolean useOCL = envOCL != null && envOCL.equals("1");
        if (useOCL) {
			System.err.printf("OCL=1\n");
			return new PSEVMMultManager_OCL(model);
        } else {
			System.err.printf("OCL=0\n");
			return new PSEVMMultManager_CPU(model);
        }
    }

    public static PSEFoxGlynn makeFoxGlynn
		( PSEModel model
		, PSEMultManager multManager

		, double[] weight
		, double   weightDef
		, int      fgL
		, int      fgR

		, PrismLog log
        )
    {
        String envPAR = System.getenv("PAR");
        boolean usePAR = envPAR != null && envPAR.equals("1");
        if (usePAR) {
            System.err.printf("PAR=1\n");
            return new PSEFoxGlynnParallel(model, multManager, weight, weightDef, fgL, fgR, log);
        } else {
            System.err.printf("PAR=0\n");
            return new PSEFoxGlynnSimple(model, multManager, weight, weightDef, fgL, fgR, log);
        }
    }

}
