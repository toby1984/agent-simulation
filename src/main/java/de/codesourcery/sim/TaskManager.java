package de.codesourcery.sim;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskManager
{
    private final AtomicInteger IDS = new AtomicInteger();

    private final LinkedList<Task> unassigned = new LinkedList<>();

    // array from ALL Task IDs to their respective plan
    private final Int2ObjectArrayMap<TaskTreeNode> plans = new Int2ObjectArrayMap<>();

    private final List<TaskExecutor> executors = new ArrayList<>();

    public void addExecutor(TaskExecutor executor) {
        Validate.notNull( executor, "executor must not be null" );
        this.executors.add( executor );
    }

    public void removeExecutor(TaskExecutor executor)
    {
        Validate.notNull( executor, "executor must not be null" );
        this.executors.remove( executor );
        final Task currentTask = executor.currentTask();
        if ( currentTask != null )
        {
            updateStatus( currentTask, Task.ExecutionResult.FAILED_TEMPORARY );
        }
    }

    public void schedule(Task task)
    {
        task.id = IDS.incrementAndGet();
        unassigned.add( task );
    }

    public void tick(float deltaSeconds)
    {
        for ( int i = 0 , len = unassigned.size() ; i < len ; i++ )
        {
            final Task task = unassigned.get(i);
            final TaskTreeNode plan = plan( task );
            if ( plan != null ) {
                unassigned.remove(i);
                i--;
                len--;
            }
        }
    }

    public void updateStatus( Task task, Task.ExecutionResult result)
    {
        task.executionResult = result;

        final TaskTreeNode planNode = plans.get( task.id );
        final TaskTreeNode plan = planNode.root();
        if ( plan == null ) {
            throw new IllegalStateException( "Task without plan ? "+task );
        }

        switch ( result )
        {
            case FAILED_TEMPORARY:
                plans.remove( task.id );
                replan(  plan );
                return;
            case FAILED_TERMINAL:
                removePlan( plan );
                return;
            case SUCCEEDED:
                plans.remove( task.id );
                final Task.ExecutionResult planExecResult = getExecutionResult( plan );
                if ( planExecResult == Task.ExecutionResult.SUCCEEDED ) {
                    removePlan( plan );
                }
                break;
            case PENDING:
                break;
            default:
                throw new RuntimeException("Unhandled switch/case: "+result);
        }
    }

    private List<TaskExecutor> findExecutorFor(Task task)
    {
        final List<TaskExecutor> executors = new ArrayList<>();
        final List<Float> costs = new ArrayList<>();
        for ( TaskExecutor e : executors )
        {
            if ( e.canSchedule( task ) )
            {
                final float cost = e.getExecutionCost( task );
                executors.add( e );
                costs.add( cost );
            }
        }
        executors.sort( (a,b) ->
                        {
                            final int idxA = executors.indexOf( a );
                            final int idxB = executors.indexOf( b );
                            return Float.compare( costs.get(idxA), costs.get(idxB) );
                        });
        return executors;
    }

    private TaskTreeNode plan(Task task)
    {
        final TaskTreeNode result = new TaskTreeNode.TaskNode(task);

        final List<TaskExecutor> executors = findExecutorFor( task );
        if ( executors.isEmpty() )
        {
            return null;
        }

        throw new RuntimeException( "Implement me" );
//        return result;
    }

    private void replan(TaskTreeNode plan)
    {
        throw new RuntimeException( "Implement me" );
    }

    private Task.ExecutionResult getExecutionResult(TaskTreeNode node)
    {
        // task is complete if all children are complete
        if ( ! node.children.isEmpty() )
        {
            for ( TaskTreeNode child : node.children )
            {
                final Task.ExecutionResult tmp = getExecutionResult( child );
                switch( tmp )
                {
                    case FAILED_TEMPORARY:
                    case FAILED_TERMINAL:
                    case PENDING:
                        return tmp;
                    case SUCCEEDED:
                    default:
                        throw new RuntimeException( "Unhandled switch/case "+tmp );
                }
            }
            if ( ! (node instanceof TaskTreeNode.TaskNode) )
            {
                return Task.ExecutionResult.SUCCEEDED;
            }
        }

        // leaf node
        final Task task = ((TaskTreeNode.TaskNode) node).task;
        Task.ExecutionResult result = null;
        switch( task.type )
        {
            case PICK_UP:
                result = ((PickUpTask) task).amountLeft == 0 ? Task.ExecutionResult.SUCCEEDED : task.executionResult;
                break;
            case DROP_OFF:
                result = ((DropOffTask) task).amountLeft == 0 ? Task.ExecutionResult.SUCCEEDED : task.executionResult;
                break;
            case MOVE_TO:
                final MoveToTask t = (MoveToTask) task;
                result = t.isCloseEnough( ((Robot) task.executor ).position ) ? Task.ExecutionResult.SUCCEEDED : task.executionResult;
        }
        return result != null ? result : Task.ExecutionResult.PENDING;
    }

    private void removePlan(TaskTreeNode plan)
    {
        plan.visitInOrder( node ->
                           {
                               if ( node instanceof TaskTreeNode)
                               {
                                   plans.remove( ((TaskTreeNode.TaskNode) node).task.id );
                               }
                           });
    }
}