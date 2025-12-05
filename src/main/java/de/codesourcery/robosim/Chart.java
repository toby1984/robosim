package de.codesourcery.robosim;

import java.awt.Color;
import java.awt.Graphics2D;

public class Chart
{
    public final Color axisColor;
    public final Color lineColor;

    private int readPtr;
    private int writePtr;
    private int size;
    private final double[] xData;
    private final double[] yData;
    private double yScaling=1;
    private int sampleEvery = 1;
    private int updateCnt = 1;

    public Chart(Color axisColor, Color lineColor, int maxPoints)
    {
        this.axisColor = axisColor;
        this.lineColor = lineColor;
        this.xData = new double[maxPoints];
        this.yData = new double[maxPoints];
    }

    public Chart sampleEvery(int rate) {
        if ( rate < 1 ) {
            throw new IllegalArgumentException("Invalid sample rate: " + rate);
        }
        this.sampleEvery = rate;
        this.updateCnt = sampleEvery;
        return this;
    }

    public Chart yScaling(double factor) {
        this.yScaling = factor;
        return this;
    }

    public void render(int x0, int y0, int x1, int y1, Graphics2D gfx) {

        // render Y axis
        gfx.setColor( axisColor );
        gfx.drawLine( x0, y0, x0, y1 );

        final int yMid = y0 + ( y1-y0 ) / 2;
        gfx.setColor( axisColor );
        gfx.drawLine( x0, yMid, x1, yMid );

        if ( size < 2 ) {
            return;
        }

        final double xMin = readX( readPtr );
        final double xMax = readX( readPtr + (size-1) );
        final double xDelta = xMax - xMin;

        final double pixelDelta = x1 - x0;
        final double xScale = pixelDelta / xDelta;

        double previousXPos = x0;
        double previousYPos = yMid - readY( readPtr ) * yScaling;

        gfx.setColor( lineColor );
        for ( int ptr = readPtr+1, len = Math.min(size-1, xData.length-1) ; len > 0 ; len--, ptr++ ) {
            double x = x0 + (readX(ptr)-xMin) * xScale;
            double y = yMid - readY(ptr) * yScaling;
            gfx.drawLine((int) previousXPos, (int) previousYPos, (int) x, (int) y);
            previousXPos = x;
            previousYPos = y;
        }
    }

    private double readX(int ptr) {
        return xData[ptr % xData.length];
    }

    private double readY(int ptr) {
        return yData[ ptr % xData.length];
    }

    public void update(double x, double y) {
        updateCnt--;
        if ( updateCnt != 0 )
        {
            return;
        }
        updateCnt = sampleEvery;

        int w = writePtr % xData.length;
        xData[w] = x;
        yData[w] = y;
        writePtr++;
        if ( writePtr >= xData.length ) {
            readPtr++;
        } else {
            size++;
        }
    }
}
