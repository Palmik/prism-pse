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
        PSEMVCreateData_CSR data = model.getCreateData_MV_CSR(modelSubset);
        PSEMVMultSettings_OCL multOpts = PSEMVMultSettings_OCL.Default();
        releaser.releaseLater(multOpts);
        PSEMVMultTopology_OCL multTopo = new PSEMVMultTopology_OCL(data, multOpts.clContext);
        releaser.releaseLater(multTopo);
        PSEMVMult_OCL mult = new PSEMVMult_OCL(multOpts, multTopo, data, weight, weightDef, weightOff);
        releaser.releaseLater(mult);
        return mult;
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
