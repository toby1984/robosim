package de.codesourcery.robosim.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.joml.Vector3f;

public final class BufferedImageRenderTarget implements RenderTarget
{
    private BufferedImage image;
    private Graphics2D g;
    private ZBuffer zBuffer;
    private final LineClipping lineClipping = new  LineClipping();

    private final IntObjectHashMap<Color> colorMap = new IntObjectHashMap<>();

    public BufferedImageRenderTarget(int w, int h)
    {
        setup( w, h );
    }

    public BufferedImage getImage()
    {
        return image;
    }

    @Override
    public ZBuffer zBuffer()
    {
        return zBuffer;
    }

    @Override
    public void beginRender(int width, int height,int argb)
    {
        if ( this.image.getWidth() != width || this.image.getHeight() != height ) {
            setup( width, height );
        }
        zBuffer.clear();
        setColor( argb );
        g.fillRect( 0,0,width(), height() );
    }

    private void setup(int width, int height) {
        if ( this.g != null ) {
            this.g.dispose();
        }
        lineClipping.setViewport( width-1,height-1 );
        image =  new BufferedImage(width,height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        this.g = (Graphics2D) image.getGraphics();
        zBuffer = new ZBuffer(width,height);
    }

    @Override
    public boolean clipLineAgainstViewport(Vector3f p0, Vector3f p1)
    {
        return lineClipping.CohenSutherlandLineClip( p0, p1 );
    }

    @Override
    public int width()
    {
        return image.getWidth();
    }

    @Override
    public int height()
    {
        return image.getHeight();
    }

    private Color getColor(int argb) {
        Color result = colorMap.get( argb );
        if ( result == null ) {
            result = new Color( argb );
            colorMap.put( argb, result );
        }
        return result;
    }

    private void setColor(int argb)
    {
        g.setColor( getColor(argb) );
    }

    @Override
    public void debugRenderLine(Vector3f p0, Vector3f p1, int argc)
    {
        setColor( argc );
        g.drawLine( (int) p0.x, (int) p0.y, (int) p1.x, (int) p1.y );
    }

    @Override
    public void drawTriangle(Vector3f p0, Vector3f p1, Vector3f p2, int argb)
    {
        drawLine( (int) p0.x, (int) p0.y, (int) p1.x, (int) p1.y, argb);
        drawLine( (int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y, argb );
        drawLine( (int) p2.x, (int) p2.y, (int) p0.x, (int) p0.y, argb );
    }

    @Override
    public void drawLine(int x0, int y0, int x1, int y1, int argb)
    {
        setColor( argb );
        g.drawLine( x0, y0, x1, y1 );
    }

    @Override
    public void drawString(String s, int x, int y, int argb)
    {
        setColor( argb );
        g.drawString( s, x, y );
    }

    @Override
    public void debugWrite()
    {
        try ( FileOutputStream stream = new FileOutputStream( "/home/tobi/tmp/debug.png" ) )
        {
            ImageIO.write( image, "png", stream );
        }
        catch( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public void setPixel(int x, int y, int argb)
    {
        image.setRGB( x, y, argb );
    }
}
