#pragma OPENCL EXTENSION cl_khr_fp64 : enable

#define WARP_SIZE 32

//#define MADTO(A, B, C) (C = mad(A,B,C))
//#define MADTO(A, B, C) (C = fma(A,B,C))
#define MADTO(A, B, C) (C = (A) * (B) + C)

typedef double real;

__kernel void WeightedSumTo
  ( const int n
  , const real w
  , __global real const* restrict vec
  , __global real* restrict sum
  )
{
  int ii = get_global_id(0);
  if (ii < n) {
    MADTO(vec[ii], w, sum[ii]);
  }
}

__kernel void WeightedSumToBoth
  ( const int n
  , const real w
  , __global real const* restrict vec1
  , __global real const* restrict vec2
  , __global real* restrict sum1
  , __global real* restrict sum2
  )
{
  int ii = get_global_id(0);
  if (ii < n) {
    MADTO(vec1[ii], w, sum1[ii]);
    MADTO(vec2[ii], w, sum2[ii]);
  }
}

// PSE MV CSR

__kernel void PSE_MV_NP_CSR
  ( const int matNPRowCnt
  , __global real const* restrict matVal
  , __global int const* restrict matNPCol
  , __global int const* restrict matNPRow
  , __global int const* restrict matNPRowBeg

  , __global real const* restrict in
  , __global real* restrict out
  )
{
  const int ii = get_global_id(0);
  if (ii < matNPRowCnt) {
    const int v0 = matNPRow[ii];
    const int te = matNPRowBeg[ii + 1];
    real dot = out[v0];
    for (int jj = matNPRowBeg[ii]; jj < te; ++jj) {
      MADTO(matVal[jj], in[matNPCol[jj]] - in[v0], dot);
    }
    out[v0] = dot;
  }
}

__kernel void PSE_MV_NP_CSR_BOTH
  ( const int matNPRowCnt
  , __global real const* restrict matVal
  , __global int const* restrict matNPCol
  , __global int const* restrict matNPRow
  , __global int const* restrict matNPRowBeg

  , __global real const* restrict inMin
  , __global real const* restrict inMax
  , __global real* restrict outMin
  , __global real* restrict outMax
  )
{
  const int ii = get_global_id(0);
  if (ii < matNPRowCnt) {
    const int v0 = matNPRow[ii];
    const int te = matNPRowBeg[ii + 1];
    real dotMin = outMin[v0];
    real dotMax = outMax[v0];
    for (int jj = matNPRowBeg[ii]; jj < te; ++jj) {
      const int v1 = matNPCol[jj];
      const real rate = matVal[jj];
      MADTO(rate, inMin[v1] - inMin[v0], dotMin);
      MADTO(rate, inMax[v1] - inMax[v0], dotMax);
    }
    outMin[v0] = dotMin;
    outMax[v0] = dotMax;
  }
}

__kernel void PSE_MV_P_CSR_BOTH
  ( const int matPRowCnt
  , __global real const* restrict matPLowerVal
  , __global real const* restrict matPUpperVal
  , __global int const* restrict matPCol
  , __global int const* restrict matPRow
  , __global int const* restrict matPRowBeg

  , __global real const* restrict inMin
  , __global real const* restrict inMax
  , __global real* restrict outMin
  , __global real* restrict outMax
  )
{
  const int ii = get_global_id(0);
  if (ii < matPRowCnt) {
    const int v0 = matPRow[ii];
    const int te = matPRowBeg[ii + 1];
    real dotMin = outMin[v0];
    real dotMax = outMax[v0];
    for (int jj = matPRowBeg[ii]; jj < te; ++jj) {
      const int v1 = matPCol[jj];

      const real diffMin = inMin[v1] - inMin[v0];
      const real diffMax = inMax[v1] - inMax[v0];
      if (diffMin > 0) {
        MADTO(matPLowerVal[jj], diffMin, dotMin);
      } else {
        MADTO(matPUpperVal[jj], diffMin, dotMin);
      }
      if (diffMax > 0) {
        MADTO(matPUpperVal[jj], diffMax, dotMax);
      } else {
        MADTO(matPLowerVal[jj], diffMax, dotMax);
      }
    }
    outMin[v0] = dotMin;
    outMax[v0] = dotMax;
  }
}

// PSE MV CSR MANY

