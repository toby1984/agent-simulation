# Agents

This is a very crude multi-agent simulation environment.

The agents (aka 'robots') task is to deliver items between factories and depots. Each robot is assigned to exactly one controller while each factory/depot is serviced by all controllers it is within range.

Controllers have a finite communication range and robots assigned to a controller cannot move beyond its communication range. 

![screenshot](https://raw.githubusercontent.com/toby1984/sim/master/agents.gif)

# Requirements

Java >= 11, Maven 3.x

# Running

    mvn install exec:java

# Controls

The simulation can be changed/inspected using the mouse and the following keyboard shortcuts. Hovering over an entity will display a tooltip showing detail information.

 d -> add depot at mouse pointer location
 f -> add factory at mouse pointer location (factory types alternate each time)
 r -> add robot
 c -> add controller
 
Hovering over an entity will highlight related entities ; hovering over a robot will highlight it's controller and the source/destination it's transferring items between.

Hovering over a controller will show all robots controlled by it. 
