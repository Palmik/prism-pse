//==============================================================================
//	
//	Copyright (c) 2014-
//	Authors:
//	* Andrej Tokarcik <andrejtokarcik@gmail.com> (Masaryk University)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package pse;

import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import parser.Values;
import parser.ast.ExpressionProb;
import parser.ast.RelOp;
import prism.PrismException;
import prism.PrismLog;

abstract class AbstractMinMaxSynthesis extends DecompositionProcedure {
	// Synthesis parameters
	protected double probTolerance;
	protected String captionForOptimising;

	// Properties of the model being model-checked
	protected int initState;

	// Solution structures
	protected List<BoxRegion> regionsOptimising = new LinkedList<BoxRegion>();
	protected List<BoxRegion> regionsNonoptimising = new LinkedList<BoxRegion>();
	double minimalLowerBoundOfOptimising;
	double maximalUpperBoundOfOptimising;

	public AbstractMinMaxSynthesis(double probTolerance, int initState)
	{
		this.probTolerance = probTolerance;
		this.initState = initState;
	}

	@Override
	protected void processPropertyExpression(boolean singleInit, Values constantValues) throws PrismException
	{
		try {
			ExpressionProb probExpr = (ExpressionProb) propExpr;
			if (probExpr.getRelOp() != RelOp.EQ)
				throw new ClassCastException();
		} catch (ClassCastException e) {
			throw new PrismException("Min and max syntheses require a P operator of the form P=?");
		}
	}

	protected abstract void determineOptimalRegions(BoxRegionValues regionValues);

	protected abstract BoxRegion chooseRegionToDecompose(BoxRegion current, BoxRegion candidate,
			boolean candidateHasMinimalLowerBound);

	@Override
	public void examineWholeComputation(BoxRegionValues regionValues) throws DecompositionNeeded
	{
		// NB: In the following, the term `bounds' refers to constraints on the probability
		// of the property's being satisfied in a given region.  This is not to be confused
		// with `bounds' in the sense of upper/lower values of parameter ranges characterising
		// the parameter regions/subspaces.

		determineOptimalRegions(regionValues);

		// Determine the deciding probability bounds
		BoxRegion regionToDecompose = null;
		minimalLowerBoundOfOptimising = Double.POSITIVE_INFINITY;
		maximalUpperBoundOfOptimising = Double.NEGATIVE_INFINITY;
		for (Entry<BoxRegion, BoxRegionValues.StateValuesPair> entry : regionValues) {
			if (!regionsOptimising.contains(entry.getKey()))
				continue;

			double currentLowerBound = (Double) entry.getValue().getMin().getValue(initState);
			if (currentLowerBound < minimalLowerBoundOfOptimising) {
				regionToDecompose = chooseRegionToDecompose(regionToDecompose, entry.getKey(), true);
				minimalLowerBoundOfOptimising = currentLowerBound;
			}

			double currentUpperBound = (Double) entry.getValue().getMax().getValue(initState);
			if (currentUpperBound > maximalUpperBoundOfOptimising) {
				regionToDecompose = chooseRegionToDecompose(regionToDecompose, entry.getKey(), false);
				maximalUpperBoundOfOptimising = currentUpperBound;
			}
		}

		// Evaluate whether a decomposition is needed
		if (maximalUpperBoundOfOptimising - minimalLowerBoundOfOptimising > probTolerance)
			throw new DecompositionNeeded(regionToDecompose);
	}

	@Override
	public void printSolution(PrismLog log)
	{
		super.printIntro(log);

		log.print("\nRegions " + captionForOptimising + " the property satisfaction probability:");
		printRegions(log, regionsOptimising);
		log.print("Non-optimal regions:");
		printRegions(log, regionsNonoptimising);

		log.println("\nmin lower prob bound of " + captionForOptimising + " regions = " + minimalLowerBoundOfOptimising);
		log.println("max upper prob bound of " + captionForOptimising + " regions = " + maximalUpperBoundOfOptimising);

		log.print("\nmax upper prob bound of " + captionForOptimising + " - min lower prob bound of " + captionForOptimising + " = ");
		log.println(maximalUpperBoundOfOptimising - minimalLowerBoundOfOptimising);
		log.println("probability tolerance = " + probTolerance);
	}
}
