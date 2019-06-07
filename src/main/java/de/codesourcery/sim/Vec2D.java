package de.codesourcery.sim;

import java.util.Random;

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

    public Vec2D cpy() {
        return new Vec2D(this);
    }

    public Vec2D scl(float value) {
        this.x *= value;
        this.y *= value;
        return this;
    }

    public Vec2D add(float dx,float dy) {
        x += dx;
        y += dy;
        return this;
    }

    public Vec2D add(Vec2D v) {
        this.x += v.x;
        this.y += v.y;
        return this;
    }

    public Vec2D sub(Vec2D v) {
        this.x -= v.x;
        this.y -= v.y;
        return this;
    }

    public float dst(Vec2D other)
    {
        return (float) Math.sqrt( dst2(other) );
    }

    float dst2(Vec2D other)
    {
        float dx = other.x - x;
        float dy = other.y - y;
        return dx*dx + dy*dy;
    }

    public float len2()
    {
        return x*x + y*y;
    }

    public float len()
    {
        return (float) Math.sqrt(len2());
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

    public Vec2D nor()
    {
        float len = len();
        if (len != 0) {
            x /= len;
            y /= len;
        }
        return this;
    }

    public Vec2D clamp(float min, float max)
    {
        final float len2 = len2();
        if (len2 == 0f)
        {
            return this;
        }
        float max2 = max * max;
        if (len2 > max2)
        {
            return scl((float)Math.sqrt(max2 / len2));
        }
        float min2 = min * min;
        if (len2 < min2)
        {
            return scl((float)Math.sqrt(min2 / len2));
        }
        return this;
    }

    public Vec2D set(Vec2D v)
    {
        this.x = v.x;
        this.y = v.y;
        return this;
    }

    public Vec2D randomize(float maxLen, Random rnd)
    {
        float len = Math.max(0.1f,rnd.nextFloat())*maxLen;
        x = 2*(rnd.nextFloat()-0.5f);
        y = 2*(rnd.nextFloat()-0.5f);
        return nor().scl(len);
    }
}
