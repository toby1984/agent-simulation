package de.codesourcery.sim.pathfinding;

import de.codesourcery.sim.Vec2Di;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.HeadlessException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class AStarTest extends JFrame
{
    private static final boolean RANDOM_DATA = false;
    private static final int GRID_WIDTH = 20;
    private static final int GRID_HEIGHT = 20;

    private final boolean[][] data;

    private final GridNavMesh mesh =
            new GridNavMesh()
            {
                @Override
                public boolean isWalkable(int x, int y)
                {
                    return x >= 0 && x < GRID_WIDTH && y >= 0 && y < GRID_HEIGHT && ! data[x][y];
                }
            };

    private final MyPanel panel = new MyPanel();

    private final class MyPanel extends JPanel
    {
        private Vec2Di start; // = new Vec2Di( 4,5 );
        private Vec2Di end; // = new Vec2Di(245,246);

        private List<Vec2Di> currentPath = new ArrayList<>();
        private List<Vec2Di> visitedNodes = new ArrayList<>();

        private final AStar astar = new AStar();

        private void reset() {
            start = end = null;
        }

        private void findPath()
        {
            for ( int i = 0 ; i < 100 ; i++ )
            {
                findPath2();
            }
            repaint();
        }

        private void findPath2() {

            visitedNodes.clear();
            currentPath.clear();
            final AStar.Spy spy = (extNodeId, f, g, h, parentExtNodeId) ->
            {
                visitedNodes.add( new Vec2Di(
                        GridNavMesh.extractX( extNodeId ),
                        GridNavMesh.extractY( extNodeId )
                        ));
                repaint();
            };
            System.out.print("Searching...");
            long time = System.currentTimeMillis();
            final List<Integer> path = astar.findPath(
                    GridNavMesh.toNodeID( start.x, start.y ),
                    GridNavMesh.toNodeID( end.x, end.y ), mesh, spy);
            long time2 = System.currentTimeMillis();
            System.out.print("Done. [ "+(time2-time)+" ms ] => ");
            if ( path.isEmpty() )
            {
                System.err.println("No path");
                currentPath = Collections.emptyList();
            } else {
                System.err.println("Path has length "+path.size());
                currentPath = path.stream()
                        .map( id -> new Vec2Di( GridNavMesh.extractX( id ) , GridNavMesh.extractY( id ) ) )
                        .collect( Collectors.toList());
            }
        }

        {
            setFocusable( true );
            addKeyListener( new KeyAdapter()
            {
                @Override
                public void keyReleased(KeyEvent e)
                {
                    if ( start != null && end != null ) {
                        findPath();
                    }
                }
            });

            addMouseListener( new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    final int w = getWidth() / GRID_WIDTH;
                    final int h = getHeight() / GRID_HEIGHT;
                    final int x = e.getX() / w ;
                    final int y = e.getY() / h ;

                    if ( e.getButton() == 1 )
                    {
                        data[x][y] = ! data[x][y];
                        repaint();
                    }
                    else if ( e.getButton() == 3 ) {
                        if ( start == null ) {
                            start = new Vec2Di( x,y );
                            System.out.println("START: "+start);
                            repaint();
                        }
                        else if ( end == null )
                        {
                            end = new Vec2Di(x,y);
                            System.out.println("END: "+end);
                            findPath();
                        } else {
                            reset();
                            repaint();
                        }
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent( g );
            int w = getWidth() / GRID_WIDTH;
            int h = getHeight() / GRID_HEIGHT;

            // draw visited nodes
            g.setColor( Color.RED );
            for (Vec2Di v: visitedNodes ) {
                g.fillRect( v.x*w, v.y*h, w , h );
            }

            // draw path
            g.setColor( Color.BLUE );
            for (Vec2Di v: currentPath ) {
                g.fillRect( v.x*w, v.y*h, w , h );
            }

            // draw start & end
            if ( start != null ) {
                g.setColor( Color.WHITE );
                g.fillRect( start.x*w, start.y*h, w , h );
            }
            if ( end != null ) {
                g.setColor( Color.YELLOW );
                g.fillRect( end.x*w, end.y*h, w , h );
            }

            // draw walls
            g.setColor( Color.BLACK );
            for ( int y = 0 ; y < GRID_HEIGHT ; y++)
            {
                for (int x = 0; x < GRID_WIDTH; x++)
                {
                    if ( data[x][y] ) {
                        g.fillRect( x*w, y*h, w , h );
                    } else {
                        g.drawRect( x*w, y*h, w , h );
                    }
                }
            }
        }

    }

    public AStarTest() throws HeadlessException
    {
        data = new boolean[ GRID_WIDTH ][GRID_HEIGHT];

        if ( RANDOM_DATA ) {
            Random rnd = new Random(0xdeadbeef);
            for ( int y = 0 ; y < GRID_HEIGHT ; y++ )
            {
                for (int x = 0; x < GRID_WIDTH; x++)
                {
                    if ( rnd.nextFloat() > 0.5f ) {
                        data[x][y] = true;
                    }
                }
            }
        }
        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        getContentPane().setLayout( new BorderLayout() );
        getContentPane().add( panel );
        panel.setPreferredSize(  new Dimension(640,320) );
        pack();
        setLocationRelativeTo( null );
        setVisible( true );
    }

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater( () -> new AStarTest() );
    }
}