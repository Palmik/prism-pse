package pse;

import parser.ast.Expression;
import prism.PrismException;

public class PSEModelForVM_CPU
{
    public PSEModelForVM_CPU
      ( int stCnt, int trCnt
      , BoxRegion completeSpace
      , int[] trRateParam
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

        this.lowerBounds = new double[completeSpace.getLowerBounds().length + 1];
        this.upperBounds = new double[completeSpace.getUpperBounds().length + 1];
        this.lowerBounds[0] = 1;
        this.upperBounds[0] = 1;

        this.trRateParam = trRateParam;
        this.trRatePopul = trRatePopul;
        this.trStSrc = trStSrc;
        this.trStTrg = trStTrg;
        this.trsI = trsI;
        this.trsO = trsO;
        this.trsIO = trsIO;
        this.trsNP = trsNP;

        configureParameterSpace(completeSpace);
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

            resMin[v1] += lowerBounds[trRateParam[t]] * trRatePopul[t] * min[v0] * qrec;
            resMax[v1] += upperBounds[trRateParam[t]] * trRatePopul[t] * max[v0] * qrec;
        }

        for (int t : trsO)
        {
            final int v0 = trStSrc[t];

            resMin[v0] -= upperBounds[trRateParam[t]] * trRatePopul[t] * min[v0] * qrec;
            resMax[v0] -= lowerBounds[trRateParam[t]] * trRatePopul[t] * max[v0] * qrec;
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
                resMin[v1] += lowerBounds[trRateParam[t1]] * midSumNumeratorMin * qrec;
            } else
            {
                resMin[v1] += upperBounds[trRateParam[t1]] * midSumNumeratorMin * qrec;
            }

            final double midSumNumeratorMax = trRatePopul[t0] * max[v0] - trRatePopul[t1] * max[v1];
            if (midSumNumeratorMax > 0.0)
            {
                resMax[v1] += upperBounds[trRateParam[t1]] * midSumNumeratorMax * qrec;
            } else
            {
                resMax[v1] += lowerBounds[trRateParam[t1]] * midSumNumeratorMax * qrec;
            }
        }

        for (int t : trsNP)
        {
            final int v0 = trStSrc[t];
            final int v1 = trStTrg[t];

            final double rate = lowerBounds[trRateParam[t]] * trRatePopul[t];

            resMin[v0] -= rate * min[v0] * qrec;
            resMax[v0] -= rate * max[v0] * qrec;

            resMin[v1] += rate * min[v0] * qrec;
            resMax[v1] += rate * max[v0] * qrec;
        }
    }

    public void configureParameterSpace(BoxRegion region)
    {
        System.arraycopy(region.getLowerBounds(), 0, lowerBounds, 1, region.getLowerBounds().length);
        System.arraycopy(region.getUpperBounds(), 0, upperBounds, 1, region.getUpperBounds().length);
        System.err.println("BEG");
        for (int i = 0; i < trCnt; ++i)
        {
            System.err.printf("LB %f\n", lowerBounds[trRateParam[i]]);
        }
        System.err.println("END");
    }

    final private int stCnt;
    final private int trCnt;

    final private int[] trRateParam;
    private double[] lowerBounds;
    private double[] upperBounds;
    final private double[] trRatePopul;

    final private int[] trStSrc;
    final private int[] trStTrg;

    final private int[] trsI;
    final private int[] trsO;
    final private int[] trsIO;
    final private int[] trsNP;
}