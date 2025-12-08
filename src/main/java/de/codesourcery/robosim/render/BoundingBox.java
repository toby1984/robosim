package de.codesourcery.robosim.render;

import org.apache.commons.lang3.Validate;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class BoundingBox
{
    public final Vector3f min;
    public final Vector3f max;

    public BoundingBox() {
        min = new Vector3f();
        max = new Vector3f();
    }

    public BoundingBox merge(BoundingBox box)
    {
        min.min( box.min );
        max.max( box.max );
        return this;
    }

    public BoundingBox(Vector3f min, Vector3f max)
    {
        Validate.notNull( min, "min must not be null" );
        Validate.notNull( max, "max must not be null" );
        this.min = min;
        this.max = max;
    }

    public static BoundingBox create(Vector3f min2, Vector3f max2) {
        final Vector3f  min = new Vector3f(min2);
        final Vector3f  max = new Vector3f(max2);
        min.x = Math.min( min2.x, max2.x );
        min.y = Math.min( min2.y, max2.y );
        min.z = Math.min( min2.z, max2.z );
        max.x = Math.max( min2.x, max2.x );
        max.y = Math.max( min2.y, max2.y );
        max.z = Math.max( min2.z, max2.z );
        return new BoundingBox(min,max);
    }

    public void getCenter(Vector3f result)
    {
        result.x = (min.x + max.x / 2);
        result.y = (min.y + max.y / 2);
        result.z = (min.z + max.z / 2);
    }

    public BoundingBox(BoundingBox other)
    {
        //noinspection IncompleteCopyConstructor
        this.min = new Vector3f(other.min);
        //noinspection IncompleteCopyConstructor
        this.max = new Vector3f(other.max);
    }

    public BoundingBox createCopy() {
        return new BoundingBox(this);
    }

    public BoundingBox transform(Matrix4f transform)
    {
        transform.transformPosition( min );
        transform.transformPosition( max );
        return this;
    }
}
