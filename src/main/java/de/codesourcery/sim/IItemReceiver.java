package de.codesourcery.sim;

import java.util.Set;

public interface IItemReceiver
{
    Set<ItemType> getAcceptedItems(World world);

    int getAcceptedAmount(ItemType type, World world);

    void addController(Controller controller);
}
