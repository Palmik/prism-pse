#pragma OPENCL EXTENSION cl_khr_fp64 : enable
#pragma OPENCL EXTENSION cl_amd_printf : enable

//#define MAD(A, B, C) (mad(A,B,C))
#define MAD(A, B, C) ((A) * (B) + (C))

typedef double real;

__kernel void WeightedSumTo
  ( const uint n
  , const real w
  , __global real const* restrict vec
  , __global real* restrict sum
  )
{
    uint ii = get_global_id(0);
    if (ii < n)
    {
        sum[ii] = MAD(vec[ii], w, sum[ii]);
    }
}

__kernel void PSE_MV_IO
  ( const uint matRowCnt
  , __global real const* restrict matLowerVal
  , __global real const* restrict matUpperVal
  , __global uint const* restrict matCol
  , __global uint const* restrict matRow
  , __global uint const* restrict matRowBeg

  , __global real const* restrict in
  , __global real* restrict out
  )
{
    uint ii = get_global_id(0);
    if (ii < matRowCnt)
    {
        const uint v0 = matRow[ii];
        const uint tb = matRowBeg[ii];
        const uint te = matRowBeg[ii + 1];
        real dot = out[v0];
        for (int jj = tb; jj < te; ++jj) {
            const uint v1 = matCol[jj];

            const real diff = in[v1] - in[v0];
            if (diff > 0) {
                dot = MAD(matLowerVal[jj], diff, dot);
            } else {
                dot = MAD(matUpperVal[jj], diff, dot);
            }
        }
        out[v0] = dot;
    }
}

__kernel void PSE_MV_NP
  ( const uint matRowCnt
  , __global real const* restrict matVal
  , __global uint const* restrict matCol
  , __global uint const* restrict matRow
  , __global uint const* restrict matRowBeg

  , __global real const* restrict in
  , __global real* restrict out
  )
{
    uint ii = get_global_id(0);
    if (ii < matRowCnt)
    {
        const uint v0 = matRow[ii];
        const uint tb = matRowBeg[ii];
        const uint te = matRowBeg[ii + 1];
        real dot = out[v0];
        for (uint jj = tb; jj < te; ++jj)
        {
            const uint v1 = matCol[jj];
            const real rate = matVal[jj];
            dot = MAD(rate, in[v1] - in[v0], dot);
        }
        out[v0] = dot;
	}
}


__kernel void SpMV1_CS
  ( const uint stCnt
  , __global real const* restrict val
  , __global uint const* restrict trg
  , __global uint const* restrict srcBeg

  , __global real const* restrict vi
  , __global real* restrict vo
  )
{
  int v0 = get_global_id(0);

  if (v0 < stCnt)
  {
    uint cb = srcBeg[v0];
    uint ce = srcBeg[v0 + 1];
    real dot = vo[v0];
    for (uint i = cb; i < ce; ++i)
    {
      dot = MAD(val[i], vi[trg[i]], dot);
    }
    vo[v0] = dot;
  }
}

__kernel void SpMV2_CS
  ( const uint stCnt
  , __global real const* restrict diagVal1
  , __global real const* restrict diagVal2
  , __global real const* restrict val
  , __global uint const* restrict trg
  , __global uint const* restrict srcBeg

  , __global real const* restrict vi1
  , __global real const* restrict vi2
  , __global real* restrict vo1
  , __global real* restrict vo2
  )
{
  int v0 = get_global_id(0);

  if (v0 < stCnt)
  {
    real dot1 = vi1[v0] * diagVal1[v0]; //vo1[v0] + vi1[v0] * diagVal1[v0];
    real dot2 = vi2[v0] * diagVal2[v0]; //vo2[v0] + vi2[v0] * diagVal2[v0];

    uint cb = srcBeg[v0];
    uint ce = srcBeg[v0 + 1];

    for (uint i = cb; i < ce; ++i)
    {
      dot1 = MAD(val[i], vi1[trg[i]], dot1);
      dot2 = MAD(val[i], vi2[trg[i]], dot2);
    }
    vo1[v0] = dot1;
    vo2[v0] = dot2;
  }
}

__kernel void SpMVIO_CS
  ( const uint stCnt
  , __global real const* restrict lowerVal0
  , __global real const* restrict lowerVal1
  , __global real const* restrict upperVal0
  , __global real const* restrict upperVal1
  , __global uint const* restrict src
  , __global uint const* restrict trgBeg

  , __global real const* restrict min0
  , __global real const* restrict max0
  , __global real* restrict min1
  , __global real* restrict max1
  )
{
  int v1 = get_global_id(0);

  if (v1 < stCnt)
  {
    real dot1 = min1[v1];
    real dot2 = max1[v1];

    uint cb = trgBeg[v1];
    uint ce = trgBeg[v1 + 1];

    for (uint i = cb; i < ce; ++i)
    {
      const uint v0 = src[i];
      const real rlowerMin = (lowerVal0[i] * min0[v0] - lowerVal1[i] * min0[v1]);
      const real rupperMax = (upperVal0[i] * max0[v0] - upperVal1[i] * max0[v1]);
      if (rlowerMin > 0.0)
      {
        dot1 += rlowerMin;
      }
      else
      {
        dot1 += (upperVal0[i] * min0[v0] - upperVal1[i] * min0[v1]);
      }
      if (rupperMax > 0.0)
      {
        dot2 += rupperMax;
      }
      else
      {
        dot2 += (lowerVal0[i] * max0[v0] - lowerVal1[i] * max0[v1]);
      }
    }
    min1[v1] = dot1;
    max1[v1] = dot2;
  }
}
