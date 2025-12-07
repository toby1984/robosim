package de.codesourcery.robosim.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.joml.Vector3f;
import de.codesourcery.robosim.Utils;

public class MeshRenderer
{
    public final Camera camera;

    private boolean backFaceCulling = false;
    private boolean depthSort = false;

    public MeshRenderer(Camera camera)
    {
        this.camera = camera;
    }

    public void render(BufferedImage image, Graphics2D graphics, List<Body> bodies) {

        // some temporary variables
        final Vector3f p0 = new Vector3f();
        final Vector3f p1 = new Vector3f();
        final Vector3f p2 = new Vector3f();
        final Vector3f avg = new Vector3f();
        final Vector3f center0 = new Vector3f();
        final Vector3f center1 = new Vector3f();

        // sort bodies back-to-front according to distance
        // to camera
        final List<Body> sortedBodies = new ArrayList<>(bodies);

        sortedBodies.sort( (a,b) -> {
           a.getBoundingBox().getCenter( center0 );
           b.getBoundingBox().getCenter( center1 );
            final float d1 = center0.distanceSquared( camera.getPosition() );
            final float d2 = center1.distanceSquared( camera.getPosition() );
            return Float.compare( d2, d1 );
        });

        for ( final Body body : sortedBodies )
        {
            // transform mesh from local space into world space
            final Mesh mesh = body.getMeshInWorldSpace();

            // transform mesh from world space into view space,
            // taking care to multiply normals with the inverted view matrix
            Mesh.transform(mesh, camera.getViewMatrix(), camera.getInverseViewMatrix()  );

            // compile array with visible triangles
            if ( mesh.indices.length % 3 != 0 ) {
                throw new IllegalArgumentException( "Mesh indices array's length must be a multiple of 3." );
            }
            int triangleCount = 0;

            // this array holds for each visible triangle the
            // offset of that triangle's first vertex inside the meshes "indices[]" array
            final int[] triangleIndices = new int[ mesh.indices.length / 3 ];

            // array of vertices of all triangles that passed the
            // back-face culling test
            for ( int i = 0; i < mesh.indices.length; i+=3 ) {
                mesh.getNormalCoords( mesh.indices[i]  , p0 );
                mesh.getNormalCoords( mesh.indices[i+1], p1 );
                mesh.getNormalCoords( mesh.indices[i+2], p2 );
                float x = (p0.x + p1.x + p2.x)/3;
                float y = (p0.y + p1.y + p2.y)/3;
                float z = (p0.z + p1.z + p2.z)/3;
                avg.set(x, y, z);

                mesh.getTriangleCenterCoords( i, center0 );

                // back-face culling
                if ( ! backFaceCulling || center0.dot( avg ) > 0 ) {
                    // visible, add triangle indices
                    triangleIndices[triangleCount++] = i;
                }
            }

            // sort triangles back to front
            if ( depthSort )
            {
                IntegerQuicksort.sort( triangleIndices, (int triangleIdxA, int triangleIdxB) -> {
                    final float z0 = mesh.getTriangleAverageZCoordinate( triangleIdxA );
                    final float z1 = mesh.getTriangleAverageZCoordinate( triangleIdxB );
                    return Float.compare( z0, z1 );
                } );
            }

//            System.out.println("Visible triangles: "+triangleCount);

            // now render all visible triangles back-to-front


            // transform mesh into normalized device coordinates (NDC)
            // in range [-1,1]
            Mesh.transformPerspectiveNDC( mesh, camera.getProjectionMatrix() );

            int currentColor=0;
            Color color = Color.BLACK;
            graphics.setColor( color );
            final int[] xPoints = new int[3];
            final int[] yPoints = new int[3];

            final int w = image.getWidth();
            final int h = image.getHeight();

            final int cx = w / 2;
            final int cy = h / 2;
            for ( int i = 0 ; i < triangleCount; i++ )
            {
                final int triangleIndex = triangleIndices[i];

                // TODO: we'll currently only use the color of the triangle's first vertex
                final int rgb = mesh.getVertexColor( triangleIndex );
                if ( rgb != currentColor )
                {
                    currentColor = rgb;
                    color = new Color( rgb );
                    graphics.setColor( color );
                }
                mesh.getTriangleVertexCoords( triangleIndex, p0, p1, p2 );

                xPoints[0] = cx + (int) (p0.x*w/2);
                xPoints[1] = cx + (int) (p1.x*w/2);
                xPoints[2] = cx + (int) (p2.x*w/2);

                yPoints[0] = cy - (int) (p0.y*h/2);
                yPoints[1] = cy - (int) (p1.y*h/2);
                yPoints[2] = cy - (int) (p2.y*h/2);

                graphics.drawPolygon( xPoints, yPoints, 3 );
                // graphics.fillPolygon( xPoints, yPoints, 3 );
            }
        }

        graphics.drawString( "Camera @ " + Utils.prettyPrint( camera.getPosition() ) , 15, 15);
        graphics.drawString( "Look @ " + Utils.prettyPrint(camera.getTarget()) , 15, 35);
    }
}