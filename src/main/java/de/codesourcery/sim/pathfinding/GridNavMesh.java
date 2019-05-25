package de.codesourcery.sim.pathfinding;

import de.codesourcery.sim.Vec2D;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

public abstract class GridNavMesh implements NavMesh
{
    public static int toNodeID(int x, int y) {
        return x << 16 | (y & 0xffff);
    }

    @Override
    public Vec2D getCoordinates(int nodeId)
    {
        return new Vec2D( extractX( nodeId ), extractY( nodeId ));
    }

    public static int extractX(int value) {
        return value >>> 16;
    }

    public static int extractY(int value) {
        return value & 0xffff;
    }

    public abstract boolean isWalkable(int x,int y);

    @Override
    public int calcH(int nodeStart, int nodeEnd)
    {
        return 2* calcG(nodeStart,nodeEnd);
    }

    @Override
    public int calcG(int nodeA, int nodeB)
    {
        final int x0 = extractX( nodeA );
        final int y0 = extractY( nodeA );
        final int x1 = extractX( nodeB );
        final int y1 = extractY( nodeB );
        final int dx = x1-x0;
        final int dy = y1-y0;
        return (dx*dx+dy*dy);
    }

    @Override
    public int getNeighbours(int nodeId,
                             IntOpenHashSet visitedNodeIds,
                             int[] result)
    {
        int neighbourCount=0;

        int x = extractX(nodeId);
        int y = extractY(nodeId);

        boolean hasLeft = x-1 >= 0;
        boolean hasRight = x+1 <= 65535;
        boolean hasTop = y-1 >= 0;
        boolean hasBottom = y+1 <= 65535;

        int dx;
        int dy;
        int externalNodeId;
        if ( hasTop )
        {
            // top-left
            dx = x-1;
            dy = y-1;
            externalNodeId = toNodeID(dx,dy);
            if (hasLeft && isWalkable(dx,dy ) && ! visitedNodeIds.contains(externalNodeId) )
            {
                result[neighbourCount++] = externalNodeId;
            }

            // top
            dx = x;
            dy = y-1;
            externalNodeId = toNodeID(dx,dy);
            if ( isWalkable(dx, dy) && ! visitedNodeIds.contains(externalNodeId))
            {
                result[neighbourCount++] = externalNodeId;
            }
            // top-right
            dx = x+1;
            dy = y-1;
            externalNodeId = toNodeID(dx,dy);
            if (hasRight && isWalkable(dx,dy) && ! visitedNodeIds.contains(externalNodeId) )
            {
                result[neighbourCount++] = externalNodeId;
            }
        }

        // left
        dx = x-1;
        dy = y;
        externalNodeId = toNodeID(dx,dy);
        if ( hasLeft && isWalkable(dx,dy ) && ! visitedNodeIds.contains(externalNodeId) )
        {
            result[neighbourCount++] = externalNodeId;
        }

        // right
        dx = x+1;
        dy = y;
        externalNodeId = toNodeID(dx,dy);
        if ( hasRight && isWalkable(dx,dy) && ! visitedNodeIds.contains(externalNodeId) )
        {
            result[neighbourCount++] = externalNodeId;
        }

        if ( hasBottom )
        {
            // bottom-left
            dx = x-1;
            dy = y+1;
            externalNodeId = toNodeID(dx,dy);
            if (hasLeft && isWalkable(dx,dy) && ! visitedNodeIds.contains(externalNodeId) )
            {
                result[neighbourCount++] = externalNodeId;
            }

            // bottom
            dx = x;
            dy = y+1;
            externalNodeId = toNodeID(dx,dy);
            if ( isWalkable(dx,dy) && ! visitedNodeIds.contains(externalNodeId))
            {
                result[neighbourCount++] = externalNodeId;
            }
            // bottom-right
            dx = x+1;
            dy = y+1;
            externalNodeId = toNodeID(dx,dy);
            if (hasRight && isWalkable(dx,dy) && ! visitedNodeIds.contains(externalNodeId))
            {
                result[neighbourCount++] = externalNodeId;
            }
        }
        return neighbourCount;
    }
}