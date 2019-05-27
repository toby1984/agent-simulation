package de.codesourcery.sim;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;

public class Main extends JFrame
{
    public static void main(String[] args)
    {
        SwingUtilities.invokeLater( () -> new Main() );
    }

    public Main()
    {
        super("Test");

        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        setPreferredSize( new Dimension(640,480) );
        pack();
        setLocationRelativeTo( null );
        setVisible( true );
    }
}
