package de.codesourcery.robosim.render;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import de.codesourcery.robosim.Utils;

public class Camera
{
    // Separate transformation matrices
    private final Matrix4f viewMatrix = new Matrix4f();
    private final Matrix4f inverseViewMatrix = new Matrix4f();
    private final Matrix4f invertedTransposedViewMatrix = new Matrix4f();
    private final Matrix4f projectionMatrix = new Matrix4f();

    // Combined matrix for rendering
    private final Matrix4f viewProjectionMatrix = new Matrix4f();

    // Frustum for visibility culling
    private final FrustumIntersection frustum = new FrustumIntersection();

    // Camera properties (can be expanded)
    private final Vector3f position = new Vector3f( 0f, 0f, 0f );
    public float pitch,yaw;

    private static final Vector3f WORLD_UP = new Vector3f( 0f, 1f, 0f );

    private final Vector3f forward = new Vector3f( 0f, 0f, -1f );
    private final Vector3f up = new Vector3f( 0f, 1f, 0f );
    private final Vector3f right = new Vector3f( 1f, 0f, 0f );

    private float fov = (float) Math.toRadians( 70.0 );
    private float aspectRatio = 16f / 9f;
    private float nearPlane = 0.1f;
    private float farPlane = 1000.0f;

    public Camera()
    {
        updateAll();
    }

    public Matrix4f getViewMatrix()
    {
        return viewMatrix;
    }

    public Matrix4f getProjectionMatrix()
    {
        return projectionMatrix;
    }

    public Matrix4f getViewProjectionMatrix()
    {
        return viewProjectionMatrix;
    }

    public FrustumIntersection getFrustum()
    {
        return frustum;
    }

    public void setPosition(float x, float y, float z)
    {
        this.position.set( x, y, z );
    }

    public void moveForward(float delta) {
        this.position.add( forward.x * delta, forward.y * delta, forward.z * delta );
        updateCameraVectors();
    }

    public void moveRight(float delta) {
        this.position.add( right.x * delta, right.y * delta, right.z * delta );
        updateCameraVectors();
    }

    public void moveUp(float delta) {
        this.position.add( up.x * delta, up.y * delta, up.z * delta );
        updateCameraVectors();
    }

    public void setYaw(float yawInRad) {
        this.yaw = yawInRad;
        updateCameraVectors();
    }

    public void setPitch(float newPitch) {
        this.pitch = (float) Utils.clamp( newPitch, 0.90*(-Math.PI / 2), 0.90*(Math.PI / 2) );
        updateCameraVectors();
    }

    public void updateAll()
    {
        updateCameraVectors(false);
        updateProjectionMatrix(true);
    }

    private void updateProjectionViewMatrix()
    {
        this.viewProjectionMatrix.set( projectionMatrix ).mul( viewMatrix );
        this.frustum.set( viewProjectionMatrix );
    }

    private void updateProjectionMatrix(boolean updatePVM)
    {
        this.projectionMatrix.setPerspective( fov, aspectRatio, nearPlane, farPlane );
        if ( updatePVM )
        {
            updateProjectionViewMatrix();
        }
    }

    public void updateViewMatrix() {
        updateViewMatrix( true );
    }

    private void updateViewMatrix(boolean updatePVM)
    {
        final Vector3f center = position.add( forward, new Vector3f() );
        this.viewMatrix.setLookAt(position,center,up);
        this.viewMatrix.invertAffine( inverseViewMatrix );
        inverseViewMatrix.transpose(invertedTransposedViewMatrix);

        if ( updatePVM )
        {
            updateProjectionViewMatrix();
        }
    }

    public Vector3f getTarget()
    {
        return forward;
    }

    public Vector3f getPosition()
    {
        return position;
    }

    public Matrix4f getInvertedTransposedViewMatrix()
    {
        return invertedTransposedViewMatrix;
    }

    private void updateCameraVectors() {
        updateCameraVectors(true);
    }

    private void updateCameraVectors(boolean updatePVM)
    {
        // Calculate the new forward vector (normalized direction the camera is looking)
        forward.x = (float) (Math.sin(yaw) * Math.cos(pitch));
        forward.y = (float) Math.sin(pitch);
        forward.z = (float) (-Math.cos(yaw) * Math.cos(pitch));
        forward.normalize();

        // Calculate the right vector: Cross product of forward and world up.
        // This must be normalized after calculation.
        forward.cross(WORLD_UP, right).normalize();

        // Calculate the up vector: Cross product of right and forward.
        // This is the camera's local "up" direction.
        right.cross( forward, up).normalize();

        updateViewMatrix(updatePVM);
    }
}
