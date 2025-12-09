package de.codesourcery.robosim.render;

public interface RenderTarget
{
    int width();
    int height();
    void setPixel(int x, int y, int argb);
}
