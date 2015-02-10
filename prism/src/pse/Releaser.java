package pse;

import java.util.ArrayList;
import java.util.List;

public class Releaser implements Releaseable
{
    public Releaser()
    {
        toBeReleased = new ArrayList<Releaseable>();
    }

    public void releaseLater(Releaseable obj)
    {
        if (obj != null) {
            toBeReleased.add(obj);
        }
    }

    @Override
    public void release()
    {
        for (Releaseable obj : toBeReleased) {
            obj.release();
        }
    }

    final private List<Releaseable> toBeReleased;
}
