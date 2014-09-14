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

import java.io.File;
import java.util.*;

import parser.Values;
import parser.ast.Expression;
import parser.ast.ModulesFile;
import prism.*;
import explicit.CTMC;
import explicit.ModelExplicit;


/**
 * Represents a parametrised CTMC model to be used for PSE-based methods.
 */
public final class PSEModelExplicit extends ModelExplicit
{
    /** total number of probabilistic transitions over all states */
    private int trCnt;
    /** begin and end of state transitions */
    private int[] rows;
    /** origins of distribution branches */
    private int[] trStSrc;
    /** targets of distribution branches */
    private int[] trStTrg;
    /** all transitions' rate parameters, as expressions */
    private Expression[] trRateExpr;
    /** all transitions' rate parameters, evaluated with lower bounds of current region */
    private double[] trRateLower;
    /** all transitions' rate parameters, evaluated with upper bounds of current region */
    private double[] trRateUpper;
    /** species populations in all transitions' origin states */
    private double[] trRatePopul;
    /** all transitions' reactions, i.e. transition kinds */
    private int[] trReac;
    /**  - per transition, <i>not</i> per action */
    private String[] trName;
    /** total sum of leaving rates for a state */
    private double[] stExitRate;
    /** set of hash codes for deciding whether state has predecessors via reaction */
    private Set<Integer> predecessorsViaReaction;
    /** map from state to non-param transitions coming out from it */
    private Map<Integer, List<Integer>> trsNPBySrc;
    private Map<Integer, List<Integer>> trsNPByTrg;
    private int trsNPCnt;
    /** map from state to transitions exclusively coming into it */
    private Map<Integer, List<Integer>> trsIBySrc;
    private Map<Integer, List<Integer>> trsIByTrg;
    private int trsICnt;
    /** map from state to transitions exclusively going out from it */
    private Map<Integer, List<Integer>> trsOBySrc;
    private Map<Integer, List<Integer>> trsOByTrg;
    private int trsOCnt;
    /** map from state to transitions both incoming in and outgoing from it */
    private Map<Integer, List<Pair<Integer, Integer>>> trsIO;
    private int trsIOCnt;

    private boolean gpu;
    private PSEModelForVM_CPU modelVMCPU;
    private PSEModelForVM_GPU modelVMGPU;

    private long timeBuildModel;
    private long timeConfigureModel;


    /**
     * Constructs a new parametric model.
     */
    PSEModelExplicit()
    {
        stCnt = 0;
        trCnt = 0;
        initialStates = new LinkedList<Integer>();
        deadlocks = new TreeSet<Integer>();
        predecessorsViaReaction = new HashSet<Integer>();

        this.gpu = false;
    }

    // Accessors (for Model)

    @Override
    public ModelType getModelType()
    {
        return ModelType.CTMC;
    }

    @Override
    public Values getConstantValues()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getNumTransitions()
    {
        return trCnt;
    }

    @Override
    public Iterator<Integer> getSuccessorsIterator(int s)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSuccessor(int s1, int s2)
    {
        for (int trans = stateBegin(s1); trans < stateEnd(s1); trans++) {
            if (toState(trans) == s2)
                return true;
        }
        return false;
    }

    @Override
    public boolean allSuccessorsInSet(int s, BitSet set)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean someSuccessorsInSet(int s, BitSet set)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void findDeadlocks(boolean fix) throws PrismException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkForDeadlocks() throws PrismException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkForDeadlocks(BitSet except) throws PrismException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void buildFromPrismExplicit(String filename) throws PrismException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportToPrismExplicit(String baseFilename) throws PrismException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportToPrismExplicitTra(String filename) throws PrismException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportToPrismExplicitTra(File file) throws PrismException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportToPrismExplicitTra(PrismLog log)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportToDotFile(String filename) throws PrismException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportToDotFile(String filename, BitSet mark) throws PrismException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportTransitionsToDotFile(int i, PrismLog out)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportToPrismLanguage(String filename) throws PrismException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String infoString()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String infoStringTable()
    {
        String s = "";
        s += "States:      " + stCnt + " (" + getNumInitialStates() + " initial)\n";
        s += "Transitions: " + getNumTransitions() + "\n";
        return s;
    }

