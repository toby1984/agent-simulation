package de.codesourcery.sim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KNearest<T extends IHasLocation>
{
    private final Map<Long,T> items = new HashMap<Long,T>();

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

    public void removeByID(long id)
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
