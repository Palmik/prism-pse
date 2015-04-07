package pse;

import prism.PrismException;
import prism.PrismLog;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class PSEFoxGlynnParallel<Mult extends  PSEMult> implements PSEFoxGlynn
{
	public final static class Worker<Mult extends PSEMult> implements Runnable
	{
		public Worker
			( PSEModel model
			, PSEMultManager<Mult> multManager
			, Mult mult

			, double[] weight
			, double   weightDef
			, int      fgL
			, int      fgR

			, PrismLog mainLog

			, ReadWriteLock modelLock
			, ReadWriteLock outLock
			)
		{
			this.model = model;
			this.multManager = multManager;
			this.mult = mult;

			this.weight = weight;
			this.weightDef = weightDef;
			this.fgL = fgL;
			this.fgR = fgR;

			this.mainLog = mainLog;

			this.modelLock = modelLock;
			this.outLock = outLock;

			this.regionsToDecompose = new LabelledBoxRegions();

			this.solnMin = new double[model.getNumStates()];
			this.solnMax = new double[model.getNumStates()];
		}

		final public void update
			( PSEFoxGlynnSimple.SolSettter solSettter
			, int itersCheckInterval

			, DecompositionProcedure decompositionProcedure

			, BlockingQueue<Map.Entry<BoxRegion, BoxRegionValues.StateValuesPair>> in
			, BoxRegionValues outPrev
			, BoxRegionValues out
			)
		{
			this.solSettter = solSettter;
			this.itersCheckInterval = itersCheckInterval;

			this.decompositionProcedure = decompositionProcedure;

			this.in = in;
			this.outPrev = outPrev;
			this.out = out;

			this.regionsToDecompose.clear();
		}

		@Override
		final public void run()
		{
			final int n = model.getNumStates();

			int iters;
			int itersTotal = 0;

			try {
				while (!in.isEmpty()) {
					Map.Entry<BoxRegion, BoxRegionValues.StateValuesPair> entry = in.poll();
					if (entry == null) {
						continue;
					}
					BoxRegion region = entry.getKey();
					boolean wasDecomposed = false;

					// If the previous region values contain probs for this region, i.e. the region
					// has not been decomposed, then just use the previous result directly.
					if (outPrev.hasRegion(region)) {
						outLock.writeLock().lock();
						out.put(region, outPrev.getMin(region), outPrev.getMax(region));
						outLock.writeLock().unlock();
						continue;
					}

					// Configure parameter space
					modelLock.writeLock().lock();
					model.evaluateParameters(region);
					multManager.update(mult);
					mainLog.println("Computing probabilities for parameter region " + region);
					modelLock.writeLock().unlock();

					// Initialise solution vectors.
					solSettter.setSol(entry, 0, solnMin, solnMax);
					final double[] sumMin = new double[n];
					final double[] sumMax = new double[n];
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
							itersStep = Math.max(Utility.leastGreaterMultiple(fgL, itersCheckInterval),
								itersCheckInterval);
						} else {
							itersStep = Math.min(itersCheckInterval, fgR - iters);
						}
						mult.mult(itersStep);
						iters += itersStep;
						itersTotal += itersStep;

						mult.getSum(sumMin, sumMax);
						if (handleCheckRegion(decompositionProcedure, out, region, sumMin, sumMax)) {
							wasDecomposed = true;
							break;
						}
					}
					// Examine this region's result after all the iters have been finished
					mult.getSum(sumMin, sumMax);
					if (wasDecomposed || handleCheckRegion(decompositionProcedure, out, region, sumMin, sumMax)) {
						continue;
					}

					// Store result
					outLock.readLock().lock();
					out.put(region, sumMin, sumMax);
					outLock.readLock().unlock();
				}
			} catch (PrismException e) {
				prismException = e;
			}
		}

		public LabelledBoxRegions getRegionsToDecompose()
		{
			return regionsToDecompose;
		}

		public PrismException getPrismException()
		{
			return prismException;
		}

		private boolean handleCheckRegion(DecompositionProcedure decompositionProcedure, BoxRegionValues out, BoxRegion region, double[] sumMin, double[] sumMax)
			throws PrismException
		{
			outLock.readLock().lock();
			try {
				decompositionProcedure.examinePartialComputation(out, region, sumMin, sumMax);
			} catch (DecompositionProcedure.DecompositionNeeded err) {
				regionsToDecompose.putAll(err.getLabelledRegionsToDecompose());
				return true;
			} finally {
				outLock.readLock().unlock();
			}
			return false;
		}

		private LabelledBoxRegions regionsToDecompose;
		private PrismException prismException;

		private PSEModel model;
		private PSEMultManager<Mult> multManager;
		private PSEFoxGlynnSimple.SolSettter solSettter;
		private Mult mult;

		private double[] weight;
		private double   weightDef;
		private int      fgL;
		private int      fgR;

		private int itersCheckInterval;

		private DecompositionProcedure decompositionProcedure;

		private BlockingQueue<Map.Entry<BoxRegion, BoxRegionValues.StateValuesPair>> in;
		private BoxRegionValues outPrev;
		private BoxRegionValues out;

		private PrismLog mainLog;

		private ReadWriteLock modelLock;
		private ReadWriteLock outLock;

		final private double[] solnMin;
		final private double[] solnMax;

	}

	public PSEFoxGlynnParallel
		( PSEModel model
		, PSEMultManager<Mult> multManager

		, double[] weight
		, double   weightDef
		, int      fgL
		, int      fgR

		, PrismLog log
		)
	{
		this.model = model;
		this.multManager = multManager;
		this.weight = weight;
		this.weightDef = weightDef;
		this.fgL = fgL;
		this.fgR = fgR;
		this.log = log;

		// TODO: Base this on number of states.
		this.multGroupSize = 3;
		this.multGroup = multManager.createGroup(weight, weightDef, fgL, multGroupSize);

		ReadWriteLock modelLock = new ReentrantReadWriteLock();
		ReadWriteLock outLock = new ReentrantReadWriteLock();

		this.multWorkerGroup = new Worker[multGroupSize];
		for (int i = 0; i < multGroupSize; ++i) {
			multWorkerGroup[i] = new Worker<Mult>(model, multManager, multGroup[i],
				weight, weightDef, fgL, fgR, log, modelLock, outLock);
		}

		System.err.printf("%s<%s>\n", this.getClass().toString(), multGroup[0].getClass());
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
		int itersTotal = 0;

		BlockingQueue<Map.Entry<BoxRegion, BoxRegionValues.StateValuesPair>> inQueue =
			new LinkedBlockingDeque<Map.Entry<BoxRegion, BoxRegionValues.StateValuesPair>>(in.entrySet());

		Thread[] workerThread = new Thread[multGroupSize];
		for (int i = 0; i < workerThread.length; ++i) {
			multWorkerGroup[i].update(solSettter, itersCheckInterval, decompositionProcedure, inQueue, outPrev, out);
			workerThread[i] = new Thread(multWorkerGroup[i]);
			workerThread[i].start();
		}

		try {
			for (Thread w : workerThread) {
				w.join();
			}
		} catch (InterruptedException err) {
			throw new PrismException("TransientBackwardsBody_OCL_Multi -- could not join the threads");
		}

		LabelledBoxRegions regionsToDecompose = new LabelledBoxRegions();
		for (Worker w : multWorkerGroup) {
			regionsToDecompose.putAll(w.getRegionsToDecompose());
		}
		if (!regionsToDecompose.isEmpty()) {
			DecompositionProcedure.DecompositionNeeded e = new DecompositionProcedure.DecompositionNeeded("significant inaccuracy",
				regionsToDecompose);
			e.setExaminedRegionValues(out);
			throw e;
		}

		for (Worker w : multWorkerGroup) {
			if (w.getPrismException() != null) {
				throw w.getPrismException();
			}
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

	final private int multGroupSize;
	final private Mult[] multGroup;
	final private Worker[] multWorkerGroup;
}
