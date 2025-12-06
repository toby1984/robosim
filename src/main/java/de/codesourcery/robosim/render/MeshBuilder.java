package de.codesourcery.robosim.render;

import java.util.Arrays;
import org.joml.Vector3f;

public class MeshBuilder
{
    private int attributesPerVertex = 3;

    private int vertexPtr = 0;
    private float[] vertices = new float[3 * attributesPerVertex];

    private int normalPtr = 0;
    private float[] normals = new float[3 * attributesPerVertex];

    public MeshBuilder addTriangle(float x0, float y0, float z0,
                                   float x1, float y1, float z1,
                                   float x2, float y2, float z2)
    {
        addVertex( x0,y0 , z0);
        addVertex( x1,y1 , z1);
        addVertex( x2,y2 , z2);

        // calculate normal vector
        float v1x = x1-x0;
        float v1y = y1-y0;
        float v1z = z1-z0;

        float v2x = x2-x0;
        float v2y = y2-y0;
        float v2z = z2-z0;

        float nx=v1y*v2z - v1z*v2y;
        float ny=v1z*v2x - v1x*v2z;
        float nz=v1x*v2y - v1y*v2x;

        float l = (float) Math.sqrt( nx * nx + ny * ny + nz * nz );
        if ( l != 0 )
        {
            nx /= l;
            ny /= l;
            nz /= l;
        }
        addNormal( nx,ny,nz);
        return this;
    }

    private void addVertex(float x, float y, float z)
    {
        if ( vertexPtr == vertices.length )
        {
            final int currentLength = vertices.length / attributesPerVertex;
            final int newLength = 1 + (currentLength * 2);
            vertices = Arrays.copyOf( vertices, newLength * attributesPerVertex );
        }
        vertices[vertexPtr    ] = x;
        vertices[vertexPtr + 1] = y;
        vertices[vertexPtr + 2] = z;
        vertexPtr += attributesPerVertex;
    }

    private void addNormal(float x, float y, float z)
    {
        if ( normalPtr == normals.length )
        {
            final int currentLength = normals.length / 3;
            final int newLength = 1 + (currentLength * 2);
            vertices = Arrays.copyOf( normals, newLength * 3 );
        }
        normals[normalPtr++] = x;
        normals[normalPtr++] = y;
        normals[normalPtr++] = z;
    }

    private MeshBuilder addTriangle(Vector3f p0, Vector3f p1, Vector3f p2)
    {
        return addTriangle( p0.x, p0.y, p0.z, p1.x, p1.y, p1.z, p2.x, p2.y, p2.z );
    }

    private MeshBuilder addQuad(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3)
    {
        addTriangle( p0, p1, p2 );
        return addTriangle( p3, p0, p2 );
    }

    public static Mesh createCube(float dimension) {

        final MeshBuilder builder = new MeshBuilder();

        // xxx create cube xxx
        return builder.build();
    }

    public Mesh build()
    {
        return new Mesh(this.attributesPerVertex,
            Arrays.copyOfRange( this.normals, 0, this.normalPtr),
            Arrays.copyOfRange( this.vertices, 0, this.vertexPtr ) );
    }

    public MeshBuilder addCylinder( float length, float diameter, int subdivisions) {
        xxx create cylinder xxx
    }
}
