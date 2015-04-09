package pse;

import prism.PrismException;
import prism.PrismLog;

import java.util.Map;

public final class PSEFoxGlynnSimple<Mult extends PSEMult> implements PSEFoxGlynn
{

    public PSEFoxGlynnSimple(PSEModel model, PSEMultManager<Mult> multManager, int iterStep, PrismLog log)
    {
        this.model = model;
        this.multManager = multManager;
        this.iterStep = iterStep;

        this.log = log;

        this.solnMin = new double[model.getNumStates()];
        this.solnMax = new double[model.getNumStates()];

        this.mult = multManager.create();

        System.err.printf("%s<%s>\n", this.getClass().toString(), mult.getClass().toString());
    }

    @Override
    final public int compute
        ( DistributionGetter distributionGetter
        , ParametersGetter parametersGetter
        , double t

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
            Params params = parametersGetter.getParameters(model, t);
            int fgL = params.fgL;
            int fgR = params.fgR;
            double[] weight = params.weight;
            double weightDef = params.weightDef;
            mult.setWeight(weight, weightDef, fgL);

            log.println("Computing probabilities for parameter region " + region);

            // Initialise solution vectors.
            distributionGetter.getDistribution(entry, 0, solnMin, solnMax);
            final double[] sumMin = new double[n];
            final double[] sumMax = new double[n];
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
                    itersStep = Math.max(Utility.leastGreaterMultiple(fgL, iterStep), iterStep);
                } else {
                    itersStep = Math.min(iterStep, fgR - iters);
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

    final private PSEModel model;
    final private PSEMultManager<Mult> multManager;
    final private int iterStep;

    final private PrismLog log;

    final private double[] solnMin;
    final private double[] solnMax;

    final private Mult mult;
}
