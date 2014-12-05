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

__kernel void PSE_VM_I
  ( const uint matRowCnt
  , __global real const* restrict matVal
  , __global uint const* restrict matCol
  , __global uint const* restrict matRowBeg

  , __global real const* restrict in
  , __global real* restrict out
  )
{
  int v0 = get_global_id(0);

  if (v0 < matRowCnt)
  {
    uint cb = matRowBeg[v0];
    uint ce = matRowBeg[v0 + 1];
    real dot = out[v0];
    for (uint i = cb; i < ce; ++i)
    {
      dot = MAD(matVal[i], in[matCol[i]], dot);
    }
    out[v0] = dot;
  }
}

__kernel void PSE_VM_NP
  ( const uint matRowCnt
  , __global real const* restrict matDiaVal
  , __global real const* restrict matVal
  , __global uint const* restrict matCol
  , __global uint const* restrict matRowBeg

  , __global real const* restrict in
  , __global real* restrict out
  )
{
  int v0 = get_global_id(0);

  if (v0 < matRowCnt)
  {
    real dot = in[v0] * matDiaVal[v0]; //out[v0] + in[v0] * matDiaVal[v0];

    uint cb = matRowBeg[v0];
    uint ce = matRowBeg[v0 + 1];
    for (uint i = cb; i < ce; ++i)
    {
      dot = MAD(matVal[i], in[matCol[i]], dot);
    }
    out[v0] = dot;
  }
}

__kernel void PSE_VM_IO
  ( const uint matRowCnt
  , __global real const* restrict matLowerVal0
  , __global real const* restrict matLowerVal1
  , __global real const* restrict matUpperVal0
  , __global real const* restrict matUpperVal1
  , __global uint const* restrict matCol
  , __global uint const* restrict matRowBeg

  , __global real const* restrict in
  , __global real* restrict out
  )
{
  int v1 = get_global_id(0);

  if (v1 < matRowCnt)
  {
    real dot = out[v1];

    uint cb = matRowBeg[v1];
    uint ce = matRowBeg[v1 + 1];

    for (uint i = cb; i < ce; ++i)
    {
      const uint v0 = matCol[i];
      const real rlower = (matLowerVal0[i] * in[v0] - matLowerVal1[i] * in[v1]);
      if (rlower > 0.0)
      {
        dot += rlower;
      }
      else
      {
        dot += (matUpperVal0[i] * in[v0] - matUpperVal1[i] * in[v1]);
      }
    }
    out[v1] = dot;
  }
}
