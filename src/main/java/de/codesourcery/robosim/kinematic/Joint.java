package de.codesourcery.robosim.kinematic;

import com.badlogic.gdx.math.Vector3;
import de.codesourcery.robosim.ITickListener;
import de.codesourcery.robosim.Utils;
import de.codesourcery.robosim.motor.Motor;
import de.codesourcery.robosim.render.Body;

public final class Joint implements Part, ITickListener
{
    /** the next link (towards the gripper) */
    public Link next;
    /** the previous link (towards the base of the arm) */
    public Link previous;

    /** motor driving this join */
    public Motor motor = new Motor("base");

    // installed orientation (rotation around X/Y/Z axis)
    public final Vector3 installOrientation = new Vector3(0,0,0);

    // axis around which this join rotates
    private final Vector3 rotationAxis = new Vector3(1,0,0);

    private final Vector3 extent  = new Vector3(0,0,0);

    // rotation around x,y and z axis
    private boolean anglesLimited = false;
    public float minAngle,maxAngle;

    public Body body;
    public final String name;

    public Joint(String name, float length, float diameter)
    {
        this.extent.x = length;
        this.extent.y = diameter;
        this.extent.z = diameter;
        this.name = name;
    }

    /**
     * Returns rotation axis in WORLD space.
     * @return
     */
    public Vector3 getRotationAxis() {
        if ( installOrientation.isZero() ) {
            return rotationAxis;
        }
        return Utils.rotate( rotationAxis.cpy(), installOrientation );
    }

    @Override
    public String name()
    {
        return name;
    }

    public float diameter() {
        return extent.z;
    }

    public float length() {
        return extent.x;
    }

    @Override
    public void tick(double elapsedSeconds)
    {
        motor.tick(elapsedSeconds);
    }

    @Override
    public void setNext(Part part)
    {
        this.next = (Link) part;
        part.setPrevious( this );
    }

    @Override
    public Part next()
    {
        return next;
    }

    @Override
    public Part previous()
    {
        return previous;
    }

    @Override
    public void setPrevious(Part part)
    {
        this.previous = (Link) part;
    }

    @Override
    public Body body()
    {
        return body;
    }

    @Override
    public void setBody(Body body)
    {
        this.body = body;
    }
}
