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

    // light position in VIEW space
    private static final Vector3f LIGHT_POS = new Vector3f(0,100,100);

    public final Camera camera;

    private final boolean renderWireframe = false;
    private final boolean backFaceCulling = true;
    private final boolean depthSortBodies = true;
    private final boolean depthSortTriangles = true;
    private final boolean useFlatShading = true;

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

        // transform meshes into world space and sort meshes
        // back-to-front according to their distance from the camera.
        //
        // We'll transform the meshes bounding box (that is always in world space)
        // into view space and then, since in view space the camera will be at (0,0,0),
        // simply order meshes ascending to their transformed BBs largest Z-index.

        final float[] largestZIndex = new float[bodies.size()]; // hold's max. Z index of mesh BBs after the BB has been transformed into view spacee

        final Mesh[] meshes = new Mesh[bodies.size()];
        final int[] meshesByAscendingZIndex = new int[bodies.size()];

        for ( int i = 0, bodiesSize = bodies.size(); i < bodiesSize; i++ )
        {
            final Body body = bodies.get( i );
            // transform mesh from local space into world space
            meshes[i] = body.getMeshInWorldSpace();
            // transform BB into view space
            final BoundingBox bbInViewSpace = body.getBoundingBox().createCopy().transform( camera.getViewMatrix() );
            largestZIndex[i] = bbInViewSpace.max.z;
            meshesByAscendingZIndex[i] = i;
        }

        // sort meshes ascending by their largest Z-index
        if ( depthSortBodies )
        {
            IntegerQuicksort.sort( meshesByAscendingZIndex, meshesByAscendingZIndex.length,
                (a, b) -> Float.compare( largestZIndex[a], largestZIndex[b] ) );
        }

        /*
         * IMPORTANT: Normals need to be transformed using the TRANSPOSED inverted view matrix.
         */
        final Matrix4f normalMatrix = camera.getInvertedTransposedViewMatrix();

        for ( final int meshIndex : meshesByAscendingZIndex )
        {
            // NOTE: Meshes have been transformed into world space by code above
            final Mesh mesh = meshes[meshIndex];

            // transform mesh from world space into view space,
            // taking care to multiply normals with the inverted view matrix
            Mesh.transform( mesh, camera.getViewMatrix(), normalMatrix );

            // compile array with visible triangles
            if ( mesh.indices.length % 3 != 0 )
            {
                throw new IllegalArgumentException( "Mesh indices array's length must be a multiple of 3." );
            }
            int visibleTriangleCount = 0;

            // this array holds for each visible triangle the
            // offset of that triangle's first vertex inside the meshes "indices[]" array
            final int[] visibleTriangleIndices = new int[mesh.indices.length / 3];

            // loop over vertices of each triangle, check visibility, calculate shaded color
            for ( int i = 0; i < mesh.indices.length; i+= 3 )
            {
                boolean isVisible = false;
                int shadedColor = 0xffffffff;
                if ( backFaceCulling )
                {
                    for ( int j = 2; j >= 0; j-- )
                    {
                        final int triangleVertexIndex = i + j;
                        mesh.getNormal( mesh.indices[triangleVertexIndex], p0 );
                        mesh.getVertexCoords( mesh.indices[triangleVertexIndex], p1 );

                        // NOTE: Since vertices have already been translated into VIEW space (=>camera is now at (0,0,0)
                        //       we can simply use the vertex coordinate itself as surface->camera vector
                        final float dotProduct = p0.dot( p1 );
                        final boolean visible = dotProduct <= 0;
                        if ( visible )
                        {
                            isVisible = true;
                            if ( useFlatShading )
                            {
                                // we know that since the surface is visible, cosine must in
                                // interval [-1,0]
                                float cos = dotProduct / (p0.length() * p1.length());
                                shadedColor = shadeRGB( mesh.getVertexColor( triangleVertexIndex ), -cos );
                            }
                            break;
                        }
                    }
                } else {
                    isVisible = true;
                }
                if ( isVisible )
                {
                    if ( backFaceCulling && useFlatShading )
                    {
                        mesh.setVertexColor( i, shadedColor );
                        mesh.setVertexColor( i+1, shadedColor );
                        mesh.setVertexColor( i+2, shadedColor );
                    }
                    // visible, add triangle indices
                    visibleTriangleIndices[visibleTriangleCount++] = i;
                }
            }

            // sort triangles back to front
            if ( depthSortTriangles )
            {
                IntegerQuicksort.sort( visibleTriangleIndices, visibleTriangleCount, (int triangleIdxA, int triangleIdxB) -> {
                    final float z0 = mesh.getTriangleMinZ( triangleIdxA );
                    final float z1 = mesh.getTriangleMinZ( triangleIdxB );
                    return Float.compare( z0, z1 );
                } );
            }

            final Line[] normals;
            if ( RENDER_NORMALS )
            {
                normals = new Line[visibleTriangleCount * 3];
                for ( int i = 0, ptr = 0; i < visibleTriangleCount; i++ )
                {
                    final int triangleIndex = visibleTriangleIndices[i];
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
            for ( int i = 0; i < visibleTriangleCount; i++ )
            {
                final int triangleIndex = visibleTriangleIndices[i];

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

            // render normals last so they do not get occluded
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

    private static int shadeRGB(int argb, float factor) {
        final int a = argb & 0xff000000;
        final int r = ((int) (factor * ( (argb & 0xff0000) >> 16 )) & 0xff);
        final int g = ((int) (factor * ( (argb & 0x00ff00) >>  8 )) & 0xff);
        final int b = ((int) (factor * ( (argb & 0x0000ff)       )) & 0xff);
        return a | r << 16 | g << 8 | b;
    }
}