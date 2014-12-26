package pse;

import prism.PrismException;
import prism.PrismLog;

import java.util.BitSet;

public interface PSETransientBackwardsBody
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
		) throws PrismException, DecompositionProcedure.DecompositionNeeded;
}
