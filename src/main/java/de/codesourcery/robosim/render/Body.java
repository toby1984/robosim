package de.codesourcery.robosim.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import org.apache.commons.lang3.Validate;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import com.badlogic.gdx.graphics.Mesh;

public class Body
{
    private static int nextBodyId = 1;
    private String debugName;

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

    public Color outlineColor;

    private final Mesh mesh;

    private boolean thisInstanceChanged=true;
    private boolean parentChanged;

    private Body parent;
    private final List<Body> children = new ArrayList<>();

    public Body(Mesh mesh) {
        this(mesh,null);
    }

    public Body(Mesh mesh, String debugName) {
        this.debugName = debugName;
        Validate.isTrue( bodyId > 0 );
        Validate.notNull( mesh, "mesh must not be null" );
        this.mesh = mesh;
    }

    public Mesh getMesh()
    {
        return mesh;
    }

    /**
     * Visit this body and all children.
     * @param visitor visitor
     */
    public void visit(Consumer<Body> visitor) {
        visitor.accept( this );
        children.forEach( child -> child.visit( visitor ) );
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
        this.parent = parent;
    }

    public void setParentChanged() {
        this.parentChanged = true;
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

    private void updateLocalMatrices() {
        final Matrix4f r = new Matrix4f().setRotationXYZ( relRotation.x, relRotation.y, relRotation.z );
        final Matrix4f t = new Matrix4f().setTranslation( relPosition );
        relativeMatrix.set(t).mul(r);
        relativeMatrix.invertAffine( relativeInvertedTransposed ).transpose();
        // invalidate AABB
        thisInstanceChanged = false;
        recalculateChildren(true);
    }

    public void recalculateChildren() {
        recalculateChildren( false );
    }

    public void recalculateChildren(boolean force) {

        if ( parent != null && (parentChanged || force ) ) {
            // recalculate this instance
            parent.getAbsoluteMatrix().mul( getLocalMatrix(), this.absoluteMatrix );
            // getLocalMatrix().mul( parent.getAbsoluteMatrix(), this.absoluteMatrix );
            this.absoluteMatrix.invertAffine(this.absoluteMatrixInvertedTransposed ).transpose();
            this.relPosition.add(parent.absolutePosition(), this.absolutePosition);
            this.relRotation.add(parent.absoluteRotation(), this.absoluteRotation);
        }

        children.forEach( b -> b.recalculateChildren(force) );

        this.parentChanged = false;
    }

    public Matrix4f getAbsoluteMatrix()
    {
        return hasParent() ? absoluteMatrix : getLocalMatrix();
    }

    @Override
    public String toString()
    {
        return "Body "+debugName+" (#"+bodyId+")";
    }
}