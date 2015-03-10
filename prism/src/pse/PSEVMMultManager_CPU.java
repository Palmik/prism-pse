package pse;

public class PSEVMMultManager_CPU implements PSEMultManager<PSEVMMult_CPU>
{
	public PSEVMMultManager_CPU(PSEModel model)
	{
		this.model = model;
	}

	@Override
	public void update(PSEVMMult_CPU mult)
	{
		mult.update(model.getCreateData_VM_CSR());
	}

	@Override
	public PSEVMMult_CPU create(final double weight[], double weightDef, int weightOff)
	{
		return createGroup(weight, weightDef, weightOff, 1)[0];
	}

	@Override
	public PSEVMMult_CPU[] createGroup(double[] weight, double weightDef, int weightOff, int n)
	{
		PSEVMCreateData_CSR data = model.getCreateData_VM_CSR();
		PSEVMMult_CPU[] group = new PSEVMMult_CPU[n];
		for (int i = 0; i < n; ++i) {
			group[i] = new PSEVMMult_CPU(data, weight, weightDef, weightOff);
		}
		return group;
	}

	@Override
	final public void release()
	{
	}

	final private PSEModel model;
}
