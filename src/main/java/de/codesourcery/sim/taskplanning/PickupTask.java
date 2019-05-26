package de.codesourcery.sim.taskplanning;

import de.codesourcery.sim.entitymanager.ItemType;

public class PickupTask extends SimpleTask
{
    public final ItemType itemType;
    public int amount;

    public PickupTask(TaskSource source, ItemType itemType,int amount)
    {
        super(source, TaskType.PICK_UP);
        this.itemType = itemType;
        this.amount = amount;
    }
}
