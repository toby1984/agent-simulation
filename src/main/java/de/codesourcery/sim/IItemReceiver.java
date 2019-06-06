package de.codesourcery.sim;

public interface IItemReceiver
{
    int getAcceptedAmount(ItemType type, World world);
}
