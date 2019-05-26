package de.codesourcery.sim.entitymanager;

import de.codesourcery.sim.Vec2D;

public interface IEntity 
{
    enum Type {
        ROBOT,
        BUILDING
    };

    Type getType();

    default boolean hasType(Type t)
    {
        return t == getType();
    }

    Vec2D getLocation();
}
