package de.codesourcery.robosim.render;

import java.util.Arrays;

public class Mesh
{
    public int attributesPerVertex = 3;
    public float[] vertices;
    public float[] normals;

    public Mesh(int attributesPerVertex, float[] normals, float[] vertices)
    {
        this.attributesPerVertex = attributesPerVertex;
        this.normals = normals;
        this.vertices = vertices;
    }

    public Mesh(int attributesPerVertex) {
        final int triangles = 6 * 2;
        final int vertexCnt = triangles * 3;
        vertices = new float[ vertexCnt * attributesPerVertex];
        normals  = new float[ triangles * 3 ];
    }

    public Mesh(Mesh other)
    {
        this.attributesPerVertex = other.attributesPerVertex;
        //noinspection IncompleteCopyConstructor
        this.vertices = Arrays.copyOfRange(other.vertices,0,other.vertices.length);
        //noinspection IncompleteCopyConstructor
        this.normals = Arrays.copyOfRange(other.normals,0,other.normals.length);
    }
}
