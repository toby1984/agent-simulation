package de.codesourcery.sim;

public class MoveableEntity extends Entity implements ITickListener
{
    private static final Vec2D TMP = new Vec2D();

    public final Vec2D velocity = new Vec2D();
    public final Vec2D acceleration = new Vec2D();

    public final float maxSpeed = 0.1f;
    public final float maxAcceleration = 0.01f;

    public MoveableEntity(Vec2D v)
    {
        super( v );
    }

    public MoveableEntity(float x, float y)
    {
        super( x, y );
    }

    @Override
    public void tick(float deltaSeconds,World world) {

        // calculate new acceleration
        TMP.scl( deltaSeconds );

        acceleration.add( TMP ).clamp( -maxAcceleration , maxAcceleration );

        // calculate new velocity
        TMP.set( acceleration ).scl( deltaSeconds );
        velocity.add( TMP ).clamp( -maxSpeed, maxSpeed );

        // calculate new position
        TMP.set( velocity ).scl( deltaSeconds );
        position.add( velocity );
    }
}
