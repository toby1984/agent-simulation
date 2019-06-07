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

public class Controller extends Entity implements ITickListener
{
    public static final float BROADCAST_DIST = 0.5f;
    public static final float BROADCAST_DIST2 = BROADCAST_DIST*BROADCAST_DIST;

    public static final Vec2D BROADCAST_RANGE = new Vec2D(
            (float) Math.sqrt( BROADCAST_DIST*BROADCAST_DIST + BROADCAST_DIST*BROADCAST_DIST),
            (float) Math.sqrt( BROADCAST_DIST*BROADCAST_DIST + BROADCAST_DIST*BROADCAST_DIST)
    );

    private final List<Message> offers = new ArrayList<>();

    private final List<Message> requests = new ArrayList<>();

    private final Long2ObjectArrayMap<Robot> robots = new Long2ObjectArrayMap<>();
    private final LongArraySet busy = new LongArraySet();
    private final LongArraySet idleEmpty =  new LongArraySet();
    private final LongArraySet idleCarrying =  new LongArraySet();

    public int maxSupportedRobots = 20;

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

    @Override
    public void tick(float deltaSeconds, World world)
    {
        // sort requests descending by priority
        requests.sort(Message.PRIO_COMPERATOR);

        // try to serve requests using idle robots carrying items first
        // so they can be used for other tasks
        for (LongIterator robotIterator = idleCarrying.iterator(); robotIterator.hasNext(); )
        {
            final long id = robotIterator.nextLong();
            final Robot robot = robots.get(id);
            final ItemType carried = robot.carriedItem(world);
            for (Iterator<Message> requestIterator = requests.iterator(); requestIterator.hasNext(); )
            {
                final Message msg = requestIterator.next();
                if ( msg.getItemAndAmount().hasType( carried ) )
                {
                    robot.receive(msg);

                    busy.add( robot.id );

                    requestIterator.remove();
                    robotIterator.remove();
                    break;
                }
            }
        }

        // now try to match remaining requests with offers

        final Int2ObjectAVLTreeMap<List<Message>> batches = new Int2ObjectAVLTreeMap<>( (a,b) -> Integer.compare(b,a) );
        for ( Message request : requests )
        {
            final List<Message> batch = batches.computeIfAbsent(request.priority, k -> new ArrayList<>());
            batch.add( request );
        }

        LongIterator robotIterator = idleEmpty.iterator();
        for ( var entry : batches.int2ObjectEntrySet() )
        {
            final List<Message> batch = entry.getValue();

            // shuffle batch of requests with the same priority
            // so we don't always serve the first one
            Collections.shuffle(batch);

            for (int i = 0; i < batch.size() && robotIterator.hasNext(); i++)
            {
                Message request = batch.get(i);

                // find matching offers and prefer the closest one
                Message candidate = null;
                float bestDist2 = 0;
                for ( var x : offers )
                {
                    if ( x.getItemAndAmount().type.matches(request.getItemAndAmount().type) )
                    {
                        float dist2 = x.sender.dst2(request.sender);
                        if ( candidate == null || dist2 < bestDist2 ) {
                            candidate = x;
                            bestDist2 = dist2;
                        }
                    }
                }

                if (candidate != null)
                {
                    var robot = robots.get(robotIterator.nextLong());
                    requests.remove( request );
                    offers.remove(candidate);
                    robotIterator.remove();
                    busy.add( robot.id );
                    if ( Main.DEBUG )
                    {
                        System.out.println( "Using robot " + robot + " that carries " + world.inventory.getAmounts( robot ) );
                    }
                    robot.transfer(candidate.sender, candidate.getItemAndAmount(), request.sender);
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
        return dst2( v ) <= BROADCAST_DIST2;
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