package pse;

public class PSEVMCreateData_CSR
{
    public PSEVMCreateData_CSR
        ( int stCnt

        , double[] matIOValZip
        , int[] matIOSrc
        , int[] matIOTrgBeg

        , double[] matIValZip
        , int[] matISrc
        , int[] matITrgBeg

        , double[] matODiagValZip
        , double[] matNPVal
        , int[] matNPSrc
        , int[] matNPTrgBeg

        , boolean enabledMatO
        )
    {
        this.stCnt = stCnt;

        this.matIOValZip = matIOValZip;
        this.matIOSrc = matIOSrc;
        this.matIOTrgBeg = matIOTrgBeg;

        this.matIValZip = matIValZip;
        this.matISrc = matISrc;
        this.matITrgBeg = matITrgBeg;

        this.matNPVal = matNPVal;
        this.matNPSrc = matNPSrc;
        this.matNPTrgBeg = matNPTrgBeg;

        this.enabledMatNP = matNPTrgBeg[stCnt] > 0;
        this.enabledMatIO = matIOTrgBeg[stCnt] > 0;
        this.enabledMatI = matITrgBeg[stCnt] > 0;
        this.enabledMatO = enabledMatO;

        if (enabledMatI || enabledMatO || enabledMatIO) {
            this.matODiagVal = matODiagValZip;
        } else {
            this.matODiagVal = new double[stCnt];
            for (int i = 0; i < stCnt; ++i) {
                this.matODiagVal[i] = matODiagValZip[i * 2];
            }
        }
    }

    final public boolean enabledMatI;
    final public boolean enabledMatO;
    final public boolean enabledMatIO;
    final public boolean enabledMatNP;

    final public int stCnt;

    final public double[] matIOValZip;
    final public int[] matIOSrc;
    final public int[] matIOTrgBeg;

    final public double[] matIValZip;
    final public int[] matISrc;
    final public int[] matITrgBeg;

    final public double[] matODiagVal;
    final public double[] matNPVal;
    final public int[] matNPSrc;
    final public int[] matNPTrgBeg;
}
