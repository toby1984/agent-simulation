package de.codesourcery.sim;

import java.util.Set;

public class Robot extends MoveableEntity implements IItemReceiver
{
    private static final float PICKUP_DIST = 0.1f;
    private static final float PICKUP_DIST2 = PICKUP_DIST*PICKUP_DIST;

    public State currentState = new IdleState();
    public int maxCarryingCapacity = 2;
    private Controller controller;

    public abstract class State {

        public void onEnter(World world) { }

        public void onExit(World world) { }

        public abstract State tick(float deltaSeconds,World world);

        public void receive(Message message) {
        }
    }

    public final class TransferState extends State
    {
        public final Entity src;
        public final ItemAndAmount details;
        public final Entity dst;

        private boolean pickingUp = true;
        private State current;

        public TransferState(Entity src, ItemAndAmount details, Entity dst) {

            this.src = src;
            this.dst = dst;
            this.details = details;
            current = new MoveToLocationState( src.position(), new PickupState(src,details) );
        }

        @Override
        public String toString()
        {
            return "TRANSFER[ "+details+" from "+src+" to "+dst+"], current: "+current;
        }

        @Override
        public State tick(float deltaSeconds, World world)
        {
            State next = current.tick(deltaSeconds,world );
            if ( next != current )
            {
                if ( next instanceof IdleState )
                {
                    if (!pickingUp)
                    {
                        return next;
                    }
                    pickingUp = false;
                    next = new MoveToLocationState(dst.position(), new DropOffState(dst,details) );
                }
            }
            current = next;
            return this;
        }
    }

    private final class PickupState extends State {

        private final Entity entity;
        private final ItemAndAmount details;

        private PickupState(Entity entity, ItemAndAmount details)
        {
            this.entity = entity;
            this.details = details;
        }

        @Override
        public String toString()
        {
            return "PICKUP[ "+details+" from "+entity+" ]";
        }

        @Override
        public State tick(float deltaSeconds, World world)
        {
            final int toTake = Math.min( maxCarryingCapacity - carriedAmount(world) , details.amount );
            world.inventory.transfer(entity,details.type,toTake,Robot.this,world);
            return new IdleState();
        }
    }

    private final class DropOffState extends State {

        private final Entity entity;
        private final ItemAndAmount details;

        private DropOffState(Entity entity, ItemAndAmount details)
        {
            this.entity = entity;
            this.details = details;
        }

        @Override
        public String toString()
        {
            return "DROP_OFF[ "+details+" at "+entity+" ]";
        }

        @Override
        public State tick(float deltaSeconds, World world)
        {
            int toGive = Math.min( carriedAmount(world) , details.amount );
            world.inventory.transfer(Robot.this, carriedItem(world), toGive,entity, world);
            return new IdleState();
        }
    }

    public final class MoveToLocationState extends State {

        public final Vec2D destination;
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
            delta.scl( 0.2f*deltaSeconds);
            position.add( delta );
            if ( position.dst2( destination ) <= PICKUP_DIST2 ) {
                return stateAtDestination;
            }
            return this;
        }
    }

    public final class IdleState extends State
    {
        private State nextState = this;

        @Override
        public String toString()
        {
            return "IDLE";
        }

        @Override
        public void onEnter(World world)
        {
            controller.busyStateChanged(Robot.this, world );
        }

        @Override
        public void onExit(World world)
        {
            controller.busyStateChanged(Robot.this, world );
        }

        @Override
        public void receive(Message msg)
        {
            if ( msg.hasType( Message.MessageType.ITEM_NEEDED) )
            {
                nextState = new MoveToLocationState(msg.sender.position, new DropOffState(
                    msg.sender,msg.getItemAndAmount()));
            }
            else if ( msg.hasType( Message.MessageType.ITEM_AVAILABLE ) )
            {
                nextState = new MoveToLocationState( msg.sender.position,
                    new PickupState( msg.sender, msg.getItemAndAmount() ) );
            } else {
                throw new IllegalArgumentException("Message not understood: "+msg);
            }
        }

        @Override
        public State tick(float deltaSeconds, World world)
        {
            return nextState;
        }
    }

    public Robot(Vec2D v)
    {
        super( v );
        extent.set(0.05f,0.05f);
    }

    @Override
    public String toString()
    {
        return "Robot #"+id+" [ controller: "+
                (controller == null ? "none": Long.toString(controller.id))+" , "+currentState+" ,  ]";
    }

    @Override
    public void tick(float deltaSeconds,World world)
    {
        State nextState = currentState.tick( deltaSeconds, world );
        if ( nextState != currentState )
        {
            if ( Main.DEBUG )
            {
                System.out.println( this + " transitioning " + currentState + " -> " + nextState );
            }
            currentState.onExit(world );
            this.currentState = nextState;
            nextState.onEnter(world);
        }
    }

    public boolean isIdle() {
        return this.currentState instanceof IdleState;
    }

    public boolean isBusy() {
        return ! isIdle();
    }

    public boolean isEmpty(World world) {
        return carriedAmount(world) == 0;
    }

    public ItemType carriedItem(World world)
    {
        return world.inventory.visitInventory(this, (itemType,amount,ctx) ->
        {
            ctx.stop(itemType);
        }, null);
    }

    public ItemAndAmount carriedItemAndAmount(World world)
    {
        return world.inventory.visitInventory(this, (itemType,amount,ctx) ->
        {
            ctx.stop(new ItemAndAmount(itemType, amount));
        }, null);
    }

    public void transfer(Entity src,ItemAndAmount details,Entity dst)
    {
        if ( isBusy() ) {
            throw new IllegalStateException("Busy robot "+this+" in state "+currentState+" cannot transfer stuff");
        }
        currentState = new TransferState(src,details,dst);
    }

    public void receive(Message msg) {
        currentState.receive(msg);
    }

    public boolean hasController() {
        return this.controller != null;
    }

    public int carriedAmount(World world) {
        return world.inventory.getStoredAmount(this);
    }

    @Override
    public Set<ItemType> getAcceptedItems( World world )
    {
        final ItemType carried = carriedItem( world );
        return Set.of( carried == null ? ItemType.ANY : carried );
    }

    @Override
    public int getAcceptedAmount(ItemType type, World world)
    {
        final ItemAndAmount itemAndAmount = carriedItemAndAmount(world);
        if ( itemAndAmount == null || itemAndAmount.amount == 0 ) {
            return maxCarryingCapacity;
        }
        return maxCarryingCapacity - itemAndAmount.amount;
    }

    @Override
    public void addController(Controller controller)
    {
        this.controller = controller;
    }

    public Controller controller() {
        return this.controller;
    }

    public void setController(Controller controller)
    {
        this.controller = controller;
    }
}