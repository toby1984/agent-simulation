package de.codesourcery.sim;

import org.apache.commons.lang3.StringUtils;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import static org.apache.commons.lang3.StringUtils.rightPad;

public class DebuggingView
{
    private final World world;

    private final Font font = new Font( "Monospaced", Font.PLAIN, 12 );

    private long lastTick;
    private long frameCounter;
    private float fps;

    public DebuggingView(World world)
    {
        this.world = world;
    }

    public void resetStatistics()
    {
        lastTick = -1;
        frameCounter = 0;
        fps = 0;
    }

    public void render(Graphics g, BufferedImage destination)
    {
        final long now = System.currentTimeMillis();
        frameCounter++;
        final Font old = g.getFont();
        try
        {
            // setup background
            g.setFont( font );
            g.setColor( Color.GRAY );
            g.fillRect( 0, 0, 300, 400 );
            final FontMetrics metrics = g.getFontMetrics();
            g.setColor( Color.WHITE );

            // print total inventory
            final List<ItemAndAmount> list = world.inventory.getTotalInventory();
            list.sort( Comparator.comparing( a -> a.type.name() ) );

            int totalItemCount = 0;
            int y = metrics.getHeight();
            for (var item : list)
            {
                totalItemCount += item.amount;
                final String typeName = rightPad( item.type.name(), 10 );
                final String value = leftPad(item.amount,4);
                g.drawString( typeName + " -> " + value, 5, y );
                y += metrics.getHeight();
            }

            // print total items #
            g.drawString( "Total items: "+leftPad(totalItemCount,5), 5, y );
            y += metrics.getHeight();

            // print FPS
            if ( lastTick != -1 )
            {
                if ( (frameCounter%30) == 0 )
                {
                    final long elapsedMillis = now - lastTick;
                    fps = (int) (1000 / (float) elapsedMillis);
                }
                g.drawString( "FPS: " + fps, 5, y );
                y += metrics.getHeight();
            }
            lastTick = now;

            // print production losses
            g.drawString( "Production losses (Output full): " +
                    world.getFactoriesProductionLossOutputFull(), 5, y );
            y += metrics.getHeight();
            g.drawString( "Production losses (Input missing): " +
                    world.getFactoriesProductionLossMissingInput(), 5, y );
            y += metrics.getHeight();

            // print controller utilization
            final int finalY = y;
            world.visitEntities(new Consumer<>()
            {
                private int y2 = finalY;

                @Override
                public void accept(Entity e)
                {
                    if ( e instanceof Controller )
                    {
                        final String s = leftPad(((Controller) e).robotCount(),3)+" robots, "+
                                             leftPad((100.0f*((Controller) e).utilization()),6)+" % busy";
                        g.drawString(s, 5, y2);
                        y2 += metrics.getHeight();
                    }
                }
            });

        } finally {
            g.setFont( old );
        }
    }

    private static String leftPad(float number, int size) {
        return StringUtils.leftPad( Float.toString( ( (int) (number*100))/100.0f ), size );
    }

    private static String leftPad(int number, int size) {
        return StringUtils.leftPad( Integer.toString( number), size );
    }
}
