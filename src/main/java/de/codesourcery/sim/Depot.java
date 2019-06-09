package de.codesourcery.sim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Depot extends Entity implements IItemReceiver,IItemProvider, ITickListener
{
    public final Set<ItemType> acceptedItemTypes;
    public int minAmount=0;
    public int capacity = 200;

    public final List<Controller> controllers = new ArrayList<>();

    public Depot(Vec2D v, ItemType... acceptedItemTypes)
    {
        super( v );
        this.acceptedItemTypes = Set.of( acceptedItemTypes );
    }

    @Override
    public void tick(float deltaSeconds, World world) {

        int remainingCapacity = capacity - availableSpace(world);

        final List<ItemAndAmount> toProcess = world.inventory.getAmounts(this,
            (type, amount) -> amount < minAmount || amount > 0);

        final Set<ItemType> notSeen = new HashSet<>(acceptedItemTypes);

        for ( ItemAndAmount item : toProcess )
        {
            final int availableAmount = item.amount;
            notSeen.remove( item.type );

            if ( availableAmount < minAmount && remainingCapacity > 0 )
            {
                int toAsk = Math.min( minAmount-availableAmount, remainingCapacity );
                world.sendMessage(
                        new Message(this,
                                Message.MessageType.ITEM_NEEDED,
                                new ItemAndAmount( item.type, toAsk ),
                                Message.LOW_PRIORITY
                        ));
                remainingCapacity -= toAsk;
            }
            if ( availableAmount > 0 ) {
                world.sendMessage(
                        new Message(this,
                                Message.MessageType.ITEM_AVAILABLE,
                                new ItemAndAmount( item.type,availableAmount ),
                            Message.MEDIUM_PRIORITY
                        ));
            }
        }

        if ( minAmount > 0 && remainingCapacity > 0 && ! notSeen.isEmpty() )
        {
            for (ItemType t : notSeen )
            {
                world.sendMessage(
                    new Message(this,
                        Message.MessageType.ITEM_NEEDED,
                        new ItemAndAmount(t, minAmount),
                        Message.LOW_PRIORITY
                    ));
                remainingCapacity -= minAmount;
                if ( remainingCapacity <= 0 ) {
                    break;
                }
            }
        }
    }

    private int availableSpace(World world) {
        return capacity - world.inventory.getStoredAmount(this );
    }

    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder();
        for( var it = acceptedItemTypes.iterator() ;  it.hasNext() ; )
        {
             buffer.append( it.next() );
             if ( it.hasNext() ) {
                 buffer.append(",");
             }
        }
        return "Depot #"+id+" {"+buffer+"}";
    }

    @Override
    public Set<ItemType> getAcceptedItems(World world)
    {
        return acceptedItemTypes;
    }

    @Override
    public int getAcceptedAmount(ItemType type, World world) {

        for ( var t : acceptedItemTypes )
        {
            if ( t.matches(type ) )
            {
                return availableSpace(world);
            }
        }
        return 0;
    }

    @Override
    public void addController(Controller controller)
    {
        this.controllers.add( controller );
    }

    public boolean isFull(World world) {
        return availableSpace( world ) == 0;
    }

    public String getDebugStatus(World world)
    {
        return toString()+"\n Stored "+world.inventory.getStoredAmount(this)+" items out of "+capacity+"\n";
    }

    @Override
    public Set<ItemType> getProvidedItems(World world)
    {
        return world.getAvailableItemTypes( this );
    }
}