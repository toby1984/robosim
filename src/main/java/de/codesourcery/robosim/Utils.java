package de.codesourcery.robosim;

import java.text.DecimalFormat;
import org.joml.Vector3f;

public class Utils
{
    private static final DecimalFormat DF = new DecimalFormat("####.##");

    public static double clamp(double value, double min, double max)
    {
        if  (value < min)
        {
            return min;
        }
        return Math.min( value, max );
    }

    public static String prettyPrint(Vector3f v) {
        return "("+DF.format( v.x )+", "+DF.format( v.y )+", "+DF.format( v.z )+")";
    }
}
