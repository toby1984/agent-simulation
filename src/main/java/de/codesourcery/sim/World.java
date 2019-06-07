package de.codesourcery.sim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class World
{
    public final Inventory inventory = new Inventory();

    private final List<Entity> entities = new ArrayList<>();
    private final List<ITickListener> tickListeners = new ArrayList<>();
    private final List<Controller> controllers = new ArrayList<>();

    public void add( Entity entity )
    {
        if ( entity instanceof Robot )
        {
            final Robot r = (Robot) entity;
            if ( ! r.hasController() )
            {
                assignToController( r );
            }
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
        controllers.removeIf( c -> c.robotCount() == c.maxSupportedRobots );
        if ( controllers.isEmpty() )
        {
            throw new IllegalStateException( "No suitable controller in range for "+r );
        }
        // assign to controller with least amount of robots
        controllers.sort( Comparator.comparing( Controller::robotCount ) );
        controllers.get(0).assignRobot( r, this );
    }

    public void tick(float deltaSeconds)
    {
        tickListeners.forEach( e -> e.tick( deltaSeconds , this ) );
    }

    public void visitEntities(Consumer<Entity> consumer)
    {
        entities.forEach( consumer );
    }

    public void sendMessage(Message msg)
    {
        boolean send = false;

        for (int i = 0, controllersSize = controllers.size(); i < controllersSize; i++)
        {
            final Controller c = controllers.get( i );
            if ( c.isInRange( msg.sender ) )
            {
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
        for (int i = 0, entitiesSize = entities.size(); i < entitiesSize; i++)
        {
            Entity e = entities.get( i );
            if ( e.contains( position ) )
            {
                return e;
            }
        }
        return null;
    }

    private List<Controller> findControllersInRange(Vec2D pos)
    {
        final List<Controller> result = new ArrayList<>();
        for ( Controller c : controllers ) {
            if ( c.isInRange( pos ) ) {
                result.add( c );
            }
        }
        return result;
    }

    public Depot findClosestDepotThatAccepts(Robot robot,ItemType item, int amount)
    {
        final List<Depot> candidates = new ArrayList<>();
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

    public int getFactoriesProductionLossMissingInput() {
        return entities.stream().filter( x -> x instanceof Factory)
                .mapToInt( x-> ((Factory) x).productionLostMissingInput ).sum();
    }

    public int getFactoriesProductionLossOutputFull() {
        return entities.stream().filter( x -> x instanceof Factory)
                .mapToInt( x-> ((Factory) x).productionLostOutputFull ).sum();
    }
}