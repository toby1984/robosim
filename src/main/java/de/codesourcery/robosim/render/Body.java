package de.codesourcery.robosim.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import org.apache.commons.lang3.Validate;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.Mesh;
import de.codesourcery.robosim.Utils;

public class Body
{
    private static int nextBodyId = 1;
    private String debugName;

    private final Vector3 relPosition = new Vector3();
    private final Vector3 absolutePosition = new Vector3();

    private final Vector3 relRotation = new Vector3();
    private final Vector3 absoluteRotation = new Vector3();

    // transformation matrices for this body only
    private final Matrix4 relativeMatrix = new Matrix4();

    // transformation matrices including this body's local matrices and this body's parent's matrices (if any)
    private final Matrix4 absoluteMatrix = new Matrix4();

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

    public Vector3 relativeRotation()
    {
        return relRotation;
    }

    public Vector3 absoluteRotation()
    {
        return hasParent() ? absoluteRotation : relRotation;
    }

    public Vector3 relativePosition() {
        return relPosition;
    }

    public Vector3 absolutePosition()
    {
        return hasParent() ? absolutePosition : relPosition;
    }

    public void setPosition(float x, float y, float z)
    {
        relPosition.set(x,y,z);
        thisInstanceChanged = true;
        children.forEach( Body::setParentChanged );
    }

    public void setRotation(Vector3 v) {
        setRotation( v.x,v.y,v.z );
    }

    public void setRotation(float x, float y, float z) {
        relRotation.set(x,y,z);
        thisInstanceChanged = true;
        children.forEach( Body::setParentChanged );
    }

    private Matrix4 getLocalMatrix() {
        if ( thisInstanceChanged ) {
            updateLocalMatrices();
        }
        return this.relativeMatrix;
    }

    private void updateLocalMatrices() {
        final Matrix4 r = Utils.setToRotation(new Matrix4(), relRotation.x, relRotation.y, relRotation.z );
        final Matrix4 t = new Matrix4().setTranslation( relPosition );
        relativeMatrix.set(t).mul(r);
        // invalidate AABB
        thisInstanceChanged = false;
        recalculateChildren(true);
    }

    public void incAngleZ(float increment) {
        setRotation( relRotation.x,  relRotation.y, relRotation.z + increment );
    }

    public void recalculateChildren() {
        recalculateChildren( false );
    }

    public void recalculateChildren(boolean force) {

        if ( parent != null && (parentChanged || force ) ) {
            // recalculate this instance
            this.absoluteMatrix.set( parent.getAbsoluteMatrix() );
            this.absoluteMatrix.mul(getLocalMatrix());

            this.absolutePosition.set( this.relPosition );
            this.absolutePosition.add( parent.absolutePosition() );

            this.absoluteRotation.set( parent.absoluteRotation() );
            this.absoluteRotation.add( this.relRotation );
        }

        children.forEach( b -> b.recalculateChildren(force) );

        this.parentChanged = false;
    }

    public Matrix4 getAbsoluteMatrix()
    {
        return hasParent() ? absoluteMatrix : getLocalMatrix();
    }

    @Override
    public String toString()
    {
        return "Body "+debugName+" (#"+bodyId+")";
    }
}