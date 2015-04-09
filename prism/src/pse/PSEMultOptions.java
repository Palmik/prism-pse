package pse;

final public class PSEMultOptions
{
	public PSEMultOptions(boolean ocl, int para, int many)
	{
		this.ocl = ocl;
		this.para = para;
		this.many = many;

		if (many > 0 && !ocl) {
			throw new RuntimeException(
				"PSE_OCL has to be enabled (PSE_OCL=1) if you want to use PSE_MANY");
		}

		if (many > 0 && para > 0) {
			throw new RuntimeException(
				"PSE_PARA has to be disabled (PSE_PARA=0) if you want to use PSE_MANY");
		}

		if (para < 0) {
			throw new RuntimeException(
				"PSE_PARA has to be set nonnegative integer");
		}

		if (many < 0) {
			throw new RuntimeException(
				"PSE_MANY has to be set nonnegative integer");
		}

		System.err.printf("PSEOptions{ ocl = %s; para = %s; many = %s }\n", ocl, para, many);
	}

	public boolean getOcl()
	{
		return ocl;
	}

	public int getPara()
	{
		return para;
	}

	public int getMany()
	{
		return many;
	}

	final private boolean ocl;
	final private int para;
	final private int many;
}
