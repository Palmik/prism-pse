//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Alessandro Bruni <albr@dtu.dk> (Technical University of Denmark)
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

package explicit;

import java.awt.Point;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import parser.ast.Expression;
import parser.ast.ExpressionBinaryOp;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionTemporal;
import parser.ast.ExpressionUnaryOp;
import parser.type.TypeBool;
import parser.type.TypePathBool;
import prism.DRA;
import prism.LTL2RabinLibrary;
import prism.Pair;
import prism.PrismException;
import explicit.SCCComputer.SCCMethod;

/**
 * LTL model checking functionality
 */
public class LTLModelChecker
{
	/**
	 * Convert an LTL formula into a DRA. The LTL formula is represented as a PRISM Expression,
	 * in which atomic propositions are represented by ExpressionLabel objects.
	 */
	public static DRA<BitSet> convertLTLFormulaToDRA(Expression ltl) throws PrismException
	{
		return LTL2RabinLibrary.convertLTLFormulaToDRA(ltl);
	}

	/**
	 * Extract maximal state formula from an LTL path formula, model check them (with passed in model checker) and
	 * replace them with ExpressionLabel objects L0, L1, etc. Expression passed in is modified directly, but the result
	 * is also returned. As an optimisation, model checking that results in true/false for all states is converted to an
	 * actual true/false, and duplicate results are given the same label. BitSets giving the states which satisfy each label
	 * are put into the vector {@code labelBS}, which should be empty when this function is called.
	 */
	public Expression checkMaximalStateFormulas(ProbModelChecker mc, Model model, Expression expr, Vector<BitSet> labelBS) throws PrismException
	{
		// A state formula
		if (expr.getType() instanceof TypeBool) {
			// Model check
			StateValues sv = mc.checkExpression(model, expr);
			BitSet bs = sv.getBitSet();
			// Detect special cases (true, false) for optimisation
			if (bs.isEmpty()) {
				return Expression.False();
			}
			if (bs.cardinality() == model.getNumStates()) {
				return Expression.True();
			}
			// See if we already have an identical result
			// (in which case, reuse it)
			int i = labelBS.indexOf(bs);
			if (i != -1) {
				sv.clear();
				return new ExpressionLabel("L" + i);
			}
			// Otherwise, add result to list, return new label
			labelBS.add(bs);
			return new ExpressionLabel("L" + (labelBS.size() - 1));
		}
		// A path formula (recurse, modify, return)
		else if (expr.getType() instanceof TypePathBool) {
			if (expr instanceof ExpressionBinaryOp) {
				ExpressionBinaryOp exprBinOp = (ExpressionBinaryOp) expr;
				exprBinOp.setOperand1(checkMaximalStateFormulas(mc, model, exprBinOp.getOperand1(), labelBS));
				exprBinOp.setOperand2(checkMaximalStateFormulas(mc, model, exprBinOp.getOperand2(), labelBS));
			} else if (expr instanceof ExpressionUnaryOp) {
				ExpressionUnaryOp exprUnOp = (ExpressionUnaryOp) expr;
				exprUnOp.setOperand(checkMaximalStateFormulas(mc, model, exprUnOp.getOperand(), labelBS));
			} else if (expr instanceof ExpressionTemporal) {
				ExpressionTemporal exprTemp = (ExpressionTemporal) expr;
				if (exprTemp.getOperand1() != null) {
					exprTemp.setOperand1(checkMaximalStateFormulas(mc, model, exprTemp.getOperand1(), labelBS));
				}
				if (exprTemp.getOperand2() != null) {
					exprTemp.setOperand2(checkMaximalStateFormulas(mc, model, exprTemp.getOperand2(), labelBS));
				}
			}
		}
		return expr;
	}

