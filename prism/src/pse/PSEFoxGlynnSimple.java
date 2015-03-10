package pse;

import prism.PrismException;
import prism.PrismLog;

import java.util.Map;

public final class PSEFoxGlynnSimple<Mult extends PSEMult> implements PSEFoxGlynn
{
    public interface SolSettter
    {
        public void setSol(Map.Entry<BoxRegion, BoxRegionValues.StateValuesPair> entry, final double[] solnMin, final double[] solnMax);
    }

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

        System.err.printf("PSEFoxGlynnSimple<%s>\n", mult.getClass());
    }

    @Override
    final public int compute
        ( PSEFoxGlynnSimple.SolSettter solSettter
        , int itersCheckInterval

        , DecompositionProcedure decompositionProcedure

        , BoxRegionValues in
        , BoxRegionValues outPrev
        , BoxRegionValues out
        ) throws PrismException, DecompositionProcedure.DecompositionNeeded
    {
        System.err.printf("!P0IN %s\n", in.size());
        System.err.printf("!P0OUT %s\n", out.size());
        System.err.printf("!P0OUTPREV %s\n", outPrev.size());
        final int n = model.getNumStates();

        int iters;
        int itersTotal = 0;


        for (Map.Entry<BoxRegion, BoxRegionValues.StateValuesPair> entry : in) {
            BoxRegion region = entry.getKey();

            // If the previous region values contain probs for this region, i.e. the region
            // has not been decomposed, then just use the previous result directly.
            if (outPrev.hasRegion(region)) {
                System.err.printf("!PCOMPNOT %s\n", region);
                out.put(region, outPrev.getMin(region), outPrev.getMax(region));
                continue;
            }

            // Configure parameter space
            model.evaluateParameters(region);
            multManager.update(mult);
            log.println("Computing probabilities for parameter region " + region);
            System.err.printf("!PCOMP %s\n", region);

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
                try {
                    decompositionProcedure.examinePartialComputation(out, region, sumMin, sumMax);
                } catch (DecompositionProcedure.DecompositionNeeded e) {
                    System.err.printf("!PDECOMP %s\n", region);
                    throw e;
                }
            }
            // Examine this region's result after all the iters have been finished
            mult.getSum(sumMin, sumMax);
            try {
                decompositionProcedure.examinePartialComputation(out, region, sumMin, sumMax);
            } catch (DecompositionProcedure.DecompositionNeeded e) {
                System.err.printf("!PDECOMP %s\n", region);
                throw e;
            }
            // Store result
            System.err.printf("!PDECOMPNOT %s\n", region);
            out.put(region, sumMin, sumMax);
        }
        System.err.printf("!P1OUT %s\n", out.size());
        System.err.printf("!P1OUTPREV %s\n", outPrev.size());
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
