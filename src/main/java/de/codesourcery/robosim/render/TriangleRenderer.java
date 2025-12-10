package de.codesourcery.robosim.render;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import de.codesourcery.robosim.Utils;

public class TriangleRenderer
{
    public void renderFilledTriangle(Vector3f p0, Vector3f p1, Vector3f p2, int argb, RenderTarget target)
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
        final ZBuffer depthBuffer = target.zBuffer();

        // NOTE: Since points are sorted ascending by Y coordinate,
        //       Only p0.y == p1.y or p1.y == p2.y can happen (or none of them are on the same y coordinate)
        if ( (int) p0.y == (int) p1.y )
        {
            processTriangle( new Line( p0, p2 ), new Line( p1, p2 ), target, argb, depthBuffer );
        }
        else if ( (int) p1.y == (int) p2.y )
        {
            processTriangle( new Line(p0,p1),new Line(p0,p2), target, argb, depthBuffer );
        }
        else
        {
            // process as two triangles
            final Line l = new Line(p0,p2);
            float x;
            if ( l.isVertical() ) {
                x = l.minX();
            } else
            {
                x = (p1.y - l.b) / l.m;
            }
            final Vector3f p3 = new Vector3f(x,p1.y,0);
            processTriangle( new Line(p0,p3),new Line(p0,p1), target, argb, depthBuffer );
//            target.debugWrite();
            processTriangle( new Line(p3,p2),new Line(p1,p2), target, argb, depthBuffer );
        }
    }

    private float maxValue(float[] data, int n) {
        float m = -Float.MAX_VALUE;
        for ( int i = 0 ; i < n; i++ )
        {
            final float v = data[i];
            if ( m < v )
            {
                m = v;
            }
        }
        return m;
    }

    // this function gives the minimum
    private float minValue(float[] data, int n) {
        float m = Float.MAX_VALUE;
        for ( int i = 0 ; i < n ; i++ )
        {
            final float v = data[i];
            if ( m > v )
            {
                m = v;
            }
        }
        return m;
    }

    private final LineClipping lineClipping = new LineClipping();

    private void wwwprocessTriangle(Line l1, Line l2, RenderTarget target, int argb, ZBuffer depthBuffer) {

        // FIXME: Broken clipping check here..
        //        We need to draw a part of the triangle if AT LEAST one
        //        of the lines is visible, not only if both are visible.
        if ( ! target.clipLineAgainstViewport( l1.p0,l1.p1 ) ||
             ! target.clipLineAgainstViewport( l2.p0, l2.p1 ) )
        {
            return;
        }

        Line left, right;
        if ( l1.p0.x <= l2.p0.x ) {
            left = l1;
            right = l2;
        } else {
            left = l2;
            right = l1;
        }

//        target.debugRenderLine( left.p0, left.p1, Color.GREEN.getRGB() );
//        target.debugRenderLine( right.p0, right.p1, Color.BLUE.getRGB() );
//        target.debugWrite();

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

        final float minY = Math.min(l1.minY(), l2.minY());
        final float maxY = Math.max(l1.maxY(), l2.maxY() );

        int minX;
        int maxX;

        for ( float y = minY ; y <= maxY; y++ )
        {
            // calculate left and right X values
            minX = (int) (left.isVertical() ? left.minX() : left.calcX( y ) );
            maxX = (int) (right.isVertical() ? right.maxX() : (int) right.calcX( y ) );
            if ( minX > maxX ) {
                int tmp = minX;
                minX = maxX;
                maxX = tmp;
            }

            float zXStep = (rz - lz) / (maxX - minX);
            float currentZ = lz;
            for ( int x = minX ; x <= maxX ; x++, currentZ += zXStep ) {
                try
                {
                    final float previousZ = depthBuffer.get( x, (int) y );
                    if ( previousZ < currentZ ) {
                        depthBuffer.set( x, (int) y, currentZ );
                        target.setPixel( x, (int) y, argb );
                    }
                } catch(RuntimeException e) {
                    System.err.println("Failed to render triangle "+l1+", "+l2+" at coordinates ("+x+","+y+")");
                    throw e;
                }
            }
            lz += lzStep;
            rz += rzStep;
        }
    }

    private static final class Line
    {
        private final Vector3f p0=new Vector3f(), p1 = new Vector3f();
        private float b;
        private float m;
        private float length;

        // @formatter:off
        public float minX() {return Math.min(p0.x, p1.x);}
        public float minY() {return Math.min(p0.y, p1.y);}
        public float maxX() {return Math.max(p0.x, p1.x);}
        public float maxY() {return Math.max(p0.y, p1.y);}
        // @formatter:on


        @Override
        public String toString()
        {
            return "Line " + Utils.prettyPrint( p0 )+" -> "+Utils.prettyPrint( p1 );
        }

        public Line(Vector3f p0, Vector3f p1)
        {
            // point data MUST be copied here because
            // line clipping will write to those points
            this.p0.set(p0);
            this.p1.set(p1);

            final float dx = p1.x - p0.x;
            final float dy = p1.y - p0.y;
            if ( dx != 0 ) {
                this.m = dy / dx;
            }
            this.b = p0.y - m * p0.x;
            this.length = p0.distance( p1 );
        }

        public float calcX(float y) {
            return (y-b)/m;
        }

        public boolean isVertical()
        {
            return Math.abs(p0.x - p1.x) < 0.0001;
        }
    }

    private static float randomize(Random r, Vector3f p0, Vector3f p1, Vector3f p2, int w, int h) {

        final float angle = (float) ((2 * Math.PI) / 360);
        final float rotAngle = angle * 0.5f;
        rotate( rotAngle, p0, p1, p2 );
        return rotAngle;
    }

    private static void rotate(float angleInRad, Vector3f p0, Vector3f p1, Vector3f p2) {

        float cx = (p0.x+p1.x+p2.x)/3;
        float cy = (p0.y+p1.y+p2.y)/3;
        float cz = (p0.z+p1.z+p2.z)/3;

        final Matrix4f t1 = new Matrix4f().setTranslation( -cx, -cy, -cz );
        final Matrix4f m = new Matrix4f().setRotationXYZ( 0, 0, angleInRad );
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

    private static boolean advance=true;

    private static float angleSum;

    static void main() throws InterruptedException, InvocationTargetException
    {
        final Vector3f p0 = new Vector3f(150,150,0);
        final Vector3f p1 = new Vector3f(300,150,0);
        final Vector3f p2 = new Vector3f(150+75,300,0);
        final List<Vector3f> allPoints = List.of( p0, p1, p2 );

        angleSum = 1.9634907f;
        rotate(angleSum, p0, p1, p2  );

        SwingUtilities.invokeAndWait( () ->
        {
            final MyJPanel panel = new MyJPanel( p0, p1, p2 );
            panel.setFocusable( true );
            panel.requestFocusInWindow();
            panel.addKeyListener( new KeyAdapter()
            {
                @Override
                public void keyPressed(KeyEvent e)
                {
                    final int inc = 3;
                    switch ( e.getKeyCode() ) {
                        case KeyEvent.VK_SPACE -> advance = true;
                        case KeyEvent.VK_A -> allPoints.forEach( p -> p.x -= inc );
                        case KeyEvent.VK_D -> allPoints.forEach( p -> p.x += inc );
                        case KeyEvent.VK_W -> allPoints.forEach( p -> p.y -= inc );
                        case KeyEvent.VK_S -> allPoints.forEach( p -> p.y += inc );
                    }
                }

                @Override
                public void keyReleased(KeyEvent e)
                {
                    if ( e.getKeyCode() == KeyEvent.VK_SPACE ) {
                        advance = false;
                    }
                }
            } );
            JFrame frame = new JFrame("Triangle Renderer");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            panel.setPreferredSize( new Dimension( 640, 480 ) );
            frame.getContentPane().add( panel );
            frame.setLocationRelativeTo( null );
            frame.pack();
            frame.setVisible( true );

            final Random r = new Random(0xdeadbeefL);
            final Timer timer = new Timer(16, ev -> {
                if ( advance )
                {
                    angleSum += randomize( r, p0, p1, p2, panel.getWidth(), panel.getHeight() );
                    // advance = false;
                }
                panel.repaint();
            } );
            timer.start();
        } );
    }

    private static class MyJPanel extends JPanel
    {
        private final Vector3f p0;
        private final Vector3f p1;
        private final Vector3f p2;
        private final TriangleRenderer renderer;
        private final BufferedImageRenderTarget target;

        public MyJPanel(Vector3f p0, Vector3f p1, Vector3f p2)
        {
            this.p0 = p0;
            this.p1 = p1;
            this.p2 = p2;
            renderer = new TriangleRenderer();
            target = new BufferedImageRenderTarget( 640,480 );
        }

        @Override
        protected void paintComponent(Graphics gfx)
        {
            long start = System.nanoTime();
            target.beginRender( getWidth(), getHeight(), Color.WHITE.getRGB() );

            try
            {
                renderer.renderFilledTriangle( p0, p1, p2, Color.RED.getRGB(), target );
            } catch(Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
            gfx.drawImage(target.getImage(),0,0,getWidth(),getHeight(),null);
            long end = System.nanoTime();
            float elapsedMillis = (end-start)/1_000_000f;
            gfx.setColor( Color.RED );
            gfx.drawString("Millis: "+elapsedMillis,15,15);
        }
    }

}
