package de.codesourcery.robosim.kinematic;

import org.joml.Vector3f;
import de.codesourcery.robosim.ITickListener;
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

    // axis around which this join rotates
    public final Vector3f rotationAxis = new Vector3f(0,1,0);

    public float length, diameter;

    // rotation around x,y and z axis
    private boolean anglesLimited = false;
    public float minAngle,maxAngle;

    public Body body;
    public final String name;

    public Joint(String name)
    {
        this.name = name;
    }

    @Override
    public String name()
    {
        return name;
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
