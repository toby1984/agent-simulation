package de.codesourcery.sim;

import org.apache.commons.lang3.Validate;

public class Factory extends Entity implements IItemProvider,IItemReceiver,ITickListener
{
    public ItemType producedItem = ItemType.CONCRETE;
    public int maxStorage=10;

    public float productionTimeSeconds=5;
    public int itemsPerCycle = 1;

    public float elapsedSeconds;

    public ItemType input1Type = ItemType.STONE;
    public int input1Consumed=1;
    public int input1MaxAmount=10;

    public Factory(Vec2D v)
    {
        super( v );
    }

    public Factory(float x, float y)
    {
        super( x, y );
    }

    private int input1Stored(World world) {
        return world.inventory.getAmount(this, input1Type );
    }

    private int storedAmount(World world) {
        return world.inventory.getAmount(this, producedItem);
    }

    public void tick(float elapsedSeconds, World world)
    {
        this.elapsedSeconds += elapsedSeconds;
        if ( this.elapsedSeconds > productionTimeSeconds )
        {
            if ( input1Stored(world) >= input1Consumed )
            {
                final int newAmount = storedAmount(world) + itemsPerCycle;
                if ( newAmount <= maxStorage )
                {
                    world.inventory.consume(this,input1Type,input1Consumed);
                    world.inventory.create(this,producedItem,itemsPerCycle);
                }
            }
            this.elapsedSeconds -= productionTimeSeconds;
        }

        final int storedAmount = storedAmount(world);
        if ( storedAmount > 0 ) {
            world.sendMessage( new Message(this, Message.MessageType.ITEM_AVAILABLE,
                new ItemAndAmount( producedItem, storedAmount ), Message.LOW_PRIORITY ) );
        }
        final int inputNeeded = input1MaxAmount - input1Stored(world );
        if ( inputNeeded > 0 ) {
            world.sendMessage( new Message(this, Message.MessageType.ITEM_NEEDED,
                    new ItemAndAmount( input1Type, inputNeeded ) ) );
        }
    }

    @Override
    public String toString()
    {
        return "Factory #"+id+"[ product_stored: " + producedItem + " (max. " +
                maxStorage + "), input1_stored: " + input1Type + " (max. " + input1MaxAmount + ") ]";
    }

    @Override
    public int getAcceptedAmount(ItemType type, World world)
    {
        if ( this.input1Type.matches(type ) )
        {
            return input1MaxAmount - input1Stored(world );
        }
        return 0;
    }
}