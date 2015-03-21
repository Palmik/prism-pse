#pragma OPENCL EXTENSION cl_khr_fp64 : enable
#pragma OPENCL EXTENSION cl_amd_printf : enable

#define WARP_SIZE 32

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

__kernel void WeightedSumToBoth
  ( const uint n
  , const real w
  , __global real const* restrict vec1
  , __global real const* restrict vec2
  , __global real* restrict sum1
  , __global real* restrict sum2
  )
{
    uint ii = get_global_id(0);
    if (ii < n)
    {
        sum1[ii] = MAD(vec1[ii], w, sum1[ii]);
        sum2[ii] = MAD(vec2[ii], w, sum2[ii]);
    }
}

__kernel void PSE_MV_IO_CSR_SCALAR
  ( const uint matIORowCnt
  , __global real const* restrict matIOLowerVal
  , __global real const* restrict matIOUpperVal
  , __global uint const* restrict matIOCol
  , __global uint const* restrict matIORow
  , __global uint const* restrict matIORowBeg

  , __global real const* restrict in
  , __global real* restrict out
  )
{
    uint ii = get_global_id(0);
    if (ii < matIORowCnt)
    {
        const uint v0 = matIORow[ii];
        const uint tb = matIORowBeg[ii];
        const uint te = matIORowBeg[ii + 1];
        real dot = out[v0];
        for (int jj = tb; jj < te; ++jj) {
            const uint v1 = matIOCol[jj];

            const real diff = in[v1] - in[v0];
            if (diff > 0) {
                dot = MAD(matIOLowerVal[jj], diff, dot);
            } else {
                dot = MAD(matIOUpperVal[jj], diff, dot);
            }
        }
        out[v0] = dot;
    }
}

__kernel void PSE_MV_NP_CSR_SCALAR
  ( const uint matIORowCnt
  , __global real const* restrict matVal
  , __global uint const* restrict matIOCol
  , __global uint const* restrict matIORow
  , __global uint const* restrict matIORowBeg

  , __global real const* restrict in
  , __global real* restrict out
  )
{
    uint ii = get_global_id(0);
    if (ii < matIORowCnt)
    {
        const uint v0 = matIORow[ii];
        const uint tb = matIORowBeg[ii];
        const uint te = matIORowBeg[ii + 1];
        real dot = out[v0];
        for (uint jj = tb; jj < te; ++jj)
        {
            const uint v1 = matIOCol[jj];
            const real rate = matVal[jj];
            dot = MAD(rate, in[v1] - in[v0], dot);
        }
        out[v0] = dot;
	}
}

__kernel void PSE_MV_NP_CSR_VECTOR
  ( const uint matIORowCnt
  , __global real const* restrict matVal
  , __global uint const* restrict matIOCol
  , __global uint const* restrict matIORow
  , __global uint const* restrict matIORowBeg

  , __global real const* restrict in
  , __global real* restrict out

  , __local volatile real* sdata
  )
{
    uint id = get_global_id(0);
    uint warp_id = id / WARP_SIZE; // == The warp of this worker
    uint warp_pos = id & (WARP_SIZE - 1); // == id % WARP_SIZE == The position of this worker within the warp
    uint warp_cnt = get_global_size(0) / WARP_SIZE;
    for (uint ii = warp_id; ii < matIORowCnt; ii += warp_cnt)
    {
        const uint v0 = matIORow[ii];
        const uint tb = matIORowBeg[ii];
        const uint te = matIORowBeg[ii + 1];
        real dot = out[v0];
        for (uint jj = tb + warp_pos; jj < te; jj += WARP_SIZE)
        {
            const uint v1 = matIOCol[jj];
            const real rate = matVal[jj];
            dot = MAD(rate, in[v1] - in[v0], dot);
        }

        const int i = get_local_id(0);
        sdata[i] = dot;
        sdata[i] = dot += sdata[i + 16];
        sdata[i] = dot += sdata[i + 8];
        sdata[i] = dot += sdata[i + 4];
        sdata[i] = dot += sdata[i + 2];
        sdata[i] = dot += sdata[i + 1];

        if (warp_pos == 0)
        {
            out[v0] = dot;
        }
	}
}

