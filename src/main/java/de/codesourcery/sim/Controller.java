package de.codesourcery.sim;

import java.util.ArrayList;
import java.util.List;

public class Controller extends Entity implements ITickListener
{
    private final List<Message> inbox = new ArrayList<>();

    private final List<Robot> robots = new ArrayList<>();

    public Controller(Vec2D v)
    {
        super( v );
    }

    public Controller(float x, float y)
    {
        super( x, y );
    }

    public void broadcast(Message msg) {
        inbox.add( msg );
    }

    public void assignRobot(Robot robot) {
        robot.controller = this;
        this.robots.add(robot);
    }

    @Override
    public void tick(float deltaSeconds, World world)
    {
        if ( ! this.inbox.isEmpty() )
        {
            for (int i = 0, robotsSize = robots.size(); i < robotsSize; i++)
            {
                robots.get( i ).receive( this.inbox );
            }
            inbox.clear();
        }
    }

    public int robotCount() {
        return robots.size();
    }

    public float utilization()
    {
        int idle = 0;
        for ( Robot r : robots )
        {
            if ( r.currentState == null || r.currentState instanceof Robot.IdleState ) {
                idle++;
            }
        }
        return (robotCount()-idle) / (float) robotCount();
    }
}