    /**
     * Allocates memory for subsequent construction of model.
     *
     * @param numStates number of states of the model
     * @param numTotalTransitions total number of probabilistic transitions of the model
     */
    void reserveMem(int numStates, int numTotalTransitions)
    {
        rows = new int[numStates + 1];
        trName = new String[numTotalTransitions];
        trReac = new int[numTotalTransitions];
        trRateExpr = new Expression[numTotalTransitions];
        trRateLower = new double[numTotalTransitions];
        trRateUpper = new double[numTotalTransitions];
        trRatePopul = new double[numTotalTransitions];
        trStTrg = new int[numTotalTransitions];
        trStSrc = new int[numTotalTransitions];
        stExitRate = new double[numStates];
    }

    /**
     * Finishes the current state.
     * Starting with the 0th state, this function shall be called once all
     * transitions outgoing from the current nth state have been added.
     * Subsequent method calls of {@code addTransition}
     * will then apply to the (n+1)th state. Notice that this method must be
     * called for each state of the method, even the last one, once all its
     * transitions have been added.
     */
    void finishState()
    {
        rows[stCnt + 1] = trCnt;
        stCnt++;
    }

    /**
     * Adds a probabilistic transition from the current state.
     *
     * @param reaction kind of the transition being added
     * @param fromState from which state the transition goes
     * @param toState to which state the transition leads
     * @param rateExpr the rate expression
     * @param ratePopulation the transition's origin state's species population
     * @param action action with which the transition is labelled
     */
    void addTransition(int reaction, int fromState, int toState, Expression rateExpr, double ratePopulation, String action)
    {
        trReac[trCnt] = reaction;
        trStSrc[trCnt] = fromState;
        trStTrg[trCnt] = toState;
        trRateExpr[trCnt] = rateExpr;
        trRatePopul[trCnt] = ratePopulation;
        trName[trCnt] = action;

        predecessorsViaReaction.add(toState ^ reaction);

        trCnt++;
    }

    /**
     * Sets the total sum of leaving rates from the current state.
     *
     * @param leaving sum of leaving rates from the current state
     */
    void setSumLeaving(double leaving)
    {
        stExitRate[stCnt] = leaving;
    }

    /**
     * Returns the number of the first transition going from {@code state}.
     *
     *
     * @param state state to return number of first transition of
     * @return number of first transition going from {@code state}
     */
    int stateBegin(int state)
    {
        return rows[state];
    }

    /**
     * Returns the number of the last transition going from {@code state} plus one.
     *
     * @param state state to return number of last transition of
     * @return number of last transition going from {@code state} plus one
     */
    int stateEnd(int state)
    {
        return rows[state + 1];
    }

    /**
     * Returns whether the rate of the given transition depends on parameters.
     *
     * @param trans transition about which to decide whether it's parametrised
     * @return true iff the rate {@code trans} depends on parameters
     */
    boolean isParametrised(int trans)
    {
        return rateParamsLowers(trans) != rateParamsUppers(trans);
    }

    /**
     * Returns reaction to which the given transition belongs.
     *
     * @param trans transition to return reaction for
     * @return reaction of {@code trans}
     */
    int getReaction(int trans)
    {
        return trReac[trans];
    }

    /**
     * Returns the predecessor state of the given transition.
     *
     * @param trans transition to return predecessor for
     * @return predecessor state of {@code trans}
     */
    int fromState(int trans)
    {
        return trStSrc[trans];
    }

    /**
     * Returns the successor state of the given transition.
     *
     * @param trans transition to return successor for
     * @return successor state of {@code trans}
     */
    int toState(int trans)
    {
        return trStTrg[trans];
    }

    /**
     * Returns the label of the given transition.
     *
     * @param trans transition to return label of
     * @return label of {@code trans}
     */
    String getLabel(int trans)
    {
        return trName[trans];
    }

    /**
     * Computes the maximum exit rate over all states in the model,
     * i.e. max_i { sum_j R(i,j) }.
     *
     * @return maximum exit rate
     */
    double getMaxExitRate()
    {
        BitSet allStates = new BitSet(stCnt);
        allStates.set(0, stCnt - 1);
        return getMaxExitRate(allStates);
    }

