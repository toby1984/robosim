package de.codesourcery.robosim;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.joml.Vector3f;
import de.codesourcery.robosim.render.Body;
import de.codesourcery.robosim.render.Camera;
import de.codesourcery.robosim.render.MeshBuilder;
import de.codesourcery.robosim.render.MeshRenderer;

public class RendererTest extends JFrame
{
    private static final float CAM_ROTATION = 0.1f;
    private static final float CAM_TRANSLATION = 0.9f;
    private static final Vector3f CAM_POSITION = new  Vector3f(0,0,100);

    private final List<Body> bodies=new ArrayList<>();
    private final Camera cam = new Camera();

    private boolean needsRendering = true;

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
            renderer.render( image, gfx, bodies );
            g.drawImage( image, 0, 0, getWidth(), getHeight(), null );
            Toolkit.getDefaultToolkit().sync();
        }
    };

    private final Set<Integer> pressedKeys = new HashSet<>();

    public RendererTest() throws HeadlessException
    {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setupBodies();

        cam.setPosition( CAM_POSITION.x, CAM_POSITION.y, CAM_POSITION.z );
        cam.updateViewMatrix();

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
        boolean cameraViewChanged = false;
        if ( pressedKeys.contains( KeyEvent.VK_W ) ) {
            cam.translate( 0, 0, -CAM_TRANSLATION ); cameraViewChanged = true;
        } else if ( pressedKeys.contains( KeyEvent.VK_A ) ) {
            cam.translate( -CAM_TRANSLATION, 0, 0 ); cameraViewChanged = true;
        } else if ( pressedKeys.contains( KeyEvent.VK_S ) ) {
            cam.translate( 0, 0, CAM_TRANSLATION ); cameraViewChanged = true;
        } else if ( pressedKeys.contains( KeyEvent.VK_D ) ) {
            cam.translate( CAM_TRANSLATION, 0, 0 ); cameraViewChanged = true;
        } else if ( pressedKeys.contains( KeyEvent.VK_PLUS ) ) {
            cam.translate( 0, CAM_TRANSLATION, 0 ); cameraViewChanged = true;
        } else if ( pressedKeys.contains( KeyEvent.VK_MINUS ) ) {
            cam.translate( 0, -CAM_TRANSLATION, 0 ); cameraViewChanged = true;
        } else if ( pressedKeys.contains( KeyEvent.VK_Q ) ) {
            cam.rotate( (float) ((2 * Math.PI / 360) * CAM_ROTATION) ); cameraViewChanged = true;
        } else if ( pressedKeys.contains( KeyEvent.VK_E ) ) {
            cam.rotate( (float) ((2 * Math.PI / 360) * -CAM_ROTATION) ); cameraViewChanged = true;
        }
        if ( cameraViewChanged ) {
            cam.updateViewMatrix();
            needsRendering = true;
        }
    }

    public void run() {
        final Timer timer = new Timer( 16, new ActionListener()
        {
            private final Vector3f angle = new Vector3f( 0, 0, 0 );
            private static float degToRad(float deg) {
                return (float)  Math.toRadians( deg );
            }
            private final Vector3f incrementsInDeg = new Vector3f( 1, 2, 3 );

            private final Random rnd = new Random( 0xdeadbeef );

            private int cnt = 0;
            @Override
            public void actionPerformed(ActionEvent ev)
            {
                cnt++;
                RendererTest.this.handleInput();

                angle.x += degToRad( incrementsInDeg.x );
                angle.y += degToRad( incrementsInDeg.y*1.33f );
                angle.z += degToRad( incrementsInDeg.z*1.5f );
                bodies.forEach( b -> b.setRotation( angle ) );
                if ( (cnt % 100) == 0 ) {
                    float r = rnd.nextFloat();
                    if ( r < 0.3f ) {
                        incrementsInDeg.x = 0.1f+rnd.nextFloat()*0.1f;
                    }
                    else  if ( r < 0.7f )
                    {
                        incrementsInDeg.y = 0.1f+rnd.nextFloat()*0.1f;
                    } else {
                        incrementsInDeg.z = 0.1f+rnd.nextFloat()*0.1f;
                    }
                }
                panel.repaint();
            }
        } );
        timer.start();
    }

    static void main() throws InterruptedException, InvocationTargetException
    {
        SwingUtilities.invokeAndWait( () -> new RendererTest().run() );
    }

    private void setupBodies() {
        float x = 50;
        float y = 50;
        Vector3f p0 = new Vector3f( -x/2f,  y/2f, 0);
        Vector3f p1 = new Vector3f(  x/2f,  y/2f, 0);
        Vector3f p2 = new Vector3f(  x/2f, -y/2f, 0);
        Vector3f p3 = new Vector3f( -x/2f, -y/2f, 0);

        // bodies.add( new Body( new MeshBuilder().addQuad( p0, p1, p2, p3, Color.RED.getRGB() ).build() ) );
        // bodies.add( new Body(new MeshBuilder().addTriangle( p0, p1, p2, Color.RED.getRGB() ).build()) );
        // final Body cube1 = new Body( MeshBuilder.createCube( 50 ) );
        final Body cube1 = new Body( MeshBuilder.createCylinder( 30, 100, 16 ) );
        cube1.setPosition( 0,0,-50 );
        // final Body cube2 = new Body( MeshBuilder.createCube( 50 ) );
        // cube2.setPosition( 50,0,-50 );
        bodies.addAll( List.of(cube1) );
    }
}