__kernel void PSE_MV_NP_CSR_MANY
  ( const int matCnt
  , const int matNPColCnt
  , const int matNPRowCnt
  , __global real const* restrict matVal
  , __global int const* restrict matNPCol
  , __global int const* restrict matNPRow
  , __global int const* restrict matNPRowBeg

  , __global real const* restrict in
  , __global real* restrict out
  )
{
  const int ii = get_global_id(0);
  const int ii_ = ii % matNPRowCnt;
  const int matColOff = (ii / matNPRowCnt) * matNPColCnt;
  if (ii < matNPRowCnt * matCnt) {
    const int v0 = matNPRow[ii_] + matColOff;
    const int te = matNPRowBeg[ii_ + 1];
    real dot = out[v0];
    for (int jj = matNPRowBeg[ii_]; jj < te; ++jj) {
      MADTO(matVal[jj], in[matNPCol[jj] + matColOff] - in[v0], dot);
    }
    out[v0] = dot;
  }
}

__kernel void PSE_MV_NP_CSR_BOTH_MANY
  ( const int matCnt
  , const int matNPColCnt
  , const int matNPRowCnt
  , __global real const* restrict matVal
  , __global int const* restrict matNPCol
  , __global int const* restrict matNPRow
  , __global int const* restrict matNPRowBeg

  , __global real const* restrict inMin
  , __global real const* restrict inMax
  , __global real* restrict outMin
  , __global real* restrict outMax
  )
{
  const int ii = get_global_id(0);
  const int ii_ = ii % matNPRowCnt;
  const int matColOff = (ii / matNPRowCnt) * matNPColCnt;
  if (ii < matNPRowCnt * matCnt) {
    const int v0 = matNPRow[ii_] + matColOff;
    const int te = matNPRowBeg[ii_ + 1];
    real dotMin = outMin[v0];
    real dotMax = outMax[v0];
    for (int jj = matNPRowBeg[ii_]; jj < te; ++jj) {
      const int v1 = matNPCol[jj] + matColOff;
      const real rate = matVal[jj];
      MADTO(rate, inMin[v1] - inMin[v0], dotMin);
      MADTO(rate, inMax[v1] - inMax[v0], dotMax);
    }
    outMin[v0] = dotMin;
    outMax[v0] = dotMax;
  }
}

__kernel void PSE_MV_P_CSR_BOTH_MANY
  ( const int matCnt
  , const int matPColCnt
  , const int matPRowCnt
  , __global real const* restrict matPLowerVal
  , __global real const* restrict matPUpperVal
  , __global int const* restrict matPCol
  , __global int const* restrict matPRow
  , __global int const* restrict matPRowBeg

  , __global real const* restrict inMin
  , __global real const* restrict inMax
  , __global real* restrict outMin
  , __global real* restrict outMax
  )
{
  const int ii = get_global_id(0);
  const int ii_ = ii % matPRowCnt;
  const int matOff = (ii / matPRowCnt) * matPRowBeg[matPRowCnt];
  const int matColOff = (ii / matPRowCnt) * matPColCnt;
  if (ii < matPRowCnt * matCnt) {
    const int v0 = matPRow[ii_] + matColOff;
    const int te = matPRowBeg[ii_ + 1];
    real dotMin = outMin[v0];
    real dotMax = outMax[v0];
    for (int jj_ = matPRowBeg[ii_]; jj_ < te; ++jj_) {
      const int jj = jj_ + matOff;
      const int v1 = matPCol[jj_] + matColOff;

      const real diffMin = inMin[v1] - inMin[v0];
      const real diffMax = inMax[v1] - inMax[v0];
      if (diffMin > 0) {
        MADTO(matPLowerVal[jj], diffMin, dotMin);
      } else {
        MADTO(matPUpperVal[jj], diffMin, dotMin);
      }
      if (diffMax > 0) {
        MADTO(matPUpperVal[jj], diffMax, dotMax);
      } else {
        MADTO(matPLowerVal[jj], diffMax, dotMax);
      }
    }
    outMin[v0] = dotMin;
    outMax[v0] = dotMax;
  }
}

// PSE VM

__kernel void PSE_VM_I_CSR_BOTH
  ( const int matRowCnt
  , __global real const* restrict matValMin
  , __global real const* restrict matValMax
  , __global int const* restrict matCol
  , __global int const* restrict matRowBeg

  , __global real const* restrict inMin
  , __global real const* restrict inMax
  , __global real* restrict outMin
  , __global real* restrict outMax
  )
{
  const int v0 = get_global_id(0);

  if (v0 < matRowCnt) {
    const int ce = matRowBeg[v0 + 1];

    real dotMin = outMin[v0];
    real dotMax = outMax[v0];
    for (int i = matRowBeg[v0]; i < ce; ++i) {
      MADTO(matValMin[i], inMin[matCol[i]], dotMin);
      MADTO(matValMax[i], inMax[matCol[i]], dotMax);
    }
    outMin[v0] = dotMin;
    outMax[v0] = dotMax;
  }
}

