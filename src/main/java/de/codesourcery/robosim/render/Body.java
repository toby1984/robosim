package de.codesourcery.robosim.render;

import org.apache.commons.lang3.Validate;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Body
{
    private final Vector3f position = new Vector3f();

    // matrix that positions this object in world space
    public final Matrix4f modelMatrix = new Matrix4f();
    private Mesh mesh;

    public Body(Mesh mesh) {
        Validate.notNull( mesh, "mesh must not be null" );
        this.mesh = mesh;
    }

    public Vector3f position() {
        return position;
    }

    public Mesh getMeshInWorldSpace()
    {
        final Mesh result = this.mesh.createCopy();
        Mesh.transform( result, modelMatrix, modelMatrix.invertAffine(new Matrix4f() ) );
        return result;
    }

    public void setPosition(float x, float y, float z)
    {
        position.set(x,y,z);
        modelMatrix.setTranslation( x, y, z );
    }
}
