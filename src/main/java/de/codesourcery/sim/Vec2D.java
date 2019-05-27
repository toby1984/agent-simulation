package de.codesourcery.sim;

public class Vec2D
{
    public float x,y;

    public Vec2D(float x, float y)
    {
        this.x = x;
        this.y = y;
    }

    public Vec2D(Vec2D other)
    {
        this.x = other.x;
        this.y = other.y;
    }

    public Vec2D()
    {
    }

    public float dst(Vec2D other)
    {
        return (float) Math.sqrt( dst2(other) );
    }

    float dst2(Vec2D other)
    {
        float dx = other.x - x;
        float dy =other.y - y;
        return dx*dx + dy*dy;
    }

    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString()
    {
        return "("+x+","+y+")";
    }
}