__kernel void PSE_VM_NP_CSR
  ( const int matRowCnt
  , __global real const* restrict matDiaVal
  , __global real const* restrict matVal
  , __global int const* restrict matCol
  , __global int const* restrict matRowBeg

  , __global real const* restrict in
  , __global real* restrict out
  )
{
  const int v0 = get_global_id(0);

  if (v0 < matRowCnt) {
    const int ce = matRowBeg[v0 + 1];
    real dot = in[v0] * matDiaVal[v0]; //out[v0] + in[v0] * matDiaVal[v0];
    for (int i = matRowBeg[v0]; i < ce; ++i) {
      MADTO(matVal[i], in[matCol[i]], dot);
    }
    out[v0] = dot;
  }
}

__kernel void PSE_VM_NP_CSR_BOTH
  ( const int matRowCnt
  , __global real const* restrict matDiaValMin
  , __global real const* restrict matDiaValMax
  , __global real const* restrict matVal
  , __global int const* restrict matCol
  , __global int const* restrict matRowBeg

  , __global real const* restrict inMin
  , __global real const* restrict inMax
  , __global real* restrict outMin
  , __global real* restrict outMax
  )
{
  const int v0 = get_global_id(0);

  if (v0 < matRowCnt) {
    const int ce = matRowBeg[v0 + 1];

    real dotMin = inMin[v0] * matDiaValMin[v0]; //out[v0] + in[v0] * matDiaVal[v0];
    real dotMax = inMax[v0] * matDiaValMax[v0]; //out[v0] + in[v0] * matDiaVal[v0];
    for (int i = matRowBeg[v0]; i < ce; ++i) {
      MADTO(matVal[i], inMin[matCol[i]], dotMin);
      MADTO(matVal[i], inMax[matCol[i]], dotMax);
    }
    outMin[v0] = dotMin;
    outMax[v0] = dotMax;
  }
}
__kernel void PSE_VM_DIAG
  ( const int matRowCnt
  , __global real const* restrict matDiaVal
  , __global real const* restrict in
  , __global real* restrict out
  )
{
  const int v0 = get_global_id(0);

  if (v0 < matRowCnt) {
    out[v0] = in[v0] * matDiaVal[v0]; //out[v0] + in[v0] * matDiaVal[v0];
  }
}

__kernel void PSE_VM_DIAG_BOTH
  ( const int matRowCnt
  , __global real const* restrict matDiaValMin
  , __global real const* restrict matDiaValMax
  , __global real const* restrict inMin
  , __global real const* restrict inMax
  , __global real* restrict outMin
  , __global real* restrict outMax
  )
{
  const int v0 = get_global_id(0);

  if (v0 < matRowCnt) {
    outMin[v0] = inMin[v0] * matDiaValMin[v0]; //out[v0] + in[v0] * matDiaVal[v0];
    outMax[v0] = inMax[v0] * matDiaValMax[v0]; //out[v0] + in[v0] * matDiaVal[v0];
  }
}

