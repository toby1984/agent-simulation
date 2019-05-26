package de.codesourcery.sim.entitymanager;

public final class Input
{
    public ItemType itemType;
    public int amountStored;
    public int maxStorageCapacity;
    public int amountConsumedPerCycle;

    public Input(ItemType itemType, int maxStorageCapacity, int amountConsumedPerCycle)
    {
        this.itemType = itemType;
        this.maxStorageCapacity = maxStorageCapacity;
        this.amountConsumedPerCycle = amountConsumedPerCycle;
    }

    public boolean isEmpty()
    {
        return amountStored == 0;
    }

    public boolean isEnough() {
        return amountStored >= amountConsumedPerCycle;
    }

    public boolean lacksInput() {
        return amountStored < amountConsumedPerCycle;
    }

    public boolean isFull() {
        return amountStored == maxStorageCapacity;
    }
}
