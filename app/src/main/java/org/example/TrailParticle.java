package org.example;

import java.util.ArrayList;
import java.util.List;

public class TrailParticle {
    // int textureId;

    byte spriteMode; //default, animated, random, sequential
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

    String image;
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

        this.image = "";
        currentFrameIndex = 0;
    }

    public TrailParticle(String image, byte spriteMode, int spriteCountX, int spriteCountY, float fps, float lifetime, boolean fade,
                         Vector2 scale, float scaleSpeed, float rotation, float rotationSpeed, Color color, float opacity,
                         Vector2 position, Vector2 velocity, Vector2 acceleration, int initialFrameIndex,
                         int imageWidth, int imageHeight) {
        // this.textureId = textureId;
        this.image = image;
        this.spriteMode = spriteMode;
        this.spriteCountX = spriteCountX;
        this.spriteCountY = spriteCountY;
        this.fps = fps;
        this.lifetime = lifetime;
        this.totalLifetime = lifetime;
        this.fade = fade;
        this.scale = scale;
        this.scaleSpeed = scaleSpeed;
        this.rotation = rotation;
        this.rotationSpeed = rotationSpeed;
        this.color = color;
        this.opacity = opacity;
        this.acceleration = acceleration;
        this.position = position;
        this.velocity = velocity;

        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;

        spriteWidth = imageWidth / spriteCountX;
        spriteHeight = imageHeight / spriteCountY;

        currentFrameIndex = initialFrameIndex;
        if (spriteMode == 2) { //random
            currentFrameIndex = GetRandomFrameIndex();
        }

        vertices = new ArrayList<>(4);
    }

    public void UpdateVertexBuffer(float deltaTime) {
        vertices = new ArrayList<>(4);
        float alpha = fade ? (lifetime / totalLifetime) : 1.0f;
        alpha *= opacity;
        Color vertexColor = new Color(color.r, color.g, color.b, alpha); //color.a is always 1
        int frameX = currentFrameIndex % spriteCountX;
        int frameY = currentFrameIndex / spriteCountX;
        float u0, v0, u1, v1;
        switch (spriteMode) {
            case 0: //default (display full image, no animation)
                u0 = 0.0f;
                v0 = 0.0f;
                u1 = 1.0f;
                v1 = 1.0f;
                break;
            case 1: //animated
                u0 = frameX * spriteWidth / imageWidth;
                v0 = frameY * spriteHeight / imageHeight;
                u1 = u0 + spriteWidth / imageWidth;
                v1 = v0 + spriteHeight / imageHeight;
                UpdateSpriteFrame(deltaTime);
                break;
            case 2: //random (chosen at initialization, does not change)
            case 3: //sequential (chosen at initialization, changes every frame)
                u0 = frameX * spriteWidth / imageWidth;
                v0 = frameY * spriteHeight / imageHeight;
                u1 = u0 + spriteWidth / imageWidth;
                v1 = v0 + spriteHeight / imageHeight;
                break;
            default:
                throw new IllegalArgumentException("Invalid sprite mode: " + spriteMode);
        }

        float scaledHalfWidth = spriteWidth * scale.x / 2.0f;
        float scaledHalfHeight = spriteHeight * scale.y / 2.0f;

        Vector2 topLeft = new Vector2(-scaledHalfWidth, -scaledHalfHeight);
        topLeft = Vector2.Rotate(topLeft, rotation);
        Vector2 bottomLeft = new Vector2(-topLeft.y, topLeft.x);
        Vector2 topRight = new Vector2(topLeft.y, -topLeft.x);
        Vector2 bottomRight = new Vector2(-topLeft.x, -topLeft.y);

        vertices.add(new Vertex(Vector2.Sum(position, topLeft), vertexColor, new Vector2(u0, v0)));
        vertices.add(new Vertex(Vector2.Sum(position, bottomLeft), vertexColor, new Vector2(u0, v1)));
        vertices.add(new Vertex(Vector2.Sum(position, topRight), vertexColor, new Vector2(u1, v0)));
        vertices.add(new Vertex(Vector2.Sum(position, bottomRight), vertexColor, new Vector2(u1, v1)));
    }

    public void Update(float deltaTime) {
        velocity = Vector2.Sum(velocity, Vector2.Scale(acceleration, deltaTime));
        position = Vector2.Sum(position, Vector2.Scale(velocity, deltaTime));
        rotation += rotationSpeed * deltaTime;
        scale = Vector2.Sum(scale, Vector2.Scale(new Vector2(scaleSpeed, scaleSpeed), deltaTime));
        lifetime -= deltaTime;
    }

    private int GetRandomFrameIndex() {
        return (int)(Math.random() * spriteCountX * spriteCountY);
    }

    private void UpdateSpriteFrame(float deltaTime) {
        timeSinceLastFrame += deltaTime;
        float frameDuration = 1.0f / fps;
        while (timeSinceLastFrame >= frameDuration) {
            timeSinceLastFrame -= frameDuration;
            currentFrameIndex = (currentFrameIndex + 1) % (spriteCountX * spriteCountY);
        }
    }

}
