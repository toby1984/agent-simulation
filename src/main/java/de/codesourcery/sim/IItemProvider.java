package de.codesourcery.sim;

import java.util.Set;

public interface IItemProvider
{
    Set<ItemType> getProvidedItems(World world);

    void addController(Controller controller);
}