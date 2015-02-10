package pse;

public class PSEVMMultManager_OCL implements PSEMultManager<PSEVMMult_OCL>
{

	public PSEVMMultManager_OCL(PSEModel model)
	{
		this.model = model;
		this.releaser = new Releaser();
	}

	@Override
	final public void update(PSEVMMult_OCL mult)
	{
		mult.update(model.getCreateData_VM_CSR());
	}

	@Override
	final public PSEVMMult_OCL create(final double weight[], double weightDef, int weightOff)
	{
		PSEVMCreateData_CSR data = model.getCreateData_VM_CSR();
		PSEVMMultSettings_OCL multOpts = PSEVMMultSettings_OCL.Default();
		releaser.releaseLater(multOpts);
		PSEVMMultTopology_OCL multTopo = new PSEVMMultTopology_OCL(data, multOpts.clContext);
		releaser.releaseLater(multTopo);
		PSEVMMult_OCL mult = new PSEVMMult_OCL(multOpts, multTopo, data, weight, weightDef, weightOff);
		releaser.releaseLater(mult);
		return mult;
	}

	@Override
	final public void release()
	{
		releaser.release();
	}

	final private PSEModel model;
	final private Releaser releaser;
}