// PSE VM

__kernel void PSE_VM_I_
  ( const uint matRowCnt
  , __global real const* restrict matValMin
  , __global real const* restrict matValMax
  , __global uint const* restrict matCol
  , __global uint const* restrict matRowBeg

  , __global real const* restrict inMin
  , __global real const* restrict inMax
  , __global real* restrict outMin
  , __global real* restrict outMax
  )
{
  const int v0 = get_global_id(0);

  if (v0 < matRowCnt)
  {
    const uint ce = matRowBeg[v0 + 1];

    real dotMin = outMin[v0];
    real dotMax = outMax[v0];
    for (uint i = matRowBeg[v0]; i < ce; ++i)
    {
      dotMin = MAD(matValMin[i], inMin[matCol[i]], dotMin);
      dotMax = MAD(matValMax[i], inMax[matCol[i]], dotMax);
    }
    outMin[v0] = dotMin;
    outMax[v0] = dotMax;
  }
}

__kernel void PSE_VM_NP_
  ( const uint matRowCnt
  , __global real const* restrict matDiaVal
  , __global real const* restrict matVal
  , __global uint const* restrict matCol
  , __global uint const* restrict matRowBeg

  , __global real const* restrict in
  , __global real* restrict out
  )
{
  const int v0 = get_global_id(0);

  if (v0 < matRowCnt)
  {
    const uint ce = matRowBeg[v0 + 1];

    real dot = in[v0] * matDiaVal[v0]; //out[v0] + in[v0] * matDiaVal[v0];
    for (uint i = matRowBeg[v0]; i < ce; ++i)
    {
      dot = MAD(matVal[i], in[matCol[i]], dot);
    }
    out[v0] = dot;
  }
}

__kernel void PSE_VM_NP_BOTH_
  ( const uint matRowCnt
  , __global real const* restrict matDiaValMin
  , __global real const* restrict matDiaValMax
  , __global real const* restrict matVal
  , __global uint const* restrict matCol
  , __global uint const* restrict matRowBeg

  , __global real const* restrict inMin
  , __global real const* restrict inMax
  , __global real* restrict outMin
  , __global real* restrict outMax
  )
{
  const int v0 = get_global_id(0);

  if (v0 < matRowCnt)
  {
    const uint ce = matRowBeg[v0 + 1];

    real dotMin = inMin[v0] * matDiaValMin[v0]; //out[v0] + in[v0] * matDiaVal[v0];
    real dotMax = inMax[v0] * matDiaValMax[v0]; //out[v0] + in[v0] * matDiaVal[v0];
    for (uint i = matRowBeg[v0]; i < ce; ++i)
    {
      dotMin = MAD(matVal[i], inMin[matCol[i]], dotMin);
      dotMax = MAD(matVal[i], inMax[matCol[i]], dotMax);
    }
    outMin[v0] = dotMin;
    outMax[v0] = dotMax;
  }
}
__kernel void PSE_VM_DIAG_
  ( const uint matRowCnt
  , __global real const* restrict matDiaVal
  , __global real const* restrict in
  , __global real* restrict out
  )
{
  const int v0 = get_global_id(0);

  if (v0 < matRowCnt)
  {
    out[v0] = in[v0] * matDiaVal[v0]; //out[v0] + in[v0] * matDiaVal[v0];
  }
}

__kernel void PSE_VM_DIAG_BOTH_
  ( const uint matRowCnt
  , __global real const* restrict matDiaValMin
  , __global real const* restrict matDiaValMax
  , __global real const* restrict inMin
  , __global real const* restrict inMax
  , __global real* restrict outMin
  , __global real* restrict outMax
  )
{
  const int v0 = get_global_id(0);

  if (v0 < matRowCnt)
  {
    outMin[v0] = inMin[v0] * matDiaValMin[v0]; //out[v0] + in[v0] * matDiaVal[v0];
    outMax[v0] = inMax[v0] * matDiaValMax[v0]; //out[v0] + in[v0] * matDiaVal[v0];
  }
}

