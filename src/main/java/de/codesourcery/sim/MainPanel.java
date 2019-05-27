package de.codesourcery.sim;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MainPanel extends JPanel
{
    private final Vec2Di mousePosition = new Vec2Di();

    private final Vec2D viewPosition = new Vec2D();

    private final World world;

    public MainPanel(World world) {
        this.world = world;
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent( g );

        addKeyListener( new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                if ( e.getKeyCode() == KeyEvent.VK_R ) { // add robot

                } else if ( e.getKeyCode() == KeyEvent.VK_F ) { // add factory

                }
            }
        } );

        addMouseListener( new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                super.mouseClicked( e );
            }

            @Override
            public void mouseMoved(MouseEvent e)
            {
                mousePosition.set(e.getX(),e.getY());
                viewPosition.set( getWidth() / (float) e.getX(), getHeight() / (float) e.getY() );
            }
        });
    }
}
