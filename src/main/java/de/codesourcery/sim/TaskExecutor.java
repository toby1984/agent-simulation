package de.codesourcery.sim;

public interface TaskExecutor
{
    void schedule(Task task);

    boolean canSchedule(Task task);

    float getExecutionCost(Task task);

    Task currentTask();
}
