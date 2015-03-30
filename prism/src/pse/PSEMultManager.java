package pse;

public interface PSEMultManager<Mult extends PSEMult> extends Releaseable
{
    public void update(Mult mult);
    public Mult create(final double weight[], double weightDef, int weightOff);
    public Mult[] createGroup(final double weight [], double weightDef, int weightOff, int n);
}