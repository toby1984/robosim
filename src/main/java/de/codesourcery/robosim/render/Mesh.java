package de.codesourcery.robosim.render;

import java.util.Arrays;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Mesh
{
    public int attributesPerVertex = 3;
    public float[] vertices;
    public int[] indices;

    public interface VertexVisitor {
        void visit(float x, float y, float z);
    }

    public Mesh(int attributesPerVertex, int[] indices, float[] vertices)
    {
        this.attributesPerVertex = attributesPerVertex;
        this.vertices = vertices;
        this.indices = indices;
    }

    private static final class BBVisitor implements VertexVisitor
    {
        public float minX=Float.MAX_VALUE, minY=Float.MAX_VALUE, minZ=Float.MAX_VALUE,
                     maxX=Float.MIN_VALUE, maxY=Float.MIN_VALUE, maxZ=Float.MIN_VALUE;

        @Override
        public void visit(float x, float y, float z)
        {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);

            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }
    }

    public BoundingBox createBoundingBox() {
        final BBVisitor v = new BBVisitor();
        visitVertices( v );
        return new BoundingBox( new Vector3f(v.minX, v.minY, v.minZ), new Vector3f(v.maxX, v.maxY, v.maxZ) );
    }

    public void visitVertices(VertexVisitor visitor)
    {
        for ( int i = 0; i < vertices.length; i+=attributesPerVertex )
        {
            visitor.visit(
                vertices[i+MeshBuilder.ATTR_VERTEX_X],
                vertices[i+MeshBuilder.ATTR_VERTEX_Y],
                vertices[i+MeshBuilder.ATTR_VERTEX_Z]
            );
        }
    }

    @Override
    public String toString()
    {
        return print();
    }

    public String print() {
        final StringBuilder sb = new StringBuilder();
        sb.append( "Attributes per index: " ).append( attributesPerVertex ).append( "\n" );
        sb.append( "Triangles: " ).append( getTriangleCount() ).append( "\n" );
        sb.append( "Vertices : " ).append( getVertexCount() ).append( "\n" );
        sb.append("\n");
        sb.append("------ Vertex Data -----\n");
        sb.append("\n");
        final StringBuilder sb2 = new StringBuilder();
        for ( int i = 0, j=0 ; i < vertices.length ; i+= attributesPerVertex,j++ ) {

            sb2.setLength( 0 );
            for ( int z = 0 ; z < attributesPerVertex; z++ ) {
                sb2.append( vertices[i+z] );
                if ( (z+1) < attributesPerVertex ) {
                    sb2.append( ", " );
                }
            }
            sb.append( "Vertex #" ).append( j ).append( ": " ).append( sb2 );
            sb.append( "\n" );
        }
        sb.append("\n");
        sb.append("------ Index Data -----\n");
        sb.append("\n");
        for ( int i = 0, indicesLength = indices.length; i < indicesLength; i+=3 )
        {
            sb.append( "(" ).append( indices[i] ).append(", ").append( indices[i+1] ).append(", ").append( indices[i+2] ).append(")");
            if ( (i+1)< indicesLength ) {
                sb.append( "\n" );
            }
        }
        return sb.toString();
    }

    public int getVertexCount() {
        return vertices.length / attributesPerVertex;
    }

    public int getTriangleCount() {
        return indices.length / 3;
    }

    /**
     *
     * @param vertexNo vertex index
     * @param result
     */
    public void getVertexCoords(int vertexNo, Vector3f result) {
        final int offset = vertexNo * attributesPerVertex;
        result.x = vertices[offset + MeshBuilder.ATTR_VERTEX_X];
        result.y = vertices[offset + MeshBuilder.ATTR_VERTEX_Y];
        result.z = vertices[offset + MeshBuilder.ATTR_VERTEX_Z];
    }

    public void getTriangleCenterCoords(int firstIndexOffset, Vector3f result) {

        float sumX=0,sumY=0,sumZ=0;
        for ( int i = firstIndexOffset, j=3; j > 0 ; j--, i++ ) {
            final int vertexIndex = indices[i] * attributesPerVertex;
            sumX += vertices[ vertexIndex + MeshBuilder.ATTR_VERTEX_X ];
            sumY += vertices[ vertexIndex + MeshBuilder.ATTR_VERTEX_Y ];
            sumZ += vertices[ vertexIndex + MeshBuilder.ATTR_VERTEX_Z ];
        }
        result.x = sumX/3;
        result.y = sumY/3;
        result.z = sumZ/3;
    }

    public void getNormal(int vertexNo, Vector3f result) {
        final int offset = vertexNo * attributesPerVertex;
        result.x = vertices[offset + MeshBuilder.ATTR_VERTEX_NX];
        result.y = vertices[offset + MeshBuilder.ATTR_VERTEX_NY];
        result.z = vertices[offset + MeshBuilder.ATTR_VERTEX_NZ];
    }

    public float getTriangleMinZ(int firstIndexOffset) {

        final int vertex0 = indices[firstIndexOffset] * attributesPerVertex;
        final int vertex1 = indices[firstIndexOffset+1] * attributesPerVertex;
        final int vertex2 = indices[firstIndexOffset+2] * attributesPerVertex;

        return Math.min(vertices[ vertex0 + MeshBuilder.ATTR_VERTEX_Z ],
            Math.min( vertices[ vertex1 + MeshBuilder.ATTR_VERTEX_Z ], vertices[ vertex2 + MeshBuilder.ATTR_VERTEX_Z ]));
    }

    public int getVertexColor(int indexNo)
    {
        final int offset = indices[indexNo] * attributesPerVertex;
        return (int) ( vertices[ offset + MeshBuilder.ATTR_VERTEX_COLOR_ARGB] );
    }

    public void setVertexColor(int indexNo, int argb) {
        final int offset = indices[indexNo] * attributesPerVertex;
        vertices[ offset + MeshBuilder.ATTR_VERTEX_COLOR_ARGB]  = argb;
    }

    public void getTriangleVertexCoords(int firstIndexNo, Vector3f p0, Vector3f p1, Vector3f p2)
    {
        getVertexCoords( indices[firstIndexNo], p0 );
        getVertexCoords( indices[firstIndexNo+1], p1 );
        getVertexCoords( indices[firstIndexNo+2], p2 );
    }

    public static Mesh merge(Mesh... meshes) {

        if ( meshes.length < 2 ) {
            throw new IllegalArgumentException( "Must provide at least 2 Meshes to merge" );
        }

        final int attributesPerVertex = meshes[0].attributesPerVertex;
        int vertexCount = meshes[0].vertices.length / attributesPerVertex;
        int indexCount = meshes[0].indices.length;
        for ( int i = 1, meshesLength = meshes.length; i < meshesLength; i++ )
        {
            final Mesh mesh = meshes[i];
            if ( attributesPerVertex != mesh.attributesPerVertex ) {
                throw new IllegalArgumentException( "Refusing to merge meshes with different vertex attributes" );
            }
            vertexCount += mesh.vertices.length / attributesPerVertex;
            indexCount += mesh.indices.length;
        }
        final float[] combinedVertices = new float[vertexCount * attributesPerVertex];
        final int[] combinedIndices = new int[indexCount * attributesPerVertex];
        int vertexDstPtr = 0;
        int indexDstPtr = 0;
        for ( final Mesh mesh : meshes )
        {
            final int vertexOffset = vertexDstPtr / attributesPerVertex;

            // copy vertex data
            System.arraycopy( mesh.vertices, 0, combinedVertices, vertexDstPtr, mesh.vertices.length );
            vertexDstPtr += mesh.vertices.length;

            // copy indices
            System.arraycopy( mesh.indices, 0, combinedIndices, indexDstPtr, mesh.indices.length );

            // fixup index ptrs
            for ( int i = 0; i < mesh.indices.length; i++ )
            {
                combinedIndices[indexDstPtr + i] += vertexOffset;
            }
            indexDstPtr += mesh.indices.length;
        }

        return new Mesh(attributesPerVertex, combinedIndices, combinedVertices);
    }

    public Mesh(int attributesPerVertex) {
        final int quads = 6;
        final int indexCnt = 8;
        final int triangles = quads * 2;
        final int vertexCnt = triangles * 3;
        vertices = new float[ vertexCnt * attributesPerVertex];
        indices = new int[ indexCnt ];
    }

    public Mesh(Mesh other)
    {
        this.attributesPerVertex = other.attributesPerVertex;
        //noinspection IncompleteCopyConstructor
        this.vertices = Arrays.copyOfRange(other.vertices,0,other.vertices.length);
        //noinspection IncompleteCopyConstructor
        this.indices = Arrays.copyOfRange(other.indices,0,other.indices.length);
    }

    public Mesh createCopy() {
        return new Mesh(this);
    }

    /**
     *
     * @param matrix view matrix used to transform vertex coordinates
     * @param normalMatrix  TRANSPOSED inverted view matrix used to transform normal vectors
     */
    public static Mesh transform(Mesh result, Matrix4f matrix, Matrix4f normalMatrix)
    {
        final Vector3f tmp = new Vector3f();
        for ( int i = 0 ; i < result.vertices.length ; i+= result.attributesPerVertex ) {
            // transform vertex coordinates
            matrix.transformPosition(
                result.vertices[i + MeshBuilder.ATTR_VERTEX_X],
                result.vertices[i + MeshBuilder.ATTR_VERTEX_Y],
                result.vertices[i + MeshBuilder.ATTR_VERTEX_Z],tmp
            );

            result.vertices[i + MeshBuilder.ATTR_VERTEX_X] = tmp.x;
            result.vertices[i + MeshBuilder.ATTR_VERTEX_Y] = tmp.y;
            result.vertices[i + MeshBuilder.ATTR_VERTEX_Z] = tmp.z;

            // transform normal vector
            normalMatrix.transformDirection(
                result.vertices[i + MeshBuilder.ATTR_VERTEX_NX],
                result.vertices[i + MeshBuilder.ATTR_VERTEX_NY],
                result.vertices[i + MeshBuilder.ATTR_VERTEX_NZ], tmp );

            result.vertices[i + MeshBuilder.ATTR_VERTEX_NX] = tmp.x;
            result.vertices[i + MeshBuilder.ATTR_VERTEX_NY] = tmp.y;
            result.vertices[i + MeshBuilder.ATTR_VERTEX_NZ] = tmp.z;
        }
        return result;
    }

    /**
     * Transforms this mesh into normalized device coordinates (NDC) in
     * range [-1,1].
     *
     * @param result
     * @param projectionMatrix
     * @return
     */
    public static Mesh transformPerspectiveNDC(Mesh result, Matrix4f projectionMatrix)
    {
        final Vector3f tmp = new Vector3f();
        for ( int i = 0 ; i < result.vertices.length ; i+= result.attributesPerVertex ) {
            // transform vertex coordinates
            projectionMatrix.transformProject(
                result.vertices[i + MeshBuilder.ATTR_VERTEX_X],
                result.vertices[i + MeshBuilder.ATTR_VERTEX_Y],
                result.vertices[i + MeshBuilder.ATTR_VERTEX_Z],tmp
            );
            result.vertices[i + MeshBuilder.ATTR_VERTEX_X] = tmp.x;
            result.vertices[i + MeshBuilder.ATTR_VERTEX_Y] = tmp.y;
            result.vertices[i + MeshBuilder.ATTR_VERTEX_Z] = tmp.z;

        }
        return result;
    }
}