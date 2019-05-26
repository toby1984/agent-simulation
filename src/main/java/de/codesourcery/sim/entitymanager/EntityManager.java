package de.codesourcery.sim.entitymanager;

import de.codesourcery.sim.motion.MotionController;
import de.codesourcery.sim.taskplanning.PickupTask;
import de.codesourcery.sim.taskplanning.SimpleTask;
import de.codesourcery.sim.taskplanning.TaskManager;
import de.codesourcery.sim.taskplanning.TaskType;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;

public class EntityManager
{
    public final TaskManager taskManager = new TaskManager();
    public final MotionController motionController = new MotionController();

    private final List<IEntity> entities = new ArrayList<>();
    private final List<IEntityTickListener> entityTickListeners = new ArrayList<>();

    private final IEntityTickListener<Factory> factoryTickListener = new IEntityTickListener<Factory>()
    {
        @Override
        public void tick(EntityManager entityManager, Factory factory, float deltaSeconds)
        {
            factory.cyclesLeft--;
            if ( factory.cyclesLeft <= 0 )
            {
                factory.cyclesLeft = factory.cyclesTillProduction;
                if ( factory.produce() )
                {
                    taskManager.enqueue( new PickupTask( factory, factory.output.itemType, factory.output.amountProducedPerCycle ) );
                }
            }
        }
    };

    private final IEntityTickListener<Robot> robotTickListener = new IEntityTickListener<Robot>()
    {
        @Override
        public void tick(EntityManager entityManager, Robot robot, float deltaSeconds)
        {

        }
    };


    public void addRobot(Robot robot) {
        add( robot, robotTickListener );
    }

    public void addFactory(Factory factory)
    {
        add( factory, factoryTickListener );
    }

    public void add(IEntity entity,IEntityTickListener tickListener)
    {
        Validate.notNull(entity, "entity must not be null");
        Validate.notNull(tickListener, "tickListener must not be null");
        this.entities.add( entity );
        this.entityTickListeners.add( tickListener );
    }

    public void tick(float deltaSeconds)
    {
        // tick entities
        for (int i = 0, entitiesSize = entities.size(); i < entitiesSize; i++)
        {
            final IEntity entity = entities.get(i);
            final IEntityTickListener listener = entityTickListeners.get(i);
            listener.tick(this, entity, deltaSeconds );
        }

        motionController.tick(deltaSeconds);
        taskManager.tick( deltaSeconds );
    }
}