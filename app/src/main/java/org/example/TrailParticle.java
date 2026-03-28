package org.example;

import java.util.ArrayList;
import java.util.List;

public class TrailParticle {
    int textureId;

    byte spriteMode; //TODO
    int spriteCountX;
    int spriteCountY;
    float fps;
    float lifetime; float totalLifetime;
    boolean fade;
    Vector2 scale;
    float scaleSpeed;
    float rotation;
    float rotationSpeed;
    Color color;
    float opacity;

    Vector2 acceleration; //gravity
    Vector2 velocity;
    Vector2 position;

    int imageWidth;
    int imageHeight;

    // precalculated from spriteCountX/Y and imageWidth/Height
    float spriteWidth;
    float spriteHeight;
    float timeSinceLastFrame;

    int currentFrameIndex;

    List<Vertex> vertices;

    public TrailParticle() {
        vertices = new ArrayList<>(4);

        spriteMode = 0;
        spriteCountX = 1;
        spriteCountY = 1;
        fps = 30;
        lifetime = 1.0f;
        totalLifetime = 1.0f;
        fade = false;
        scale = new Vector2(1, 1);
        scaleSpeed = 0;
        rotation = 0;
        rotationSpeed = 0;
        color = new Color(1, 1, 1, 1);
        opacity = 1;

        acceleration = new Vector2(0, 0);
        velocity = new Vector2(0, 0);
        position = new Vector2(0, 0);

        imageWidth = 1;
        imageHeight = 1;

        spriteWidth = imageWidth / spriteCountX;
        spriteHeight = imageHeight / spriteCountY;

        currentFrameIndex = 0;
    }

    public void UpdateVertexBuffer() {
        vertices = new ArrayList<>();
        float alpha = fade ? (lifetime / totalLifetime) : 1.0f;
        alpha *= opacity;
        Color vertexColor = new Color(color.r, color.g, color.b, alpha); //color.a is always 1
        int frameX = currentFrameIndex % spriteCountX;
        int frameY = currentFrameIndex / spriteCountX;
        float u0 = frameX * spriteWidth / imageWidth;
        float v0 = frameY * spriteHeight / imageHeight;
        float u1 = u0 + spriteWidth / imageWidth;
        float v1 = v0 + spriteHeight / imageHeight;

        float scaledHalfWidth = spriteWidth * scale.x / 2.0f;
        float scaledHalfHeight = spriteHeight * scale.y / 2.0f;

        Vector2 topLeft = new Vector2(-scaledHalfWidth, -scaledHalfHeight);
        Vector2.Rotate(topLeft, rotation);

        Vector2 bottomLeft = new Vector2(topLeft.x, -topLeft.y);
        Vector2 topRight = new Vector2(-topLeft.x, topLeft.y);
        Vector2 bottomRight = new Vector2(topLeft.x, topLeft.y);

        vertices.set(0, new Vertex(Vector2.Sum(position, topLeft), vertexColor, new Vector2(u0, v0)));
        vertices.set(1, new Vertex(Vector2.Sum(position, bottomLeft), vertexColor, new Vector2(u0, v1)));
        vertices.set(2, new Vertex(Vector2.Sum(position, topRight), vertexColor, new Vector2(u1, v0)));
        vertices.set(3, new Vertex(Vector2.Sum(position, bottomRight), vertexColor, new Vector2(u1, v1)));
    }

    public void Update(float deltaTime) {
        UpdateSpriteFrame(deltaTime);
        velocity = Vector2.Sum(velocity, Vector2.Scale(acceleration, deltaTime));
        position = Vector2.Sum(position, Vector2.Scale(velocity, deltaTime));
        rotation += rotationSpeed * deltaTime;
        lifetime -= deltaTime;
    }

    private void UpdateSpriteFrame(float deltaTime) {
        timeSinceLastFrame += deltaTime;
        float frameDuration = 1.0f / fps;
        while (timeSinceLastFrame >= frameDuration) {
            timeSinceLastFrame -= frameDuration;
            currentFrameIndex = (currentFrameIndex + 1) % (spriteCountX * spriteCountY);
        }
    }

    private void NextFrame() {
        //TODO: support for different sprite modes
    }
}
