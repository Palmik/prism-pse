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

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import parser.Values;
import explicit.Utils;

final class BoxRegion implements Comparable<BoxRegion>
{
	private double[] lowerBounds;
	private double[] upperBounds;

	private double volume = 0.0;

	public BoxRegion(double[] boundsLower, double[] boundsUpper)
    {
		this.lowerBounds = boundsLower;
		this.upperBounds = boundsUpper;
	}


	public double[] getLowerBounds()
	{
		return lowerBounds;
	}

	public double[] getUpperBounds()
	{
		return upperBounds;
	}

	private double[] computeMidBounds()
	{
		final double[] midBounds = new double[lowerBounds.length];
		for (int i = 0; i < lowerBounds.length; ++i) {
			final double lowerValue = lowerBounds[i];
			final double upperValue = upperBounds[i];
			midBounds[i] = lowerValue + 0.5 * (upperValue - lowerValue);
		}
		return midBounds;
	}

	public Set<BoxRegion> decompose()
	{
		Set<BoxRegion> subregions = new HashSet<BoxRegion>();
		double[] midBounds = computeMidBounds();
		Set<Integer> allIndices = new HashSet<Integer>();
		for (int i = 0; i < midBounds.length; i++) {
			allIndices.add(i);
		}

		for (Set<Integer> indices : Utils.powerSet(allIndices)) {
			double[] newLowerBounds = new double[lowerBounds.length];
			double[] newUpperBounds = new double[lowerBounds.length];
			for (int i = 0; i < midBounds.length; ++i) {
				final double midValue = midBounds[i];
				if (indices.contains(i)) {
					newLowerBounds[i] = lowerBounds[i];
					newUpperBounds[i] = midValue;
				} else {
					newLowerBounds[i] = midValue;
					newUpperBounds[i] = upperBounds[i];
				}
			}
			subregions.add(new BoxRegion(newLowerBounds, newUpperBounds));
		}
		return subregions;
	}

	public double volume()
	{
		if (volume > 0.0)
			return volume;

		volume = 1.0;
		for (int i = 0; i < lowerBounds.length; ++i) {
			double lowerValue = lowerBounds[i];
			double upperValue = upperBounds[i];
			if (lowerValue != upperValue) {
				volume *= upperValue - lowerValue;
			}
		}
		return volume;
	}

	public Set<double[]> generateSamplePoints()
	{
		return generateSamplePoints(2);
	}

	public Set<double[]> generateSamplePoints(int numSamples)
	{
		Set<double[]> samples = new HashSet<double[]>();
		Random r = new Random();
		while (samples.size() != numSamples) {
			double[] dimensions = new double[lowerBounds.length];
			for (int i = 0; i < lowerBounds.length; ++i) {
				double lowerValue = lowerBounds[i];
				double upperValue = upperBounds[i];
				double randomValue = lowerValue + r.nextDouble() * (upperValue - lowerValue);
				dimensions[i] = randomValue;
			}
			samples.add(dimensions);
		}
		return samples;
	}

    private int DoubleArrayCompareTo(double[] aa, double[] bb)
    {
        for (int i = 0; i < aa.length; ++i)
        {
            int r = Double.compare(aa[i], bb[i]);
            if (r != 0) return r;
        }
        return 0;
    }

	@Override
	public int compareTo(BoxRegion r)
	{
		int lowerRes = DoubleArrayCompareTo(lowerBounds, r.lowerBounds);
		int upperRes = DoubleArrayCompareTo(upperBounds, r.upperBounds);
		if (lowerRes == upperRes)
			return lowerRes;

		int min = Math.min(lowerRes, upperRes);
		int max = Math.max(lowerRes, upperRes);
		if (min == 0)
			return max;
		if (max == 0)
			return min;
		return lowerRes;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((lowerBounds == null) ? 0 : lowerBounds.hashCode());
		result = prime * result
				+ ((upperBounds == null) ? 0 : upperBounds.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BoxRegion other = (BoxRegion) obj;
		if (lowerBounds == null) {
			if (other.lowerBounds != null)
				return false;
		} else if (!lowerBounds.equals(other.lowerBounds))
			return false;
		if (upperBounds == null) {
			if (other.upperBounds != null)
				return false;
		} else if (!upperBounds.equals(other.upperBounds))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < lowerBounds.length; i++) {
			if (i != 0) builder.append(",");
			builder.append(Integer.toString(i));
			builder.append("=");
			builder.append((Double) lowerBounds[i]);
			builder.append(":");
			builder.append((Double) upperBounds[i]);
		}
		return builder.toString();
	}
}
