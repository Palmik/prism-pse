package pse;

final public class PSEMultOptions
{
	public PSEMultOptions(boolean ocl, int para, int many, boolean adatptiveFoxGlynn)
	{
		this.ocl = ocl;
		this.para = para;
		this.many = many;
		this.adatptiveFoxGlynn = adatptiveFoxGlynn;

		if (many > 0 && !ocl) {
			throw new RuntimeException(
				"PSE_OCL has to be enabled (PSE_OCL=1) if you want to use PSE_MANY");
		}

		if (many > 0 && para > 0) {
			throw new RuntimeException(
				"PSE_PARA has to be disabled (PSE_PARA=0) if you want to use PSE_MANY");
		}

		if (many > 0 && adatptiveFoxGlynn) {
			throw new RuntimeException(
				"PSE_ADAPTIVE_FOX_GLYNN has to be disabled (PSE_ADAPTIVE_FOX_GLYNN=0) if you want to use PSE_MANY");
		}

		if (para < 0) {
			throw new RuntimeException(
				"PSE_PARA has to be set nonnegative integer");
		}

		if (many < 0) {
			throw new RuntimeException(
				"PSE_MANY has to be set nonnegative integer");
		}

		System.err.printf("PSEMultOptions = %s\n", toString());
	}

	@Override
	public String toString()
	{
		return String.format("{ ocl = %s; para = %s; many = %s; adaptiveFoxGlynn = %s }",
			ocl, para, many, adatptiveFoxGlynn);
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

	public boolean getAdaptiveFoxGlynn()
	{
		return adatptiveFoxGlynn;
	}

	final private boolean ocl;
	final private int para;
	final private int many;
	final private boolean adatptiveFoxGlynn;
}
