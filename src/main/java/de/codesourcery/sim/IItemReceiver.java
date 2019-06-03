package de.codesourcery.sim;

public interface IItemReceiver
{
    /**
     *
     * @param type
     * @param amount
     * @return amount that really could be placed
     */
    int put(ItemType type,int amount);
}
