package pse;

import prism.PrismException;
import prism.PrismLog;

import java.util.BitSet;
import java.util.Map;

public final class PSETransientBackwardsBody_CPU implements PSETransientBackwardsBody
{
	public int doWork
		( PSEModel model

		, BitSet targetMin
		, BitSet targetMax

		, BitSet subset, boolean complement
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
		double[] soln2Min = new double[n];
		double[] sumMin = new double[n];
		double[] solnMax = new double[n];
		double[] soln2Max = new double[n];
		double[] sumMax = new double[n];
		double[] tmpsoln;

		Output<PSEMVMult_CPU>[] mult_ = new Output[]{new Output<PSEMVMult_CPU>()};
		model.createMVMult_CPU(subset, complement, weight, weightDef, fgL, mult_);
		PSEMVMult_CPU mult = mult_[0].value;
		for (Map.Entry<BoxRegion, BoxRegionValues.StateValuesPair> entry : in) {
			BoxRegion region = entry.getKey();

			// If the previous region values contain probs for this region, i.e. the region
			// has not been decomposed, then just use the previous result directly.
			if (outPrev.hasRegion(region)) {
				out.put(region, outPrev.getMin(region), outPrev.getMax(region));
				continue;
			}

			double[] multProbsMin = entry.getValue().getMin().getDoubleArray();
			double[] multProbsMax = entry.getValue().getMax().getDoubleArray();

			// Configure parameter space
			model.configureParameterSpace(region);
			model.createMVMult_CPU(subset, complement, weight, weightDef, fgL, mult_);
			mainLog.println("Computing probabilities for parameter region " + region);

			// Create solution vectors

			// Initialise solution vectors.
			// Vectors soln/soln2 are multProbs[i] for target states.
			// Vector sum is all zeros (done by array creation).
			for (int i = 0; i < n; i++) {
				solnMin[i] = soln2Min[i] = targetMin.get(i) ? multProbsMin[i] : 0.0;
				solnMax[i] = soln2Max[i] = targetMax.get(i) ? multProbsMax[i] : 0.0;
			}

			// If necessary, do 0th element of summation (doesn't require any matrix powers)
			if (fgL == 0) {
				for (int i = 0; i < n; i++) {
					sumMin[i] += weight[0] * solnMin[i];
					sumMax[i] += weight[0] * solnMax[i];
				}
			}

			// Matrix-vector multiply
			mult.mvMult(solnMin, soln2Min, solnMax, soln2Max, fgL);
			iters = fgL;
			itersTotal += fgL;

			// Swap vectors for next iter
			tmpsoln = solnMin;
			solnMin = soln2Min;
			soln2Min = tmpsoln;
			tmpsoln = solnMax;
			solnMax = soln2Max;
			soln2Max = tmpsoln;
			// Start iterations
			while (iters <= fgR) {
				// Add to sum
				mult.getSum(sumMin, sumMax);
				decompositionProcedure.examinePartialComputation(out, region, sumMin, sumMax);

				// Matrix-vector multiply
				int numIters = itersCheckInterval;
				if (iters + numIters > fgR)
				{
					numIters = fgR - iters;
				}
				if (numIters == 0)
				{
					break;
				}
				mult.mvMult(solnMin, soln2Min, solnMax, soln2Max, numIters);

				// Swap vectors for next iter
				tmpsoln = solnMin;
				solnMin = soln2Min;
				soln2Min = tmpsoln;
				tmpsoln = solnMax;
				solnMax = soln2Max;
				soln2Max = tmpsoln;

				iters +=numIters;
				itersTotal +=numIters;
			}

			// Examine this region's result after all the iters have been finished
			decompositionProcedure.examinePartialComputation(out, region, sumMin, sumMax);

			// Store result
			out.put(region, sumMin, sumMax);
		}
		return itersTotal;
	}
}
