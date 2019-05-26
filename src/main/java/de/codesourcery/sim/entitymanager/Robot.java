package de.codesourcery.sim.entitymanager;

import de.codesourcery.sim.Vec2D;
import de.codesourcery.sim.taskplanning.Task;
import de.codesourcery.sim.taskplanning.TaskExecutor;
import de.codesourcery.sim.taskplanning.TaskManager;

import java.util.ArrayList;
import java.util.List;

public class Robot implements TaskExecutor, IEntity
{
    private static final int MAX_CARRYING_CAPACITY = 1;

    private static final float MAX_SPEED = 10.0f; // meters/second
    private static final float MAX_CHARGE_PER_METER = 10.0f;

    private static final float MAX_BATTERY_CAPACITY = 100 * MAX_CHARGE_PER_METER; // can do 100m at full speed

    public final int id;
    public final Vec2D location;

    public ItemType carriedItem;
    public int carriedAmount;

    public int maxStorage = MAX_CARRYING_CAPACITY;

    public float maxBatteryCapacity = MAX_BATTERY_CAPACITY;
    public float batteryCapacity = MAX_BATTERY_CAPACITY;

    public Task activeTask = null;

    public final List<Task> taskQueue = new ArrayList<>();

    public Robot(int id, Vec2D location)
    {
        this.id = id;
        this.location = location;
    }

    @Override
    public int getID()
    {
        return id;
    }

    @Override
    public Vec2D getLocation()
    {
        return location;
    }

    @Override
    public void enqueue(Task task)
    {
        taskQueue.add( task );
    }

    @Override
    public void tick(TaskManager taskManager, float elapsedSeconds)
    {
        if ( activeTask == null ) {
            if ( taskQueue.isEmpty() ) {
                return;
            }
            activeTask = taskQueue.remove(0);
        }
        // TODO: Handle active task
    }

    @Override
    public boolean canDo(Task task)
    {
        switch ( task.getType() )
        {
            case MOVE_ITEM:
            case PICK_UP:
            case DROP_OFF:
            case RECHARGE:
                return true;
        }
        return false;
    }

    @Override
    public float getExecutionCost(Task task)
    {
        return task.getSource().getLocation().dst( getLocation() )*MAX_SPEED;
    }

    @Override
    public Type getType()
    {
        return Type.ROBOT;
    }
}