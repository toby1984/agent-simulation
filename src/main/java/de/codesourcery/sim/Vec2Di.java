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

    public Vec2Di add(Vec2D v) {
        this.x += v.x;
        this.y += v.y;
        return this;
    }

    public Vec2Di add(Vec2Di v) {
        this.x += v.x;
        this.y += v.y;
        return this;
    }

    public Vec2Di sub(Vec2D v) {
        this.x -= v.x;
        this.y -= v.y;
        return this;
    }

    public Vec2Di sub(Vec2Di v) {
        this.x -= v.x;
        this.y -= v.y;
        return this;
    }

    public Vec2Di scl(float value) {
        this.x = (int) ( this.x * value );
        this.y = (int) ( this.y * value );
        return this;
    }

    public Vec2Di scl(int value) {
        this.x *= value;
        this.y *= value;
        return this;
    }

    public Vec2Di set(Vec2Di v) {
        this.x = v.x;
        this.y = v.y;
        return this;
    }

    public Vec2Di set(Vec2D v) {
        this.x = (int) v.x;
        this.y = (int) v.y;
        return this;
    }

    public Vec2Di set(int x,int y) {
        this.x = x;
        this.y = y;
        return this;
    }
}
