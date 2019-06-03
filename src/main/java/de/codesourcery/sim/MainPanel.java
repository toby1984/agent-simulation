package de.codesourcery.sim;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MainPanel extends JPanel
{
    private final Vec2Di mousePosition = new Vec2Di();

    private final Vec2D viewPosition = new Vec2D();

    private final World world;

    public MainPanel(World world)
    {
        this.world = world;

        setFocusable( true );
        requestFocusInWindow();

        addKeyListener( new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                if ( e.getKeyCode() == KeyEvent.VK_R ) { // add robot
                    System.out.println("Adding robot @ "+viewPosition);
                    world.add( new Robot(viewPosition) );
                } else if ( e.getKeyCode() == KeyEvent.VK_F ) { // add factory
                    System.out.println("Adding factory @ "+viewPosition);
                    world.add( new Factory(viewPosition) );
                }
            }
        } );

        final MouseAdapter mouseAdapter = new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                super.mouseClicked( e );
            }

            @Override
            public void mouseMoved(MouseEvent e)
            {
                mousePosition.set( e.getX(), e.getY() );
                viewPosition.set( e.getX() / (float) getWidth() , e.getY() / (float) getHeight() );
            }
        };
        addMouseMotionListener( mouseAdapter );
        addMouseListener( mouseAdapter );
    }

    private final Vec2Di TMP1 = new Vec2Di();

    private final Rectangle bounds = new Rectangle();

    private Vec2Di toWorldCoords(Vec2D v)
    {
        TMP1.x = (int) (v.x*getWidth());
        TMP1.y = (int) (v.y*getHeight());
        return TMP1;
    }

    private Rectangle getBoundingBox(Entity entity) {

        float w = getWidth() * entity.extent.x;
        float h = getHeight() * entity.extent.y;

        final Vec2Di xy = toWorldCoords( entity.position );

        bounds.width = (int) w;
        bounds.height = (int) h;

        bounds.x = (int) (xy.x - (w/2.0f));
        bounds.y = (int) (xy.y - (h/2.0f));
        return bounds;
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent( g );

        world.visitEntities( entity ->
        {
            final Rectangle bounds = getBoundingBox( entity );

            System.out.println("Drawing "+entity+" with bounds "+bounds);
            if ( entity instanceof Factory)
            {
                // factory -> blue
                g.setColor( Color.BLUE );
                g.drawRect( bounds.x , bounds.y ,bounds.width, bounds.height);
            } else {
                // robot -> red circle
                g.setColor( Color.RED );
                g.drawArc( bounds.x , bounds.y ,bounds.width, bounds.height, 0 , 359);
            }
        });
    }
}