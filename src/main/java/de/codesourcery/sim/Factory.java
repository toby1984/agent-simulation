package de.codesourcery.sim;

import org.apache.commons.lang3.Validate;

public class Factory extends Entity implements IItemProvider,IItemReceiver,ITickListener
{
    public ItemType producedItem = ItemType.CONCRETE;

    public int storedAmount;
    public int maxStorage=10;

    public float productionTimeSeconds=5;
    public int itemsPerCycle = 1;

    public float elapsedSeconds;

    public ItemType input1Type = ItemType.STONE;
    public int input1Consumed=1;
    public int input1Stored=0;
    public int input1MaxAmount=10;

    public Factory(Vec2D v)
    {
        super( v );
    }

    public Factory(float x, float y)
    {
        super( x, y );
    }

    public void tick(float elapsedSeconds, World world)
    {
        this.elapsedSeconds += elapsedSeconds;
        if ( this.elapsedSeconds > productionTimeSeconds )
        {
            if ( input1Stored >= input1Consumed )
            {
                int newAmount = storedAmount + itemsPerCycle;
                if ( newAmount <= maxStorage )
                {
                    storedAmount += itemsPerCycle;
                }
            }
            this.elapsedSeconds -= productionTimeSeconds;
        }

        if ( storedAmount > 0 ) {
            world.sendMessage( new Message(this, Message.MessageType.ITEM_AVAILABLE, new ItemAndAmount( producedItem, storedAmount ) ) );
        }
        final int inputNeeded = input1MaxAmount - input1Stored;
        if ( inputNeeded > 0 ) {
            world.sendMessage( new Message(this, Message.MessageType.ITEM_NEEDED,
                    new ItemAndAmount( input1Type, inputNeeded ) ) );
        }
    }

    @Override
    public String toString()
    {
        return "Factory #"+id+"[ product_stored: " + producedItem + " " + storedAmount + "/" +
                maxStorage + ", input1_stored: " + input1Type + " " + input1Stored + "/" + input1MaxAmount + " ]";
    }

    @Override
    public int take(ItemType type, int amount)
    {
        Validate.isTrue( amount > 0 );

        if ( producedItem.matches( type ) )
        {
            final int result = Math.min( amount , storedAmount );
            storedAmount -= amount;
            return result;
        }
        return 0;
    }

    @Override
    public int offer(ItemType type, int amount)
    {
        if ( type.matches( input1Type ) ) {
            int availableSpace = input1MaxAmount - input1Stored;
            if ( availableSpace > 0 )
            {
                final int taken = Math.min(amount,availableSpace);
                input1Stored += taken;
                return taken;
            }
        }
        return 0;
    }
}