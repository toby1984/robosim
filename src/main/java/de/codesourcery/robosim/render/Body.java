package de.codesourcery.robosim.render;

import org.apache.commons.lang3.Validate;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Body
{
    private final Vector3f position = new Vector3f();
    private final Vector3f rotation = new Vector3f();

    // matrix that positions this object in world space
    private final Matrix4f modelMatrix = new Matrix4f();
    private final Mesh mesh;

    private final BoundingBox initialBoundingBox;
    private BoundingBox transformedBoundingBox;

    public Body(Mesh mesh) {
        Validate.notNull( mesh, "mesh must not be null" );
        this.mesh = mesh;
        this.initialBoundingBox = mesh.createBoundingBox();
    }

    public BoundingBox getBoundingBox()
    {
        if ( transformedBoundingBox == null ) {
            transformedBoundingBox = new BoundingBox( initialBoundingBox );
            transformedBoundingBox.transform( modelMatrix );
        }
        return transformedBoundingBox;
    }

    public Vector3f position() {
        return position;
    }

    public Mesh getMeshInWorldSpace()
    {
        final Mesh result = this.mesh.createCopy();
        Mesh.transform( result, modelMatrix, modelMatrix.invertAffine( new Matrix4f() ).transpose() );
        return result;
    }

    public void setPosition(float x, float y, float z)
    {
        position.set(x,y,z);
        updateMatrix();
    }

    private void updateMatrix() {
        Matrix4f rot = new  Matrix4f().rotateAffineXYZ( rotation.x,rotation.y,rotation.z );
        modelMatrix.translation( position ).mul( rot );
        transformedBoundingBox = null;
    }

    public void setRotation(Vector3f v) {
        setRotation( v.x,v.y,v.z );
    }

    public void setRotation(float x, float y, float z) {
        rotation.set(x,y,z);
        updateMatrix();
    }
}
