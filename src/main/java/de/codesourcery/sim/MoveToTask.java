package de.codesourcery.sim;

public class MoveToTask extends Task
{
    public static final float CLOSE_ENOUGH_SQRT = 0.1f*0.1f;

    public final Vec2D destination = new Vec2D();

    public boolean isCloseEnough(Vec2D pos)
    {
        final float dst2 = pos.dst2( destination );
        return dst2 <= CLOSE_ENOUGH_SQRT;
    }
}
