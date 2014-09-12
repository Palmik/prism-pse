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

      , int trsICnt
      , double[] trsIVal
      , int[] trsISrc
      , int[] trsITrgBeg

      , int trsOCnt
      , double[] trsOVal
      , int[] trsOSrcBeg

      , int trsNPCnt
      , double[] trsNPVal
      , int[] trsNPTrg
      , int[] trsNPSrcBeg
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

        this.trsICnt = trsICnt;
        this.trsIVal = trsIVal;
        this.trsISrc = trsISrc;
        this.trsITrgBeg = trsITrgBeg;

        this.trsOCnt = trsOCnt;
        this.trsOVal = trsOVal;
        this.trsOSrcBeg = trsOSrcBeg;

        this.trsNPCnt = trsNPCnt;
        this.trsNPVal = trsNPVal;
        this.trsNPTrg = trsNPTrg;
        this.trsNPSrcBeg = trsNPSrcBeg;
    }

    public void vmMult(double min[], double resMin[], double max[], double resMax[], double q)
    {
        System.arraycopy(min, 0, resMin, 0, min.length);
        System.arraycopy(max, 0, resMax, 0, max.length);
        double qrec = 1 / q;

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

        for (int v1 = 0; v1 < stCnt; ++v1)
        {
            for (int ii = trsITrgBeg[v1]; ii < trsITrgBeg[v1 + 1]; ++ii)
            {
                final int v0 = trsISrc[ii];
                final double rateLower = trsIVal[2*ii];
                final double rateUpper = trsIVal[2*ii+1];

                resMin[v1] += rateLower * min[v0] * qrec;
                resMax[v1] += rateUpper * max[v0] * qrec;
            }
        }

        for (int v0 = 0; v0 < stCnt; ++v0)
        {
            for (int ii = trsOSrcBeg[v0]; ii < trsOSrcBeg[v0 + 1]; ++ii)
            {
                final double rateLower = trsOVal[2*ii];
                final double rateUpper = trsOVal[2*ii+1];

                resMin[v0] -= rateUpper * min[v0] * qrec;
                resMax[v0] -= rateLower * max[v0] * qrec;
            }
        }

        for (int v0 = 0; v0 < stCnt; ++v0)
        {
            for (int ii = trsNPSrcBeg[v0]; ii < trsNPSrcBeg[v0 + 1]; ++ii)
            {
                final int v1 = trsNPTrg[ii];
                final double rate = trsNPVal[ii];

                resMin[v0] -= rate * min[v0] * qrec;
                resMax[v0] -= rate * max[v0] * qrec;

                resMin[v1] += rate * min[v0] * qrec;
                resMax[v1] += rate * max[v0] * qrec;
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

    final private int trsICnt;
    final private double[] trsIVal;
    final private int[] trsISrc;
    final private int[] trsITrgBeg;

    final private int trsOCnt;
    final private double[] trsOVal;
    final private int[] trsOSrcBeg;

    final private int trsNPCnt;
    final private double[] trsNPVal;
    final private int[] trsNPTrg;
    final private int[] trsNPSrcBeg;
}