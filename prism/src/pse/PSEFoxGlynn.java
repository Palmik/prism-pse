package pse;

import prism.PrismException;
import prism.PrismLog;

import java.util.Map;

public final class PSEFoxGlynn
{
    public interface SolSettter
    {
        public void setSol(Map.Entry<BoxRegion, BoxRegionValues.StateValuesPair> entry, final double[] solnMin, final double[] solnMax);
    }

    public static<Mult extends  PSEMult> int compute
        ( PSEModel model
        , PSEMultManager<Mult> multManager
        , PSEFoxGlynn.SolSettter solSettter

        , double[] weight
        , double   weightDef
        , int      fgL
        , int      fgR

        , int itersCheckInterval

        , DecompositionProcedure decompositionProcedure

        , BoxRegionValues in
        , BoxRegionValues outPrev
        , BoxRegionValues out

        , PrismLog mainLog
        ) throws PrismException, DecompositionProcedure.DecompositionNeeded
    {
        final int n = model.getNumStates();

        int iters;
        int itersTotal = 0;

        double[] solnMin = new double[n];
        double[] sumMin = new double[n];
        double[] solnMax = new double[n];
        double[] sumMax = new double[n];

        Mult mult = multManager.create(weight, weightDef, fgL);
        for (Map.Entry<BoxRegion, BoxRegionValues.StateValuesPair> entry : in) {
            BoxRegion region = entry.getKey();

            // If the previous region values contain probs for this region, i.e. the region
            // has not been decomposed, then just use the previous result directly.
            if (outPrev.hasRegion(region)) {
                out.put(region, outPrev.getMin(region), outPrev.getMax(region));
                continue;
            }

            // Configure parameter space
            model.evaluateParameters(region);
            multManager.update(mult);
            mainLog.println("Computing probabilities for parameter region " + region);

            // Initialise solution vectors.
            solSettter.setSol(entry, solnMin, solnMax);
            // If necessary, do 0th element of summation (doesn't require any matrix powers)
            {
                double w = (fgL == 0) ? weight[0] : weightDef;
                if (w != 0) {
                    for (int i = 0; i < n; i++) {
                        sumMin[i] = weight[0] * solnMin[i];
                        sumMax[i] = weight[0] * solnMax[i];
                    }
                    mult.setSum(sumMin, sumMax);
                }
            }

            // Start iterations
            iters = 0;
            mult.setMult(solnMin, solnMax);
            while (iters < fgR) {
                // Matrix-vector multiply
                int itersStep;
                if (iters == 0 && weightDef == 0) {
                    itersStep = Math.max(Utility.leastGreaterMultiple(fgL, itersCheckInterval), itersCheckInterval);
                } else {
                    itersStep = Math.min(itersCheckInterval, fgR - iters);
                }
                mult.mult(itersStep);
                iters += itersStep;
                itersTotal += itersStep;

                mult.getSum(sumMin, sumMax);
                decompositionProcedure.examinePartialComputation(out, region, sumMin, sumMax);
            }
            // Examine this region's result after all the iters have been finished
            mult.getSum(sumMin, sumMax);
            decompositionProcedure.examinePartialComputation(out, region, sumMin, sumMax);

            // Store result
            out.put(region, sumMin, sumMax);
        }
        return itersTotal;
    }
}
