package pse;

import java.util.BitSet;

final public class PSEMVMultManager_OCL implements PSEMultManager<PSEMVMult_OCL>
{
    public PSEMVMultManager_OCL(PSEModel model, BitSet modelSubset, boolean modelSubsetComplement)
    {
        this.model = model;
        this.modelSubset = Utility.makeSubset(modelSubset, modelSubsetComplement, model.getNumStates());

        this.releaser = new Releaser();
    }

    @Override
    final public void update(PSEMVMult_OCL mult)
    {
        mult.update(model.getCreateData_MV_CSR(modelSubset));
    }

    @Override
    final public PSEMVMult_OCL create(final double weight[], double weightDef, int weightOff)
    {
        return createGroup(weight, weightDef, weightOff, 1)[0];
    }

    @Override
    public PSEMVMult_OCL[] createGroup(double[] weight, double weightDef, int weightOff, int n)
    {
        PSEMVCreateData_CSR data = model.getCreateData_MV_CSR(modelSubset);
        PSEMVMultSettings_OCL multOpts = PSEMVMultSettings_OCL.Default();
        releaser.releaseLater(multOpts);
        PSEMVMultTopology_OCL multTopo = new PSEMVMultTopology_OCL(data, multOpts.clContext);
        releaser.releaseLater(multTopo);

        PSEMVMult_OCL[] group = new PSEMVMult_OCL[n];
        for (int i = 0; i < n; ++i) {
            group[i] = new PSEMVMult_OCL(multOpts, multTopo, data, weight, weightDef, weightOff);
            releaser.releaseLater(group[i]);
        }

        return group;
    }

    @Override
    final public void release()
    {
        releaser.release();
    }

    final private PSEModel model;
    final private BitSet modelSubset;

    final private Releaser releaser;
}
