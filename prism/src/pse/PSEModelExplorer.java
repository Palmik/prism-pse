//==============================================================================
//	
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import param.SymbolicEngine;
import param.TransitionList;
import parser.State;
import parser.Values;
import parser.ast.Expression;
import parser.ast.ExpressionConstant;
import parser.ast.ModulesFile;
import parser.visitor.ASTTraverse;
import prism.PrismException;
import explicit.ModelExplorer;

public final class PSEModelExplorer implements ModelExplorer<Expression>
{
	private SymbolicEngine engine;
	private State currentState;
	private TransitionList transitionList;

	private BoxRegionFactory regionFactory;

	private Map<Expression, ParamIdAndPopulRate> rateDataCache = new HashMap<Expression, ParamIdAndPopulRate>();
	private Map<State, TransitionList> transitionsCache = new HashMap<State, TransitionList>();
    private Map<String, Integer> paramIds;

	protected static class ParamIdAndPopulRate
	{
        ParamIdAndPopulRate(int n, double r)
        {
            this.paramId = n;
            this.populRate = r;
        }
		public final int paramId;
        public final double populRate;
	}

	public PSEModelExplorer(ModulesFile modulesFile) throws PrismException
	{
		modulesFile = (ModulesFile) modulesFile.deepCopy().replaceConstants(modulesFile.getConstantValues()).simplify();
		engine = new SymbolicEngine(modulesFile);
        paramIds = new HashMap<String, Integer>();
	}

    // Param ids start from 0.
    public int getParamId(String paramName)
    {
        return paramIds.get(paramName);
    }

	public void setParameters(String[] paramNames, double[] lower, double[] upper)
	{
		Values lowerParams = new Values();
		Values upperParams = new Values();
		for (int i = 0; i < paramNames.length; i++) {
			lowerParams.addValue(paramNames[i], lower[i]);
			upperParams.addValue(paramNames[i], upper[i]);
		    paramIds.put(paramNames[i], i);
        }
		regionFactory = new BoxRegionFactory(lowerParams, upperParams);
	}

	public BoxRegionFactory getRegionFactory()
	{
		return regionFactory;
	}

	protected ParamIdAndPopulRate extractRateParametersAndPopulation(Expression rateExpression) throws PrismException
	{
		if (rateDataCache.containsKey(rateExpression)) {
			return rateDataCache.get(rateExpression);
		}

        // NOTE: I do this, because non-final variables can not be accessed from closures.
        final List<ExpressionConstant> rateParameters_ = new ArrayList<ExpressionConstant>();
		rateExpression.accept(new ASTTraverse()
		{
			// TODO: visit() for doubles to handle rate population directly.
			// Subsequently, the whole method could be made static and moved
			// into pse.RateUtils or something.
			public Object visit(ExpressionConstant e)
			{
				rateParameters_.add(e);
				return null;
			}
		});
        if (rateParameters_.size() > 1)
            throw new PrismException("The PSE algorithms assumes that every transition is parametrised by at most one parameter.");

		Expression rateParameters = Expression.Double(1);
        for (ExpressionConstant c : rateParameters_) { rateParameters = Expression.Times(rateParameters, c); }

		double ratePopulation = Expression.Divide(rateExpression, rateParameters).evaluateDouble(regionFactory.completeSpaceUpperParamsValues());

        int paramId = -1;
        if (rateParameters_.size() > 0) { paramId = getParamId(rateParameters_.get(0).getName()); }
        ParamIdAndPopulRate result = new ParamIdAndPopulRate(paramId, ratePopulation);
        rateDataCache.put(rateExpression, result);
		return result;
	}

	protected ParamIdAndPopulRate[] extractRateParametersAndPopulation(Expression[] rateExpressions) throws PrismException
	{
		if (rateExpressions == null) {
			return null;
		}
		int n = rateExpressions.length;
		ParamIdAndPopulRate[] result = new ParamIdAndPopulRate[n];
		for (int i = 0; i < n; i++) {
			result[i] = extractRateParametersAndPopulation(rateExpressions[i]);
		}
		return result;
	}

	protected ModulesFile getModulesFile()
	{
		return engine.getModulesFile();
	}

	@Override
	public State getDefaultInitialState() throws PrismException
	{
		return getModulesFile().getDefaultInitialState();
	}

	@Override
	public void queryState(State state) throws PrismException
	{
		currentState = state;
		if (transitionsCache.containsKey(state)) {
			transitionList = transitionsCache.get(state);
		} else {
			transitionList = engine.calculateTransitions(state);
			transitionsCache.put(state, transitionList);
		}
	}

	@Override
	public void queryState(State state, double time) throws PrismException
	{
		queryState(state);
	}

	@Override
	public int getNumChoices() throws PrismException
	{
		return transitionList.getNumChoices();
	}

	@Override
	public int getNumTransitions() throws PrismException
	{
		return transitionList.getNumTransitions();
	}

	@Override
	public int getNumTransitions(int choiceNr) throws PrismException
	{
		return transitionList.getChoice(choiceNr).size();
	}

	protected int getTotalIndexOfTransition(int choiceNr, int offset)
	{
		return transitionList.getTotalIndexOfTransition(choiceNr, offset);
	}

	@Override
	public String getTransitionAction(int choiceNr, int offset) throws PrismException
	{
		int a = transitionList.getTransitionModuleOrActionIndex(getTotalIndexOfTransition(choiceNr, offset));
		return a < 0 ? null : getModulesFile().getSynch(a - 1);
	}

	@Override
	public String getTransitionAction(int succNr) throws PrismException
	{
		int a = transitionList.getTransitionModuleOrActionIndex(succNr);
		return a < 0 ? null : getModulesFile().getSynch(a - 1);
	}

	@Override
	public Expression getTransitionProbability(int choiceNr, int offset) throws PrismException
	{
		return getTransitionProbability(getTotalIndexOfTransition(choiceNr, offset));
	}

	@Override
	public Expression getTransitionProbability(int succNr) throws PrismException
	{
		return transitionList.getTransitionProbability(succNr);
	}

	@Override
	public State computeTransitionTarget(int choiceNr, int offset) throws PrismException
	{
		return computeTransitionTarget(getTotalIndexOfTransition(choiceNr, offset));
	}

	@Override
	public State computeTransitionTarget(int succNr) throws PrismException
	{
		return transitionList.computeTransitionTarget(succNr, currentState);
	}

	public int getReaction(int succNr)
	{
		return transitionList.getChoiceOfTransition(succNr).hashCode();
	}
	
}
