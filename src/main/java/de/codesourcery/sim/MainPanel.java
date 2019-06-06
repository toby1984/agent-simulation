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
    private final Vec2Di mousePositionView = new Vec2Di();

    private final Vec2D mousePositionWorld = new Vec2D();

    private final Vec2D cameraPosition = new Vec2D( 0, 0 );
    private final Vec2D initialViewPort = new Vec2D( 2,2 );
    private final Vec2D viewPort = new Vec2D(initialViewPort);

    private float zoomFactor = 1.0f;

    private final World world;

    public boolean simulationRunning = true;

    public MainPanel(World world)
    {
        this.world = world;

        setFocusable( true );
        requestFocusInWindow();

        addKeyListener( new KeyAdapter()
        {
            private boolean flip;

            @Override
            public void keyReleased(KeyEvent e)
            {
                switch ( e.getKeyCode() )
                {
                    case KeyEvent.VK_LEFT:
                        cameraPosition.x -= 0.1f;
                        break;
                    case KeyEvent.VK_RIGHT:
                        cameraPosition.x += 0.1f;
                        break;
                    case KeyEvent.VK_UP:
                        cameraPosition.y -= 0.1f;
                        break;
                    case KeyEvent.VK_DOWN:
                        cameraPosition.y += 0.1f;
                        break;
                    case KeyEvent.VK_PLUS:
                        if ( zoomFactor > 0.1f )
                        {
                            zoomFactor -= 0.1f;
                            viewPort.set( initialViewPort ).scl( zoomFactor );
                        }
                        break;
                    case KeyEvent.VK_MINUS:
                        zoomFactor += 0.1f;
                        viewPort.set( initialViewPort ).scl( zoomFactor );
                        break;
                    case KeyEvent.VK_SPACE:
                        if ( simulationRunning )
                        {
                            System.out.println( "*** Simulation stopped. ***" );
                            simulationRunning = false;
                        }
                        else
                        {
                            System.out.println( "*** Simulation started. ***" );
                            simulationRunning = true;
                        }
                        break;
                    case KeyEvent.VK_R:  // add robot
                        final Robot robot = new Robot( mousePositionWorld );
                        System.out.println( "Adding " + robot );
                        world.add( robot );
                        break;
                    case KeyEvent.VK_F:  // add factory
                        final Factory factory = new Factory( mousePositionWorld );
                        if ( !flip )
                        {
                            factory.producedItem = ItemType.STONE;
                            factory.input1Type = ItemType.CONCRETE;
                        }
                        flip = !flip;

                        world.add( factory );
                        System.out.println( "Adding " + factory );
                        break;
                    case KeyEvent.VK_D:  // add depot
                        final Depot depot = new Depot( mousePositionWorld, ItemType.CONCRETE, ItemType.STONE );
                        System.out.println( "Adding " + depot );
                        world.inventory.create( depot, ItemType.STONE, 10 );
                        world.add( depot );
                        break;
                    case KeyEvent.VK_C:  // add controller
                        final Controller ctrl = new Controller( mousePositionWorld );
                        System.out.println( "Adding " + ctrl );
                        world.add( ctrl );
                        break;
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
                mousePositionView.set( e.getX(), e.getY() );
                mousePositionWorld.set( viewToModel( e.getX(), e.getY() ) );

                final Entity entity = world.getEntityAt( mousePositionWorld );
                if ( entity != null )
                {
                    final StringBuilder buffer = new StringBuilder( "<HTML>" );
                    buffer.append( entity.toString() ).append( "<BR>" );
                    world.inventory.visitInventory( entity, (itemType, amount, ctx) ->
                    {
                        buffer.append( itemType ).append( "x" ).append( amount ).append( "<BR>" );
                    }, null );
                    buffer.append( "</HTML>" );
                    setToolTipText( buffer.toString() );
                }
                else
                {
                    setToolTipText( null );
                }
            }
        };
        addMouseMotionListener( mouseAdapter );
        addMouseListener( mouseAdapter );
        ToolTipManager.sharedInstance().registerComponent( this );
    }

    private final Vec2Di TMP1 = new Vec2Di();
    private final Vec2D  TMP2 = new Vec2D();

    private final Rectangle bounds = new Rectangle();

    private Vec2D viewToModel(Vec2Di input)
    {
        return viewToModel(input.x, input.y );
    }

    private Vec2D viewToModel(int vx, int vy) {
        /*
        TMP1.x = ((v.x - cameraPosition.x)/viewPort.x) * getWidth();

        =>
        TMP1.x/getWidth() = (v.x - cameraPosition.x)/viewPort.x
        (TMP1.x/getWidth())*viewPort.x = v.x - cameraPosition.x
        (TMP1.x/getWidth())*viewPort.x + cameraPosition.x = v.x
         */
        TMP2.x = (vx/(float) getWidth())* viewPort.x + cameraPosition.x;
        TMP2.y = (vy/(float) getHeight())* viewPort.y + cameraPosition.y;
        return TMP2;
    }

    private Vec2Di modelToView(Vec2D v)
    {
        TMP1.x = (int) (((v.x - cameraPosition.x)/viewPort.x) * getWidth());
        TMP1.y = (int) (((v.y - cameraPosition.y)/viewPort.y) * getHeight());
        return TMP1;
    }

    private Rectangle getBoundingBox(Entity entity)
    {
        return getBoundingBox( entity, entity.extent );
    }

    private Rectangle getBoundingBox(Entity entity, Vec2D extent)
    {
        float w = getWidth() * extent.x;
        float h = getHeight() * extent.y;

        final Vec2Di xy = modelToView( entity.position );

        bounds.width = (int) w;
        bounds.height = (int) h;

        bounds.x = (int) (xy.x - (w / 2.0f));
        bounds.y = (int) (xy.y - (h / 2.0f));
        return bounds;
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent( g );

        world.visitEntities( entity ->
        {
            Rectangle bounds = getBoundingBox( entity );

            if ( entity instanceof Factory )
            {
                // factory -> blue
                g.setColor( Color.BLUE );
                g.fillRect( bounds.x, bounds.y, bounds.width, bounds.height );
                g.setColor( Color.BLACK );
            }
            else if ( entity instanceof Robot )
            {
                // robot -> red circle
                g.setColor( Color.RED );
                g.fillArc( bounds.x, bounds.y, bounds.width, bounds.height, 0, 359 );
                g.setColor( Color.BLACK );
            }
            else if ( entity instanceof Depot )
            {
                // depot -> black box
                g.setColor( Color.BLACK );
                g.fillRect( bounds.x, bounds.y, bounds.width, bounds.height );
            }
            else if ( entity instanceof Controller )
            {
                // controller green box
                g.setColor( Color.GREEN );
                g.fillRect( bounds.x, bounds.y, bounds.width, bounds.height );
                // draw broadcast range
                bounds = getBoundingBox( entity, Controller.BROADCAST_RANGE );
                g.setColor( Color.GREEN );
                g.drawArc( bounds.x, bounds.y, bounds.width, bounds.height, 0, 359 );
            }
        } );
    }
}