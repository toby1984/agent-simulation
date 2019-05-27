package de.codesourcery.sim;

public class Vec2Di
{
    public int x,y;

    public Vec2Di() {
    }

    public Vec2Di(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public float dst(Vec2Di o) {
        return (float) Math.sqrt( dst2(o) );
    }

    public int dst2(Vec2Di o) {
        int dx = this.x - o.x;
        int dy = this.y - o.y;
        return dx*dx+dy*dy;
    }

    @Override
    public String toString()
    {
        return "(" + x + "," + y + ")";
    }

    public void set(int x,int y) {
        this.x = x;
        this.y = y;
    }
}