	/**
	 * Construct the product of a DRA and a DTMC.
	 * @param dra The DRA
	 * @param dtmc The DTMC
	 * @param labelBS BitSets giving the set of states for each AP in the DRA
	 * @return a Pair consisting of the product DTMC and a map from 
	 *   (s_i * draSize + q_j) to the right state in the DRA product 
	 */
	public Pair<Model, int[]> constructProductMC(DRA<BitSet> dra, DTMC dtmc, Vector<BitSet> labelBS) throws PrismException
	{
		if (!(dtmc instanceof DTMCSimple)) {
			throw new PrismException("Expecting a DTMC here");
		}

		DTMCSimple modelDTMC = (DTMCSimple) dtmc;
		DTMCSimple prodModel = new DTMCSimple();

		int draSize = dra.size();
		int numAPs = dra.getAPList().size();
		int modelNumStates = modelDTMC.getNumStates();
		int prodNumStates = modelNumStates * draSize;

		// Encoding: 
		// each state s' = <s, q> = s * draSize + q
		// s(s') = s' / draSize
		// q(s') = s' % draSize

		// Initial states
		LinkedList<Point> queue = new LinkedList<Point>();
		int map[] = new int[prodNumStates];
		Arrays.fill(map, -1);
		int q_0 = dra.getStartState();
		for (int s_0 : dtmc.getInitialStates()) {
			queue.add(new Point(s_0, q_0));
			prodModel.addState();
			prodModel.addInitialState(prodModel.getNumStates() - 1);
			map[s_0 * draSize + q_0] = prodModel.getNumStates() - 1;
		}

		// Product states
		BitSet visited = new BitSet(prodNumStates);
		int q_1 = 0, q_2 = 0, s_1 = 0, s_2 = 0;
		BitSet s_2_labels = new BitSet(numAPs);
		while (!queue.isEmpty()) {
			Point p = queue.pop();
			s_1 = p.x;
			q_1 = p.y;
			visited.set(s_1 * draSize + q_1);

			// Go through transitions from state s_1 in original DTMC
			Iterator<Map.Entry<Integer, Double>> iter = dtmc.getTransitionsIterator(s_1);
			while (iter.hasNext()) {
				Map.Entry<Integer, Double> e = iter.next();
				s_2 = e.getKey();
				double prob = e.getValue();
				// Get BitSet representing APs (labels) satisfied by successor state s_2
				for (int k = 0; k < numAPs; k++) {
					s_2_labels.set(k, labelBS.get(k).get(s_2));
				}
				// Find corresponding successor in DRA
				q_2 = dra.getEdgeDestByLabel(q_1, s_2_labels);
				// Add state/transition to model
				if (!visited.get(s_2 * draSize + q_2) && map[s_2 * draSize + q_2] == -1) {
					queue.add(new Point(s_2, q_2));
					prodModel.addState();
					map[s_2 * draSize + q_2] = prodModel.getNumStates() - 1;
				}
				prodModel.setProbability(map[s_1 * draSize + q_1], map[s_2 * draSize + q_2], prob);
			}
		}

		int invMap[] = new int[prodModel.getNumStates()];
		for (int i = 0; i < map.length; i++) {
			if (map[i] != -1) {
				invMap[map[i]] = i;
			}
		}

		prodModel.findDeadlocks(false);

		return new Pair<Model, int[]>(prodModel, invMap);
	}

	public BitSet findAcceptingBSCCs(DRA<BitSet> dra, Model modelProduct, int invMap[], SCCMethod sccMethod)
	{
		// Compute bottom strongly connected components (BSCCs)
		SCCComputer sccComputer = SCCComputer.createSCCComputer(sccMethod, modelProduct);
		sccComputer.computeBSCCs();
		List<BitSet> bsccs = sccComputer.getBSCCs();

		int draSize = dra.size();

		BitSet result = new BitSet();
		for (BitSet bscc : bsccs) {
			int numAcceptancePairs = dra.getNumAcceptancePairs();
			boolean isLEmpty = true;
			boolean isKEmpty = true;
			for (int acceptancePair = 0; acceptancePair < numAcceptancePairs && isLEmpty && isKEmpty; acceptancePair++) {
				BitSet L = dra.getAcceptanceL(acceptancePair);
				BitSet K = dra.getAcceptanceK(acceptancePair);

				for (int state = bscc.nextSetBit(0); state != -1; state = bscc.nextSetBit(state + 1)) {
					int draState = invMap[state] % draSize;
					isLEmpty &= !L.get(draState);
					isKEmpty &= !K.get(draState);
				}
			}

			if (isLEmpty && !isKEmpty) {
				// Acceptance condition
				result.or(bscc);
			}
		}

		return result;
	}
}
