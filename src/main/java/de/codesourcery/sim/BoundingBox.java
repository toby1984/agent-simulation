package de.codesourcery.sim;

public class BoundingBox
{
    public final Vec2D position = new Vec2D();
    public final Vec2D extent = new Vec2D();

    public BoundingBox(Entity entity)
    {
        this.position.set( entity.position );
        this.extent.set( entity.extent );
    }

    public BoundingBox(Vec2D position,Vec2D extent)
    {
        this.position.set(position);
        this.extent.set(extent);
    }

    public float xmin() {
        return position.x-extent.x/2;
    }

    public float xmax() {
        return position.x+extent.x/2;
    }

    public float ymin() {
        return position.y-extent.y/2;
    }

    public float ymax() {
        return position.y+extent.y/2;
    }

    public boolean intersects(BoundingBox other)
    {
        return intersects(other.position,other.extent);
    }

    public boolean intersects(Vec2D op, Vec2D oe)
    {
        float xminA = position.x - extent.x/2;
        float xminB = op.x - oe.x/2;

        float xmaxA = position.x + extent.x/2;
        float xmaxB = op.x + oe.x/2;

        float yminA = position.y - extent.y/2;
        float yminB = op.y - oe.y/2;

        float ymaxA = position.y + extent.y/2;
        float ymaxB = op.y + oe.y/2;

        if ( xmaxA < xminB ) {
            return false;
        }
        if ( xmaxB < xminA ) {
            return false;
        }
        if ( ymaxA < yminB ) {
            return false;
        }
        if ( ymaxB < yminA ) {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return "AABB [ ("+xmin()+","+ymin()+") - ("+xmax()+","+ymax()+") ]";
    }
}