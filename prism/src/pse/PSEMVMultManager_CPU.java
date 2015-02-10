package pse;

import java.util.BitSet;

final public class PSEMVMultManager_CPU implements PSEMultManager<PSEMVMult_CPU>
{
	public PSEMVMultManager_CPU(PSEModel model, BitSet modelSubset, boolean modelSubsetComplement)
	{
		this.model = model;
		this.modelSubset = Utility.makeSubset(modelSubset, modelSubsetComplement, model.getNumStates());
	}

	@Override
	public void update(PSEMVMult_CPU mult)
	{
		mult.update(model.getCreateData_MV_CSR(modelSubset));
	}

	@Override
	public PSEMVMult_CPU create(final double weight[], double weightDef, int weightOff)
	{
		return new PSEMVMult_CPU(model.getCreateData_MV_CSR(modelSubset), weight, weightDef, weightOff);
	}

	@Override
	final public void release()
	{
	}

	final private PSEModel model;
	final private BitSet modelSubset;
}
