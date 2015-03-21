package pse;

import prism.PrismException;
import prism.PrismLog;

import java.util.Map;

public final class PSEFoxGlynnSimple<Mult extends PSEMult> implements PSEFoxGlynn
{

    public PSEFoxGlynnSimple(PSEModel model, PSEMultManager<Mult> multManager
        , double[] weight, double weightDef, int fgL, int fgR
        , PrismLog log)
    {
        this.model = model;
        this.multManager = multManager;

        this.weight = weight;
        this.weightDef = weightDef;
        this.fgL = fgL;
        this.fgR = fgR;
        this.log = log;

        this.solnMin = new double[model.getNumStates()];
        this.sumMin = new double[model.getNumStates()];
        this.solnMax = new double[model.getNumStates()];
        this.sumMax = new double[model.getNumStates()];

        this.mult = multManager.create(weight, weightDef, fgL);

    }

    @Override
    final public int compute
        ( PSEFoxGlynn.SolSettter solSettter
        , int itersCheckInterval

        , DecompositionProcedure decompositionProcedure

        , BoxRegionValues in
        , BoxRegionValues outPrev
        , BoxRegionValues out
        ) throws PrismException, DecompositionProcedure.DecompositionNeeded
    {
        final int n = model.getNumStates();

        int iters;
        int itersTotal = 0;


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
            log.println("Computing probabilities for parameter region " + region);

            // Initialise solution vectors.
            solSettter.setSol(entry, 0, solnMin, solnMax);
            // If necessary, do 0th element of summation (doesn't require any matrix powers)
            {
                double w = (fgL == 0) ? weight[0] : weightDef;
                if (w != 0) {
                    for (int i = 0; i < n; i++) {
                        sumMin[i] = w * solnMin[i];
                        sumMax[i] = w * solnMax[i];
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
                try {
                    decompositionProcedure.examinePartialComputation(out, region, sumMin, sumMax);
                } catch (DecompositionProcedure.DecompositionNeeded e) {
                    throw e;
                }
            }
            // Examine this region's result after all the iters have been finished
            mult.getSum(sumMin, sumMax);
            try {
                decompositionProcedure.examinePartialComputation(out, region, sumMin, sumMax);
            } catch (DecompositionProcedure.DecompositionNeeded e) {
                throw e;
            }
            // Store result
            out.put(region, sumMin, sumMax);
        }
        return itersTotal;
    }

    final private PSEModel model;
    final private PSEMultManager<Mult> multManager;
    final private double[] weight;
    final private double weightDef;
    final private int fgL;
    final private int fgR;

    final private PrismLog log;

    final private double[] solnMin;
    final private double[] sumMin;
    final private double[] solnMax;
    final private double[] sumMax;

    final private Mult mult;
}
