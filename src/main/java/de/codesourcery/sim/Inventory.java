package de.codesourcery.sim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Inventory
{
    private Map<ItemType, Map<Long,Integer>> amountsByType = new HashMap<>();
    private Map<Long, Map<ItemType,Integer>> itemsByEntity = new HashMap<>();

    public interface IInventoryPredicate
    {
        boolean test(ItemType type,int amount);
    }

    public static abstract class IterationContext
    {
        public boolean stop;
        public Object result;

        public void stop(Object result) {
            this.result = result;
        }

        public void stop() {
            this.stop = true;
        }

        public abstract long getOwningEntityId();
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
        for (Map.Entry<ItemType, Map<Long, Integer>> entry : amountsByType.entrySet())
        {
            int sum = entry.getValue().values().stream().reduce(0, Integer::sum);
            buffer.append(entry.getKey()).append(" x ").append(sum).append("\n");
        }
        buffer.append("-------------------------------\n");
        for ( var entry : itemsByEntity.entrySet() )
        {
            final Long entityId = entry.getKey();
            buffer.append("Entity #").append(entityId).append(":\n");
            final Map<ItemType, Integer> items = entry.getValue();
            for (Map.Entry<ItemType, Integer> item : items.entrySet())
            {
                if ( item.getValue() > 0 )
                {
                    buffer.append("     ").append(item.getKey()).append(" x ").append(item.getValue()).append("\n");
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
        final Map<ItemType, Integer> map = itemsByEntity.get(entity.id);
        if ( map != null ) {
            Integer amount = map.get( type );
            return amount == null ? 0 : amount;
        }
        return 0;
    }

    public int getStoredAmount(Entity entity)
    {
        final Map<ItemType, Integer> map = itemsByEntity.get(entity.id);
        int result = 0;
        if ( map != null )
        {
            for ( Integer v : map.values() ) {
                result += v;
            }
        }
        return result;
    }

    public List<ItemAndAmount> getAmounts(Entity entity)
    {
        final Map<ItemType, Integer> map = itemsByEntity.get(entity.id);
        if ( map == null ) {
            return new ArrayList<>();
        }
        final List<ItemAndAmount> result = new ArrayList<>();
        for (Map.Entry<ItemType, Integer> entry : map.entrySet() )
        {
            if (entry.getValue() > 0 )
            {
                result.add(new ItemAndAmount(entry.getKey(), entry.getValue()));
            }
        }
        return result;
    }

    public <T> T visitInventory(Entity entity, IInventoryVisitor<T> visitor,T defaultValue)
    {
        final Map<ItemType, Integer> map = itemsByEntity.get(entity.id);
        if ( map == null )
        {
            return defaultValue;
        }

        final long id = entity.id;
        final IterationContext ctx = new IterationContext()
        {
            @Override
            public long getOwningEntityId()
            {
                return id;
            }
        };
        for (Map.Entry<ItemType, Integer> entry : map.entrySet() )
        {
            if (entry.getValue() > 0 )
            {
                visitor.visit(entry.getKey(), entry.getValue(), ctx);
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
        final Map<ItemType, Integer> map = itemsByEntity.get(entity.id);
        if ( map == null ) {
            return new ArrayList<>();
        }
        final List<ItemAndAmount> result = new ArrayList<>();
        for (Map.Entry<ItemType, Integer> entry : map.entrySet() )
        {
            if (entry.getValue() > 0 && predicate.test(entry.getKey(), entry.getValue()))
            {
                result.add(new ItemAndAmount(entry.getKey(), entry.getValue()));
            }
        }
        return result;
    }

    private void apply(Entity location,ItemType type,int amount)
    {
        // update amountsByType
        Map<Long, Integer> byEntity = amountsByType.get(type);
        if ( byEntity == null )
        {
            byEntity = new HashMap<>();
            amountsByType.put( type, byEntity );
        }
        Integer currentAmount = byEntity.get(location.id);
        int newValue = currentAmount == null ? amount : currentAmount + amount;
        if ( newValue < 0 ) {
            throw new IllegalStateException("Error when adding "+amount+" x "+type+" to "+location+": Amount must not go negative");
        }
        byEntity.put(location.id, newValue);

        // update itemsByEntity
        Map<ItemType, Integer> byType = itemsByEntity.get(location.id);
        if ( byType == null ) {
            byType = new HashMap<>();
            itemsByEntity.put(location.id,byType);
        }
        currentAmount = byType.get( type );
        newValue = currentAmount == null ? amount : currentAmount + amount;
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

    public int transfer(Entity from, ItemType type,int amount,Entity to, World world)
    {
        try
        {
            final int acceptedAmount = ((IItemReceiver) to).getAcceptedAmount(type, world);
            final int toTransfer = Math.min( amount, acceptedAmount );
            if ( toTransfer > 0 )
            {
                consume(from, type, amount);
                create(to, type, amount);
                stateChanged();
            }
            return toTransfer;
        }
        catch(IllegalStateException e)
        {
            System.out.println("Failed to transfer "+type+"x"+amount+" from "+from+" to "+to);
            throw e;
        }
    }

    private void stateChanged() {
        System.out.println( toString() );
    }
}