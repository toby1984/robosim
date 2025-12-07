package de.codesourcery.robosim.render;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

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
    private float rotationInRadians = 0f;
    private final Vector3f position = new Vector3f( 0f, 0f, 0f );
    private final Vector3f target = new Vector3f( 0f, 0f, -1f );
    private final Vector3f up = new Vector3f( 0f, 1f, 0f );

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

    public void translate(float x, float y, float z)
    {
        this.position.add( x, y, z );
        this.target.add( x, y, z );
    }

    public void rotate(float angleInRad)
    {
        this.rotationInRadians += angleInRad;
    }

    public void lookAt(float x, float y, float z)
    {
        this.target.set( x, y, z );
    }

    public void updateAll()
    {
        updateViewMatrix(false);
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
        this.viewMatrix.setLookAt( position, target, up );

        if ( rotationInRadians != 0 )
        {
            final Matrix4f rotationMatrix = new Matrix4f().rotateY( rotationInRadians );
            viewMatrix.set( rotationMatrix.mul( viewMatrix ) );
        }

        this.viewMatrix.invertAffine( inverseViewMatrix );
        invertedTransposedViewMatrix.set( inverseViewMatrix ).transpose();

        if ( updatePVM )
        {
            updateProjectionViewMatrix();
        }
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

    public Matrix4f getInvertedTransposedViewMatrix()
    {
        return invertedTransposedViewMatrix;
    }
}
