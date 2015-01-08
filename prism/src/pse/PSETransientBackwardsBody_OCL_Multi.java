package pse;

import org.jocl.cl_device_id;
import org.jocl.cl_platform_id;
import prism.PrismException;
import prism.PrismLog;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.jocl.CL.CL_DEVICE_TYPE_ALL;

public final class PSETransientBackwardsBody_OCL_Multi implements PSETransientBackwardsBody
{
	public final class Worker implements Runnable
	{

		public Worker
			( PSEModel model
			, Lock modelLock
			, PSEMVMultSettings_OCL opts
			, PSEMVMultTopology_OCL topo
			, PSEMVMult_OCL mult

			, BitSet targetMin
			, BitSet targetMax

			, BitSet subset, boolean complement
			, double[] weight
			, double   weightDef
			, int      fgL
			, int      fgR

			, int itersCheckInterval

			, DecompositionProcedure decompositionProcedure

			, BlockingQueue<Map.Entry<BoxRegion, BoxRegionValues.StateValuesPair>> in
			, BoxRegionValues outPrev
			, BoxRegionValues out
			, Lock outLock

			, PrismLog mainLog
			)
		{
			final int n = model.getNumTransitions();
			this.model = model;
			this.modelLock = modelLock;
			this.opts = opts;
			this.topo = topo;
			this.mult = mult;

			this.targetMin = targetMin;
			this.targetMax = targetMax;

			this.subset = subset;
			this.complement = complement;
			this.weight = weight;
			this.weightDef = weightDef;
			this.fgL = fgL;
			this.fgR = fgR;

			this.itersCheckInterval = itersCheckInterval;

			this.decompositionProcedure = decompositionProcedure;

			this.in = in;
			this.outPrev = outPrev;
			this.out = out;
			this.outLock = outLock;

			this.mainLog = mainLog;

			solnMin = new double[n];
			soln2Min = new double[n];
			sumMin = new double[n];
			solnMax = new double[n];
			soln2Max = new double[n];
			sumMax = new double[n];
		}

		@Override
		final public void run()
		{
			try {
				final int n = model.getNumTransitions();
				double[] tmpsoln;

				Output<PSEMVMult_OCL>[] mult_ = new Output[]{new Output<PSEMVMult_OCL>(mult)};
				Output<PSEMVMultTopology_OCL> topo_ = new Output<PSEMVMultTopology_OCL>(topo);
				int iters = 0;
				int itersTotal = 0;
				while (!in.isEmpty()) {
					Map.Entry<BoxRegion, BoxRegionValues.StateValuesPair> entry = in.poll();
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

					modelLock.lock();
					model.configureParameterSpace(region);
					model.createMVMult_OCL(subset, complement, weight, weightDef, fgL, opts, topo_, mult_);
					modelLock.unlock();

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
						decompositionProcedure.examinePartialComputation(null /* out */, region, sumMin, sumMax);

						// Matrix-vector multiply
						int numIters = itersCheckInterval;
						if (iters + numIters > fgR) {
							numIters = fgR - iters;
						}
						if (numIters == 0) {
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

						iters += numIters;
						itersTotal += numIters;
					}

					// Examine this region's result after all the iters have been finished
					decompositionProcedure.examinePartialComputation(null /* out */, region, sumMin, sumMax);

					// Store result
					outLock.lock();
					out.put(region, sumMin, sumMax);
					outLock.unlock();
				}
			} catch (DecompositionProcedure.DecompositionNeeded e) {
				decompositionNeededException = e;
			} catch (PrismException e) {
				prismException = e;
			}
		}

		public DecompositionProcedure.DecompositionNeeded getDecompositionNeededException() {
			return decompositionNeededException;
		}

		public PrismException getPrismException() {
			return prismException;
		}

		private DecompositionProcedure.DecompositionNeeded decompositionNeededException;
		private PrismException prismException;

		private double[] solnMin;
		private double[] soln2Min;
		private double[] sumMin;
		private double[] solnMax;
		private double[] soln2Max;
		private double[] sumMax;

		final private PSEModel model;
		final private Lock modelLock;
		final private PSEMVMultSettings_OCL opts;
		final private PSEMVMultTopology_OCL topo;
		final private PSEMVMult_OCL mult;

