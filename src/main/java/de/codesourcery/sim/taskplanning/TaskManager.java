package de.codesourcery.sim.taskplanning;

import de.codesourcery.sim.KNearest;
import de.codesourcery.sim.Vec2D;
import de.codesourcery.sim.entitymanager.IHasLocation;

import java.util.Iterator;
import java.util.LinkedHashMap;


public class TaskManager
{
    public interface ITaskVisitor
    {
        void visit(Task task,TaskSource source,TaskExecutor assignee);
    }

    public enum ProcessingStatus
    {
        WAITING_FOR_EXECUTOR,
        WAITING_FOR_RETRY,
        PROCESSING,
        FINISHED_SUCCESSFUL,
        FINISHED_ERROR,
        RETRY
    }

    private static final class TaskEntry
    {
        final long creationTick;
        final TaskSource source;
        final Task task;

        long lastStatusUpdateOnTick;
        ProcessingStatus status = ProcessingStatus.WAITING_FOR_EXECUTOR;
        long assignTick;
        TaskExecutorEntry assignee;

        private TaskEntry(TaskSource source, Task task,long creationTick)
        {
            this.creationTick = creationTick;
            this.source = source;
            this.task = task;
        }

        public boolean isAssigned() {
            return assignee != null;
        }

        public boolean isNotAssigned() {
            return assignee == null;
        }
    }

    static final class TaskExecutorEntry implements IHasLocation
    {
        public final TaskExecutor executor;
        public int queueSize;

        public TaskExecutorEntry(TaskExecutor executor)
        {
            this.executor = executor;
        }

        @Override
        public int getID()
        {
            return executor.getID();
        }

        @Override
        public Vec2D getLocation()
        {
            return executor.getLocation();
        }
    }

    private final LinkedHashMap<Long,TaskEntry> assigned = new LinkedHashMap<>();
    private final LinkedHashMap<Long,TaskEntry> unassigned = new LinkedHashMap<>();

    private final KNearest<TaskExecutorEntry> executors = new KNearest<>();

    public void register(TaskExecutor executor)
    {
        executors.add( new TaskExecutorEntry( executor ) );
    }

    public void unregister(TaskExecutor executor)
    {
        executors.removeByID( executor.getID() );
    }

    public void enqueue(TaskSource source,Task task,long tickCounter)
    {
        unassigned.put( task.getID(), new TaskEntry( source, task, tickCounter) );
    }

    public void visitTasks(TaskSource source, ITaskVisitor visitor)
    {
        assigned.values().stream().filter( x -> source.equals( x.source ) ).forEach(  x -> visitor.visit( x.task, x.source, x.assignee.executor ) );
        unassigned.values().stream().filter( x -> source.equals( x.source ) ).forEach(  x -> visitor.visit( x.task, x.source, x.assignee.executor ) );
    }

    public void delete(Task task)
    {
        if ( assigned.remove( task.getID() ) == null )
        {
            unassigned.remove( task.getID() );
        }
    }

    public void tick(float elapsedSeconds) {

        for (Iterator<TaskEntry> iterator = unassigned.values().iterator(); iterator.hasNext(); )
        {
            final TaskEntry taskEntry = iterator.next();
            if ( assignToBestExecutor( taskEntry, elapsedSeconds ) )
            {
                iterator.remove();
                assigned.put( taskEntry.task.getID(), taskEntry );
            }
        }
    }

    private TaskEntry findAssigned(Task task)
    {
        for (TaskEntry x : assigned.values() )
        {
            if ( task.equals( x.task ) ) {
                return x;
            }
        }
        return null;
    }

    public void updateTaskStatus(Task task, ProcessingStatus result,long tickCounter)
    {
        final TaskEntry entry = findAssigned( task );
        if ( entry == null ) {
            throw new IllegalArgumentException( "Failed to find task "+task );
        }

        entry.lastStatusUpdateOnTick = tickCounter;
        entry.status = ProcessingStatus.PROCESSING;

        switch( result )
        {
            case RETRY:
                entry.status = ProcessingStatus.WAITING_FOR_RETRY;
                entry.assignee = null;
                assigned.remove( task.getID() );
                unassigned.put( task.getID() , entry );
                break;
            case PROCESSING:
                break;
            case FINISHED_SUCCESSFUL:
            case FINISHED_ERROR:
                entry.assignee = null;
                assigned.remove( task.getID() );
                break;
        }
    }

    private boolean assignToBestExecutor(TaskEntry taskEntry,float radiusSquared)
    {
        // Check executors in order of distance to task originator
        // and select the one with the lowest cost

        final TaskExecutorEntry[] bestExecutorEntry = {null};
        final float[] executionCost = { TaskExecutor.EXECUTION_COST_IMPOSSIBLE };

        executors.visitInRangeOrdered( taskEntry.source.getLocation(), radiusSquared, (entry,dst2) ->
        {
            if ( entry.executor.canDo( taskEntry.task ) )
            {
                final float cost = entry.executor.getExecutionCost( taskEntry.task );
                if ( cost < executionCost[0]) {
                    executionCost[0]= cost;
                    bestExecutorEntry[0] = entry;
                }
            }
            return true;
        });

        if ( bestExecutorEntry[0] == null )
        {
            return false;
        }

        bestExecutorEntry[0].executor.enqueue( taskEntry.task );
        bestExecutorEntry[0].queueSize++;

        final long now = System.currentTimeMillis();
        taskEntry.assignTick = now;
        taskEntry.assignee = bestExecutorEntry[0];
        taskEntry.lastStatusUpdateOnTick = now;
        taskEntry.status = ProcessingStatus.PROCESSING;

        return true;
    }
}