package de.codesourcery.sim.entitymanager;

public final class Output
{
    public ItemType itemType;
    public int amountStored;
    public int maxStorageCapacity;
    public int amountProducedPerCycle;

    public Output(ItemType itemType, int maxStorageCapacity, int amountProducedPerCycle)
    {
        this.itemType = itemType;
        this.maxStorageCapacity = maxStorageCapacity;
        this.amountProducedPerCycle = amountProducedPerCycle;
    }

    public boolean isEmpty()
    {
        return amountStored == 0;
    }

    public boolean isFull() {
        return amountStored == maxStorageCapacity;
    }
}