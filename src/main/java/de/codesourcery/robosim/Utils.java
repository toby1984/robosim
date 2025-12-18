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

    /**
     * Rotates a given vector around the X/Y/Z axis (in this order).
     *
     * @param vecToRotate vector to rotate (MUTATED in place)
     * @param angles      X/Y/Z angles to rotate by
     * @return <code>toRotate</code>
     */
    public static Vector3 rotate(Vector3 vecToRotate, Vector3 angles)
    {
        return vecToRotate.mul( createRotationMatrix(angles.x, angles.y, angles.z) );
    }


    /**
     * Create rotation matrix to rotate around the X/Y/Z axis (in this order)
     * @param v Rotation angles in degrees around X/Y/Z axis
     * @return
     */
    public static Matrix4 createRotationMatrix(Vector3 v) {
            return createRotationMatrix(v.x,v.y,v.z);
    }

    /**
     * Creates a Matrix to rotate a around the X, Y and Z axis (in that order).
     *
     * @param angleX angle in degree
     * @param angleY angle in degree
     * @param angleZ angle in degree
     * @return rotation matrix
     */
    public static Matrix4 createRotationMatrix(float angleX, float angleY, float angleZ)
    {
        final Matrix4 m1 = new Matrix4()
            .setToRotation( 0, 0, 1, angleZ );
        m1.rotate( 0, 1, 0, angleY );
        return m1.rotate( 1, 0, 0, angleX );
    }

    /**
     * Set a matrix to be a given rotation around the X,Y and Z axis.
     *
     * @param matrix
     * @param x angle around X axis in degrees
     * @param y angle around Y axis in degrees
     * @param z angle around Z axis in degrees
     * @return
     */
    public static Matrix4 setToRotation(Matrix4 matrix, float x, float y, float z)
    {
        matrix.setToRotation( Z_AXIS, z );
        matrix.rotate( Y_AXIS, y);
        return matrix.rotate( X_AXIS, x);
    }

    public static float radToDeg(float rad) {
        return (float) (rad * (180 / Math.PI));
    }

    public static float degToRad(float deg)
    {
        return (float) (deg * (Math.PI / 180));
    }
}
