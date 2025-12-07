package de.codesourcery.robosim.render;

import java.awt.Color;
import java.util.Arrays;
import org.joml.Vector3f;

public class MeshBuilder
{
    public static final int ATTR_VERTEX_X     = 0;
    public static final int ATTR_VERTEX_Y     = 1;
    public static final int ATTR_VERTEX_Z     = 2;
    public static final int ATTR_VERTEX_NX    = 3;
    public static final int ATTR_VERTEX_NY    = 4;
    public static final int ATTR_VERTEX_NZ    = 5;
    public static final int ATTR_VERTEX_COLOR_ARGB = 6;

    private final int attributesPerVertex = 7;

    private int vertexPtr = 0;
    private float[] vertices = new float[3 * attributesPerVertex];

    private int indexPtr = 0;
    private int[] indices = new int[3];

    private final Vector3f tmp = new Vector3f();

    public MeshBuilder addTriangle(float x0, float y0, float z0,
                                   float x1, float y1, float z1,
                                   float x2, float y2, float z2, int argb)
    {
        // calculate normal vector
        calculateNormalVector( x0, y0, z0, x1, y1, z1, x2, y2, z2, tmp );

        int i0 = addVertex( x0,y0 , z0, tmp.x, tmp.y, tmp.z, argb);
        int i1 = addVertex( x1,y1 , z1, tmp.x, tmp.y, tmp.z, argb);
        int i2 = addVertex( x2,y2 , z2, tmp.x, tmp.y, tmp.z, argb);
        addTriangle( i0, i1, i2 );
        return this;
    }

    private void calculateNormalVector(Vector3f p0,Vector3f p1, Vector3f p2,Vector3f result)
    {
        calculateNormalVector(
            p0.x, p0.y, p0.z,
            p1.x, p1.y, p1.z,
            p2.x, p2.y, p2.z, result );
    }

    private void calculateNormalVector(float x0, float y0, float z0,
                                       float x1, float y1, float z1,
                                       float x2, float y2, float z2, Vector3f result)
    {
        float v1x = x1-x0;
        float v1y = y1-y0;
        float v1z = z1-z0;

        float v2x = x2-x0;
        float v2y = y2-y0;
        float v2z = z2-z0;

        // dot product in counter clock-wise winding order
        float nx=v1y*v2z - v1z*v2y;
        float ny=v1z*v2x - v1x*v2z;
        float nz=v1x*v2y - v1y*v2x;

        // dot product in clock-wise winding order
//        float nx = v2y * v1z - v2z * v1y; // v2y * v1z instead of v1y * v2z
//        float ny = v2z * v1x - v2x * v1z; // v2z * v1x instead of v1z * v2x
//        float nz = v2x * v1y - v2y * v1x; // v2x * v1y instead of v1x * v2y

        float l = (float) Math.sqrt( nx * nx + ny * ny + nz * nz );
        if ( l != 0 )
        {
            nx /= l;
            ny /= l;
            nz /= l;
        }
        result.set(nx,ny,nz);
    }

    private int addVertex(float x, float y, float z, float nx, float ny, float nz,int argb)
    {
        if ( vertexPtr == vertices.length )
        {
            final int currentVertexCount = vertices.length / attributesPerVertex;
            final int newVertexCount = 1 + (currentVertexCount * 2);
            vertices = Arrays.copyOf( vertices, newVertexCount * attributesPerVertex );
        }
        final int idx = vertexPtr / attributesPerVertex;
        vertices[vertexPtr + ATTR_VERTEX_X    ] = x;
        vertices[vertexPtr + ATTR_VERTEX_Y    ] = y;
        vertices[vertexPtr + ATTR_VERTEX_Z    ] = z;
        vertices[vertexPtr + ATTR_VERTEX_NX   ] = nx;
        vertices[vertexPtr + ATTR_VERTEX_NY   ] = ny;
        vertices[vertexPtr + ATTR_VERTEX_NZ   ] = nz;
        vertices[vertexPtr + ATTR_VERTEX_COLOR_ARGB] = argb;
        vertexPtr += attributesPerVertex;
        return idx;
    }

    private void addTriangle(int idx0,int idx1, int idx2)
    {
        if ( indexPtr+3 >= indices.length )
        {
            final int newLength = 3 + (indices.length * 2);
            indices = Arrays.copyOf( indices, newLength );
        }
        indices[indexPtr++] = idx0;
        indices[indexPtr++] = idx1;
        indices[indexPtr++] = idx2;
    }

    private void addIndex(int idx)
    {
        if ( indexPtr == indices.length )
        {
            final int newLength = 1 + (indices.length * 2);
            indices = Arrays.copyOf( indices, newLength );
        }
        indices[indexPtr++] = idx;
    }

    public MeshBuilder addTriangle(Vector3f p0, Vector3f p1, Vector3f p2, int argb)
    {
        return addTriangle( p0.x, p0.y, p0.z, p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, argb);
    }

    public MeshBuilder addQuad(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, int argb)
    {
        calculateNormalVector( p0, p1, p2, tmp );
        float nx = tmp.x;
        float ny = tmp.y;
        float nz = tmp.z;

        int pi0 = addVertex( p0.x, p0.y, p0.z, nx, ny, nz, argb );
        int pi1 = addVertex( p1.x, p1.y, p1.z, nx, ny, nz, argb );
        int pi2 = addVertex( p2.x, p2.y, p2.z, nx, ny, nz, argb );
        int pi3 = addVertex( p3.x, p3.y, p3.z, nx, ny, nz, argb );

        addTriangle( pi0, pi1, pi2 );
        addTriangle( pi3, pi0, pi2 );

        return this;
    }

