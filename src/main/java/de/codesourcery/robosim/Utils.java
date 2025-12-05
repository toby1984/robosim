package de.codesourcery.robosim;

public class Utils
{
    public static double clamp(double value, double min, double max)
    {
        if  (value < min)
        {
            return min;
        }
        return Math.min( value, max );
    }
}
