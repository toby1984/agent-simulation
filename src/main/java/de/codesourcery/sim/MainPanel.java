package de.codesourcery.sim;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class MainPanel extends JPanel
{
    // data at mouse pointer location
    private final Vec2Di mousePositionView = new Vec2Di();
    private final Vec2D mousePositionWorld = new Vec2D();
    private Entity highlightedEntity;

    private final Vec2D cameraPosition = new Vec2D( 0, 0 );
    private final Vec2D initialViewPort = new Vec2D( 2,2 );
    private final Vec2D viewPort = new Vec2D(initialViewPort);

    private final Color highlightColor = Color.PINK;

    private float zoomFactor = 1.0f;

    private final World world;

    public boolean simulationRunning = true;

    private BufferedImage image;
    private Graphics2D graphics;

    private boolean showDebugInfo;

    private final DebuggingView debugView;

    public MainPanel(World world)
    {
        this.world = world;
        debugView = new DebuggingView( world );

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
                    case KeyEvent.VK_I:
                        showDebugInfo = ! showDebugInfo;
                        break;
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
                        final Factory factory =
                                new Factory( mousePositionWorld, flip ? ItemType.CONCRETE : ItemType.STONE );

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
            public void mouseWheelMoved(MouseWheelEvent e)
            {
                super.mouseWheelMoved( e );
                final double rotation = e.getPreciseWheelRotation();
                System.out.println("Mouse wheel rotation: "+rotation);
            }

            @Override
            public void mouseMoved(MouseEvent e)
            {
                mousePositionView.set( e.getX(), e.getY() );
                mousePositionWorld.set( viewToModel( e.getX(), e.getY() ) );
                highlightedEntity = world.getEntityAt( mousePositionWorld );

                final Entity entity = world.getEntityAt( mousePositionWorld );
                if ( entity != null )
                {
                    final StringBuilder buffer = new StringBuilder( "<HTML>" );

                    if ( entity instanceof Depot ) {
                        buffer.append( ((Depot) entity).getDebugStatus( world ).replace("\n","<BR>"));
                        buffer.append("<BR>");
                        world.inventory.visitInventory( entity, (itemType, amount, ctx) ->
                        {
                            buffer.append( itemType ).append( "x" ).append( amount ).append( "<BR>" );
                        }, null );
                    }
                    else if ( entity instanceof Controller) {
                        buffer.append( ((Controller) entity).getDebugStatus( world ).replace("\n","<BR>"));
                    }
                    else
                    {
                        buffer.append( entity.toString() ).append( "<BR>" );
                        world.inventory.visitInventory( entity, (itemType, amount, ctx) ->
                        {
                            buffer.append( itemType ).append( "x" ).append( amount ).append( "<BR>" );
                        }, null );
                    }
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
        ToolTipManager.sharedInstance().setDismissDelay( 6000 );
    }

    private final Vec2Di TMP1 = new Vec2Di();
    private final Vec2D  TMP2 = new Vec2D();

    private final Rectangle bounds = new Rectangle();

    private Vec2D viewToModel(Vec2Di input)
    {
        return viewToModel(input.x, input.y );
    }

    private Vec2D viewToModel(int vx, int vy)
    {
        TMP2.x = (vx/(float) getWidth())*viewPort.x + (cameraPosition.x - viewPort.x/2);
        TMP2.y = (vy/(float) getHeight())*viewPort.y + (cameraPosition.y - viewPort.y/2);
        return TMP2;
    }

    private Vec2Di modelToView(Vec2D v)
    {
        return modelToView(v.x, v.y);
    }

    private Vec2Di modelToView(float vx, float vy)
    {
        TMP1.x = (int) (((vx - (cameraPosition.x - viewPort.x/2))/viewPort.x) * getWidth());
        TMP1.y = (int) (((vy - (cameraPosition.y - viewPort.y/2))/viewPort.y) * getHeight());
        return TMP1;
    }

    private Rectangle getBoundingBox(Entity entity)
    {
        return getBoundingBox( entity, entity.extent );
    }

    private Rectangle getBoundingBox(Entity entity, Vec2D extent)
    {
        Vec2Di p0 = modelToView(entity.position.x - extent.x/2, entity.position.y - extent.y/2);

        int p0x = p0.x;
        int p0y = p0.y;

        final Vec2Di p1 = modelToView(entity.position.x + extent.x/2, entity.position.y + extent.y/2);

        bounds.width = p1.x - p0x;
        bounds.height = p1.y - p0y;

        bounds.x = p0x;
        bounds.y = p0y;
        return bounds;
    }

    private void setup() {

        if ( image == null || image.getWidth() != getWidth() || image.getHeight() != getHeight() ) {
            if ( graphics != null ) {
                graphics.dispose();
            }
            image = new BufferedImage( getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB );
            graphics = image.createGraphics();
        }
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        setup();
        internalPaint( graphics );
        if ( showDebugInfo ) {
            debugView.render( graphics, image );
        }
        g.drawImage( image,0,0,getWidth(),getHeight(),null);
    }

    private void internalPaint(Graphics g)
    {
        super.paintComponent( g );

        Entity src = null;
        Entity dst = null;
        if ( highlightedEntity instanceof Robot )
        {
            final Robot r = (Robot) highlightedEntity;
            if ( r.currentState instanceof Robot.TransferState)
            {
                final Robot.TransferState t = (Robot.TransferState) r.currentState;
                src = t.src;
                dst = t.dst;
            } else if ( r.currentState instanceof Robot.MoveToLocationState ) {
                dst = world.getEntityAt( ((Robot.MoveToLocationState) r.currentState).destination );
            }
        }

        final Entity finalSrc = src;
        final Entity finalDst = dst;

        world.visitEntities( entity ->
        {
            Rectangle bounds = getBoundingBox( entity );

            if ( entity instanceof Factory )
            {
                final Factory f = (Factory) entity;

                // factory -> blue
                g.setColor( Color.BLUE );
                if ( (entity == finalSrc || entity == finalDst ) || (highlightedEntity instanceof Controller && ((Controller) highlightedEntity).isInRange( f ) ) ) {
                    g.setColor( highlightColor );
                }
                g.fillRect( bounds.x, bounds.y, bounds.width, bounds.height );
                if ( f.productionLostOutputFull > 0 || f.productionLostMissingInput > 0 ) {
                    g.setColor(Color.RED);
                    g.drawRect( bounds.x, bounds.y, bounds.width, bounds.height );
                }
                g.setColor( Color.BLACK );
            }
            else if ( entity instanceof Robot )
            {
                // robot -> red circle
                final Robot r = (Robot) entity;
                g.setColor( Color.RED );
                if ( (entity == finalSrc || entity == finalDst ) || ( highlightedEntity instanceof Controller && r.controller() == highlightedEntity ) )
                {
                    g.setColor( highlightColor);
                }
                g.fillArc( bounds.x, bounds.y, bounds.width, bounds.height, 0, 360 );
                g.setColor( Color.BLACK );
            }
            else if ( entity instanceof Depot )
            {
                final Depot d = (Depot) entity;
                // depot -> black box
                g.setColor( Color.BLACK );
                if ( (entity == finalSrc || entity == finalDst ) || ( highlightedEntity instanceof Controller && ((Controller) highlightedEntity).isInRange( d ) ) ) {
                    g.setColor( highlightColor );
                }
                g.fillRect( bounds.x, bounds.y, bounds.width, bounds.height );
                if ( d.isFull(world) ) {
                    g.setColor(Color.RED);
                    g.drawRect( bounds.x, bounds.y, bounds.width, bounds.height );
                }
            }
            else if ( entity instanceof Controller )
            {
                final Controller c = (Controller) entity;
                // controller green box
                g.setColor( Color.GREEN );
                boolean doHighlight = false;
                if ( (entity == finalSrc || entity == finalDst ) || highlightedEntity == c ) {
                    doHighlight = true;
                }
                if ( highlightedEntity instanceof Depot && c.isInRange( highlightedEntity ) ) {
                    doHighlight = true;
                }
                if ( highlightedEntity instanceof Factory && c.isInRange( highlightedEntity ) ) {
                    doHighlight = true;
                }
                if ( highlightedEntity instanceof Robot && ((Robot) highlightedEntity).controller() == c )
                {
                    doHighlight = true;
                }
                if ( doHighlight )
                {
                    g.setColor( highlightColor );
                }
                g.fillRect( bounds.x, bounds.y, bounds.width, bounds.height );

                // draw broadcast range
                bounds = getBoundingBox( entity, Controller.RANGE_EXTENT );
                g.setColor( Color.GREEN );
                if ( doHighlight ) {
                    g.setColor( highlightColor );
                }
                g.drawArc( bounds.x, bounds.y, bounds.width, bounds.height, 0, 360 );
            }
        });

        if ( src != null )
        {
            final Vec2Di modelVec = modelToView(src.position);
            drawTextBox("SRC", modelVec.x , modelVec.y,g );
        }
        if ( dst != null )
        {
            final Vec2Di modelVec = modelToView(dst.position);
            drawTextBox("DST", modelVec.x , modelVec.y,g );
        }
    }

    private void drawTextBox(String msg,int x,int y,Graphics g)
    {
        final FontMetrics metrics = g.getFontMetrics();
        final Rectangle2D bounds = metrics.getStringBounds(msg, g);
        g.setColor(Color.BLACK);
        g.fillRect(x, (int) (y-bounds.getHeight()+metrics.getDescent()), (int) bounds.getWidth(), (int) bounds.getHeight() );
        g.setColor(Color.WHITE);
        g.drawString(msg,x,y);
    }

}