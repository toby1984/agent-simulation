package de.codesourcery.sim;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;
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
                    final Robot robot = new Robot( viewPosition );
                    System.out.println("Adding "+robot);
                    world.add( robot );
                } else if ( e.getKeyCode() == KeyEvent.VK_F ) { // add factory
                    final Factory factory = new Factory( viewPosition );
                    world.add( factory );
                    System.out.println("Adding "+factory);
                } else if ( e.getKeyCode() == KeyEvent.VK_D ) { // add depot
                    final Depot depot = new Depot( viewPosition, ItemType.CONCRETE, ItemType.STONE );
                    System.out.println("Adding "+depot);
                    world.inventory.create(depot,ItemType.STONE,10);
                    world.add( depot );
                } else if ( e.getKeyCode() == KeyEvent.VK_C ) { // add controller
                    final Controller ctrl = new Controller( viewPosition );
                    System.out.println("Adding "+ctrl);
                    world.add( ctrl );
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

                final Entity entity = world.getEntityAt( viewPosition );
                if ( entity != null ) {
                    setToolTipText( entity.toString() );
                } else {
                    setToolTipText( null );
                }
            }
        };
        addMouseMotionListener( mouseAdapter );
        addMouseListener( mouseAdapter );
        ToolTipManager.sharedInstance().registerComponent( this );
    }

    private final Vec2Di TMP1 = new Vec2Di();

    private final Rectangle bounds = new Rectangle();

    private Vec2Di toWorldCoords(Vec2D v)
    {
        TMP1.x = (int) (v.x*getWidth());
        TMP1.y = (int) (v.y*getHeight());
        return TMP1;
    }

    private Rectangle getBoundingBox(Entity entity)
    {
        return getBoundingBox(entity,entity.extent);
    }

    private Rectangle getBoundingBox(Entity entity,Vec2D extent) {

        float w = getWidth() * extent.x;
        float h = getHeight() * extent.y;

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
            Rectangle bounds = getBoundingBox( entity );

//            System.out.println("Drawing "+entity+" with bounds "+bounds);
            if ( entity instanceof Factory)
            {
                // factory -> blue
                g.setColor( Color.BLUE );
                g.fillRect( bounds.x , bounds.y ,bounds.width, bounds.height);
                g.setColor( Color.BLACK );
                g.drawString(entity.toString(), bounds.x, bounds.y );
            }
            else if ( entity instanceof Robot )
            {
                // robot -> red circle
                g.setColor( Color.RED );
                g.fillArc( bounds.x , bounds.y ,bounds.width, bounds.height, 0 , 359);
                g.setColor( Color.BLACK );
                final Robot r = (Robot) entity;
                if ( r.isBusy() )
                {
                    String carrying = "nothing";
                    if ( r.carriedItem(world) != null ) {
                        carrying = r.carriedItem(world)+"x"+r.carriedAmount(world);
                    }
                    g.drawString("Busy ("+carrying+")", bounds.x, bounds.y );
                } else {
                    g.drawString("Idle", bounds.x, bounds.y );
                }
            } else if ( entity instanceof Depot ) {
                // depot -> black box
                g.setColor( Color.BLACK );
                g.fillRect( bounds.x , bounds.y ,bounds.width, bounds.height);
                g.setColor( Color.BLACK );
                g.drawString(entity.toString(), bounds.x, bounds.y );
            } else if ( entity instanceof Controller ) {
                // controller green box
                g.setColor( Color.GREEN );
                g.fillRect( bounds.x , bounds.y ,bounds.width, bounds.height);
                g.setColor( Color.BLACK );
                g.drawString(entity.toString(), bounds.x, bounds.y );
                // draw broadcast range
                bounds = getBoundingBox(entity, World.BROADCAST_RANGE );
                g.setColor( Color.GREEN );
                g.drawArc( bounds.x , bounds.y ,bounds.width, bounds.height, 0 , 359);
            }
        });
    }
}