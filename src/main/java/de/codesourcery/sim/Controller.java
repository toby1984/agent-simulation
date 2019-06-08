package de.codesourcery.sim;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongIterator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Controller extends Entity implements ITickListener
{
    public static final float BROADCAST_RADIUS = 0.75f;
    public static final float BROADCAST_RADIUS2 = BROADCAST_RADIUS * BROADCAST_RADIUS;

    public static final Vec2D RANGE_EXTENT = new Vec2D(BROADCAST_RADIUS*2,BROADCAST_RADIUS*2);

    private final List<Message> offers = new ArrayList<>();

    private final List<Message> requests = new ArrayList<>();

    private final Long2ObjectArrayMap<Robot> robots = new Long2ObjectArrayMap<>();
    private final LongArraySet busy = new LongArraySet();
    private final LongArraySet idleEmpty =  new LongArraySet();
    private final LongArraySet idleCarrying =  new LongArraySet();

    public int maxSupportedRobots = 500;

    public Controller(Vec2D v)
    {
        super( v );
    }

    public void broadcast(Message msg) {

        switch( msg.type.kind ) {

            case OFFER:
                offers.add( msg );
                break;
            case REQUEST:
                requests.add( msg );
                break;
            default:
                throw new IllegalArgumentException("Unhandled switch/case: "+msg.type.kind);
        }
    }

    public void assignRobot(Robot robot,World world)
    {
        if ( robots.size() == maxSupportedRobots ) {
            throw new IllegalStateException( "Cannot assign robot to controller #" + id + " that is already at max. capacity" );
        }
        robot.controller = this;
        robots.put( robot.id, robot );
        busyStateChanged(robot,world);
    }

    private Robot findClosestIdleCarrying(Message request, World world)
    {
        Robot closest = null;
        float closestDst2 = 0;
        for ( LongIterator it = idleCarrying.iterator() ; it.hasNext() ; )
        {
            final Robot r = robots.get( it.nextLong() );
            if ( r.carriedItem(world).matches( request.getItemAndAmount().type ) )
            {
                float dst2 = request.sender.dst2( r );
                if ( closest == null || dst2 < closestDst2 ) {
                    closestDst2 = dst2;
                    closest = r;
                }
            }
        }
        return closest;
    }

    @Override
    public void tick(float deltaSeconds, World world)
    {
        final Int2ObjectAVLTreeMap<List<Message>> batchedRequests = new Int2ObjectAVLTreeMap<>( (a,b) -> Integer.compare(b,a) );
        for ( Message request : requests )
        {
            final List<Message> batch = batchedRequests.computeIfAbsent(request.priority, k -> new ArrayList<>());
            batch.add( request );
        }

        // shuffle each batch so messages with the same priority don't get processed
        // in the same order all the time, causing some requests to starve
        for ( var batch : batchedRequests.int2ObjectEntrySet() )
        {
            Collections.shuffle(batch.getValue());
        }

        // first, try to serve requests using idle robots carrying items first
        // so they can be used for other tasks ASAP
        for ( var entry : batchedRequests.int2ObjectEntrySet() )
        {
            final List<Message> batch = entry.getValue();
            for (Iterator<Message> reqIt = batch.iterator(); reqIt.hasNext(); )
            {
                final Message req = reqIt.next();
                final Robot robot = findClosestIdleCarrying(req, world);
                if ( robot != null )
                {
                    idleCarrying.remove( robot.id );
                    busy.add( robot.id );
                    reqIt.remove();
                    robot.receive(req);
                }
            }
        }

        // now traverse requests looking for matching offers
        // and use idle empty robots to fulfill them
        for ( var entry : batchedRequests.int2ObjectEntrySet() )
        {
            final List<Message> batch = entry.getValue();

            for (int i = 0; i < batch.size() && ! idleEmpty.isEmpty() ; i++)
            {
                Message request = batch.get(i);

                // look for matching offers and prefer the closest one
                Message offer = null;
                float bestDist2 = 0;
                for ( var x : offers )
                {
                    if ( x.getItemAndAmount().type.matches(request.getItemAndAmount().type) )
                    {
                        float dist2 = x.sender.dst2(request.sender);
                        if ( offer == null || dist2 < bestDist2 ) {
                            offer = x;
                            bestDist2 = dist2;
                        }
                    }
                }

                if (offer != null)
                {
                    var robot = findClosestRobot( idleEmpty, offer.sender );
                    if ( robot != null )
                    {
                        offers.remove( offer );
                        // no need to remove request here as we're
                        // not using them afterward anyway
                        idleEmpty.remove( robot.id );
                        busy.add( robot.id );
                        if ( Main.DEBUG )
                        {
                            System.out.println( "Using robot " + robot + " that carries " + world.inventory.getAmounts( robot ) );
                        }
                        robot.transfer( offer.sender, offer.getItemAndAmount(), request.sender );
                    }
                }
            }
        }

        // if there are no requests for items but we have
        // idle robots carrying stuff, have them drop it off at the nearest depot
        // so they're eligible for other tasks
        for ( LongIterator iterator = idleCarrying.iterator(); iterator.hasNext(); )
        {
            final long id = iterator.nextLong();
            final var r = robots.get(id);
            final ItemAndAmount carried = r.carriedItemAndAmount(world);
            final Depot depot = world.findClosestDepotThatAccepts(r, carried.type, carried.amount);
            if ( depot != null )
            {
                final int toDeliver = Math.min( depot.getAcceptedAmount( carried.type, world ), carried.amount );
                if ( toDeliver > 0 )
                {
                    carried.amount = toDeliver;
                    r.receive( new Message( depot, Message.MessageType.ITEM_NEEDED, carried ) );
                    iterator.remove();
                    busy.add( r.id );
                }
            }
        }

        // if we have factories offering stuff, try moving it to the
        // closes possible depot
        offers.sort(Message.PRIO_COMPERATOR);
        for (int i = 0, offerCount = offers.size(); i < offerCount && ! idleEmpty.isEmpty() ; i++)
        {
            Message offer = offers.get(i);

            final long id = idleEmpty.iterator().nextLong();
            var robot = robots.get(id);
            final Depot depot = world.findClosestDepotThatAccepts(robot, offer.getItemAndAmount().type, offer.getItemAndAmount().amount);
            if ( depot != null && depot != offer.sender )
            {
                idleEmpty.remove(id);
                busy.add(id);
                if ( Main.DEBUG )
                {
                    System.out.println( "Using robot " + robot + " that carries " + world.inventory.getAmounts( robot ) );
                }
                robot.transfer( offer.sender,offer.getItemAndAmount(),depot);
            }
        }
        offers.clear();
        requests.clear();
    }

    private Robot findClosestRobot(LongArraySet availableRobotIds, Entity destination)
    {
        Robot result = null;
        float closestDst2 = 0;
        for ( var it = availableRobotIds.iterator() ; it.hasNext() ;  )
        {
            final long robotId = it.nextLong();
            final Robot r = robots.get( robotId );

            float dist2 = r.dst2( destination );
            if ( result == null || dist2 < closestDst2 ) {
                closestDst2 = dist2;
                result = r;
            }
        }
        return result;
    }

    public void busyStateChanged(Robot robot,World world) {

        final long id = robot.id;

        if ( robot.isBusy() )
        {
            // robot is busy
            busy.add(id);
            idleEmpty.remove(id);
            idleCarrying.remove(id);
        } else {
            // robot is idle
            busy.remove(id);
            if ( robot.isEmpty(world) ) {
                idleEmpty.add(id);
                idleCarrying.remove(id);
            } else {
                idleEmpty.remove( id );
                idleCarrying.add( id );
            }
        }
    }

    public int robotCount() {
        return robots.size();
    }

    public float utilization()
    {
        return busy.size() / (float) robotCount();
    }

    @Override
    public String toString()
    {
        return "Controller #"+id+" [ "+busy.size()+" busy, "+idleCarrying.size()+" idle carrying, "+
                idleEmpty.size()+" idle empty, "+(utilization()*100)+"% busy ]";
    }

    public boolean isInRange(Vec2D v) {
        return dst2( v ) <= BROADCAST_RADIUS2;
    }

    public boolean isInRange(Entity entity)
    {
        return isInRange( entity.position );
    }

    public String getDebugStatus(World world)
    {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("Controller #"+id )
                .append(" ")
                .append( robots.size() )
                .append( " robots, " )
                .append( busy.size() )
                .append( " busy, " )
                .append( idleCarrying.size() )
                .append( " idle (carrying), " )
                .append( idleEmpty.size() )
                .append( " idle (empty)" )
                .append("\n");
        robots.values().stream().sorted( Comparator.comparing( a -> a.id ) ).forEach( r -> {
            buffer.append( "Robot #" ).append( r.id )
                    .append( ", state: " )
                    .append( r.currentState )
                    .append( " [carrying: " )
                    .append( r.carriedItem( world ) )
                    .append( " x " )
                    .append( r.carriedAmount( world ) )
                    .append(" ]\n");
        });
        return buffer.toString();
    }
}