package de.codesourcery.robosim.render;

import org.apache.commons.lang3.Validate;
import com.badlogic.gdx.math.Vector3;

public class BoundingBox
{
    public final Vector3 min;
    public final Vector3 max;

    public BoundingBox() {
        min = new Vector3();
        max = new Vector3();
    }

    public float getCenterZ() {
        return (min.z + max.z) / 2;
    }

    public void getCenter(Vector3 dst)
    {
        dst.set(
            (min.x + max.x) / 2,
            (min.y + max.y ) / 2,
            (min.z + max.z ) /2
        );
    }

    public BoundingBox merge(BoundingBox box)
    {
        min.set( Math.min( this.min.x, box.min.x ),
            Math.min( this.min.y, box.min.y ),
            Math.min( this.min.z, box.min.z )
        );
        max.set( Math.max( this.max.x, box.max.x ),
            Math.max( this.max.y, box.max.y ),
            Math.max( this.max.z, box.max.z )
        );
        return this;
    }

    public BoundingBox(Vector3 min, Vector3 max)
    {
        Validate.notNull( min, "min must not be null" );
        Validate.notNull( max, "max must not be null" );
        this.min = min;
        this.max = max;
    }

    public static BoundingBox create(Vector3 min2, Vector3 max2) {
        final Vector3  min = new Vector3(min2);
        final Vector3  max = new Vector3(max2);
        min.x = Math.min( min2.x, max2.x );
        min.y = Math.min( min2.y, max2.y );
        min.z = Math.min( min2.z, max2.z );
        max.x = Math.max( min2.x, max2.x );
        max.y = Math.max( min2.y, max2.y );
        max.z = Math.max( min2.z, max2.z );
        return new BoundingBox(min,max);
    }

    public BoundingBox(BoundingBox other)
    {
        //noinspection IncompleteCopyConstructor
        this.min = new Vector3(other.min);
        //noinspection IncompleteCopyConstructor
        this.max = new Vector3(other.max);
    }

    public BoundingBox createCopy() {
        return new BoundingBox(this);
    }
}
