package pse;

import prism.PrismException;

import java.util.Map;

public interface PSEFoxGlynn
{
    public int compute
        ( PSEFoxGlynn.SolSettter solSettter
        , int itersCheckInterval

        , DecompositionProcedure decompositionProcedure

        , BoxRegionValues in
        , BoxRegionValues outPrev
        , BoxRegionValues out
        ) throws PrismException, DecompositionProcedure.DecompositionNeeded;

    interface SolSettter
    {
        public void setSol(Map.Entry<BoxRegion, BoxRegionValues.StateValuesPair> entry, int solnOff, final double[]
            solnMin, final double[] solnMax);
    }
}
