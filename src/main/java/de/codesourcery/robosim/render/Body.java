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

    private BoundingBox axisAlignedBB;

    public Body(Mesh mesh) {
        Validate.notNull( mesh, "mesh must not be null" );
        this.mesh = mesh;
    }

    public BoundingBox getAABB()
    {
        if ( axisAlignedBB == null ) {
            final AABBCalculator visitor = new AABBCalculator();
            mesh.visitVertices( visitor );
            axisAlignedBB = visitor.getBoundingBox();
        }
        return axisAlignedBB;
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
        // invalidate AABB
        axisAlignedBB = null;
    }

    public void setRotation(Vector3f v) {
        setRotation( v.x,v.y,v.z );
    }

    public void setRotation(float x, float y, float z) {
        rotation.set(x,y,z);
        updateMatrix();
    }

    private class AABBCalculator implements Mesh.VertexVisitor
    {
        private float xMin=Float.MAX_VALUE, yMin=Float.MAX_VALUE, zMin=Float.MAX_VALUE;
        private float xMax=Float.MIN_VALUE, yMax=Float.MIN_VALUE, zMax=Float.MIN_VALUE;
        private final Vector3f p = new Vector3f();

        @Override
        public void visit(float x, float y, float z)
        {
            modelMatrix.transformPosition( x, y, z, p);
            xMin = Math.min( xMin, p.x );
            yMin = Math.min( yMin, p.y );
            zMin = Math.min( zMin, p.z );
            xMax = Math.max( xMax, p.x );
            yMax = Math.max( yMax, p.y );
            zMax = Math.max( zMax, p.z );
        }

        public BoundingBox getBoundingBox()
        {
            return new BoundingBox( new Vector3f(xMin, yMin, zMin), new Vector3f(xMax, yMax, zMax) );
        }
    }
}
