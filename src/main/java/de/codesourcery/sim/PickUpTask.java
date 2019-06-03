package de.codesourcery.sim;

public class PickUpTask extends Task
{
    public final ItemType itemType;
    public final int amount;

    public int amountLeft;

    public PickUpTask(ItemType type, int amount)
    {
        this.itemType = type;
        this.amount = amount;
        this.amountLeft = amount;
    }
}
