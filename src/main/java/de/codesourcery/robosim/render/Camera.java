package de.codesourcery.robosim.render;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {

    // Separate transformation matrices
    private final Matrix4f viewMatrix = new Matrix4f();
    private final Matrix4f inverseViewMatrix = new Matrix4f();
    private final Matrix4f projectionMatrix = new Matrix4f();

    // Combined matrix for rendering
    private final Matrix4f viewProjectionMatrix = new Matrix4f();

    // Frustum for visibility culling
    private final FrustumIntersection frustum = new FrustumIntersection();

    // Camera properties (can be expanded)
    private final Vector3f position = new Vector3f(0f, 0f, 0f);
    private final Vector3f target = new Vector3f(0f, 0f, -1f);
    private final Vector3f up = new Vector3f(0f, 1f, 0f);

    private float fov = (float) Math.toRadians(70.0);
    private float aspectRatio = 16f / 9f;
    private float nearPlane = 0.1f;
    private float farPlane = 500.0f;

    /**
     * Creates a new Camera instance with default settings.
     * Initializes both the view and perspective projection matrices.
     */
    public Camera() {
        updateViewMatrix();
        updateProjectionMatrix();
        updateCombinedMatrix();
    }

    // --- Core Matrix Update Methods ---

    public void updateViewMatrix() {
        //
        this.viewMatrix.setLookAt(position, target, up);
        this.viewMatrix.invertAffine(inverseViewMatrix);
    }

    /**
     * Recalculates the **perspective projection matrix**.
     * Uses JOML's `perspective` method.
     */
    private void updateProjectionMatrix() {
        //
        this.projectionMatrix.setPerspective(fov, aspectRatio, nearPlane, farPlane);
    }

    /**
     * Combines the view and projection matrices and updates the frustum.
     */
    private void updateCombinedMatrix() {
        this.viewProjectionMatrix.set(projectionMatrix).mul(viewMatrix);
        // Update the frustum intersection for culling
        this.frustum.set(viewProjectionMatrix);
    }

    // --- Accessors ---

    /**
     * @return The current view transformation matrix.
     */
    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }

    /**
     * @return The current perspective projection matrix.
     */
    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    /**
     * @return The combined view-projection matrix (P * V). This is typically used for rendering.
     */
    public Matrix4f getViewProjectionMatrix() {
        return viewProjectionMatrix;
    }

    /**
     * @return The JOML object representing the current view **frustum** for culling checks.
     */
    public FrustumIntersection getFrustum() {
        return frustum;
    }

    // --- Mutators (Example for movement) ---

    /**
     * Sets the camera's position and recalculates the view matrix.
     * @param x X-coordinate
     * @param y Y-coordinate
     * @param z Z-coordinate
     */
    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
    }

    public void translate(float x, float y, float z)
    {
        this.position.add( x, y, z );
        this.target.add( x, y, z );
    }

    public void lookAt(float x, float y, float z)
    {
        this.target.set( x, y, z );
    }

    public void updateAll() {
        updateViewMatrix();
        updateProjectionMatrix();
        updateCombinedMatrix();
    }

    public Vector3f getTarget()
    {
        return target;
    }

    public Vector3f getPosition()
    {
        return position;
    }

    public Matrix4f getInverseViewMatrix()
    {
        return inverseViewMatrix;
    }
}
