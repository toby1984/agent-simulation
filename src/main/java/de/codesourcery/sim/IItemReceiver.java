package de.codesourcery.sim;

public interface IItemReceiver
{
    /**
     * Offer a given item to this receiver.
     *
     * @param type item type offered
     * @param amount amount offered
     * @return amount accepted by this receiver (may be 0)
     */
    int offer(ItemType type, int amount);
}