__kernel void PSE_VM_IO_CSR_BOTH
  ( const int matRowCnt
  , __global real const* restrict matLowerVal0
  , __global real const* restrict matLowerVal1
  , __global real const* restrict matUpperVal0
  , __global real const* restrict matUpperVal1
  , __global int const* restrict matCol
  , __global int const* restrict matRowBeg

  , __global real const* restrict inMin
  , __global real const* restrict inMax
  , __global real* restrict outMin
  , __global real* restrict outMax
  )
{
  const int v1 = get_global_id(0);

  if (v1 < matRowCnt) {
    real dotMin = outMin[v1];
    real dotMax = outMax[v1];
    for (int i = matRowBeg[v1]; i < matRowBeg[v1 + 1]; ++i) {
      const int v0 = matCol[i];
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

__kernel void PSE_VM_I_CSR_BOTH_MANY
  ( const int matCnt
  , const int matRowCnt
  , __global real const* restrict matValMin
  , __global real const* restrict matValMax
  , __global int const* restrict matCol
  , __global int const* restrict matRowBeg

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

  if (v0 < matRowCnt * matCnt) {
    const int ce = matRowBeg[v0_ + 1];

    real dotMin = outMin[v0];
    real dotMax = outMax[v0];
    for (int i = matRowBeg[v0_]; i < ce; ++i) {
      MADTO(matValMin[i + matOff], inMin[matCol[i] + matColOff], dotMin);
      MADTO(matValMax[i + matOff], inMax[matCol[i] + matColOff], dotMax);
    }
    outMin[v0] = dotMin;
    outMax[v0] = dotMax;
  }
}

__kernel void PSE_VM_NP_CSR_MANY
  ( const int matCnt
  , const int matRowCnt
  , __global real const* restrict matDiaVal
  , __global real const* restrict matVal
  , __global int const* restrict matCol
  , __global int const* restrict matRowBeg

  , __global real const* restrict in
  , __global real* restrict out
  )
{
  const int v0 = get_global_id(0);
  const int v0_ = v0 % matRowCnt;
  const int matColOff = (v0 / matRowCnt) * matRowCnt;

  if (v0 < matRowCnt * matCnt) {
    const int ce = matRowBeg[v0_ + 1];

    real dot = in[v0] * matDiaVal[v0]; //out[v0] + in[v0] * matDiaVal[v0];
    for (int i = matRowBeg[v0_]; i < ce; ++i) {
      MADTO(matVal[i], in[matCol[i] + matColOff], dot);
    }
    out[v0] = dot;
  }
}

__kernel void PSE_VM_NP_CSR_BOTH_MANY
  ( const int matCnt
  , const int matRowCnt
  , __global real const* restrict matDiaValMin
  , __global real const* restrict matDiaValMax
  , __global real const* restrict matVal
  , __global int const* restrict matCol
  , __global int const* restrict matRowBeg

  , __global real const* restrict inMin
  , __global real const* restrict inMax
  , __global real* restrict outMin
  , __global real* restrict outMax
  )
{
  const int v0 = get_global_id(0);
  const int v0_ = v0 % matRowCnt;
  const int matColOff = (v0 / matRowCnt) * matRowCnt;

  if (v0 < matRowCnt * matCnt) {
    const int ce = matRowBeg[v0_ + 1];

    real dotMin = inMin[v0] * matDiaValMin[v0]; //out[v0] + in[v0] * matDiaVal[v0];
    real dotMax = inMax[v0] * matDiaValMax[v0]; //out[v0] + in[v0] * matDiaVal[v0];
    for (int i = matRowBeg[v0_]; i < ce; ++i) {
      MADTO(matVal[i], inMin[matCol[i] + matColOff], dotMin);
      MADTO(matVal[i], inMax[matCol[i] + matColOff], dotMax);
    }
    outMin[v0] = dotMin;
    outMax[v0] = dotMax;
  }
}

__kernel void PSE_VM_DIAG_MANY
  ( const int matCnt
  , const int matRowCnt
  , __global real const* restrict matDiaVal
  , __global real const* restrict in
  , __global real* restrict out
  )
{
  const int v0 = get_global_id(0);
  const int v0_ = v0 % matRowCnt;

  if (v0 < matRowCnt * matCnt) {
    out[v0] = in[v0] * matDiaVal[v0_]; //out[v0] + in[v0] * matDiaVal[v0];
  }
}

__kernel void PSE_VM_DIAG_BOTH_MANY
  ( const int matCnt
  , const int matRowCnt
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

  if (v0 < matRowCnt * matCnt) {
    outMin[v0] = inMin[v0] * matDiaValMin[v0_]; //out[v0] + in[v0] * matDiaVal[v0];
    outMax[v0] = inMax[v0] * matDiaValMax[v0_]; //out[v0] + in[v0] * matDiaVal[v0];
  }
}

__kernel void PSE_VM_IO_CSR_BOTH_MANY
  ( const int matCnt
  , const int matRowCnt
  , __global real const* restrict matLowerVal0
  , __global real const* restrict matLowerVal1
  , __global real const* restrict matUpperVal0
  , __global real const* restrict matUpperVal1
  , __global int const* restrict matCol
  , __global int const* restrict matRowBeg

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

  if (v1 < matRowCnt * matCnt) {
    const int ce = matRowBeg[v1_ + 1];

    real dotMin = outMin[v1];
    real dotMax = outMax[v1];
    for (int i_ = matRowBeg[v1_]; i_ < ce; ++i_) {
      const int i = i_ + matOff;
      const int v0 = matCol[i_] + matColOff;
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