    /**
     * Computes the maximum exit rate over states in {@code subset},
     * i.e. max_{i in subset} { sum_j R(i,j) }.
     *
     * @param subset subset of states over which to compute maximum exit rate
     * @return maximum exit rate over states in {@code subset}
     */
    double getMaxExitRate(BitSet subset)
    {
        double max = Double.NEGATIVE_INFINITY;
        for (int state = subset.nextSetBit(0); state >= 0; state = subset.nextSetBit(state + 1)) {
            if (stExitRate[state] > max)
                max = stExitRate[state];
        }
        return max;
    }

    /**
     * Computes the default rate used to uniformise this parametrised CTMC.
     */
    double getDefaultUniformisationRate()
    {
        return 1.02 * getMaxExitRate();
    }

    /**
     * Computes the default rate used to uniformise this parametrised CTMC,
     * assuming that all states *not* in {@code nonAbs} have been made absorbing.
     */
    double getDefaultUniformisationRate(BitSet nonAbs)
    {
        return 1.02 * getMaxExitRate(nonAbs);
    }

    /**
     * Analyses the model's transitions in order to divide them between exclusively
     * incoming, exclusively outgoing or both incoming/outgoing from the perspective
     * of particular states. The results are stored in {@code trsIByTrg},
     * {@code trsOBySrc} and {@code trsIO}, respectively.
     */
    public void computeInOutReactions()
    {
        if (trsIByTrg != null && trsIO != null && trsOBySrc != null && trsNPBySrc != null)
            return;

        // Initialise the reaction sets
        trsIBySrc = new HashMap<Integer, List<Integer>>(stCnt);
        trsIByTrg = new HashMap<Integer, List<Integer>>(stCnt);
        trsOBySrc = new HashMap<Integer, List<Integer>>(stCnt);
        trsOByTrg = new HashMap<Integer, List<Integer>>(stCnt);
        trsIO = new HashMap<Integer, List<Pair<Integer, Integer>>>(stCnt);
        trsNPBySrc = new HashMap<Integer, List<Integer>>(stCnt);
        trsNPByTrg = new HashMap<Integer, List<Integer>>(stCnt);
        trsICnt = 0;
        trsIOCnt = 0;
        trsOCnt = 0;
        for (int state = 0; state < stCnt; state++) {
            trsIBySrc.put(state, new LinkedList<Integer>());
            trsIByTrg.put(state, new LinkedList<Integer>());
            trsOBySrc.put(state, new LinkedList<Integer>());
            trsOByTrg.put(state, new LinkedList<Integer>());
            trsIO.put(state, new LinkedList<Pair<Integer, Integer>>());
            trsNPBySrc.put(state, new LinkedList<Integer>());
            trsNPByTrg.put(state, new LinkedList<Integer>());
        }

        // Populate the sets with transition indices
        // t0 goes from v0 to v1.
        // t1 goes from v1 to (some) v2.
        for (int v0 = 0; v0 < stCnt; v0++) {
            for (int t0 = stateBegin(v0); t0 < stateEnd(v0); ++t0) {
                final int r0 = getReaction(t0);
                final int v1 = toState(t0);
                if (!isParametrised(t0)) {
                    trsNPBySrc.get(v0).add(t0);
                    trsNPByTrg.get(v1).add(t0);
                    ++trsNPCnt;
                    continue;
                }
                boolean inout = false;
                for (int t1 = stateBegin(v1); t1 < stateEnd(v1); t1++) {
                    if (getReaction(t1) == r0) {
                        inout = true;
                        trsIO.get(v1).add(new Pair<Integer, Integer>(t0, t1));
                        ++trsIOCnt;
                        break;
                    }
                }
                if (!inout) {
                    trsIBySrc.get(v0).add(t0);
                    trsIByTrg.get(v1).add(t0);
                    ++trsICnt;
                }
                if (!predecessorsViaReaction.contains(v0 ^ r0)) {
                    trsOBySrc.get(v0).add(t0);
                    trsOByTrg.get(v1).add(t0);
                    ++trsOCnt;
                }
            }
        }

        System.err.printf("I: %s; O: %s, IO: %s, NP: %s\n", trsICnt, trsOCnt, trsIOCnt, trsNPCnt);

        if (gpu) { modelVMGPU = buildModelForVM_GPU(); }
        else { modelVMCPU = buildModelForVM_CPU(); }
    }

