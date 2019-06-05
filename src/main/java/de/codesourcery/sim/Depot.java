package de.codesourcery.sim;

public class Depot extends Entity implements IItemReceiver,IItemProvider, ITickListener
{
    public ItemType[] acceptedItemTypes;
    public int[] amountByType;
    public int minAmount=3;
    public int storedAmount;
    public int capacity =100;

    public Depot(Vec2D v, ItemType... acceptedItemTypes)
    {
        super( v );
        this.acceptedItemTypes = acceptedItemTypes;
        this.amountByType = new int[ acceptedItemTypes.length ];
    }

    @Override
    public void tick(float deltaSeconds, World world) {

        int remainingCapacity = capacity - storedAmount;
        for (int i = 0; remainingCapacity > 0 && i < acceptedItemTypes.length; i++)
        {
            final int availableAmount = amountByType[i];
            if ( availableAmount < minAmount )
            {
                int toAsk = Math.min( minAmount, remainingCapacity );
                world.sendMessage(
                        new Message(this,
                                Message.MessageType.ITEM_NEEDED,
                                new ItemAndAmount( this.acceptedItemTypes[i],
                                        toAsk )) );
            }
            if ( availableAmount > 0 ) {
                world.sendMessage(
                        new Message(this,
                                Message.MessageType.ITEM_AVAILABLE,
                                new ItemAndAmount( this.acceptedItemTypes[i],
                                        availableAmount )) );
            }
        }
    }

    @Override
    public int offer(ItemType type, int amount)
    {
        for ( int i =0 ; i < acceptedItemTypes.length ; i++ ) {
            if ( acceptedItemTypes[i].matches( type ) )
            {
                final int slotsAvailable = capacity - storedAmount;
                final int transferred = Math.min( amount, slotsAvailable );
                storedAmount += transferred;
                amountByType[i] += transferred;
                return transferred;
            }
        }
        return 0;
    }

    @Override
    public int take(ItemType type, int amount)
    {
        for ( int i =0 ; i < acceptedItemTypes.length ; i++ )
        {
            if ( acceptedItemTypes[i].matches( type ) )
            {
                final int available = amountByType[i];
                if ( available > 0 )
                {
                    final int transferred = Math.min( amount, available );
                    storedAmount -= transferred;
                    amountByType[i] -= transferred;
                    return transferred;
                }
            }
        }
        return 0;
    }

    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder();
        for( int i = 0, len= acceptedItemTypes.length ; i < len ; i++)
        {
             buffer.append( acceptedItemTypes[i]+"x"+amountByType[i] );
             if ( (i+1) < len ) {
                 buffer.append(",");
             }
        }
        return "Depot #"+id+" {"+buffer+"}";
    }
}