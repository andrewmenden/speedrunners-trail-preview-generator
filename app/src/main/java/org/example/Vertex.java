package org.example;

public class Vertex {
    Vector2 position;
    Color color;
    Vector2 textureCoordinate;

    Vertex(Vector2 position, Color color, Vector2 textureCoordinate) {
        this.position = position;
        this.color = color;
        this.textureCoordinate = textureCoordinate;
    }

    Vertex(float[] data, int offset) {
        this.position = new Vector2(data[offset], data[offset + 1]);
        this.color = new Color(data[offset + 2], data[offset + 3], data[offset + 4], data[offset + 5]);
        this.textureCoordinate = new Vector2(data[offset + 6], data[offset + 7]);
    }
}
