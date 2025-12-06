package de.codesourcery.robosim;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.joml.Vector3f;
import de.codesourcery.robosim.render.Body;
import de.codesourcery.robosim.render.Camera;
import de.codesourcery.robosim.render.Mesh;
import de.codesourcery.robosim.render.MeshBuilder;
import de.codesourcery.robosim.render.MeshRenderer;

public class RendererTest extends JFrame
{
    private final Body body;
    private final Camera cam = new Camera();

    private final JPanel panel = new JPanel() {

        private BufferedImage image;
        private Graphics2D gfx;

        private final MeshRenderer renderer = new MeshRenderer( cam );

        void setupImage() {
            if ( image == null || image.getWidth() != getWidth() || image.getHeight() != getHeight() ) {
                if ( gfx != null ) {
                    gfx.dispose();
                }
                image =  new BufferedImage( getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB );
                gfx = image.createGraphics();
            }
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            setupImage();
            gfx.setColor( Color.BLACK );
            gfx.fillRect( 0, 0, getWidth(), getHeight() );
            renderer.render( image, gfx, body.getMeshInWorldSpace() );
            g.drawImage( image, 0, 0, getWidth(), getHeight(), null );
            Toolkit.getDefaultToolkit().sync();
        }
    };

    private final Set<Integer> pressedKeys = new HashSet<>();

    public RendererTest() throws HeadlessException
    {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        float x = 100;
        float y = 100;
        Vector3f p0 = new Vector3f(-x,  y, 0);
        Vector3f p1 = new Vector3f( x,  y, 0);
        Vector3f p2 = new Vector3f( x, -y, 0);
        Vector3f p3 = new Vector3f( -x, -y, 0);

        final Mesh mesh;
        // mesh = new MeshBuilder().addQuad( p0, p1, p2, p3, Color.RED.getRGB() ).build();

        // mesh = new MeshBuilder().addTriangle( p0, p1, p2, Color.RED.getRGB() ).build();
        mesh = MeshBuilder.createCube( 50 );
        body = new Body(mesh);

        cam.setPosition( 0, 0, 10 );
        cam.updateAll();

        panel.setFocusable(  true );
        panel.requestFocus();
        panel.addKeyListener( new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                pressedKeys.add( e.getKeyCode() );
            }

            @Override
            public void keyReleased(KeyEvent e)
            {
                pressedKeys.remove( e.getKeyCode() );
            }
        } );
        getContentPane().add( panel );
        setPreferredSize( new Dimension( 640, 480 ) );
        setLocationRelativeTo(null);
        pack();
        setVisible( true );
    }

    private void handleInput() {
        boolean camChanged = false;
        float inc = 1;
        if ( pressedKeys.contains( KeyEvent.VK_W ) ) {
            cam.translate( 0, 0, -inc*10 ); camChanged = true;
        } else if ( pressedKeys.contains( KeyEvent.VK_A ) ) {
            cam.translate( -inc, 0, 0 ); camChanged = true;
        } else if ( pressedKeys.contains( KeyEvent.VK_S ) ) {
            cam.translate( 0, 0, inc*10 ); camChanged = true;
        } else if ( pressedKeys.contains( KeyEvent.VK_D ) ) {
            cam.translate( inc, 0, 0 ); camChanged = true;
        } else if ( pressedKeys.contains( KeyEvent.VK_PLUS ) ) {
            cam.translate( 0, inc, 0 );
            camChanged = true;
        } else if ( pressedKeys.contains( KeyEvent.VK_MINUS ) ) {
            cam.translate( 0, -inc, 0 ); camChanged = true;
        }
        if ( camChanged ) {
            cam.updateAll();
        }
    }

    public void run() {
        final Timer timer = new Timer( 16, _ -> {
            handleInput();
            panel.repaint();
        } );
        timer.start();
    }

    static void main() throws InterruptedException, InvocationTargetException
    {
        SwingUtilities.invokeAndWait( () -> new RendererTest().run() );
    }
}