__kernel void PSE_VM_IO_
  ( const uint matRowCnt
  , __global real const* restrict matLowerVal0
  , __global real const* restrict matLowerVal1
  , __global real const* restrict matUpperVal0
  , __global real const* restrict matUpperVal1
  , __global uint const* restrict matCol
  , __global uint const* restrict matRowBeg

  , __global real const* restrict inMin
  , __global real const* restrict inMax
  , __global real* restrict outMin
  , __global real* restrict outMax
  )
{
  const int v1 = get_global_id(0);

  if (v1 < matRowCnt)
  {
    real dotMin = outMin[v1];
    real dotMax = outMax[v1];
    for (uint i = matRowBeg[v1]; i < matRowBeg[v1 + 1]; ++i)
    {
      const uint v0 = matCol[i];
      const real rlowerMin = (matLowerVal0[i] * inMin[v0] - matLowerVal1[i] * inMin[v1]);
      const real rupperMax = (matUpperVal0[i] * inMax[v0] - matUpperVal1[i] * inMax[v1]);

      if (rlowerMin > 0.0) {
        dotMin += rlowerMin;
      } else {
        dotMin += (matUpperVal0[i] * inMin[v0] - matUpperVal1[i] * inMin[v1]);
      }
      if (rupperMax > 0.0) {
        dotMax += rupperMax;
      } else {
        dotMax += (matLowerVal0[i] * inMax[v0] - matLowerVal1[i] * inMax[v1]);
      }
    }
    outMin[v1] = dotMin;
    outMax[v1] = dotMax;
  }
}

// PSE VM MANY

__kernel void PSE_VM_I_MANY
  ( const uint matCnt
  , const uint matRowCnt
  , __global real const* restrict matValMin
  , __global real const* restrict matValMax
  , __global uint const* restrict matCol
  , __global uint const* restrict matRowBeg

  , __global real const* restrict inMin
  , __global real const* restrict inMax
  , __global real* restrict outMin
  , __global real* restrict outMax
  )
{
  const int v0 = get_global_id(0);
  const int v0_ = v0 % matRowCnt;
  const int matId = v0 / matRowCnt;
  const int matOff = matId * matRowBeg[matRowCnt];
  const int matColOff = matId * matRowCnt;

  if (v0 < matRowCnt * matCnt)
  {
    const uint ce = matRowBeg[v0_ + 1];

    real dotMin = outMin[v0];
    real dotMax = outMax[v0];
    for (uint i = matRowBeg[v0_]; i < ce; ++i)
    {
      dotMin = MAD(matValMin[i + matOff], inMin[matCol[i] + matColOff], dotMin);
      dotMax = MAD(matValMax[i + matOff], inMax[matCol[i] + matColOff], dotMax);
    }
    outMin[v0] = dotMin;
    outMax[v0] = dotMax;
  }
}

__kernel void PSE_VM_NP_MANY
  ( const uint matCnt
  , const uint matRowCnt
  , __global real const* restrict matDiaVal
  , __global real const* restrict matVal
  , __global uint const* restrict matCol
  , __global uint const* restrict matRowBeg

  , __global real const* restrict in
  , __global real* restrict out
  )
{
  const int v0 = get_global_id(0);
  const int v0_ = v0 % matRowCnt;
  const int matColOff = (v0 / matRowCnt) * matRowCnt;

  if (v0 < matRowCnt * matCnt)
  {
    const uint ce = matRowBeg[v0_ + 1];

    real dot = in[v0] * matDiaVal[v0]; //out[v0] + in[v0] * matDiaVal[v0];
    for (uint i = matRowBeg[v0_]; i < ce; ++i)
    {
      dot = MAD(matVal[i], in[matCol[i] + matColOff], dot);
    }
    out[v0] = dot;
  }
}

