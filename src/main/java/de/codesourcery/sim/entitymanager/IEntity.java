package de.codesourcery.sim.entitymanager;

public interface IEntity extends IHasLocation
{
    enum Type {
        ROBOT,
        BUILDING
    }

    Type getType();

    default boolean hasType(Type t)
    {
        return t == getType();
    }
}
