package de.codesourcery.sim;

public interface IItemProvider
{
    /**
     * Try to retrieve a specific amount of an item.
     *
     * @param type item type
     * @param amount requested amount
     * @return amount the provider was willing to give
     */
    int take(ItemType type, int amount);
}
