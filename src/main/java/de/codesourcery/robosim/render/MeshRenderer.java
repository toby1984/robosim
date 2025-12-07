package de.codesourcery.robosim.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import de.codesourcery.robosim.Utils;

public class MeshRenderer
{
    private static final boolean RENDER_NORMALS = false;
    private static final boolean RENDER_DEBUG_INFO = false;

    public final Camera camera;

    private final boolean renderWireframe = false;
    private final boolean backFaceCulling = true;
    private final boolean depthSortBodies = true;
    private final boolean depthSortTriangles = true;

    private static final class Line {
        public final float x0,y0;
        public final float x1,y1;
        public final int argb;

        public Line(Vector3f p0, Vector3f p1, int argb)
        {
            this.x0 = p0.x;
            this.y0 = p0.y;
            this.x1 = p1.x;
            this.y1 = p1.y;
            this.argb = argb;
        }
    }

    public MeshRenderer(Camera camera)
    {
        this.camera = camera;
    }

    private final Map<Integer,Color> colorMap = new HashMap<>();

    private Color getColor(int argb) {
        return colorMap.computeIfAbsent( argb, Color::new );
    }

    public void render(BufferedImage image, Graphics2D graphics, List<Body> bodies) {

        // some temporary variables
        final Vector3f p0 = new Vector3f();
        final Vector3f p1 = new Vector3f();
        final Vector3f p2 = new Vector3f();
        final Vector3f avg = new Vector3f();
        final Vector3f center0 = new Vector3f();

        // sort meshes of bodies back-to-front according to distance
        // their distance to the camera.
        // Since in view space the camera will be at (0,0,0),
        // a meshes largest Z-index is simply the max. Z coordinate
        // of the meshes bounding box after transforming it into view space.
        final float[] largestZIndex = new float[bodies.size()];
        final Mesh[] meshesInViewSpace = new Mesh[bodies.size()];
        final int[] meshesByAscendingZIndex = new int[bodies.size()];

        for ( int i = 0, bodiesSize = bodies.size(); i < bodiesSize; i++ )
        {
            final Body body = bodies.get( i );
            // transform mesh from local space into world space
            meshesInViewSpace[i] = body.getMeshInWorldSpace();
            // transform BB into view space
            final BoundingBox bbInViewSpace = body.getBoundingBox().createCopy().transform( camera.getViewMatrix() );
            largestZIndex[i] = bbInViewSpace.max.z;
            meshesByAscendingZIndex[i] = i;
        }

        // sort meshes ascending by their largest Z-index
        if ( depthSortBodies )
        {
            IntegerQuicksort.sort( meshesByAscendingZIndex, meshesByAscendingZIndex.length, (a, b) -> Float.compare( largestZIndex[a], largestZIndex[b] ) );
        }

        /*
         * IMPORTANT: Normals need to be transformed using the TRANSPOSED inverted view matrix.
         */
        final Matrix4f normalMatrix = camera.getInvertedTransposedViewMatrix();

        for ( final int meshIndex : meshesByAscendingZIndex )
        {
            final Mesh mesh = meshesInViewSpace[meshIndex];

            // transform mesh from world space into view space,
            // taking care to multiply normals with the inverted view matrix
            Mesh.transform( mesh, camera.getViewMatrix(), normalMatrix );

            // compile array with visible triangles
            if ( mesh.indices.length % 3 != 0 )
            {
                throw new IllegalArgumentException( "Mesh indices array's length must be a multiple of 3." );
            }
            int triangleCount = 0;

            // this array holds for each visible triangle the
            // offset of that triangle's first vertex inside the meshes "indices[]" array
            final int[] triangleIndices = new int[mesh.indices.length / 3];

            // array of vertices of all triangles that passed the
            // back-face culling test
            for ( int i = 0; i < mesh.indices.length; i+= 3 )
            {
                for ( int j = 2; j >= 0 ; j-- ) {
                    final int triangleVertexIndex = i+j;
                    mesh.getNormal( mesh.indices[triangleVertexIndex], p0 );
                    mesh.getVertexCoords( mesh.indices[triangleVertexIndex], p1);
                    p1.sub( camera.getPosition() );

                    if ( ! backFaceCulling || p0.dot( p1 ) <= 0 )
                    {
                        // visible, add triangle indices
                        triangleIndices[triangleCount++] = i;
                        break;
                    }
                }
            }

            // sort triangles back to front
            if ( depthSortTriangles )
            {
                IntegerQuicksort.sort( triangleIndices, triangleCount, (int triangleIdxA, int triangleIdxB) -> {
                    final float z0 = mesh.getTriangleMinZ( triangleIdxA );
                    final float z1 = mesh.getTriangleMinZ( triangleIdxB );
                    return Float.compare( z0, z1 );
                } );
            }

            final Line[] normals;
            if ( RENDER_NORMALS )
            {
                normals = new Line[triangleCount * 3];
                for ( int i = 0, ptr = 0; i < triangleCount; i++ )
                {
                    final int triangleIndex = triangleIndices[i];
                    for ( int j = 0; j < 3; j++ )
                    {
                        final int argb = mesh.getVertexColor( triangleIndex + j );
                        mesh.getVertexCoords( mesh.indices[triangleIndex + j], p0 ); // p0 == start point
                        mesh.getNormal( mesh.indices[triangleIndex + j], p1 );
                        p1.mul( 10 ).add( p0 ); // p1 == end point
                        camera.getProjectionMatrix().transformProject( p0 );
                        camera.getProjectionMatrix().transformProject( p1 );
                        normals[ptr++] = new Line( p0, p1, argb );
                    }
                }
            }
            else
            {
                normals = null;
            }
            // now render all visible triangles back-to-front

            // transform mesh into normalized device coordinates (NDC)
            // in range [-1,1]
            Mesh.transformPerspectiveNDC( mesh, camera.getProjectionMatrix() );

            int currentColorARGB = 0;
            graphics.setColor( Color.BLACK );
            final int[] xPoints = new int[3];
            final int[] yPoints = new int[3];

            final int w = image.getWidth();
            final int h = image.getHeight();

            final int cx = w / 2;
            final int cy = h / 2;
            for ( int i = 0; i < triangleCount; i++ )
            {
                final int triangleIndex = triangleIndices[i];

                // TODO: we'll currently only use the color of the triangle's first vertex
                final int argb = mesh.getVertexColor( triangleIndex );
                if ( argb != currentColorARGB )
                {
                    graphics.setColor( getColor( argb ) );
                    currentColorARGB = argb;
                }
                mesh.getTriangleVertexCoords( triangleIndex, p0, p1, p2 );

                xPoints[0] = cx + (int) (p0.x * w / 2);
                xPoints[1] = cx + (int) (p1.x * w / 2);
                xPoints[2] = cx + (int) (p2.x * w / 2);

                yPoints[0] = cy - (int) (p0.y * h / 2);
                yPoints[1] = cy - (int) (p1.y * h / 2);
                yPoints[2] = cy - (int) (p2.y * h / 2);

                if ( renderWireframe ) {
                    graphics.drawPolygon( xPoints, yPoints, 3 );
                } else {
                    graphics.fillPolygon( xPoints, yPoints, 3 );
                }
            }

            if ( RENDER_NORMALS )
            {
                for ( final Line line : normals )
                {
                    final int x0 = cx + (int) (line.x0 * w / 2);
                    final int y0 = cy - (int) (line.y0 * h / 2);

                    final int x1 = cx + (int) (line.x1 * w / 2);
                    final int y1 = cy - (int) (line.y1 * h / 2);

                    final int argb = line.argb;
                    if ( argb != currentColorARGB )
                    {
                        currentColorARGB = argb;
                        graphics.setColor( getColor( argb ) );
                    }
                    graphics.drawLine( x0, y0, x1, y1 );
                }
            }
        }

        if ( RENDER_DEBUG_INFO)
        {
            graphics.drawString( "camera @ " +Utils.prettyPrint( camera.getPosition() ) , 15, 15);
            graphics.drawString( "Look @ " + Utils.prettyPrint(camera.getTarget()) , 15, 35);
        }
    }
}