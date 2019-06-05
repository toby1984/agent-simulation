package de.codesourcery.sim;

public enum ItemType
{
    CONCRETE,
    STONE,
    IRON,
    ANY;

    public boolean matches(ItemType other)
    {
        if ( this == ANY ) {
            return other != ANY;
        }
        if ( other == ANY ) {
            return true;
        }
        return this == other;
    }
}
