package de.codesourcery.sim;

import org.apache.commons.lang3.Validate;

public class Factory extends Entity implements IItemProvider
{
    public ItemType producedItem = ItemType.CONCRETE;

    public int storedAmount;
    public int maxStorage=10;

    public float productionTimeSeconds=5;
    public int itemsPerCycle = 1;

    public float elapsedSeconds;

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
            int newAmount = storedAmount + itemsPerCycle;
            if ( newAmount <= maxStorage )
            {
                storedAmount += itemsPerCycle;
                world.taskManager.schedule( new PickUpTask( this.producedItem, this.itemsPerCycle) );
            }
            this.elapsedSeconds -= productionTimeSeconds;
        }
    }

    @Override
    public String toString()
    {
        return "Factory[ "+producedItem+" ] = { pos: "+position+" , stored: "+storedAmount+" }";
    }

    @Override
    public int get(ItemType type, int amount)
    {
        Validate.isTrue( amount > 0 );

        if ( producedItem == type )
        {
            final int result = Math.min( amount , storedAmount );
            storedAmount -= amount;
            return result;
        }
        return 0;
    }
}
