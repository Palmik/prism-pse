package pse;

import prism.PrismException;

public interface PSEFoxGlynn
{
    public int compute
        ( PSEFoxGlynnSimple.SolSettter solSettter
        , int itersCheckInterval

        , DecompositionProcedure decompositionProcedure

        , BoxRegionValues in
        , BoxRegionValues outPrev
        , BoxRegionValues out
        ) throws PrismException, DecompositionProcedure.DecompositionNeeded;
}
