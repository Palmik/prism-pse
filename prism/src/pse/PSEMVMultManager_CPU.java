package pse;

import java.util.BitSet;

final public class PSEMVMultManager_CPU implements PSEMultManager<PSEMVMult_CPU>
{
	public PSEMVMultManager_CPU(PSEModel model, BitSet modelSubset, boolean modelSubsetComplement)
	{
		this.model = model;
		this.modelSubset = Utility.makeBitSetCopy(modelSubset, model.getNumStates());
		this.modelSubsetComplement = modelSubsetComplement;
	}

	@Override
	public void update(PSEMVMult_CPU mult)
	{
		mult.update(model.getCreateData_MV_CSR(modelSubset, modelSubsetComplement));
	}

	@Override
	public PSEMVMult_CPU create(final double weight[], double weightDef, int weightOff)
	{
		return createGroup(weight, weightDef, weightOff, 1)[0];
	}

	@Override
	public PSEMVMult_CPU[] createGroup(double[] weight, double weightDef, int weightOff, int n)
	{
		PSEMVCreateData_CSR data = model.getCreateData_MV_CSR(modelSubset, modelSubsetComplement);
		PSEMVMult_CPU[] group = new PSEMVMult_CPU[n];
		for (int i = 0; i < n; ++i) {
			group[i] = new PSEMVMult_CPU(data, weight, weightDef, weightOff);
		}
		return group;
	}

	@Override
	final public void release()
	{
	}

	final private PSEModel model;
	final private BitSet modelSubset;
	final private boolean modelSubsetComplement;
}
