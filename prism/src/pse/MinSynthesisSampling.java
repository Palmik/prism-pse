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
import java.util.Map.Entry;

import parser.ast.Expression;
import prism.PrismException;
import prism.PrismLog;

/**
 * Decomposition procedure solving the min synthesis problem using
 * the sampling-based approach to determine the demarcation
 * probability bound.
 */
public final class MinSynthesisSampling extends MinSynthesis
{
	PSEModelExplorer modelExplorer;
	PSEModel pointModel;
	PSEModelChecker pointMC;
	/** list of sample points used for demarcation */
	private LinkedList<Point> samples;

	public MinSynthesisSampling(double probTolerance, PSEModelExplorer modelExplorer)
	{
		super(probTolerance);
		this.modelExplorer = modelExplorer;
	}

	@Override
	public void initialiseModelChecking(PSEModelChecker modelChecker, PSEModel model, Expression propExpr)
			throws PrismException
	{
		super.initialiseModelChecking(modelChecker, model, propExpr);

		PSEModelBuilder builder = new PSEModelBuilder(modelChecker, modelExplorer);
		builder.build();
		pointModel = builder.getModel();
		// Allow the builder to be garbage-collected
		builder = null;

		pointMC = new PSEModelChecker(modelChecker);
		pointMC.setModulesFileAndPropertiesFile(modelChecker.getModulesFile(), modelChecker.getPropertiesFile());
		//pointMC.setVerbosity(0);

		samples = new LinkedList<Point>();
	}

	/**
	 * Sampling-based approach to determining the maximal lower bound.
	 */
	@Override
	protected double getMinimalUpperBound(BoxRegionValues regionValues) throws PrismException
	{
		double minimalUpperBound = Double.POSITIVE_INFINITY;
		BoxRegion minimalUpperBoundRegion = null;
		for (Entry<BoxRegion, BoxRegionValues.StateValuesPair> entry : regionValues) {
			double currentUpperBound = (Double) entry.getValue().getMax().getValue(initState);
			if (currentUpperBound < minimalUpperBound) {
				minimalUpperBound = currentUpperBound;
				minimalUpperBoundRegion = entry.getKey();
			}
		}
		/* TODO: If the region found above is the same as the previously found region
		 * for the X-th time (where X is the value of a PRISM setting, configurable
		 * via CLI), then don't even attempt to generate more samples, and simply
		 * return the last minimal sample probability. */

		double minimalSampleProb = Double.POSITIVE_INFINITY;
		Point minimalSample = null;
		DecompositionProcedure noDecomposing = SimpleDecompositionProcedure.NoDecomposing.getInstance();
		for (Point sample : minimalUpperBoundRegion.generateSamplePoints()) {
			pointModel.setParameterSpace(sample.asRegion());
			BoxRegionValues sampleResult = pointMC.checkExpression(pointModel, propExpr, noDecomposing);
			assert sampleResult.getNumRegions() == 1;
			// getMin() and getMax() are identical for point regions
			double currentSampleProb = (Double) sampleResult.lastEntry().getValue().getMin().getValue(initState);
			if (currentSampleProb < minimalSampleProb) {
				minimalSampleProb = currentSampleProb;
				minimalSample = sample;
			}
		}

		if (demarcationProbBounds.isEmpty() || minimalSampleProb < demarcationProbBounds.getLast()) {
			samples.add(minimalSample);
			return minimalSampleProb;
		} else {
			samples.add(samples.getLast());
			return demarcationProbBounds.getLast();
		}
	}

	@Override
	public String toString()
	{
		return super.toString() + " (sampling)";
	}

	@Override
	public void printSolution(PrismLog log, boolean verbose)
	{
		super.printSolution(log, verbose);

		if (verbose) {
			log.println("\nSample points in the order they were used to exclude regions:");
			log.println(samples);
		}
	}
}
