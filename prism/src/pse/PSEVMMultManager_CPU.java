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
		return new PSEVMMult_CPU(model.getCreateData_VM_CSR(), weight, weightDef, weightOff);
	}

	@Override
	final public void release()
	{
	}

	final private PSEModel model;
}
