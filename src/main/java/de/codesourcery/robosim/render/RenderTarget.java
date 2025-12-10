package de.codesourcery.robosim.render;

import org.joml.Vector3f;

public interface RenderTarget
{
    int width();
    int height();

    private static float handleNegativeZero(float x) {
        return Float.floatToIntBits(x) == 0x80000000 ? 0 : x;
    }
    default boolean contains(Vector3f p) {
        return contains( handleNegativeZero(p.x), handleNegativeZero(p.y) );
    }

    default boolean contains(float x, float y) {
        return x >= 0 && x < width() && y >= 0 && y < height();
    }

    /**
     *
     * @param p0
     * @param p1
     * @return true if line is visible, false if not
     */
    boolean clipLineAgainstViewport(Vector3f p0, Vector3f p1);

    ZBuffer zBuffer();
    void beginRender(int width, int height,int argb);
    void setPixel(int x, int y,int argb);
    void debugWrite();
    void debugRenderLine(Vector3f p0, Vector3f p1, int argb);
    void drawTriangle(Vector3f p0, Vector3f p1, Vector3f p2, int argb);
    void drawLine(int x0, int y0, int x1, int y1, int argb);
    void drawString(String s, int x, int y, int argb);
}