__kernel void PSE_VM_NP_BOTH_MANY
  ( const uint matCnt
  , const uint matRowCnt
  , __global real const* restrict matDiaValMin
  , __global real const* restrict matDiaValMax
  , __global real const* restrict matVal
  , __global uint const* restrict matCol
  , __global uint const* restrict matRowBeg

  , __global real const* restrict inMin
  , __global real const* restrict inMax
  , __global real* restrict outMin
  , __global real* restrict outMax
  )
{
  const int v0 = get_global_id(0);
  const int v0_ = v0 % matRowCnt;
  const int matColOff = (v0 / matRowCnt) * matRowCnt;

  if (v0 < matRowCnt * matCnt)
  {
    const uint ce = matRowBeg[v0_ + 1];

    real dotMin = inMin[v0] * matDiaValMin[v0]; //out[v0] + in[v0] * matDiaVal[v0];
    real dotMax = inMax[v0] * matDiaValMax[v0]; //out[v0] + in[v0] * matDiaVal[v0];
    for (uint i = matRowBeg[v0_]; i < ce; ++i)
    {
      dotMin = MAD(matVal[i], inMin[matCol[i] + matColOff], dotMin);
      dotMax = MAD(matVal[i], inMax[matCol[i] + matColOff], dotMax);
    }
    outMin[v0] = dotMin;
    outMax[v0] = dotMax;
  }
}
__kernel void PSE_VM_DIAG_MANY
  ( const uint matCnt
  , const uint matRowCnt
  , __global real const* restrict matDiaVal
  , __global real const* restrict in
  , __global real* restrict out
  )
{
  const int v0 = get_global_id(0);
  const int v0_ = v0 % matRowCnt;

  if (v0 < matRowCnt * matCnt)
  {
    out[v0] = in[v0] * matDiaVal[v0_]; //out[v0] + in[v0] * matDiaVal[v0];
  }
}

__kernel void PSE_VM_DIAG_BOTH_MANY
  ( const uint matCnt
  , const uint matRowCnt
  , __global real const* restrict matDiaValMin
  , __global real const* restrict matDiaValMax
  , __global real const* restrict inMin
  , __global real const* restrict inMax
  , __global real* restrict outMin
  , __global real* restrict outMax
  )
{
  const int v0 = get_global_id(0);
  const int v0_ = v0 % matRowCnt;

  if (v0 < matRowCnt * matCnt)
  {
    outMin[v0] = inMin[v0] * matDiaValMin[v0_]; //out[v0] + in[v0] * matDiaVal[v0];
    outMax[v0] = inMax[v0] * matDiaValMax[v0_]; //out[v0] + in[v0] * matDiaVal[v0];
  }
}

__kernel void PSE_VM_IO_MANY
  ( const uint matCnt
  , const uint matRowCnt
  , __global real const* restrict matLowerVal0
  , __global real const* restrict matLowerVal1
  , __global real const* restrict matUpperVal0
  , __global real const* restrict matUpperVal1
  , __global uint const* restrict matCol
  , __global uint const* restrict matRowBeg

  , __global real const* restrict inMin
  , __global real const* restrict inMax
  , __global real* restrict outMin
  , __global real* restrict outMax
  )
{
  const int v1 = get_global_id(0);
  const int v1_ = v1 % matRowCnt;
  const int matId = v1 / matRowCnt;
  const int matOff = matId * matRowBeg[matRowCnt];
  const int matColOff = matId * matRowCnt;

  if (v1 < matRowCnt * matCnt)
  {
    const uint ce = matRowBeg[v1_ + 1];

    real dotMin = outMin[v1];
    real dotMax = outMax[v1];
    for (uint i_ = matRowBeg[v1_]; i_ < ce; ++i_)
    {
      const uint i = i_ + matOff;
      const uint v0 = matCol[i_] + matColOff;
      const real rlowerMin = (matLowerVal0[i] * inMin[v0] - matLowerVal1[i] * inMin[v1]);
      const real rupperMax = (matUpperVal0[i] * inMax[v0] - matUpperVal1[i] * inMax[v1]);

      if (rlowerMin > 0.0) {
        dotMin += rlowerMin;
      } else {
        dotMin += (matUpperVal0[i] * inMin[v0] - matUpperVal1[i] * inMin[v1]);
      }
      if (rupperMax > 0.0) {
        dotMax += rupperMax;
      } else {
        dotMax += (matLowerVal0[i] * inMax[v0] - matLowerVal1[i] * inMax[v1]);
      }
    }
    outMin[v1] = dotMin;
    outMax[v1] = dotMax;
  }
}

