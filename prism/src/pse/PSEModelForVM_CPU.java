package pse;

import parser.ast.Expression;
import prism.PrismException;

public class PSEModelForVM_CPU
{
    public PSEModelForVM_CPU
      ( int stCnt, int trCnt
      , double[] trRateLower
      , double[] trRateUpper
      , double[] trRatePopul
      , int[] trStSrc
      , int[] trStTrg

      , int[] trsIO

      , double[] matMinDiagVal
      , double[] matMinVal
      , int[] matMinSrc
      , int[] matMinTrgBeg

      , double[] matMaxDiagVal
      , double[] matMaxVal
      , int[] matMaxSrc
      , int[] matMaxTrgBeg

      , double[] matVal
      , int[] matSrc
      , int[] matTrgBeg
      )
    {
        this.stCnt = stCnt;
        this.trCnt = trCnt;

        this.trRatePopul = trRatePopul;
        this.trRateLower = trRateLower;
        this.trRateUpper = trRateUpper;

        this.trStSrc = trStSrc;
        this.trStTrg = trStTrg;

        this.trsIO = trsIO;

        this.matMinDiagVal = matMinDiagVal;
        this.matMinVal = matMinVal;
        this.matMinSrc = matMinSrc;
        this.matMinTrgBeg = matMinTrgBeg;

        this.matMaxDiagVal = matMaxDiagVal;
        this.matMaxVal = matMaxVal;
        this.matMaxSrc = matMaxSrc;
        this.matMaxTrgBeg = matMaxTrgBeg;

        this.matVal = matVal;
        this.matSrc = matSrc;
        this.matTrgBeg = matTrgBeg;
    }

    final public void vmMult
        ( final double min[], final double resMin[]
        , final double max[], final double resMax[]
        , double q
        )
    {
        System.arraycopy(min, 0, resMin, 0, min.length);
        System.arraycopy(max, 0, resMax, 0, max.length);
        final double qrec = 1 / q;

        int IOZ = 0;
        for (int ii = 0; ii < trsIO.length; )
        {
            final int t0 = trsIO[ii++]; // t0 goes from v0 to v1
            final int t1 = trsIO[ii++]; // t1 goes from v1 to v2

            final int v0 = trStSrc[t0];
            final int v1 = trStTrg[t0];
            // int v1 = st; // == trStTrg[t0] == trStSrc[t1]
            // int v2 = trStTrg[t1];

            // The rate params of t1 and t2 must be identical
            // assert trRateLower[t0] == trRateLower[t1];
            // assert trRateUpper[t0] == trRateUpper[t1];

            final double midSumNumeratorMin = trRatePopul[t0] * min[v0] - trRatePopul[t1] * min[v1];
            if (midSumNumeratorMin > 0.0)
            {
                resMin[v1] += trRateLower[t1] * midSumNumeratorMin * qrec;
            } else
            {
                resMin[v1] += trRateUpper[t1] * midSumNumeratorMin * qrec;
            }

            final double midSumNumeratorMax = trRatePopul[t0] * max[v0] - trRatePopul[t1] * max[v1];
            if (midSumNumeratorMax > 0.0)
            {
                resMax[v1] += trRateUpper[t1] * midSumNumeratorMax * qrec;
            } else
            {
                resMax[v1] += trRateLower[t1] * midSumNumeratorMax * qrec;
            }
        }

        vmMultRawMinOrMax(matMinDiagVal, matMinVal, matMinSrc, matMinTrgBeg, min, resMin, qrec);
        vmMultRawMinOrMax(matMaxDiagVal, matMaxVal, matMaxSrc, matMaxTrgBeg, max, resMax, qrec);
        vmMultRaw(matVal, matSrc, matTrgBeg, min, max, resMin, resMax, qrec);
    }

    final private void vmMultRawMinOrMax
        ( final double[] mDiagVal, final double[] mVal, final int[] mSrc, final int[] mTrgBeg
        , final double[] vi, final double[] vo
        , final double qrec
        )
    {
       for (int v1 = 0; v1 < stCnt; ++v1)
       {
           final int ib = mTrgBeg[v1];
           final int ie = mTrgBeg[v1 + 1];
           vo[v1] += mDiagVal[v1] * vi[v1] * qrec;
           for (int ii = ib; ii < ie; ++ii)
           {
               final int v0 = mSrc[ii];
               final double rate = mVal[ii];
               vo[v1] += rate * vi[v0] * qrec;
           }
       }
    }

    final private void vmMultRaw
        ( final double[] mVal, final int[] mSrc, final int[] mTrgBeg
        , final double[] vmini, final double[] vmaxi, final double[] vmino, final double[] vmaxo
        , final double qrec
        )
    {
        for (int v1 = 0; v1 < stCnt; ++v1)
        {
            final int ib = mTrgBeg[v1];
            final int ie = mTrgBeg[v1 + 1];
            for (int ii = ib; ii < ie; ++ii)
            {
                final int v0 = mSrc[ii];
                final double rate = mVal[ii];
                vmino[v1] += rate * vmini[v0] * qrec;
                vmaxo[v1] += rate * vmaxi[v0] * qrec;
            }
        }
    }

    final private int stCnt;
    final private int trCnt;

    private double[] trRateLower;
    private double[] trRateUpper;
    final private double[] trRatePopul;

    final private int[] trStSrc;
    final private int[] trStTrg;

    final private int[] trsIO;

    final private double[] matMinDiagVal;
    final private double[] matMinVal;
    final private int[] matMinSrc;
    final private int[] matMinTrgBeg;

    final private double[] matMaxDiagVal;
    final private double[] matMaxVal;
    final private int[] matMaxSrc;
    final private int[] matMaxTrgBeg;

    final private double[] matVal;
    final private int[] matSrc;
    final private int[] matTrgBeg;
}