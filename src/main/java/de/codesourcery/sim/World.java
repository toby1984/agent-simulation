package de.codesourcery.sim;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class World
{
    private static final float BROADCAST_DIST = 1;
    private static final float BROADCAST_DIST2 = BROADCAST_DIST*BROADCAST_DIST;

    private final List<Entity> entities = new ArrayList<>();
    private final List<ITickListener> tickListeners = new ArrayList<>();
    private final List<Robot> robots = new ArrayList<>();

    public long frameCounter;

    public void add( Entity entity )
    {
        this.entities.add( entity );
        if ( entity instanceof ITickListener) {
            tickListeners.add( (ITickListener) entity );
        }
        if ( entity instanceof Robot ) {
            robots.add( (Robot) entity );
        }
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

    public void sendMessage(Message msg) {

//        System.out.println( "RECEIVED: "+msg );
        for ( Robot r : robots )
        {
            if ( r != msg.sender && r.dst2( msg.sender.position() ) <= BROADCAST_DIST2 )
            {
                r.receive(msg);
            }
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
}