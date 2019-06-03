package de.codesourcery.sim;

public class Robot extends MoveableEntity implements TaskExecutor
{
    public ItemType carriedItem;
    public int carriedAmount;
    public int maxCarryingCapacity = 10;

    public Task currentTask;

    public Robot(Vec2D v)
    {
        super( v );
    }

    public Robot(float x, float y)
    {
        super( x, y );
    }

    public int availableCarryingCapacity()
    {
        return maxCarryingCapacity - carriedAmount;
    }

    @Override
    public String toString()
    {
        final String carrying = carriedItem == null ? "<nothing>" : carriedItem.toString();
        return "Robot[ "+carrying+" ] = { pos: "+position+" , vel: "+velocity+", accel: "+acceleration+" }";
    }

    @Override
    public void schedule(Task newTask)
    {
        if ( currentTask != null )
        {
            throw new IllegalStateException( this+" is still busy" );
        }
        currentTask = newTask;
    }

    @Override
    public boolean canSchedule(Task task)
    {
        switch( task.type )
        {
            case PICK_UP:
                return carriedItem == null;
            case DROP_OFF:
                final DropOffTask t = (DropOffTask) task;
                return ( t.itemType == carriedItem );
            case MOVE_TO:
                return true;
            default:
                return false;
        }
    }

    @Override
    public float getExecutionCost(Task task)
    {
        final Vec2D dest;
        if ( task.hasType( Task.Type.MOVE_TO ) ) {
            dest = ((MoveToTask) task).destination;
        }
        else
        {
            // destination is task source position
            dest = ( (IHasPosition) task.source ).position();
        }
        return position.dst2( dest );
    }

    @Override
    public Task currentTask()
    {
        return currentTask;
    }

    private void taskFinished(World world, Task.ExecutionResult result)
    {
        world.taskManager.updateStatus( currentTask, result );
        currentTask = null;
    }

    @Override
    public void tick(float deltaSeconds,World world)
    {
        super.tick( deltaSeconds, world );

        final Task currentTask = this.currentTask;
        if ( currentTask == null ) {
            return;
        }

        // execute current task
        switch( currentTask.type )
        {
            case PICK_UP:
                PickUpTask pt = (PickUpTask) currentTask;
                if ( this.carriedItem != null && this.carriedItem != pt.itemType )
                {
                    taskFinished( world, Task.ExecutionResult.FAILED_TEMPORARY );
                    return;
                }
                int got = ((IItemProvider) currentTask.source).get( pt.itemType, pt.amountLeft );
                pt.amountLeft -= got;
                this.carriedAmount += got;
                taskFinished( world, Task.ExecutionResult.SUCCEEDED );
                break;
            case DROP_OFF:
                DropOffTask dt = (DropOffTask) currentTask;
                if ( this.carriedItem == null || this.carriedItem != dt.itemType ) {
                    taskFinished( world, Task.ExecutionResult.FAILED_TEMPORARY );
                    return;
                }
                int gave = ((IItemReceiver) currentTask.source).put( carriedItem, carriedAmount );
                dt.amountLeft -= gave;
                this.carriedAmount -= gave;
                taskFinished( world, Task.ExecutionResult.SUCCEEDED );
                break;
            case MOVE_TO:
                // TODO: Calculate movement vector & speed
                break;
        }
    }
}