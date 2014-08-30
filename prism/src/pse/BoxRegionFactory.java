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

import parser.Values;
import prism.PrismLangException;

public final class BoxRegionFactory
{
	private Values lowerParamsValues;
	private Values upperParamsValues;
    private double[] lowerParams;
    private double[] upperParams;

	public BoxRegionFactory(Values paramsLower, Values paramsUpper)
	{
		assert paramsLower.getNumValues() == paramsUpper.getNumValues();
		this.lowerParamsValues = paramsLower;
		this.upperParamsValues = paramsUpper;
        this.lowerParams = new double[paramsLower.getNumValues()];
        this.upperParams = new double[paramsUpper.getNumValues()];
        for (int i = 0; i < paramsLower.getNumValues(); ++i)
        {
            this.lowerParams[i] = (Double)paramsLower.getValue(i);
            this.upperParams[i] = (Double)paramsUpper.getValue(i);
        }
	}

    public BoxRegion completeSpace()
    {
        return new BoxRegion(lowerParams, upperParams);
    }
    public Values completeSpaceLowerParamsValues()
    {
        return lowerParamsValues;
    }

    public Values completeSpaceUpperParamsValues()
    {
        return upperParamsValues;
    }
}
