package de.codesourcery.sim;

public class DropOffTask extends Task
{
    public final ItemType itemType;
    public final int amount;

    public int amountLeft;

    public DropOffTask(ItemType itemType, int amount)
    {
        this.itemType = itemType;
        this.amount = amount;
        this.amountLeft = amount;
    }
}
