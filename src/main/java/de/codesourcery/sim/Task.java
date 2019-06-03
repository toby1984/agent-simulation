package de.codesourcery.sim;

import java.util.ArrayList;
import java.util.List;

public abstract class Task
{
    public enum Type
    {
        PICK_UP,
        DROP_OFF,
        MOVE_TO,
    }

    public enum ExecutionResult
    {
        // execution failed temporarily, try again with a possibly different executor
        FAILED_TEMPORARY,
        // execution failed terminally, discard task
        FAILED_TERMINAL,
        // task succeeded
        SUCCEEDED,
        PENDING;
    }

    public int id;
    public Type type;
    public Object source;
    public int priority;

    public TaskExecutor executor;
    public Task.ExecutionResult executionResult;

    public boolean hasType(Type t)
    {
        if ( t == null ) {
            throw new IllegalArgumentException( "Type must not be NULL" );
        }
        return this.type == t;
    }

    public boolean hasType(Type t1,Type t2) {
        if ( t1 == null || t2 == null ) {
            throw new IllegalArgumentException( "Type must not be NULL" );
        }
        return this.type == t1 || this.type == t2;
    }
}