package de.codesourcery.robosim.render;

import java.util.Arrays;

public class ZBuffer
{
    private final float[] data;
    public final int width,height;

    public ZBuffer(int width, int height)
    {
        this.width = width;
        this.height = height;
        this.data = new float[width*height];
        clear();
    }

    public void clear() {
        Arrays.fill(data,-Float.MAX_VALUE);
    }

    public float get(int x, int y) {
        final int idx = y * width + x;
        return data[idx];
    }

    public void set(int x, int y, float value)
    {
        final int idx = y *width + x ;
        data[idx] = value;
    }
}
