package de.codesourcery.sim;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class World
{
    public static final float BROADCAST_DIST = 0.5f;
    public static final float BROADCAST_DIST2 = BROADCAST_DIST*BROADCAST_DIST;

    public static final Vec2D BROADCAST_RANGE = new Vec2D(
        (float) Math.sqrt( BROADCAST_DIST*BROADCAST_DIST + BROADCAST_DIST*BROADCAST_DIST),
        (float) Math.sqrt( BROADCAST_DIST*BROADCAST_DIST + BROADCAST_DIST*BROADCAST_DIST)
    );

    public final Inventory inventory = new Inventory();

    private final List<Entity> entities = new ArrayList<>();
    private final List<ITickListener> tickListeners = new ArrayList<>();
    private final List<Robot> robots = new ArrayList<>();
    private final List<Controller> controllers = new ArrayList<>();

    public long frameCounter;

    public void add( Entity entity )
    {
        if ( entity instanceof Robot )
        {
            final Robot r = (Robot) entity;
            if ( ! r.hasController() )
            {
                assignToController( r );
            }
            robots.add( (Robot) entity );
        }

        this.entities.add( entity );
        if ( entity instanceof ITickListener) {
            tickListeners.add( (ITickListener) entity );
        }
        if ( entity instanceof Controller) {
            controllers.add( (Controller) entity );
        }
    }

    private void assignToController(Robot r)
    {
        final List<Controller> controllers = findControllersInRange( r.position );
        if ( controllers.isEmpty() )
        {
            throw new IllegalStateException( "No controller in range for "+r );
        }
        // assign to controller with highest utilization
        controllers.sort( (a,b) -> Float.compare( b.utilization() ,a.utilization() ) );
        controllers.get(0).assignRobot( r, this );
    }

    public void tick(float deltaSeconds)
    {
        frameCounter++;
        tickListeners.forEach( e -> e.tick( deltaSeconds , this ) );
    }

    public void visitEntities(Consumer<Entity> consumer)
    {
        entities.forEach( consumer );
    }

    public void sendMessage(Message msg)
    {
        boolean send = false;

        for ( Controller c : controllers )
        {
            if ( c.dst2( msg.sender ) <= BROADCAST_DIST2 ) {
                c.broadcast( msg );
                send = true;
            }
        }

        if ( ! send ) {
            System.err.println("No controller in range, message lost: "+msg);
        }
    }

    public Entity getEntityAt(Vec2D position)
    {
        for ( Entity e : entities )
        {
            if ( e.contains( position ) ) {
                return e;
            }
        }
        return null;
    }

    private Controller findControllerInRange(Vec2D pos)
    {
        for ( Controller c : controllers ) {
            if ( c.dst2( pos ) <= BROADCAST_DIST2 ) {
                return c;
            }
        }
        return null;
    }

    private List<Controller> findControllersInRange(Vec2D pos)
    {
        final List<Controller> result = new ArrayList<>();
        for ( Controller c : controllers ) {
            if ( c.dst2( pos ) <= BROADCAST_DIST2 ) {
                result.add( c );
            }
        }
        return result;
    }

    public Depot findClosestDepotThatAccepts(Robot robot,ItemType item, int amount)
    {
        List<Depot> candidates = new ArrayList<>();
        for ( var e : entities )
        {
            if ( e instanceof Depot )
            {
                final int acceptedAmount = ((Depot) e).getAcceptedAmount(item, this);
                if ( acceptedAmount > 0 ) {
                    candidates.add( (Depot) e );
                }
            }
        }
        if ( candidates.isEmpty() ) {
            return null;
        }
        candidates.sort( (a,b) -> Float.compare( a.dst2(robot) , b.dst2(robot) ) );
        return candidates.get(0);
    }
}