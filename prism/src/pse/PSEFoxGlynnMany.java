package pse;

import prism.PrismException;
import prism.PrismLog;

import java.util.Iterator;
import java.util.Map;

public final class PSEFoxGlynnMany<Mult extends PSEMultMany> implements PSEFoxGlynn
{
    public PSEFoxGlynnMany(PSEModel model, PSEMultManyManager<Mult> multManager, int iterStep, PrismLog log)
    {
        this.matCntMax = 4;
        this.model = model;
        this.multManager = multManager;

        this.log = log;

        this.solnMin = new double[model.getNumStates()];
        this.sumMin = new double[model.getNumStates()];
        this.solnMax = new double[model.getNumStates()];
        this.sumMax = new double[model.getNumStates()];

        this.mult = multManager.create(matCntMax);
        this.region = new BoxRegion[matCntMax];
        this.regionDecomposed = new boolean[matCntMax];
        this.regionsToDecompose = new LabelledBoxRegions();

        this.iterStep = iterStep;

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
        int itersTotalEffective = 0;

        PSEFoxGlynn.Params params = parametersGetter.getParameters(model, t);
        double[] weight = params.weight;
        double weightDef = params.weightDef;
        int fgL = params.fgL;
        int fgR = params.fgR;
        mult.setWeight(weight, weightDef, fgL);

        this.regionsToDecompose.clear();
        Iterator<Map.Entry<BoxRegion, BoxRegionValues.StateValuesPair>> it = in.iterator();
        while (it.hasNext()) {
            int matCntDecomposed = 0;
            int matCnt = 0;
            while (matCnt < matCntMax && it.hasNext()) {
                Map.Entry<BoxRegion, BoxRegionValues.StateValuesPair> entry = it.next();
                BoxRegion region = entry.getKey();
				// If the previous region values contain probs for this region, i.e. the region
				// has not been decomposed, then just use the previous result directly.
				if (outPrev.hasRegion(region)) {
					out.put(region, outPrev.getMin(region), outPrev.getMax(region));
					continue;
				}
                this.region[matCnt] = region;
                this.regionDecomposed[matCnt] = false;

				// Configure parameter space
				model.evaluateParameters(region);
				multManager.update(matCnt, mult);
				log.println("Computing probabilities for parameter region " + region);

				// Initialise solution vectors.
				distributionGetter.getDistribution(entry, 0, solnMin, solnMax);
				// If necessary, do 0th element of summation (doesn't require any matrix powers)
				{
					double w = (fgL == 0) ? weight[0] : weightDef;
					if (w != 0) {
						for (int i = 0; i < n; i++) {
							sumMin[i] = w * solnMin[i];
							sumMax[i] = w * solnMax[i];
						}
						mult.setSum(matCnt, sumMin, sumMax);
					}
				}
				mult.setMult(matCnt, solnMin, solnMax);
                ++matCnt;
            }

            if (matCnt == 0) {
                continue;
            }

            // Start iterations
            iters = 0;
            while (iters < fgR) {
                // Matrix-vector multiply
                int itersStep;
                if (iters == 0 && weightDef == 0) {
                    itersStep = Math.max(Utility.leastGreaterMultiple(fgL, iterStep), iterStep);
                } else {
                    itersStep = Math.min(iterStep, fgR - iters);
                }

                mult.mult(matCnt, itersStep);
                iters += itersStep;
                itersTotal += itersStep * matCntMax;

                for (int matId = 0; matId < matCnt; ++matId) {
                    if (regionDecomposed[matId]) {
                        continue;
                    }
                    itersTotalEffective += itersStep;
                    mult.getSum(matId, sumMin, sumMax);
                    if (handleCheckRegion(decompositionProcedure, out, region[matId], sumMin, sumMax)) {
                        regionDecomposed[matId] = true;
                        ++matCntDecomposed;
                    }
                }
                if (matCntDecomposed == matCnt) {
                    break;
                }
            }
            // Examine this region's result after all the iters have been finished
            for (int matId = 0; matId < matCnt; ++matId) {
                if (regionDecomposed[matId]) {
                    continue;
                }

                mult.getSum(matId, sumMin, sumMax);
                if (!handleCheckRegion(decompositionProcedure, out, region[matId], sumMin, sumMax)) {
                    out.put(region[matId], sumMin.clone(), sumMax.clone());
                }
            }
        }
        if (!regionsToDecompose.isEmpty()) {
            DecompositionProcedure.DecompositionNeeded e =
                new DecompositionProcedure.DecompositionNeeded("significant inaccuracy", regionsToDecompose);
            e.setExaminedRegionValues(out);
            throw e;
        }
        log.print(String.format("PSEFoxGlynnMany: iters_total_effective=%s; iters_total=%s; ratio=%s\n", itersTotalEffective, itersTotal, (double)itersTotalEffective/(double)itersTotal));
        return itersTotalEffective;
    }

    final private boolean handleCheckRegion(DecompositionProcedure decompositionProcedure, BoxRegionValues out, BoxRegion region, double[] sumMin, double[] sumMax)
        throws PrismException
    {
        try {
            decompositionProcedure.examinePartialComputation(out, region, sumMin, sumMax);
        } catch (DecompositionProcedure.DecompositionNeeded err) {
            regionsToDecompose.putAll(err.getLabelledRegionsToDecompose());
            return true;
        }
        return false;
    }

    final int matCntMax;
    final private PSEModel model;
    final private PSEMultManyManager<Mult> multManager;

    final private PrismLog log;
    final private int iterStep;

    final private double[] solnMin;
    final private double[] sumMin;
    final private double[] solnMax;
    final private double[] sumMax;

    final private Mult mult;

    private BoxRegion[] region;
    private boolean[] regionDecomposed;
    private LabelledBoxRegions regionsToDecompose;
}
