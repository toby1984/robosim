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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import org.joml.Vector2i;
import org.joml.Vector3f;
import de.codesourcery.robosim.render.Body;
import de.codesourcery.robosim.render.Camera;
import de.codesourcery.robosim.render.MeshBuilder;
import de.codesourcery.robosim.render.MeshRenderer;

public class RendererTest extends JFrame
{
    private static final float CAM_TRANSLATION = 1f;
    private static final Vector3f CAM_POSITION = new  Vector3f(0,0,100);

    public static final boolean ROTATE_BODIES = true;

    private final List<Body> topLevelBodies = new ArrayList<>();
    private final List<Body> bodiesToRender = new ArrayList<>();
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
            topLevelBodies.forEach( Body::recalculateChildren );
            renderer.render( image, gfx, bodiesToRender );
            g.drawImage( image, 0, 0, getWidth(), getHeight(), null );
            Toolkit.getDefaultToolkit().sync();
        }
    };

    private boolean camAngleChanged;
    private float newPitch, newYaw;
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
        final MouseAdapter mouseAdapter = new MouseAdapter() {

            private boolean isDragging;
            private final Vector2i dragStart = new Vector2i();
            private float initialPitch, initialYaw;

            private final int mouseButton = MouseEvent.BUTTON1;

            @Override
            public void mouseDragged(MouseEvent e)
            {
                if ( isDragging ) {
                    final int dx = e.getX() - dragStart.x;
                    final int dy = e.getY() - dragStart.y;

                    final float PITCH_DEGREES = 0.005f;
                    final float YAW_DEGREES = 0.005f;
                    newPitch = initialPitch + dy * PITCH_DEGREES;
                    newYaw = initialYaw + -dx * YAW_DEGREES;
                    camAngleChanged = true;
                }
            }

            @Override
            public void mousePressed(MouseEvent e)
            {
                if ( ! isDragging && e.getButton() == mouseButton ) {
                    isDragging = true;
                    initialPitch = cam.pitch;
                    initialYaw = cam.yaw;
                    dragStart.set(e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                if ( isDragging && e.getButton() == mouseButton )
                {
                    isDragging = false;
                }
            }
        };
        panel.addMouseListener( mouseAdapter );
        panel.addMouseMotionListener( mouseAdapter );
        getContentPane().add( panel );
        setPreferredSize( new Dimension( 640, 480 ) );
        setLocationRelativeTo(null);
        pack();
        setVisible( true );
    }

    private void handleInput() {
        if ( pressedKeys.contains( KeyEvent.VK_W ) ) { // forward
            cam.moveForward( CAM_TRANSLATION ); needsRendering = true;
        }
        else if ( pressedKeys.contains( KeyEvent.VK_S ) ) { // backward
            cam.moveForward( -CAM_TRANSLATION ); needsRendering = true;
        }

        if ( pressedKeys.contains( KeyEvent.VK_A ) ) { // left
            cam.moveRight( -CAM_TRANSLATION ); needsRendering = true;
        }
        else if ( pressedKeys.contains( KeyEvent.VK_D ) ) { // right
            cam.moveRight( CAM_TRANSLATION ); needsRendering = true;
        }

        if ( pressedKeys.contains( KeyEvent.VK_PLUS ) ) { // up
            cam.moveUp( CAM_TRANSLATION ); needsRendering = true;
        }
        else if ( pressedKeys.contains( KeyEvent.VK_MINUS ) ) { // down
            cam.moveUp( -CAM_TRANSLATION ); needsRendering = true;
        }
        if ( camAngleChanged ) {
            cam.setYaw( newYaw );
            cam.setPitch( newPitch );
            camAngleChanged = false;
            needsRendering = true;
        }
    }

    public void run() {
        final Timer timer = new Timer( 16, new ActionListener()
        {
            private final Vector3f angle = new Vector3f( 0, 0, 0 );

            private static final float ONE_DEGREE_IN_RAD = (float) (2*Math.PI)/360;
            private final Vector3f incrementsInDeg = new Vector3f(ONE_DEGREE_IN_RAD,ONE_DEGREE_IN_RAD,ONE_DEGREE_IN_RAD);

            private final Random rnd = new Random( 0xdeadbeefL );

            private int cnt = 0;
            @Override
            public void actionPerformed(ActionEvent ev)
            {
                RendererTest.this.handleInput();

                if ( ROTATE_BODIES )
                {
                    angle.x += incrementsInDeg.x;
                    angle.y += incrementsInDeg.y;
                    angle.z += incrementsInDeg.z;
                    topLevelBodies.forEach( b -> b.setRotation( angle ) );
                    if ( (cnt++ % 100) == 0 )
                    {
                        float r = rnd.nextFloat();
                        if ( r < 0.3f )
                        {
                            incrementsInDeg.x = (0.5f + rnd.nextFloat() * ONE_DEGREE_IN_RAD) / 100;
                        }
                        else if ( r < 0.7f )
                        {
                            incrementsInDeg.y = (0.5f + rnd.nextFloat() * ONE_DEGREE_IN_RAD) / 100;
                        }
                        else
                        {
                            incrementsInDeg.z = (0.5f + rnd.nextFloat() * ONE_DEGREE_IN_RAD) / 100;
                        }
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
        // final Body cube1 = new Body( MeshBuilder.createCylinder( 30, 100, 32 ) );
        // final Body cube1 = new Body( MeshBuilder.createCylinder( 100, 30, 32, Color.LIGHT_GRAY.getRGB() ) );
        // final Body cube2 = new Body( MeshBuilder.createCube( 50 ) );
        // cube2.setPosition( 50,0,-50 );

        final Body parent = new Body( MeshBuilder.createCube( 50 , Color.RED.getRGB()) );
        final Body child = new Body( MeshBuilder.createCube( 50 , Color.BLUE.getRGB() ) );
        child.outlineColor = Color.WHITE;
        child.setPosition( 60,60,60 );
        parent.addChild( child );
        parent.setPosition( 0,0,-50 );

        topLevelBodies.add( parent );
        bodiesToRender.addAll( List.of(parent,child) );
    }
}