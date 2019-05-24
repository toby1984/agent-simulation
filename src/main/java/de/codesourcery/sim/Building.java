package de.codesourcery.sim;

public class Building implements TaskSource
{
    public final long id;
    public final Vec2D location;
    public final BuildingType type;

    public Building(long id, BuildingType type, Vec2D location)
    {
        this.id = id;
        this.location = new Vec2D(location);
        this.type = type;
    }

    public BuildingType getType()
    {
        return type;
    }

    public boolean hasType(BuildingType t) {
        return t.equals( this.type );
    }

    @Override
    public long getID()
    {
        return id;
    }

    @Override
    public Vec2D getLocation()
    {
        return location;
    }
}