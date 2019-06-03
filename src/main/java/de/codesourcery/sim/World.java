package de.codesourcery.sim;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;

import java.util.function.Consumer;

public class World
{
    private final Long2ObjectArrayMap<Factory> factories = new Long2ObjectArrayMap<>();
    private final Long2ObjectArrayMap<Robot> robots = new Long2ObjectArrayMap<>();

    public final TaskManager taskManager = new TaskManager();

    public void add( Robot robot )
    {
        this.robots.put( robot.id, robot );
        taskManager.addExecutor( robot );
    }

    public void add( Factory entity)
    {
        this.factories.put( entity.id, entity );
    }

    public void tick(float deltaSeconds)
    {
        factories.values().forEach( e -> e.tick( deltaSeconds , this ) );
        robots.values().forEach( e -> e.tick(deltaSeconds, this) );
        taskManager.tick( deltaSeconds );
    }

    public void visitEntities(Consumer<Entity> consumer)
    {
        factories.values().forEach( consumer );
        robots.values().forEach( consumer );
    }
}
