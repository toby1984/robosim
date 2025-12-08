package de.codesourcery.robosim.kinematic;

import org.joml.Vector3f;
import de.codesourcery.robosim.render.Body;

public final class Link implements Part
{
    /** next joint (towards gripper) */
    public Joint next;
    /** previous joint (towards base of arm) */
    public Joint previous;

    public Body body;

    // in meters
    public float length,width,height;

    // weight in KG
    public float weight;

    public final String name;

    public Link(String name)
    {
        this.name = name;
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
