package de.codesourcery.sim;

import de.codesourcery.sim.entitymanager.IHasLocation;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.List;

public class KNearest<T extends IHasLocation>
{
    private final Int2ObjectOpenHashMap<T> items = new Int2ObjectOpenHashMap<>();

    public interface IVisitor<T>
    {
        boolean visit(T item,float distanceSquared);
    }

    public void add(T item) {
        items.put( item.getID(), item );
    }

    public void remove(T item) {
        items.remove( item.getID() );
    }

    public void removeByID(int id)
    {
        items.remove( id );
    }

    public void visitInRange(Vec2D center, float rangeSquared, IVisitor<T> visitor) {

        for ( var item : items.values() ) {
            float dist2 = item.getLocation().dst2( center );
            if ( dist2 < rangeSquared )
            {
                if ( ! visitor.visit( item, dist2 ) ) {
                    return;
                }
            }
        }
    }

    public void visitInRangeOrdered(Vec2D center, float rangeSquared, IVisitor<T> visitor)
    {
        final List<T> ordered = new ArrayList<>();

        for ( var item : items.values() ) {
            float dist2 = item.getLocation().dst2( center );
            if ( dist2 < rangeSquared )
            {
                ordered.add( item );
            }
        }

        ordered.sort( (a,b) -> Float.compare( a.getLocation().dst2( center ), b.getLocation().dst2( center ) ) );

        for ( int i = 0, orderedSize = ordered.size(); i < orderedSize; i++ )
        {
            final T item = ordered.get( i );
            if ( ! visitor.visit( item, item.getLocation().dst2( center ) ) )
            {
                break;
            }
        }

    }
}
