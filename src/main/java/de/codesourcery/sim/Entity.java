package de.codesourcery.sim;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class Entity implements IHasPosition
{
    private static final AtomicLong IDS = new AtomicLong();

    public final long id = IDS.incrementAndGet();

    public final Vec2D position = new Vec2D();
    public final Vec2D extent = new Vec2D(0.01f,0.01f );

    public Entity( Vec2D v) {
        this(v.x,v.y);
    }

    @Override
    public boolean equals(Object obj)
    {
        if ( obj == null ) {
            return false;
        }
        return this.id == ((Entity) obj).id;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode( id );
    }

    public Entity(float x, float y)
    {
        this.position.set(x,y);
    }

    @Override
    public Vec2D position()
    {
        return position;
    }
}
