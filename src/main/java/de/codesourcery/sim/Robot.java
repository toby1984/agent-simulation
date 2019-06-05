package de.codesourcery.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Robot extends MoveableEntity
{
    private static final float PICKUP_DIST = 0.1f;
    private static final float PICKUP_DIST2 = PICKUP_DIST*PICKUP_DIST;

    public State nextState = new IdleState();
    public State currentState = null;
    public ItemType carriedItem;
    public int carriedAmount;
    public int maxCarryingCapacity = 1;

    private List<Message> inbox = new ArrayList<>();

    private abstract class State {

        public void onEnter(World world) { }

        public abstract State tick(float deltaSeconds,World world);
    }

    private final class PreparePickupState extends State
    {
        private final Message request;
        private int randomDelay = 3;
        private int pickedUpByOtherRobots;

        private PreparePickupState(Message incomingMessage)
        {
            this.request = incomingMessage;
            randomDelay = 1+new Random().nextInt( 10 );
        }

        @Override
        public String toString()
        {
            return "PREPARE_PICKUP[ delay: "+randomDelay+", pickedUpByOthers: "+
                    pickedUpByOtherRobots+", reply_to: "+request+" ]";
        }

        @Override
        public void onEnter(World world)
        {
            final int amount = maxCarryingCapacity - carriedAmount;
            world.sendMessage(
                    request.createReply( Robot.this,Message.MessageType.PICKING_UP, new ItemAndAmount( request.getItemAndAmount().type, amount )) );
        }

        @Override
        public State tick(float deltaSeconds, World world)
        {
            // check whether other robots also responded and if yes, how many items they're picking up
            for ( Message msg : inbox )
            {
                if ( msg.hasType( Message.MessageType.PICKING_UP ) &&
                        msg.isReplyTo( request ) )
                {
                    pickedUpByOtherRobots += msg.getItemAndAmount().amount;
                }
            }
            inbox.clear();
            if ( pickedUpByOtherRobots >= request.getItemAndAmount().amount ) {
                return new IdleState();
            }
            if ( --randomDelay == 0 ) {
                // timeout elapsed, pick up items for real
                return new MoveToLocationState( request.sender.position(),
                        new PickupState( request ) );
            }
            return this;
        }
    }

    private final class PrepareDropOffState extends State
    {
        private final Message request;
        private int randomDelay = 3;
        private int droppedByOtherRobots;

        private PrepareDropOffState(Message incomingMessage)
        {
            this.request = incomingMessage;
            randomDelay = 1+new Random().nextInt( 10 );
        }

        @Override
        public String toString()
        {
            return "PREPARE_DROPOFF[ delay: "+randomDelay+", droppedByOthers: "+
                    droppedByOtherRobots+", reply_to: "+request+" ]";
        }

        @Override
        public void onEnter(World world)
        {
            final int amount = Math.min( request.getItemAndAmount().amount, carriedAmount);
            world.sendMessage(
                    request.createReply( Robot.this,Message.MessageType.DROPPING_OFF,
                            new ItemAndAmount( request.getItemAndAmount().type, amount )) );
        }

        @Override
        public State tick(float deltaSeconds, World world)
        {
            // check whether other robots also responded and if yes, how many items they're picking up
            for ( Message msg : inbox )
            {
                if ( msg.hasType( Message.MessageType.DROPPING_OFF ) &&
                     msg.isReplyTo( request ) )
                {
                    droppedByOtherRobots += msg.getItemAndAmount().amount;
                }
            }
            inbox.clear();
            if ( droppedByOtherRobots >= request.getItemAndAmount().amount ) {
                return new IdleState();
            }
            if ( --randomDelay == 0 ) {
                // timeout elapsed, pick up items for real
                return new MoveToLocationState(request.sender.position(), new DropOffState( request ) );
            }
            return this;
        }
    }

    private final class PickupState extends State {

        private final Message request;

        private PickupState(Message request)
        {
            this.request = request;
        }

        @Override
        public String toString()
        {
            return "PICKUP[ reply_to: "+request+" ]";
        }

        @Override
        public State tick(float deltaSeconds, World world)
        {
            int toTake = Math.min( maxCarryingCapacity - carriedAmount , request.getItemAndAmount().amount );
            int taken = ((IItemProvider) request.sender).take(request.getItemAndAmount().type, toTake );
            if ( taken > 0 )
            {
                carriedAmount += taken;
                carriedItem = request.getItemAndAmount().type;
            }
            return new IdleState();
        }
    }

    private final class DropOffState extends State {

        private final Message request;

        private DropOffState(Message request)
        {
            this.request = request;
        }

        @Override
        public String toString()
        {
            return "DROP_OFF[ reply_to: "+request+" ]";
        }

        @Override
        public State tick(float deltaSeconds, World world)
        {
            int toGive = Math.min( carriedAmount , request.getItemAndAmount().amount );
            int accepted = ((IItemReceiver) request.sender).offer(request.getItemAndAmount().type, toGive );
            if ( accepted > 0 )
            {
                carriedAmount -= accepted;
                if ( carriedAmount == 0 ) {
                    carriedItem = null;
                }
            }
            return new IdleState();
        }
    }

    private final class MoveToLocationState extends State {

        private final Vec2D destination;
        private final State stateAtDestination;

        private MoveToLocationState(Vec2D destination,State stateAtDestination)
        {
            this.destination = destination.cpy();
            this.stateAtDestination = stateAtDestination;
        }

        @Override
        public String toString()
        {
            return "MOVE_TO "+destination;
        }

        @Override
        public State tick(float deltaSeconds, World world)
        {
            // move towards target
            final Vec2D delta = destination.cpy().sub( position() ).nor();
            delta.scl( 0.05f*deltaSeconds);
            position.add( delta );
            if ( position.dst2( destination ) <= PICKUP_DIST2 ) {
                return stateAtDestination;
            }
            return this;
        }
    }

    private final class IdleState extends State
    {
        @Override
        public String toString()
        {
            return "IDLE";
        }

        @Override
        public State tick(float deltaSeconds, World world)
        {
            if ( inbox.isEmpty() ) {
                return this;
            }
            if ( carriedAmount > 0 )
            {
                // look for "drop off" messages
                for ( Message msg : inbox )
                {
                    if ( msg.hasType( Message.MessageType.ITEM_NEEDED) )
                    {
                        final ItemAndAmount it = msg.getItemAndAmount();
                        if ( it.hasType( carriedItem ) )
                        {
                            inbox.clear();
                            return new PrepareDropOffState( msg );
                        }
                    }
                }
            } else {
                // look for "pick up" messages
                for ( Message msg : inbox )
                {
                    if ( msg.hasType( Message.MessageType.ITEM_AVAILABLE ) )
                    {
                        final ItemAndAmount it = msg.getItemAndAmount();
                        if ( carriedItem == null ||
                                availableCarryingCapacity() > 0 &&
                              it.hasType( carriedItem ) )
                        {
                            inbox.clear();
                            return new PreparePickupState( msg );
                        }
                    }
                }
            }
            inbox.clear();
            return this;
        }
    }

    public Robot(Vec2D v)
    {
        super( v );
    }

    public Robot(float x, float y)
    {
        super( x, y );
    }

    public int availableCarryingCapacity()
    {
        return maxCarryingCapacity - carriedAmount;
    }

    @Override
    public String toString()
    {
        final String carrying = carriedItem == null ? "<nothing>" : carriedItem+"x"+carriedAmount+"/"+maxCarryingCapacity;
        return "Robot #"+id+" [ "+carrying+" ] = { pos: "+position+" , vel: "+velocity+", accel: "+acceleration+" }";
    }

    @Override
    public void tick(float deltaSeconds,World world)
    {
        if ( nextState != currentState ) {
            currentState = nextState;
            System.out.println( this+" is now in state "+currentState);
            currentState.onEnter( world );
        }
        else
        {
            nextState = currentState.tick( deltaSeconds, world );
        }
    }

    public void receive(Message msg) {
        this.inbox.add(msg);
    }
}