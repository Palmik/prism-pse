package pse;

import prism.PrismLog;

import java.util.BitSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static BitSet makeBitSetCopy(BitSet original, int n)
    {
        if (original == null) {
            original = new BitSet(n);
            original.set(0, n);
            return original;
        }
        return (BitSet)original.clone();
    }

    public static BitSet makeBitSetComplement(BitSet original, boolean complement, int n)
    {
        if (original == null) {
            original = new BitSet(n);
            original.set(complement ? 1 : 0, n);
            return original;
        }

        original = (BitSet)original.clone();
        if (complement) {
            original.flip(0, n);
        }
        return original;
    }

    public static PSEMultManager makeMVMultManager(PSEModel model, BitSet modelSubset, boolean modelSubsetComplement)
    {
        final int ocl = getOCL();
        if (ocl == 1) {
            System.err.printf("OCL=1\n");
            return new PSEMVMultManager_OCL(model, modelSubset, modelSubsetComplement);
        } else {
            System.err.printf("OCL=0\n");
            return new PSEMVMultManager_CPU(model, modelSubset, modelSubsetComplement);
        }
    }

    public static PSEMultManager makeVMMultManager(PSEModel model)
    {
        final int ocl = getOCL();
        if (ocl == 1) {
			System.err.printf("OCL=1\n");
			return new PSEVMMultManager_OCL(model);
        } else {
			System.err.printf("OCL=0\n");
			return new PSEVMMultManager_CPU(model);
        }
    }

    public static PSEMultManyManager makeVMMultManyManager(PSEModel model)
    {
        return new PSEVMMultManyManager_OCL(model);
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

    public static PSEFoxGlynn makeVMFoxGlynn
		( PSEModel model

		, double[] weight
		, double   weightDef
		, int      fgL
		, int      fgR

		, PrismLog log
        )
    {
        final int ocl = getOCL();
        final int par = getPAR();
        if (par == 1) {
            return new PSEFoxGlynnParallel(model, makeVMMultManager(model), weight, weightDef, fgL, fgR, log);
        } else if (par == 2) {
            if (ocl != 1) {
                throw new RuntimeException("In order to use PAR=2, you have to use OCL=1");
            }
            return new PSEFoxGlynnMany(model, makeVMMultManyManager(model), weight, weightDef, fgL, fgR, log);
        } else {
            return new PSEFoxGlynnSimple(model, makeVMMultManager(model), weight, weightDef, fgL, fgR, log);
        }
    }

    public static int getOCL()
    {
        String envOCL = System.getenv("OCL");
        return (envOCL == null) ? 0 : Integer.parseInt(envOCL);
    }

    public static int getPAR()
    {
        String envPAR = System.getenv("PAR");
        return (envPAR == null) ? 0 : Integer.parseInt(envPAR);
    }
}
