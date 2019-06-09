package de.codesourcery.sim;

import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Factory extends Entity implements IItemProvider,IItemReceiver,ITickListener
{
    private final Set<ItemType> produced;

    public final ItemType producedItem;
    public int maxStorage=10;

    public float productionTimeSeconds=2;
    public int itemsPerCycle = 1;

    public float elapsedSeconds;

    private ItemType input1Type = ItemType.STONE;
    private Set<ItemType> inputTypes = Set.of( input1Type );

    public int input1Consumed=1;
    public int input1MaxAmount=10;

    public int productionLostMissingInput;
    public int productionLostOutputFull;

    public final List<Controller> controllers = new ArrayList<>();

    public Factory(Vec2D v, ItemType producedItem)
    {
        super( v );
        Validate.notNull( producedItem, "producedItem must not be null" );
        this.producedItem = producedItem;
        produced = Set.of( producedItem );
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
                } else {
                    productionLostOutputFull++;
                }
            } else {
                productionLostMissingInput++;
            }
            this.elapsedSeconds -= productionTimeSeconds;
        }

        final int storedAmount = storedAmount(world);
        if ( storedAmount > 0 )
        {
            int prio = storedAmount < maxStorage ? Message.LOW_PRIORITY : Message.HIGH_PRIORITY;
            world.sendMessage( new Message(this, Message.MessageType.ITEM_AVAILABLE,
                new ItemAndAmount( producedItem, storedAmount ), prio ) );
        }
        final int input1Stored = input1Stored(world );
        final int inputNeeded = input1MaxAmount - input1Stored;
        if ( inputNeeded > 0 )
        {
            int prio = input1Stored >= input1Consumed ? Message.LOW_PRIORITY : Message.HIGH_PRIORITY;
            world.sendMessage( new Message(this, Message.MessageType.ITEM_NEEDED,
                    new ItemAndAmount( input1Type, inputNeeded ), prio ) );
        }
    }

    @Override
    public String toString()
    {
        return "Factory #"+id+"[ "+input1Consumed+" "+input1Type+" -> "+itemsPerCycle+" "+producedItem+" ]";
    }

    @Override
    public Set<ItemType> getAcceptedItems(World world)
    {
        return null;
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

    @Override
    public Set<ItemType> getProvidedItems(World world)
    {
        return produced;
    }

    @Override
    public void addController(Controller controller)
    {
        this.controllers.add( controller );
    }

    public void setInput1Type(ItemType input1Type)
    {
        Validate.notNull( input1Type, "input1Type must not be null" );
        this.input1Type = input1Type;
        this.inputTypes = Set.of( input1Type );
    }

    public ItemType input1Type()
    {
        return input1Type;
    }
}