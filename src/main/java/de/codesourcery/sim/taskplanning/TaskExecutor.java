package de.codesourcery.sim.taskplanning;

import de.codesourcery.sim.entitymanager.IHasLocation;

public interface TaskExecutor extends IHasLocation
{
    public static final float EXECUTION_COST_IMPOSSIBLE = Float.MAX_VALUE;

    void enqueue(Task task);

    void tick(TaskManager taskManager, float elapsedSeconds);

    boolean canDo(Task task);

    float getExecutionCost(Task task);
}
