package de.codesourcery.sim;

import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class Entity implements IHasPosition
{
    private static final AtomicLong IDS = new AtomicLong();

    public final long id = IDS.incrementAndGet();

    public final Vec2D position = new Vec2D();
    public final Vec2D extent = new Vec2D(0.05f,0.05f );

    public Entity( Vec2D v) {
        this(v.x,v.y);
    }

    public boolean contains(Vec2D v) {

        float left = position.x - extent.x/2;
        if ( v.x < left ) {
            return false;
        }
        float right = position.x + extent.x/2;
        if ( v.x > right ) {
            return false;
        }
        float top = position.y - extent.y/2;
        if ( v.y < top ) {
            return false;
        }
        float bottom = position.y + extent.y/2;
        if ( v.y > bottom ) {
            return false;
        }
        return true;
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
    public final Vec2D position()
    {
        return position;
    }

    public final float dst(Vec2D v) {
        return position.dst( v );
    }

    public final float dst2(Vec2D v) {
        return position.dst2( v );
    }

    public final float dst(Entity other) {
        return position.dst( other.position );
    }

    public final float dst2(Entity other) {
        return position.dst2( other.position );
    }
}
