package de.codesourcery.sim;

import de.codesourcery.sim.entitymanager.Building;
import de.codesourcery.sim.entitymanager.BuildingType;
import de.codesourcery.sim.taskplanning.TaskManager;

import java.util.ArrayList;
import java.util.List;

public class World
{
    private final TaskManager taskManager = new TaskManager();

    public interface IVisitor
    {
        boolean visit(Building building, float distance);
    }

    private final List<Building> buildings = new ArrayList<>();

    public void visitClosestBuildings(Vec2D location, int count, BuildingType type, IVisitor visitor)
    {
        final List<Building> candidates = new ArrayList<>(count);
        for (int i = 0, buildingsSize = buildings.size(); i < buildingsSize; i++)
        {
            Building x = buildings.get( i );
            if ( x.hasType( type ) )
            {
                candidates.add( x );
            }
        }
        candidates.sort( (a,b) -> Float.compare( a.location.dst2( location) , b.location.dst2( location ) ) );
        for ( int i = 0 , len = Math.min( candidates.size() , count ) ; i < len ; i++)
        {
            final Building building = candidates.get( i );
            final float distance = building.location.dst( location );
            if ( ! visitor.visit( building, distance ) ) {
                return;
            }
        }
    }
}