    final public PSEModelForVM_GPU buildModelForVM_GPU()
    {
        return null;
    }

    final public PSEModelForVM_CPU buildModelForVM_CPU()
    {
        final long timeBeg = System.nanoTime();

        int[] trsIO_ = new int[trsIOCnt * 2];

        VectorOfDouble matMinVal = new VectorOfDouble();
        VectorOfInt matMinSrc = new VectorOfInt();
        int[] matMinTrgBeg = new int [stCnt + 1];
        int matMinPos = 0;

        VectorOfDouble matMaxVal = new VectorOfDouble();
        VectorOfInt matMaxSrc = new VectorOfInt();
        int[] matMaxTrgBeg = new int [stCnt + 1];
        int matMaxPos = 0;

        VectorOfDouble matVal = new VectorOfDouble();
        VectorOfInt matSrc = new VectorOfInt();
        int[] matTrgBeg = new int [stCnt + 1];
        int matPos = 0;

        double[] matMinDiagVal = new double[stCnt];
        double[] matMaxDiagVal = new double[stCnt];

        int trsIOPos = 0;
        for (int state = 0; state < stCnt; ++state)
        {
            matMinTrgBeg[state] = matMinPos;
            matMaxTrgBeg[state] = matMaxPos;
            matTrgBeg[state] = matPos;

            List<Integer> stTrsI = trsIByTrg.get(state);
            List<Integer> stTrsO = trsOBySrc.get(state);
            List<Pair<Integer, Integer>> stTrsIO = trsIO.get(state);
            List<Integer> stTrsNP = trsNPBySrc.get(state);

            /*
            for (Pair<Integer, Integer> p : stTrsIO)
            {
                trsIO_[trsIOPos++] = p.first;
                trsIO_[trsIOPos++] = p.second;
            }

            for (Integer t : stTrsI)
            {
                final double valMin = trRateLower[t] * trRatePopul[t];
                final double valMax = trRateUpper[t] * trRatePopul[t];
                if (valMin != 0)
                {
                    matMinVal.pushBack(valMin);
                    matMinSrc.pushBack(trStSrc[t]);
                    ++matMinPos;
                }
                if (valMax != 0)
                {
                    matMaxVal.pushBack(valMax);
                    matMaxSrc.pushBack(trStSrc[t]);
                    ++matMaxPos;
                }
            }
            for (Integer t : stTrsO)
            {
                matMinDiagVal[trStSrc[t]] -= trRateUpper[t] * trRatePopul[t];
                matMaxDiagVal[trStSrc[t]] -= trRateLower[t] * trRatePopul[t];
                if (Double.isNaN(matMaxDiagVal[trStSrc[t]]))
                {
                    throw new Error("O MAX");
                }
                if (Double.isNaN(matMinDiagVal[trStSrc[t]]))
                {
                    throw new Error("O MIN");
                }
            }
            */
            for (Integer t : stTrsNP)
            {
                final double val = trRateLower[t] * trRatePopul[t];
                matMinDiagVal[trStSrc[t]] -= val;
                matMaxDiagVal[trStSrc[t]] -= val;
                if (Double.isNaN(matMaxDiagVal[trStSrc[t]]))
                {
                    throw new Error("NP MAX");
                }
                if (Double.isNaN(matMinDiagVal[trStSrc[t]]))
                {
                    throw new Error("NP MIN");
                }
                if (val != 0)
                {
                    matVal.pushBack(val);
                    matSrc.pushBack(trStSrc[t]);
                    ++matPos;
                }
            }
        }
        matMinTrgBeg[stCnt] = matMinPos;
        matMaxTrgBeg[stCnt] = matMaxPos;
        matTrgBeg[stCnt] = matPos;

        PSEModelForVM_CPU res = new PSEModelForVM_CPU
          ( stCnt, trCnt
          , trRateLower
          , trRateUpper
          , trRatePopul
          , trStSrc
          , trStTrg

          , trsIO_

          , matMinDiagVal
          , matMinVal.data()
          , matMinSrc.data()
          , matMinTrgBeg

          , matMaxDiagVal
          , matMaxVal.data()
          , matMaxSrc.data()
          , matMaxTrgBeg

          , matVal.data()
          , matSrc.data()
          , matTrgBeg
          );

        timeBuildModel += System.nanoTime() - timeBeg;
        System.err.printf("Total build time: %s\n", (double)timeBuildModel/1000000000.0);
        return res;
    }

