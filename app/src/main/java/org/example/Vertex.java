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
}
