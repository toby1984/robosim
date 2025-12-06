package de.codesourcery.robosim.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
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

    public void render(BufferedImage image, Graphics2D graphics, Mesh... meshes) {

        // some temporary variables
        final Vector3f p0 = new Vector3f();
        final Vector3f p1 = new Vector3f();
        final Vector3f p2 = new Vector3f();
        final Vector3f avg = new Vector3f();

        // create a merged copy so we don't perturb the input meshes
        final Mesh mesh = switch( meshes.length )
        {
            case 0 -> throw new IllegalArgumentException( "" );
            case 1 -> meshes[0].createCopy();
            default -> Mesh.merge( meshes );
        };

        final Vector3f viewVec =
            new Vector3f( camera.getTarget() ).sub( camera.getPosition() ).normalize();

        // compile array with visible triangles
        if ( mesh.indices.length % 3 != 0 ) {
            throw new IllegalArgumentException( "Mesh indices array's length must be a multiple of 3." );
        }
        int triangleCount = 0;

        // this array holds for each visible triangle the
        // offset of the triangle's first vertex inside the mesh.indices[] array
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

            // back-face culling
            if ( ! backFaceCulling || viewVec.dot( avg ) > 0 ) {
                // visible, add triangle indices
                triangleIndices[triangleCount++] = i;
            }
        }

        // painter's algorith: sort triangles so we render from back to front
        if ( depthSort )
        {
            IntegerQuicksort.sort( triangleIndices, (int triangleIdxA, int triangleIdxB) -> {
                final float z0 = mesh.getTriangleAverageZCoordinate( triangleIdxA );
                final float z1 = mesh.getTriangleAverageZCoordinate( triangleIdxB );
                return Float.compare( z0, z1 );
            } );
        }

        Mesh.transform( mesh, camera.getViewProjectionMatrix(), camera.getInverseViewMatrix() );

        // now render all visible triangles back-to-front
        int currentColor=0;
        Color color = Color.BLACK;
        graphics.setColor( color );
        final int[] xPoints = new int[3];
        final int[] yPoints = new int[3];

        final int cx = image.getWidth()/2;
        final int cy = image.getHeight()/2;
        for ( int i = 0 ; i < triangleCount; i++ )
        {
            final int triangleIndex = triangleIndices[i];
            final int rgb = mesh.getVertexColor( triangleIndex );
            if ( rgb != currentColor )
            {
                currentColor = rgb;
                color = new Color( rgb );
                graphics.setColor( color );
            }
            mesh.getTriangleVertexCoords( triangleIndex, p0, p1, p2 );

            xPoints[0] = cx + (int) p0.x;
            xPoints[1] = cx + (int) p1.x;
            xPoints[2] = cx + (int) p2.x;

            yPoints[0] = cy - (int) p0.y;
            yPoints[1] = cy - (int) p1.y;
            yPoints[2] = cy - (int) p2.y;

            graphics.drawPolygon( xPoints, yPoints, 3 );
            // graphics.fillPolygon( xPoints, yPoints, 3 );
        }

        graphics.drawString( "Camera @ " + Utils.prettyPrint( camera.getPosition() ) , 15, 15);
        graphics.drawString( "Look @ " + Utils.prettyPrint(camera.getTarget()) , 15, 35);
    }
}