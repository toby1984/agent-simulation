package de.codesourcery.sim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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

    private final Map<Long,Robot> robots = new HashMap<>();
    private final Set<Long> busy = new HashSet<>();
    private final Set<Long> idleEmpty =  new HashSet<>();
    private final Set<Long> idleCarrying =  new HashSet<>();

    public Controller(Vec2D v)
    {
        super( v );
    }

    public Controller(float x, float y)
    {
        super( x, y );
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

    public void assignRobot(Robot robot,World world) {
        robot.controller = this;
        robots.put( robot.id, robot );
        busyStateChanged(robot,world);
    }

    @Override
    public void tick(float deltaSeconds, World world)
    {
        requests.sort(Message.PRIO_COMPERATOR);

        // try to serve requests using idle robots carrying items first
        // so they can be used for other tasks
        for (Iterator<Long> robotIterator = idleCarrying.iterator(); robotIterator.hasNext(); )
        {
            final Long id = robotIterator.next();
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

        // now try to match requests with offers
        Iterator<Long> robotIterator = idleEmpty.iterator();

        final TreeMap<Integer, List<Message>> batches = new TreeMap<>( (a,b) -> Integer.compare(b,a) );
        for ( Message request : requests )
        {
            final List<Message> batch = batches.computeIfAbsent(request.priority, k -> new ArrayList<>());
            batch.add( request );
        }

        for ( var entry : batches.entrySet() )
        {
            final List<Message> batch = entry.getValue();

            // shuffle batch of requests with the same priority
            // so we don't always serve the first one
            Collections.shuffle(batch);

            for (int i = 0; i < batch.size() && robotIterator.hasNext(); i++)
            {
                Message request = batch.get(i);

                // find matching offers and prefer the closest one
                Message candidate = offers.stream().filter(x -> x.getItemAndAmount().type.matches(request.getItemAndAmount().type))
                                        .min((a, b) -> Float.compare(a.sender.dst2(request.sender), b.sender.dst2(request.sender)))
                                        .orElse(null);

                if (candidate != null)
                {
                    var robot = robots.get(robotIterator.next());
                    requests.remove( request );
                    offers.remove(candidate);
                    robotIterator.remove();
                    System.out.println("Using robot " + robot + " that carries " + world.inventory.getAmounts(robot));
                    robot.transfer(candidate.sender, candidate.getItemAndAmount(), request.sender);
                }
            }
        }

        // if there are no requests for items but we have
        // idle robots carrying stuff, have them drop it off at the nearest depot
        // so they're eligible for other tasks
        for (Iterator<Long> iterator = idleCarrying.iterator(); iterator.hasNext(); )
        {
            Long id = iterator.next();
            final var r = robots.get(id);
            final ItemAndAmount carried = r.carriedItemAndAmount(world);
            final Depot depot = world.findClosestDepotThatAccepts(r, carried.type, carried.amount);
            final int toDeliver = Math.min(depot.getAcceptedAmount(carried.type, world), carried.amount);
            if (toDeliver > 0)
            {
                carried.amount = toDeliver;
                r.receive(new Message(depot, Message.MessageType.ITEM_NEEDED, carried));
                iterator.remove();
                busy.add( r.id );
            }
        }

        // if we have factories offering stuff, try moving it to the
        // closes possible depot
        offers.sort(Message.PRIO_COMPERATOR);
        for (int i = 0, offerCount = offers.size(); i < offerCount && ! idleEmpty.isEmpty() ; i++)
        {
            Message offer = offers.get(i);

            final Long id = idleEmpty.iterator().next();
            var robot = robots.get(id);
            final Depot depot = world.findClosestDepotThatAccepts(robot, offer.getItemAndAmount().type, offer.getItemAndAmount().amount);
            if ( depot != null && depot != offer.sender )
            {
                idleEmpty.remove(id);
                System.out.println("Using robot "+robot+" that carries "+world.inventory.getAmounts(robot));
                robot.transfer( offer.sender,offer.getItemAndAmount(),depot);
            }
        }
        offers.clear();
        requests.clear();
    }

    public void busyStateChanged(Robot robot,World world) {

        final Long id = robot.id;

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
}