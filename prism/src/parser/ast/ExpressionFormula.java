//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

package parser.ast;

import parser.*;
import parser.visitor.*;
import prism.PrismLangException;

public class ExpressionFormula extends Expression
{
	String name;
	
	// Constructors
	
	public ExpressionFormula()
	{
	}
	
	public ExpressionFormula(String n)
	{
		name = n;
	}
			
	// Set method
	
	public void setName(String n) 
	{
		name = n;
	}
	
	// Get method
	
	public String getName()
	{
		return name;
	}
	
	// Methods required for Expression:
	
	/**
	 * Is this expression constant?
	 */
	public boolean isConstant()
	{
		// Don't know - err on the side of caution
		return false;
	}

	/**
	 * Evaluate this expression, return result.
	 * Note: assumes that type checking has been done already.
	 */
	public Object evaluate(Values constantValues, Values varValues) throws PrismLangException
	{
		// Should never be called
		throw new PrismLangException("Could not evaluate formula", this);
	}

	// Methods required for ASTElement:
	
	/**
	 * Visitor method.
	 */
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}
		
	/**
	 * Convert to string.
	 */
	public String toString()
	{
		return name;
	}

	/**
	 * Perform a deep copy.
	 */
	public Expression deepCopy()
	{
		Expression ret = new ExpressionFormula(name);
		ret.setPosition(this);
		return ret;
	}
}

//------------------------------------------------------------------------------
