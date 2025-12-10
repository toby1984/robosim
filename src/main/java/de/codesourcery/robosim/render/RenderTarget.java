package de.codesourcery.robosim.render;

import org.joml.Vector3f;

public interface RenderTarget
{
    int width();
    int height();
    void setPixel(int x, int y, int argb);
    void debugWrite();
    void debugRenderLine(Vector3f p0, Vector3f p1, int argb);
}
