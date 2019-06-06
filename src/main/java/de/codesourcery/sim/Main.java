package de.codesourcery.sim;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Main extends JFrame
{
    private final World world = new World();
    private final MainPanel mainPanel;

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater( () -> new Main() );
    }

    public Main()
    {
        super("Test");

        getContentPane().setLayout( new GridBagLayout() );

        // add main panel
        mainPanel = new MainPanel(world);

        GridBagConstraints cnstrs = new GridBagConstraints();
        cnstrs.weightx=1.0;
        cnstrs.weighty=1.0;
        cnstrs.fill = GridBagConstraints.BOTH;
        getContentPane().add( mainPanel, cnstrs );

        // setup frame & tick timer
        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        setPreferredSize( new Dimension(640,480) );
        pack();
        setLocationRelativeTo( null );
        setVisible( true );

        new Timer( 16, new ActionListener()
        {
            private long lastTick = -1;

            @Override
            public void actionPerformed(ActionEvent ev)
            {
                long now = System.currentTimeMillis();
                if ( lastTick != -1 )
                {
                    final float elapsedSeconds = (now-lastTick) / 1000.0f;
                    world.tick( elapsedSeconds );
                    mainPanel.repaint();
                }
                lastTick = now;
            }
        } ).start();
    }
}