    /**
     * Does a vector-matrix multiplication for this parametrised CTMC's transition
     * probability matrix (uniformised with rate {@code q}) and the vector's min/max
     * components ({@code vectMin} and {@code vectMax}, respectively) passed in.
     * The code follows closely the algorithm described in the article:
     * <p>
     * L. Brim‚ M. Češka‚ S. Dražan and D. Šafránek: Exploring Parameter Space
     * of Stochastic Biochemical Systems Using Quantitative Model Checking
     * In Computer Aided Verification (CAV'13): 107−123, 2013.
     *
     * @param vectMin vector to multiply by when computing minimised result
     * @param resultMin vector to store minimised result in
     * @param vectMax vector to multiply by when computing maximised result
     * @param resultMax vector to store maximised result in
     * @param q uniformisation rate
     */
    final public void vmMult(double vectMin[], double resultMin[], double vectMax[], double resultMax[], double q)
            throws PrismException
    {
        if (gpu)
        {
            modelVMGPU.vmMult(vectMin, resultMin, vectMax, resultMax, q, 1);
        }
        else { modelVMCPU.vmMult(vectMin, resultMin, vectMax, resultMax, q); }
    }

    private double rateParamsLowers(int t)
    {
        return trRateLower[t];
    }

    private double rateParamsUppers(int t)
    {
        return trRateUpper[t];
    }

    private double mvMultMidSumEvalMin(int trans, double vectMinPred, double vectMinState, double q)
    {
        double midSumNumeratorMin = trRatePopul[trans] * vectMinPred - trRatePopul[trans] * vectMinState;
        if (midSumNumeratorMin > 0.0) {
            return rateParamsLowers(trans) * midSumNumeratorMin / q;
        } else {
            return rateParamsUppers(trans) * midSumNumeratorMin / q;
        }
    }

    private double mvMultMidSumEvalMax(int trans, double vectMaxPred, double vectMaxState, double q)
    {
        double midSumNumeratorMax = trRatePopul[trans] * vectMaxPred - trRatePopul[trans] * vectMaxState;
        if (midSumNumeratorMax > 0.0) {
            return rateParamsUppers(trans) * midSumNumeratorMax / q;
        } else {
            return rateParamsLowers(trans) * midSumNumeratorMax / q;
        }
    }

