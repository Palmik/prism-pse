#pragma OPENCL EXTENSION cl_khr_fp64 : enable
#pragma OPENCL EXTENSION cl_amd_printf : enable

typedef double real;


__kernel void SpMV1_CS
  ( const uint stCnt
  , __global real const* val
  , __global uint const* trg
  , __global uint const* srcBeg

  , __global real const* vi
  , __global real* vo
  )
{
  int v0 = get_global_id(0);

  real prod = vo[v0];
  if (v0 < stCnt)
  {
    uint cb = srcBeg[v0];
    uint ce = srcBeg[v0 + 1];

    for (uint i = cb; i < ce; ++i)
    {
      prod += val[i] * vi[trg[i]];
    }
    vo[v0] = prod;
  }
}

__kernel void PSE_MV_IO
  ( const uint matRowCnt
  , __global real const* matLowerVal
  , __global real const* matUpperVal
  , __global uint const* matCol
  , __global uint const* matRow
  , __global uint const* matRowBeg

  , __global real const* in
  , __global real* out
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
                dot += matLowerVal[jj] * diff;
            } else {
                dot += matUpperVal[jj] * diff;
            }
        }
        out[v0] = dot;
    }
}

__kernel void PSE_MV_NP
  ( const uint matRowCnt
  , __global real const* matVal
  , __global uint const* matCol
  , __global uint const* matRow
  , __global uint const* matRowBeg

  , __global real const* in
  , __global real* out
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
            dot += rate * (in[v1] - in[v0]);
        }
        out[v0] = dot;
	}
}

__kernel void SpMV2_CS
  ( const uint stCnt
  , __global real const* diagVal1
  , __global real const* diagVal2
  , __global real const* val
  , __global uint const* trg
  , __global uint const* srcBeg

  , __global real const* vi1
  , __global real const* vi2
  , __global real* vo1
  , __global real* vo2
  )
{
  int v0 = get_global_id(0);

  if (v0 < stCnt)
  {
    real prod1 = vi1[v0] * diagVal1[v0]; //vo1[v0] + vi1[v0] * diagVal1[v0];
    real prod2 = vi2[v0] * diagVal2[v0]; //vo2[v0] + vi2[v0] * diagVal2[v0];

    uint cb = srcBeg[v0];
    uint ce = srcBeg[v0 + 1];

    for (uint i = cb; i < ce; ++i)
    {
      prod1 += val[i] * vi1[trg[i]];
      prod2 += val[i] * vi2[trg[i]];
    }
    vo1[v0] = prod1;
    vo2[v0] = prod2;
  }
}

__kernel void SpMVIO_CS
  ( const uint stCnt
  , __global real const* lowerVal0
  , __global real const* lowerVal1
  , __global real const* upperVal0
  , __global real const* upperVal1
  , __global uint const* src
  , __global uint const* trgBeg

  , __global real const* min0
  , __global real const* max0
  , __global real* min1
  , __global real* max1
  )
{
  int v1 = get_global_id(0);

  if (v1 < stCnt)
  {
    real prod1 = min1[v1];
    real prod2 = max1[v1];

    uint cb = trgBeg[v1];
    uint ce = trgBeg[v1 + 1];

    for (uint i = cb; i < ce; ++i)
    {
      const uint v0 = src[i];
      const real rlowerMin = (lowerVal0[i] * min0[v0] - lowerVal1[i] * min0[v1]);
      const real rupperMax = (upperVal0[i] * max0[v0] - upperVal1[i] * max0[v1]);
      if (rlowerMin > 0.0)
      {
        prod1 += rlowerMin;
      }
      else
      {
        prod1 += (upperVal0[i] * min0[v0] - upperVal1[i] * min0[v1]);
      }
      if (rupperMax > 0.0)
      {
        prod2 += rupperMax;
      }
      else
      {
        prod2 += (lowerVal0[i] * max0[v0] - lowerVal1[i] * max0[v1]);
      }
    }
    min1[v1] = prod1;
    max1[v1] = prod2;
  }
}
