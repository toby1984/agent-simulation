package de.codesourcery.sim;

public interface Task
{
    long getID();

    TaskType getType();

    default boolean hasType(TaskType t) {
        return t.equals( getType() );
    }

    boolean isAssigned();

    void setAssignee(TaskExecutor executor);

    TaskSource getSource();

    TaskExecutor getAssignee();
}