		final private BitSet targetMin;
		final private BitSet targetMax;

		final private BitSet subset;
		final private boolean complement;
		final private double[] weight;
		final private double   weightDef;
		final private int      fgL;
		final private int      fgR;

		final private int itersCheckInterval;

		final private DecompositionProcedure decompositionProcedure;

		final private BlockingQueue<Map.Entry<BoxRegion, BoxRegionValues.StateValuesPair>> in;
		final private BoxRegionValues outPrev;
		final private BoxRegionValues out;
		final private Lock outLock;

		final private PrismLog mainLog;
	}

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
		int itersTotal = 0;

		// TODO: Base this on number of states.
		int multCnt = Math.min(in.size(), 2);

		Output<PSEMVMultTopology_OCL> topo = new Output<PSEMVMultTopology_OCL>();
		Output<PSEMVMult_OCL>[] mult = new Output[multCnt];
		for (int i = 0; i < mult.length; ++i) {
			mult[i] = new Output<PSEMVMult_OCL>();
		}

		PSEMVMultSettings_OCL opts = new PSEMVMultSettings_OCL();
		cl_platform_id[] clPlatformIds = OCLProgram.getPlatformIds();
		cl_device_id[] clDeviceIds = OCLProgram.getDeviceIds(clPlatformIds[0], CL_DEVICE_TYPE_ALL);
		opts.clDeviceIdMax = clDeviceIds[0];
		opts.clDeviceIdMin = clDeviceIds[0];
		opts.clContext = OCLProgram.createContext(clPlatformIds[0], new cl_device_id[]{clDeviceIds[0]});
		model.createMVMult_OCL(subset, complement, weight, weightDef, fgL, opts, topo, mult);
		Lock modelLock = new ReentrantLock();
		Lock outLock = new ReentrantLock();

		BlockingQueue<Map.Entry<BoxRegion, BoxRegionValues.StateValuesPair>> inQueue =
			new LinkedBlockingQueue<Map.Entry<BoxRegion, BoxRegionValues.StateValuesPair>>(Math.max(20, in.size() * 2));
		for (Map.Entry<BoxRegion, BoxRegionValues.StateValuesPair> e : in) {
			try {
				inQueue.put(e);
			} catch (InterruptedException err) {
				throw new PrismException("TransientBackwardsBody_OCL_Multi -- could not construct the input queue");
			}
		}

		this.worker = new Worker[mult.length];
		this.workerThread = new Thread[mult.length];
		for (int i = 0; i < workerThread.length; ++i) {
			worker[i] = new Worker
				( model
				, modelLock
				, opts
				, topo.value
			    , mult[i].value
			    , targetMin
				, targetMax
				, subset, complement
				, weight
				, weightDef
				, fgL
				, fgR
				, itersCheckInterval
				, decompositionProcedure
				, inQueue
				, outPrev
				, out
				, outLock
				, mainLog
				);
			workerThread[i] = new Thread(worker[i]);
		}
		for (Thread w : workerThread) {
			try {
				w.join();
			} catch (InterruptedException err) {
				throw new PrismException("TransientBackwardsBody_OCL_Multi -- could not join the threads");
			}
		}

		LabelledBoxRegions regionsToDecompose = null;
		for (Worker w : worker) {
			DecompositionProcedure.DecompositionNeeded decompositionNeeded = w.getDecompositionNeededException();
			if (decompositionNeeded != null) {
				if (regionsToDecompose == null) {
					regionsToDecompose = new LabelledBoxRegions();
				}
				for (BoxRegion region : decompositionNeeded.getRegionsToDecompose()) {
					regionsToDecompose.add(region, "inaccurate region");
				}
			}
		}
		if (regionsToDecompose != null) {
			throw new DecompositionProcedure.DecompositionNeeded("significant inaccuracy", regionsToDecompose);
		}

		for (Worker w : worker) {
			if (w.getPrismException() != null) {
				throw w.getPrismException();
			}
		}

		return itersTotal;
	}

	private Worker[] worker;
	private Thread[] workerThread;
}
