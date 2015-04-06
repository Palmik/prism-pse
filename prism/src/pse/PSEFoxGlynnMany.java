package pse;

import prism.PrismException;
import prism.PrismLog;

import java.util.Iterator;
import java.util.Map;

public final class PSEFoxGlynnMany<Mult extends PSEMultMany> implements PSEFoxGlynn
{
    public PSEFoxGlynnMany(PSEModel model, PSEMultManyManager<Mult> multManager, double[] weight, double weightDef, int
        fgL, int fgR, PrismLog log)
    {
        this.matCntMax = 2;
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

        this.mult = multManager.create(matCntMax, weight, weightDef, fgL);
        this.region = new BoxRegion[matCntMax];
        this.regionDecomposed = new boolean[matCntMax];
        this.regionsToDecompose = new LabelledBoxRegions();

        System.err.printf("%s<%s>\n", this.getClass().toString(), mult.getClass().toString());
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
				solSettter.setSol(entry, 0, solnMin, solnMax);
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
                    itersStep = Math.max(Utility.leastGreaterMultiple(fgL, itersCheckInterval), itersCheckInterval);
                } else {
                    itersStep = Math.min(itersCheckInterval, fgR - iters);
                }

                mult.mult(matCnt, itersStep);
                iters += itersStep;
                itersTotal += itersStep;

                for (int matId = 0; matId < matCnt; ++matId) {
                    if (regionDecomposed[matId]) {
                        continue;
                    }
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
        return itersTotal;
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

    private BoxRegion[] region;
    private boolean[] regionDecomposed;
    private LabelledBoxRegions regionsToDecompose;
}