    public static Mesh createCube(float dimension) {

        final MeshBuilder builder = new MeshBuilder();

        float xMin = -dimension/2.0f;
        float yMin = -dimension/2.0f;
        float zMin = -dimension/2.0f;

        float xMax = dimension/2.0f;
        float yMax = dimension/2.0f;
        float zMax = dimension/2.0f;

        // front
        final Vector3f p0 = new Vector3f(xMin, yMax, zMax);
        final Vector3f p3 = new Vector3f(xMax, yMax, zMax);
        final Vector3f p7 = new Vector3f(xMax, yMin, zMax);
        final Vector3f p4 = new Vector3f(xMin, yMin, zMax);

        // back
        final Vector3f p2 = new Vector3f(xMax, yMax, zMin);
        final Vector3f p1 = new Vector3f(xMin, yMax, zMin);
        final Vector3f p5 = new Vector3f(xMin, yMin, zMin);
        final Vector3f p6 = new Vector3f(xMax, yMin, zMin);

        // front surface WORKS
          builder.addQuad( p0, p3, p7, p4, Color.RED.getRGB() );

        // right surface
        builder.addQuad( p3, p2, p6, p7, Color.GREEN.getRGB() );

//        // left surface
         builder.addQuad( p1, p0, p4, p5, Color.YELLOW.getRGB() );
//
//        // back surface WORKS
        builder.addQuad( p2, p1, p5, p6, Color.PINK.getRGB() );
//
//        // top surface
        builder.addQuad( p1, p2, p3, p0, Color.LIGHT_GRAY.getRGB() );
//
//        // bottom surface
         builder.addQuad( p1, p2, p3, p0, Color.CYAN.getRGB() );

        return builder.build();
    }

    public Mesh build()
    {
        return new Mesh(this.attributesPerVertex,
            Arrays.copyOfRange( this.indices, 0, this.indexPtr),
            Arrays.copyOfRange( this.vertices, 0, this.vertexPtr ) );
    }

    private void addTriangleStrip(float cx, float cy, float cz, float[] points, int argb) {
        if ( points.length %3 != 0 ) {
            throw new IllegalArgumentException( "Triangle strip points array length must be a multiple of 3" );
        }
        final int pntCnt = points.length / 3;
        if ( pntCnt < 2 ) {
            throw new IllegalArgumentException( "Triangle strip needs at least 2 points (=3 in total" );
        }
        for ( int i = 1 ; i < pntCnt ; i++ ) {
            final int offset0 = (i-1)*3;
            final int offset1 = i*3;
            addTriangle( cx,cy,cz,
                points[offset0],points[offset0+1],points[offset0+2],
                points[offset1],points[offset1+1],points[offset1+2],argb
                );
        }
    }

    public static Mesh createCylinder( float length, float diameter, int subdivisions) {

        final MeshBuilder builder = new MeshBuilder();

        float xMin = -length/2.0f;
        float xMax =  length/2.0f;

        final float radius = diameter / 2.0f;
        final float angleStep = (float) (2 * Math.PI / subdivisions);

        final float[] leftVertices = new float[subdivisions*3];
        final float[] rightVertices = new float[subdivisions*3];

        final Color[] colors = new Color[]{ Color.GRAY, Color.LIGHT_GRAY};

        final Vector3f p0 = new Vector3f(xMin, 0, 0);
        final Vector3f p1 = new Vector3f(xMin, 0, 0);
        final Vector3f p2 = new Vector3f(xMin, 0, 0);
        final Vector3f p3 = new Vector3f(xMin, 0, 0);

        for ( int i = 0 ; i < subdivisions ; i++ ) {
            final float angle0 = i * angleStep;
            float z0 = (float) (radius * Math.cos( angle0 ));
            float y0 = (float) (radius * Math.sin( angle0 ));

            // render tube using quads
            if ( (i+1) < subdivisions ) {
                final float angle1 = (i+1) * angleStep;
                float z1 = (float) (radius * Math.cos( angle1 ));
                float y1 = (float) (radius * Math.sin( angle1 ));

                p0.set( xMin, y0, z0 );
                p1.set( xMin, y1, z1 );
                p2.set( xMax, y1, z1 );
                p3.set( xMax, y0, z0 );

                builder.addQuad( p0, p1, p2, p3, colors[ i % colors.length].getRGB() );
            }

            // points of triangle strips need to be wound in clock-wise order
            // to not break visibility tests
            // so we need to use (2*Math.PI - angle) for the right cap
            z0 = (float) (radius * Math.cos( angle0 ));
            y0 = (float) (radius * Math.sin( angle0 ));
            float z1 = (float) (radius * Math.cos( 2*Math.PI - angle0 ));
            float y1 = (float) (radius * Math.sin( 2*Math.PI - angle0 ));

            // gather indices for caps (rendered as triangle strips)
            final int ptr = i*3;
            leftVertices[ptr] = xMin;
            leftVertices[ptr+1] = y0;
            leftVertices[ptr+2] = z0;

            rightVertices[ptr] = xMax;
            rightVertices[ptr+1] = y1;
            rightVertices[ptr+2] = z1;
        }

        builder.addTriangleStrip( xMin, 0, 0, leftVertices, Color.GREEN.getRGB() );
        builder.addTriangleStrip( xMax, 0, 0, rightVertices, Color.RED.getRGB() );

        return builder.build();
    }
}
