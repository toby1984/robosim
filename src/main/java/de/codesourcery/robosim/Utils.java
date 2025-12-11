package de.codesourcery.robosim;

import java.text.DecimalFormat;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

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

    public static String prettyPrint(Vector3 v) {
        return "("+DF.format( v.x )+", "+DF.format( v.y )+", "+DF.format( v.z )+")";
    }

    private static final Vector3 X_AXIS = new Vector3( 0, 1, 0 );
    private static final Vector3 Y_AXIS = new Vector3( 0, 1, 0 );
    private static final Vector3 Z_AXIS = new Vector3( 0, 0, 1 );

    public static Matrix4 setToRotation(Matrix4 matrix, float x, float y, float z)
    {
        matrix.setToRotation( Z_AXIS, z );
        matrix.rotate( Y_AXIS, y);
        return matrix.rotate( X_AXIS, x);
    }
}