    /**
     * Does a matrix-vector multiplication for this parametrised CTMC's transition
     * probability matrix (uniformised with rate {@code q}) and the vector's min/max
     * components ({@code vectMin} and {@code vectMax}, respectively) passed in.
     * <p>
     * NB: Semantics of {@code mvMult} is <i>not</i> analogical to that of {@link #vmMult},
     * the difference is crucial:  {@code result[k]_i} in {@link #vmMult} is simply
     * the probability of being in state {@code k} after {@code i} iterations starting
     * from the initial state.  On the other hand, {@code mvMult}'s {@code result[k]_i}
     * denotes the probability that an absorbing state (i.e., a state not in {@code subset})
     * is reached after {@code i} iterations starting from {@code k}.
     *
     * @param vectMin vector to multiply by when computing minimised result
     * @param resultMin vector to store minimised result in
     * @param vectMax vector to multiply by when computing maximised result
     * @param resultMax vector to store maximised result in
     * @param subset Only do multiplication for these rows (ignored if null)
     * @param complement If true, {@code subset} is taken to be its complement
     * @param q uniformisation rate
     */
    public void mvMult(double vectMin[], double resultMin[], double vectMax[], double resultMax[], BitSet subset, boolean complement, double q)
            throws PrismException
    {
        if (subset == null) {
            // Loop over all states
            subset = new BitSet(stCnt);
            subset.set(0, stCnt - 1);
        }

        if (complement) {
            subset.flip(0, stCnt - 1);
        }

        for (int state = subset.nextSetBit(0); state >= 0; state = subset.nextSetBit(state + 1)) {
            // Initialise the result
            resultMin[state] = vectMin[state];
            resultMax[state] = vectMax[state];

            for (int trans : trsOBySrc.get(state)) {
                int succ = toState(trans);
                resultMin[state] += mvMultMidSumEvalMin(trans, vectMin[succ], vectMin[state], q);
                resultMax[state] += mvMultMidSumEvalMax(trans, vectMax[succ], vectMax[state], q);
            }

            for (Pair<Integer, Integer> transs : trsIO.get(state)) {
                int trans = transs.first;
                int succTrans = transs.second;

                assert toState(trans) == state;
                int succ = toState(succTrans);

                if (!subset.get(fromState(trans))) {
                    // Reduce to the case of an incoming reaction
                    resultMin[state] += mvMultMidSumEvalMin(trans, vectMin[succ], vectMin[state], q);
                    resultMax[state] += mvMultMidSumEvalMax(trans, vectMax[succ], vectMax[state], q);
                    continue;
                }

                // The rate params of the two considered transitions must be identical
                assert rateParamsLowers(succTrans) == rateParamsLowers(trans);
                assert rateParamsUppers(succTrans) == rateParamsUppers(trans);

                resultMin[state] += mvMultMidSumEvalMin(succTrans, vectMin[succ], vectMin[state], q);
                resultMax[state] += mvMultMidSumEvalMax(succTrans, vectMax[succ], vectMax[state], q);
            }
        }

        // Optimisation: Non-parametrised transitions
        for (int trans = 0; trans < trCnt; trans++) {
            if (isParametrised(trans))
                continue;

            int state = fromState(trans);
            int succ = toState(trans);

            if (!subset.get(state))
                continue;

            double rate = rateParamsLowers(trans) * trRatePopul[trans];
            resultMin[state] += rate * (vectMin[succ] - vectMin[state]) / q;
            resultMax[state] += rate * (vectMax[succ] - vectMax[state]) / q;
        }
    }

    /**
     * Updates the transition rates of this parametrised CTMC according
     * to the given parameter region.
     *
     * @param region parameter region according to which configure the model's
     * parameter space
     * @throws PrismException thrown if rates cannot be evaluated with the new
     * parameter region's bounds
     */
    public void configureParameterSpace(BoxRegion region) throws PrismLangException
    {
        final long timeBeg = System.nanoTime();
        int npCnt = 0;
        for (int t = 0; t < trCnt; ++t)
        {
            trRateLower[t] = trRateExpr[t].evaluateDouble(region.getLowerBounds());
            trRateUpper[t] = trRateExpr[t].evaluateDouble(region.getUpperBounds());
            if (!isParametrised(t)) ++npCnt;
        }

        timeConfigureModel += System.nanoTime() - timeBeg;
        System.err.printf("Total configure time: %s\n", (double)timeConfigureModel/1000000000.0);

        if (modelVMCPU != null) {
            modelVMCPU = buildModelForVM_CPU();
        }
        if (modelVMGPU != null) {
            modelVMGPU = buildModelForVM_GPU();
        }

    }

    /**
     * Returns a particular non-parametrised CTMC associated with
     * the given point of the parameter space of this parametrised CTMC.
     *
     * @param point point of parameter space determining the parameters' values
     * @param modulesFile model file
     * @param constructModel object conducting construction of {@code explicit.CTMC}
     * models
     * @return non-parametrised CTMC obtained by substituting {@code point}
     * for parameter ranges
     * @throws PrismException thrown if an error occurred during construction
     * of the non-parametrised CTMC
     */
    public CTMC instantiate(Point point, ModulesFile modulesFile, explicit.ConstructModel constructModel)
            throws PrismException
    {
        modulesFile = (ModulesFile) modulesFile.deepCopy();
        // Add point dimensions to constants of the modules file
        modulesFile.getConstantValues().addValues(point.getDimensions());
        return (CTMC) constructModel.constructModel(modulesFile);
    }
}
