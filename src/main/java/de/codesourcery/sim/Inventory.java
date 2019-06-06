package de.codesourcery.sim;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.longs.Long2IntArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Inventory
{
    private Map<ItemType, Long2IntArrayMap> amountsByType = new HashMap<>();

    private Long2ObjectArrayMap<Object2IntArrayMap<ItemType>> itemsByEntity = new Long2ObjectArrayMap<>();

    public interface IInventoryPredicate
    {
        boolean test(ItemType type,int amount);
    }

    public static final class IterationContext
    {
        boolean stop;
        Object result;

        void stop(Object result) {
            this.result = result;
        }

        public void stop() {
            this.stop = true;
        }
    }

    public interface IInventoryVisitor<T>
    {
        /**
         *
         * @param type
         * @param amount
         * @return <code>true</code> if more items in the inventory should be visited
         */
        void visit(ItemType type,int amount,IterationContext context);
    }

    @Override
    public String toString()
    {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("-------------------------------\n");
        for (Map.Entry<ItemType, Long2IntArrayMap> entry : amountsByType.entrySet())
        {
            int sum = entry.getValue().values().stream().reduce(0, Integer::sum);
            buffer.append(entry.getKey()).append(" x ").append(sum).append("\n");
        }
        buffer.append("-------------------------------\n");
        for ( var entry : itemsByEntity.long2ObjectEntrySet() )
        {
            final long entityId = entry.getLongKey();
            buffer.append("Entity #").append(entityId).append(":\n");
            final Object2IntArrayMap<ItemType> items = entry.getValue();
            for (var item : items.object2IntEntrySet() )
            {
                if ( item.getIntValue() > 0 )
                {
                    buffer.append("     ").append(item.getKey()).append(" x ").append(item.getIntValue()).append("\n");
                }
            }
        }
        buffer.append("-------------------------------\n");
        return buffer.toString();
    }

    public void create(Entity location, ItemType type, int amount)
    {
        if ( amount > 0 )
        {
            apply(location, type, amount);
            stateChanged();
        } else if ( amount < 0 ) {
            throw new IllegalArgumentException("Invalid amount: "+amount);
        }
    }

    public int getAmount(Entity entity,ItemType type)
    {
        final Object2IntArrayMap<ItemType> map = itemsByEntity.get( entity.id );
        if ( map != null ) {
            return map.getInt( type );
        }
        return 0;
    }

    public int getStoredAmount(Entity entity)
    {
        final Object2IntArrayMap<ItemType> map = itemsByEntity.get( entity.id );
        int result = 0;
        if ( map != null )
        {
            for (IntIterator it = map.values().iterator() ; it.hasNext() ;  ) {
                result += it.nextInt();
            }
        }
        return result;
    }

    public List<ItemAndAmount> getAmounts(Entity entity)
    {
        final Object2IntArrayMap<ItemType> map = itemsByEntity.get(entity.id);
        if ( map == null ) {
            return new ArrayList<>();
        }
        final List<ItemAndAmount> result = new ArrayList<>();
        for (var entry : map.object2IntEntrySet() )
        {
            final int amount = entry.getIntValue();
            if ( amount > 0 )
            {
                result.add(new ItemAndAmount(entry.getKey(), amount));
            }
        }
        return result;
    }

    public <T> T visitInventory(Entity entity, IInventoryVisitor<T> visitor,T defaultValue)
    {
        final Object2IntArrayMap<ItemType> map = itemsByEntity.get( entity.id );
        if ( map == null )
        {
            return defaultValue;
        }

        final IterationContext ctx = new IterationContext();

        for (var entry : map.object2IntEntrySet() )
        {
            int amount = entry.getIntValue();
            if ( amount > 0 )
            {
                visitor.visit(entry.getKey(), amount, ctx);
                if (ctx.stop)
                {
                    break;
                }
            }
        }
        return (T) ctx.result;
    }

    public List<ItemAndAmount> getAmounts(Entity entity, IInventoryPredicate predicate)
    {
        final Object2IntArrayMap<ItemType> map = itemsByEntity.get( entity.id );
        if ( map == null ) {
            return new ArrayList<>();
        }
        final List<ItemAndAmount> result = new ArrayList<>();
        for ( var entry : map.object2IntEntrySet() )
        {
            int value = entry.getIntValue();
            if ( value > 0 && predicate.test(entry.getKey(), value ) )
            {
                result.add(new ItemAndAmount(entry.getKey(), value ));
            }
        }
        return result;
    }

    private void apply(Entity location,ItemType type,int amount)
    {
        // update amountsByType
        Long2IntArrayMap byEntity = amountsByType.get(type);
        if ( byEntity == null )
        {
            byEntity = new Long2IntArrayMap();
            amountsByType.put( type, byEntity );
        }
        int newValue = byEntity.get(location.id) + amount;
        if ( newValue < 0 ) {
            throw new IllegalStateException("Error when adding "+amount+" x "+type+" to "+location+": Amount must not go negative");
        }
        byEntity.put(location.id, newValue);

        // update itemsByEntity
        Object2IntArrayMap<ItemType> byType = itemsByEntity.get(location.id);
        if ( byType == null ) {
            byType = new Object2IntArrayMap<>();
            itemsByEntity.put(location.id,byType);
        }
        newValue = byType.getInt( type ) + amount;
        if ( newValue < 0 ) {
            throw new IllegalStateException("Error when adding "+amount+" x "+type+" to "+location+": Amount must not go negative");
        }
        byType.put( type, newValue);
    }

    public void consume(Entity location,ItemType type,int amount)
    {
        if ( amount > 0 )
        {
            apply(location, type, -amount);
            stateChanged();
        } else if ( amount < 0 ) {
            throw new IllegalArgumentException("Invalid amount: "+amount);
        }
    }

    public void transfer(Entity from, ItemType type,int amount,Entity to, World world)
    {
        try
        {
            final int acceptedAmount = ((IItemReceiver) to).getAcceptedAmount(type, world);
            final int availableAmount = getAmount( from,type );
            final int toTransfer = Math.min( availableAmount , Math.min( amount, acceptedAmount ) );
            if ( toTransfer > 0 )
            {
                consume(from, type, toTransfer);
                create(to, type, toTransfer);
                stateChanged();
            }
        }
        catch(IllegalStateException e)
        {
            System.out.println("Failed to transfer "+type+"x"+amount+" from "+from+" to "+to);
            throw e;
        }
    }

    private void stateChanged() {
        if ( Main.DEBUG )
        {
            System.out.println( toString() );
        }
    }
}