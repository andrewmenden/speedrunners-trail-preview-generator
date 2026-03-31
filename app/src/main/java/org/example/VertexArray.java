package org.example;

public class VertexArray {
    private float[] vertices;
    int vertexCount;
    int index;

    public VertexArray() {
        vertices = new float[0];
        this.index = 0;
        this.vertexCount = 0;
    }

    public VertexArray(int size) {
        vertices = new float[size];
        this.index = 0;
        this.vertexCount = 0;
    }

    public void SetSize(int size) {
        vertices = new float[size];
        this.index = 0;
        this.vertexCount = 0;
    }

    public float[] GetVertices() {
        return vertices;
    }

    public Vertex GetVertex(int i) {
        return new Vertex(vertices, i * 8);
    }
    
    public void AddVertex(Vertex vertex) {
        if (index + 8 >= vertices.length) {
            float[] newVertices = new float[vertices.length * 2];
            System.arraycopy(vertices, 0, newVertices, 0, vertices.length);
            vertices = newVertices;
        }

        vertices[index++] = vertex.position.x;
        vertices[index++] = vertex.position.y;
        vertices[index++] = vertex.color.r;
        vertices[index++] = vertex.color.g;
        vertices[index++] = vertex.color.b;
        vertices[index++] = vertex.color.a;
        vertices[index++] = vertex.textureCoordinate.x;
        vertices[index++] = vertex.textureCoordinate.y;
        vertexCount++;
    }

    public void AddVertices(java.util.List<Vertex> vertices) {
        for (Vertex vertex : vertices) {
            AddVertex(vertex);
        }
    }
}
