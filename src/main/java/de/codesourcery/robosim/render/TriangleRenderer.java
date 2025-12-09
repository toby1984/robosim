package de.codesourcery.robosim.render;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class TriangleRenderer
{
    public void renderTriangle(Vector3f p0, Vector3f p1, Vector3f p2, int argb, RenderTarget target, ZBuffer depthBuffer)
    {
        // sort points by ascending Y coordinate
        Vector3f tmp;
        if ( p1.y < p0.y ) {
            tmp = p0;
            p0 = p1;
            p1 = tmp;
        }
        if ( p2.y < p0.y ) {
            tmp = p0;
            p0 = p2;
            p2 = tmp;
        }
        if ( p2.y < p1.y ) {
            tmp = p1;
            p1 = p2;
            p2 = tmp;
        }

        // NOTE: Since points are sorted ascending by Y coordinate,
        //       Only p0.y == p1.y or p1.y == p2.y can happen (or none of them are on the same y coordinate)
        if ( p0.y == p1.y )
        {
            processTriangle( new Line( p0, p2 ), new Line( p1, p2 ), target, argb, depthBuffer );
        }
        else if ( p1.y == p2.y )
        {
            processTriangle( new Line(p1,p0),new Line(p2,p0), target, argb, depthBuffer );
        }
        else
        {
            // process as two triangles
            final Line l = new Line(p0,p2);
            float x = (p1.y - l.b) / l.m;
            final Vector3f p4 = new Vector3f(x,p1.y,0);
            processTriangle( new Line(p0,p4),new Line(p0,p1), target, argb, depthBuffer );
            processTriangle( new Line(p4,p2),new Line(p1,p2), target, argb, depthBuffer );
        }
    }

    private void processTriangle(Line l1, Line l2, RenderTarget target, int argb, ZBuffer depthBuffer) {

        Line left,right;
        if ( l1.p0.x <= l2.p0.x ) {
            left = l1;
            right = l2;
        } else {
            left = l2;
            right = l1;
        }
        int lx;
        int rx;

        // Z values for interpolation along left line
        float leftZStart = left.p0.z;
        float leftZEnd = left.p1.z;

        // Z values for interpolation along right line
        float rightZStart = right.p0.z;
        float rightZEnd = right.p1.z;

        float lz = leftZStart, rz = rightZStart;

        // line lengths
        float lzStep = (leftZEnd-leftZStart)/left.length;
        float rzStep = (rightZEnd-rightZStart)/right.length;

        final int minY = Math.min(l1.minY(), l2.minY());
        final int maxY = Math.max(l1.maxY(), l2.maxY() );
        for ( int y = minY ; y <= maxY; y++ )
        {
            // calculate left and right X values
            lx = (int) left.calcX( y );
            rx = (int) right.calcX( y );

            float zXStep = (rz - lz) / (rx - lx);
            float currentZ = lz;
            for ( int x = lx ; x <= rx ; x++, currentZ += zXStep ) {
                final float previousZ = depthBuffer.get( x, y );
                if ( previousZ < currentZ ) {
                    depthBuffer.set( x, y, currentZ );
                    target.setPixel( x,y, argb );
                }
            }
            lz += lzStep;
            rz += rzStep;
        }
    }

    private static final class Line
    {
        private final Vector3f p0, p1;
        private float b;
        private float m;
        private float length;

        // @formatter:off
        public int minY() {return (int)Math.min(p0.y, p1.y);}
        public int maxY() {return (int)Math.max(p0.y, p1.y);}
        // @formatter:on

        public Line(Vector3f p0, Vector3f p1)
        {
            this.p0 = p0;
            this.p1 = p1;

            final float dx = p1.x - p0.x;
            final float dy = p1.y - p0.y;
            if ( dx != 0 ) {
                this.m = dy / dx;
            }
            this.b = p0.y - m * p0.x;

            float l = p0.distance( p1 );
            this.length = l;
        }

        public float calcX(float y) {
            return (y-b)/m;
        }

        public boolean isHorizontal() {
            return p0.y == p1.y;
        }

        public boolean isVertical()
        {
            return p0.x == p1.x;
        }
    }

    private static void randomize(Random r, Vector3f p0, Vector3f p1, Vector3f p2, int w, int h) {

        float cx = (p0.x+p1.x+p2.x)/3;
        float cy = (p0.y+p1.y+p2.y)/3;
        float cz = (p0.z+p1.z+p2.z)/3;

        final Matrix4f t1 = new Matrix4f().setTranslation( -cx, -cy, -cz );
        final float angle = (float) ((2 * Math.PI) / 360);
        final Matrix4f m = new Matrix4f().setRotationXYZ( 0, 0, angle*0.5f );
        final Matrix4f t2 = new Matrix4f().setTranslation( cx, cy, cz );

        // t2*m*t1
        t1.transformPosition( p0 );
        t1.transformPosition( p1 );
        t1.transformPosition( p2 );

        m.transformPosition( p0 );
        m.transformPosition( p1 );
        m.transformPosition( p2 );

        t2.transformPosition( p0 );
        t2.transformPosition( p1 );
        t2.transformPosition( p2 );
    }

    static void main() throws InterruptedException, InvocationTargetException
    {
        final Vector3f p0 = new Vector3f(150,150,0);
        final Vector3f p1 = new Vector3f(300,150,0);
        final Vector3f p2 = new Vector3f(150+75,300,0);

        SwingUtilities.invokeAndWait( () ->
        {
            final MyJPanel panel = new MyJPanel( p0, p1, p2 );
            JFrame frame = new JFrame("Triangle Renderer");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            panel.setPreferredSize( new Dimension( 640, 480 ) );
            frame.getContentPane().add( panel );
            frame.setLocationRelativeTo( null );
            frame.pack();
            frame.setVisible( true );

            final Random r = new Random(0xdeadbeefL);
            final Timer timer = new Timer(16, _ -> {
                randomize( r, p0, p1, p2, panel.getWidth(), panel.getHeight() );
                panel.repaint();
            });
            timer.start();
        } );
    }

    private static class MyJPanel extends JPanel
    {
        private final Vector3f p0;
        private final Vector3f p1;
        private final Vector3f p2;
        private BufferedImage image;
        private Graphics2D g;
        private ZBuffer zBuffer;
        private final TriangleRenderer renderer;
        private final RenderTarget target;

        public MyJPanel(Vector3f p0, Vector3f p1, Vector3f p2)
        {
            this.p0 = p0;
            this.p1 = p1;
            this.p2 = p2;
            renderer = new TriangleRenderer();
            target = new RenderTarget()
            {
                @Override
                public int width()
                {
                    return getWidth();
                }

                @Override
                public int height()
                {
                    return getHeight();
                }

                @Override
                public void setPixel(int x, int y, int argb)
                {
                    image.setRGB( x, y, argb );
                }
            };
        }

        @Override
        protected void paintComponent(Graphics gfx)
        {
            if ( zBuffer == null || zBuffer.width != getWidth() || zBuffer.height != getHeight() ) {
                zBuffer = new ZBuffer(getWidth(),getHeight());
                image =  new BufferedImage(getWidth(),getHeight(),BufferedImage.TYPE_INT_ARGB);
                if ( g != null ) {
                    g.dispose();
                }
                g = image.createGraphics();
            }
            zBuffer.clear();
            g.setColor( Color.WHITE );
            g.fillRect(0,0,getWidth(),getHeight());
            try
            {
                renderer.renderTriangle( p0, p1, p2, Color.RED.getRGB(), target, zBuffer );
            } catch(Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
            gfx.drawImage(image,0,0,getWidth(),getHeight(),null);
        }
    }
}
