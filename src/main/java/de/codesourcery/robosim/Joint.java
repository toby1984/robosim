package de.codesourcery.robosim;

import org.joml.Vector3d;

public final class Joint implements ArmPart, ITickListener
{
    /** the next link (towards the gripper) */
    public Link next;
    /** the previous link (towards the base of the arm) */
    public Link previous;

    /** motor driving this join */
    public Motor motor = new Motor("base");

    /** Current position of joint's center of mass in 3D space*/
    public final Vector3d position = new Vector3d();

    /**
     * Normal vector indicating the direction the next link attached to this motor
     * is facing when this part of the robot arm is in its initial position
     */
    public final Vector3d orientation = new Vector3d();

    /** axis around which this join will rotate the next link around */
    public final Vector3d rotationAxis = new Vector3d();

    /** current rotation in rad */
    public double rotationAngle;

    /** min/max join rotation angle in radian */
    public double minAngle, maxAngle;

    public double friction;

    @Override
    public void tick(double elapsedSeconds)
    {
        motor.tick(elapsedSeconds);
    }
}
