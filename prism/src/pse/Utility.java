package pse;

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
        String envPAR = System.getenv("PAR");
        boolean useOCL = envOCL != null && envOCL.equals("1");
        boolean usePAR = envPAR != null && envPAR.equals("1");
        PSEMultManager multManager;
        if (useOCL) {
            if (usePAR) {
                throw new RuntimeException("Use PAR=0");
            } else {
                System.err.printf("OCL=1 PAR=0\n");
                multManager = new PSEMVMultManager_OCL(model, modelSubset, modelSubsetComplement);
            }
        } else {
            if (usePAR) {
                throw new RuntimeException("Use PAR=0");
            } else {
                System.err.printf("OCL=0 PAR=0\n");
                multManager = new PSEMVMultManager_CPU(model, modelSubset, modelSubsetComplement);
            }
        }
        return multManager;
    }

    public static PSEMultManager makeVMMultManager(PSEModel model)
    {
        String envOCL = System.getenv("OCL");
        String envPAR = System.getenv("PAR");
        boolean useOCL = envOCL != null && envOCL.equals("1");
        boolean usePAR = envPAR != null && envPAR.equals("1");
        PSEMultManager multManager;
        if (useOCL) {
            if (usePAR) {
                throw new RuntimeException("Use PAR=0");
            } else {
                System.err.printf("OCL=1 PAR=0\n");
                multManager = new PSEVMMultManager_OCL(model);
            }
        } else {
            if (usePAR) {
                throw new RuntimeException("Use PAR=0");
            } else {
                System.err.printf("OCL=0 PAR=0\n");
                multManager = new PSEVMMultManager_CPU(model);
            }
        }
        return multManager;
    }

}
