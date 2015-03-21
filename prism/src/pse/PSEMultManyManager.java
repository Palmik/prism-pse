package pse;

public interface PSEMultManyManager<Mult extends PSEMultMany> extends Releaseable
{
	public void update(int matId, Mult mult);
	public Mult create(int matCnt, final double weight[], double weightDef, int weightOff);
}
