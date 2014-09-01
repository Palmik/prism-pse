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

import java.util.Map.Entry;

import parser.ast.Expression;
import prism.PrismException;
import simulator.SimulatorEngine;
import explicit.CTMC;
import explicit.CTMCModelChecker;

public final class MaxSynthesisSampling extends MaxSynthesis
{
	private SimulatorEngine simulatorEngine;
	private explicit.ConstructModel constructModel;
	private CTMCModelChecker ctmcModelChecker;
	private double lastMaximalSampleProb;

	public MaxSynthesisSampling(double probTolerance, int initState, SimulatorEngine simulatorEngine)
	{
		super(probTolerance, initState);
		this.simulatorEngine = simulatorEngine;
	}

	@Override
	public void initialise(PSEModelChecker modelChecker, PSEModelExplicit model, Expression propExpr)
			throws PrismException
	{
		super.initialise(modelChecker, model, propExpr);
		constructModel = new explicit.ConstructModel(modelChecker, simulatorEngine);
		ctmcModelChecker = new CTMCModelChecker(modelChecker);
		ctmcModelChecker.setModulesFileAndPropertiesFile(modelChecker.getModulesFile(), modelChecker.getPropertiesFile());
		lastMaximalSampleProb = Double.NEGATIVE_INFINITY;
	}

	/**
	 * Sampling approach to determining the maximal lower bound.
	 */
	@Override
	protected double getMaximalLowerBound(BoxRegionValues regionValues) throws PrismException
	{
		double maximalLowerBound = Double.NEGATIVE_INFINITY;
		BoxRegion maximalLowerBoundRegion = null;
		for (Entry<BoxRegion, BoxRegionValues.StateValuesPair> entry : regionValues) {
			double currentLowerBound = (Double) entry.getValue().getMin().getValue(initState);
			if (currentLowerBound > maximalLowerBound) {
				maximalLowerBound = currentLowerBound;
				maximalLowerBoundRegion = entry.getKey();
			}
		}
		/* TODO: If the region found above is the same as the previously found region
		 * for the X-th time (where X is a PRISM setting, configurable via CLI), then
		 * don't even attempt to generate more samples, and simply return the last
		 * maximal sample probability.
		 */

		double maximalSampleProb = Double.NEGATIVE_INFINITY;
		for (Point sample : maximalLowerBoundRegion.generateSamplePoints()) {
			CTMC ctmc = model.instantiate(sample, modelChecker.getModulesFile(), constructModel);
			double currentSampleProb = (Double) ctmcModelChecker.checkExpression(ctmc, propExpr).getValue(initState);
			if (currentSampleProb > maximalSampleProb)
				maximalSampleProb = currentSampleProb;
		}

		if (maximalSampleProb > lastMaximalSampleProb) {
			lastMaximalSampleProb = maximalSampleProb;
			return maximalSampleProb;
		}
		return lastMaximalSampleProb;
	}

	@Override
	public String toString()
	{
		return super.toString() + " (sampling)";
	}
}
