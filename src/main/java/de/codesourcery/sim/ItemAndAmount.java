package de.codesourcery.sim;

public final class ItemAndAmount
{
    public final ItemType type;
    public int amount;

    public ItemAndAmount(ItemType type, int amount)
    {
        this.type = type;
        this.amount = amount;
    }

    public boolean hasType(ItemType t) {
        return this.type.matches( t );
    }

    @Override
    public String toString()
    {
        return type+"x"+amount;
    }
}
