package de.codesourcery.sim.taskplanning;

import org.apache.commons.lang3.Validate;

public class SimpleTask implements Task
{
    public long id;
    public final TaskType type;

    public TaskSource source;
    public TaskExecutor assignee;

    public SimpleTask(TaskSource source, TaskType type)
    {
        Validate.notNull(source, "source must not be null");
        Validate.notNull(type, "type must not be null");
        this.source = source;
        this.type = type;
    }

    @Override
    public void setID(long id)
    {
        this.id = id;
    }

    @Override
    public long getID()
    {
        return id;
    }

    @Override
    public TaskType getType()
    {
        return type;
    }

    @Override
    public boolean isAssigned()
    {
        return assignee != null;
    }

    @Override
    public void setAssignee(TaskExecutor executor)
    {
        this.assignee = executor;
    }

    @Override
    public TaskSource getSource()
    {
        return source;
    }

    @Override
    public TaskExecutor getAssignee()
    {
        return assignee;
    }
}
