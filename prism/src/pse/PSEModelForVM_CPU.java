package pse;

import parser.ast.Expression;
import prism.PrismException;

public class PSEModelForVM_CPU
{
    public PSEModelForVM_CPU
      ( int stCnt, int trCnt
        , Expression[] trRateParam
                    , double[] trRateLower
                    , double[] trRateUpper
                    , double[] trRatePopul
                    , int[] trStSrc
                    , int[] trStTrg
                    , int[] trsI
                    , int[] trsO
                    , int[] trsIO
                    , int[] trsNP
            )
    {
        this.stCnt = stCnt;
        this.trCnt = trCnt;
        this.trRateParam = trRateParam;
        this.trRateLower = trRateLower;
        this.trRateUpper = trRateUpper;
        this.trRatePopul = trRatePopul;
        this.trStSrc = trStSrc;
        this.trStTrg = trStTrg;
        this.trsI = trsI;
        this.trsO = trsO;
        this.trsIO = trsIO;
        this.trsNP = trsNP;
    }

    public void vmMult(double min[], double resMin[], double max[], double resMax[], double q)
    {
        System.arraycopy(min, 0, resMin, 0, min.length);
        System.arraycopy(max, 0, resMax, 0, max.length);
        double qrec = 1 / q;

        for (int t : trsI)
        {
            final int v0 = trStSrc[t];
            final int v1 = trStTrg[t];

            resMin[v1] += trRateLower[t] * trRatePopul[t] * min[v0] * qrec;
            resMax[v1] += trRateUpper[t] * trRatePopul[t] * max[v0] * qrec;
        }

        for (int t : trsO)
        {
            final int v0 = trStSrc[t];

            resMin[v0] -= trRateUpper[t] * trRatePopul[t] * min[v0] * qrec;
            resMax[v0] -= trRateLower[t] * trRatePopul[t] * max[v0] * qrec;
        }

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

        for (int t : trsNP)
        {
            final int v0 = trStSrc[t];
            final int v1 = trStTrg[t];

            final double rate = trRateLower[t] * trRatePopul[t];

            resMin[v0] -= rate * min[v0] * qrec;
            resMax[v0] -= rate * max[v0] * qrec;

            resMin[v1] += rate * min[v0] * qrec;
            resMax[v1] += rate * max[v0] * qrec;
        }
    }

    public void configureParameterSpace(BoxRegion region) throws PrismException
    {
        for (int t = 0; t < trCnt; t++)
        {
            trRateLower[t] = trRateParam[t].evaluateDouble(region.getLowerBounds());
            trRateUpper[t] = trRateParam[t].evaluateDouble(region.getUpperBounds());
        }
    }

    final private int stCnt;
    final private int trCnt;

    final private Expression[] trRateParam;
    final private double[] trRateLower;
    final private double[] trRateUpper;
    final private double[] trRatePopul;

    final private int[] trStSrc;
    final private int[] trStTrg;

    final private int[] trsI;
    final private int[] trsO;
    final private int[] trsIO;
    final private int[] trsNP;
}