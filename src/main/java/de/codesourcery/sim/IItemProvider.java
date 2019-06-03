package de.codesourcery.sim;

public interface IItemProvider
{
    /**
     *
     * @param type
     * @param amount
     * @return amount that really was available
     */
    int get(ItemType type,int amount);
}
