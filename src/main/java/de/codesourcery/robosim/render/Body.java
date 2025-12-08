package de.codesourcery.robosim.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.apache.commons.lang3.Validate;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Body
{
    private static int nextBodyId = 1;

    private class AABBCalculator implements Mesh.VertexVisitor
    {
        private float xMin=Float.MAX_VALUE, yMin=Float.MAX_VALUE, zMin=Float.MAX_VALUE;
        private float xMax=Float.MIN_VALUE, yMax=Float.MIN_VALUE, zMax=Float.MIN_VALUE;
        private final Vector3f p = new Vector3f();

        @Override
        public void visit(float x, float y, float z)
        {
            relativeMatrix.transformPosition( x, y, z, p);
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

    private final Vector3f relPosition = new Vector3f();
    private final Vector3f absolutePosition = new Vector3f();

    private final Vector3f relRotation = new Vector3f();
    private final Vector3f absoluteRotation = new Vector3f();

    // transformation matrices for this body only
    private final Matrix4f relativeMatrix = new Matrix4f();
    private final Matrix4f relativeInvertedTransposed = new Matrix4f();

    // transformation matrices including this body's local matrices and this body's parent's matrices (if any)
    private final Matrix4f absoluteMatrix = new Matrix4f();
    private final Matrix4f absoluteMatrixInvertedTransposed = new Matrix4f();

    public final int bodyId = nextBodyId++;

    private BoundingBox axisAlignedBB;

    public Color outlineColor;

    private final Mesh mesh;

    private boolean thisInstanceChanged;
    private boolean parentChanged;

    private Body parent;
    private final List<Body> children = new ArrayList<>();

    public Body(Mesh mesh) {
        Validate.isTrue( bodyId > 0 );
        Validate.notNull( mesh, "mesh must not be null" );
        this.mesh = mesh;
        this.mesh.bodyId = this.bodyId;
    }

    public void addChild(Body child)
    {
        Validate.notNull( child, "child must not be null" );
        this.children.add(child);
        child.setParent(this);
    }

    public void removeChild(Body child)
    {
        Validate.notNull( child, "child must not be null" );
        if ( ! children.remove(child) ) {
            throw new NoSuchElementException( "Child " + child + " not found" );
        }
        child.setParent(null);
    }

    public void setParent(Body parent)
    {
        this.parentChanged = parent != null && this.parent != parent;
        this.axisAlignedBB = null;
        this.parent = parent;
    }

    public void setParentChanged() {
        this.parentChanged = true;
    }

    /**
     *
     * @return this object's AABB in LOCAL space
     */
    public BoundingBox getAABB()
    {
        if ( axisAlignedBB == null ) {
            final AABBCalculator visitor = new AABBCalculator();
            mesh.visitVerticesPositionTransformed( visitor, getAbsoluteMatrix() );
            axisAlignedBB = visitor.getBoundingBox();
        }
        return axisAlignedBB;
    }

    public Body getParent()
    {
        return parent;
    }

    public boolean hasParent() {
        return parent != null;
    }

    public boolean hasNoParent() {
        return ! hasParent();
    }

    public Mesh getMeshInWorldSpace()
    {
        final Mesh result = this.mesh.createCopy();
        Mesh.transform( result, getAbsoluteMatrix(), getAbsoluteMatrixInvertedTransposed() );
        return result;
    }

    public Vector3f relativeRotation()
    {
        return relRotation;
    }

    public Vector3f absoluteRotation()
    {
        return hasParent() ? absoluteRotation : relRotation;
    }

    public Vector3f relativePosition() {
        return relPosition;
    }

    public Vector3f absolutePosition()
    {
        return hasParent() ? absolutePosition : relPosition;
    }

    public void setPosition(float x, float y, float z)
    {
        relPosition.set(x,y,z);
        thisInstanceChanged = true;
        children.forEach( Body::setParentChanged );
    }

    public void setRotation(Vector3f v) {
        setRotation( v.x,v.y,v.z );
    }

    public void setRotation(float x, float y, float z) {
        relRotation.set(x,y,z);
        thisInstanceChanged = true;
        children.forEach( Body::setParentChanged );
    }

    private Matrix4f getLocalMatrix() {
        if ( thisInstanceChanged ) {
            updateLocalMatrices();
        }
        return this.relativeMatrix;
    }

    private Matrix4f getLocalMatrixInvertedTransposed() {
        if ( thisInstanceChanged ) {
            updateLocalMatrices();
        }
        return this.relativeInvertedTransposed;
    }

    private void updateLocalMatrices() {
        relativeMatrix.translation( relPosition ).rotateAffineXYZ( relRotation.x, relRotation.y, relRotation.z );
        relativeMatrix.invertAffine( relativeInvertedTransposed ).transpose();
        // invalidate AABB
        axisAlignedBB = null;
        thisInstanceChanged = false;
    }

    public void recalculateChildren() {

        if ( parentChanged ) {
            // recalculate this instance
            parent.getAbsoluteMatrix().mul( getLocalMatrix(), this.absoluteMatrix );
            this.absoluteMatrix.invertAffine(this.absoluteMatrixInvertedTransposed ).transpose();
            this.relPosition.add(parent.absolutePosition(), this.absolutePosition);
            this.relRotation.add(parent.absoluteRotation(), this.absoluteRotation);
            this.axisAlignedBB = null;
        }

        children.forEach( Body::recalculateChildren );

        this.parentChanged = false;
    }

    public Matrix4f getAbsoluteMatrix()
    {
        return hasParent() ? absoluteMatrix : getLocalMatrix();
    }

    private Matrix4f getAbsoluteMatrixInvertedTransposed() {
        return hasParent() ? absoluteMatrixInvertedTransposed : getLocalMatrixInvertedTransposed();
    }
}