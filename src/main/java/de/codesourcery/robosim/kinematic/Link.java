package de.codesourcery.robosim.kinematic;

import com.badlogic.gdx.math.Vector3;
import de.codesourcery.robosim.render.Body;

public final class Link implements Part
{
    /** next joint (towards gripper) */
    public Joint next;
    /** previous joint (towards base of arm) */
    public Joint previous;

    public Body body;

    public final Vector3 extent = new Vector3(0,0,0);

    // weight in KG
    public float weight;

    public final String name;

    public Link(String name, float length, float depth)
    {
        this.extent.x = depth;
        this.extent.y = length;
        this.extent.z = depth;
        this.name = name;
    }

    /* Orientation is
     *
     *    <<< X axis >>>
     *  <<<<< length >>>>>
     */
    public float length() {
        return extent.y;
    }

    public float depth() { // depth == width
        return extent.x;
    }

    public String name() {
        return name;
    }

    @Override
    public Part next()
    {
        return next;
    }

    @Override
    public void setPrevious(Part part)
    {
        this.previous = (Joint) part;
    }

    @Override
    public void setNext(Part part)
    {
        this.next = (Joint) part;
        part.setPrevious( this );
    }

    @Override
    public Part previous()
    {
        return previous;
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
