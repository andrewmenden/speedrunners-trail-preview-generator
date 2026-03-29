package org.example;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class TrailParticleEmitter {
    List<TrailParticle> particles;
    
    //trail settings
    byte enabled; //0 = never, 1 = always, 2 = only at superspeed, 3 = not at superspeed
    byte order;
    String layer; //TODO
    String image;
    boolean visible;
    boolean isAnimated;
    byte spriteMode; //probably default, ping pong, once then stop (?)
    Vector2 spriteSize;
    Vector2 spriteCount;
    int spriteFPS;
    float spawnRate; //seconds between spawns
    int amount;
    float lifetime;
    boolean fade;
    float scale;
    float scaleSpeed;
    float scaleVariance;
    float rotation;
    float rotatationVariance;
    float rotationSpeed;
    float rotationSpeedVariance;
    boolean rotateWithPlayer;
    Color color;
    float alpha;
    Vector2 offset;
    Vector2 offsetVariance;
    float force;
    float forceVariance;
    Vector2 direction;
    Vector2 directionVariance;
    boolean useWorldAxis;
    boolean sameSideUp;
    boolean hasGravity;
    Vector2 gravityStrength;
    // boolean isBetaTrail;

    float timeSinceLastSpawn;
    Random random;
    int imageWidth;
    int imageHeight;

    int sequentialFrameIndex; //for sequential sprite mode

    List<Vertex> vertices; //list of quads for all particles-- for one draw call per emitter rather than per particle

    //not sure how the game does this, but this is probably good enough
    //return -1 to 1
    private float Noise() {
        return (random.nextFloat() - 0.5f) * 2;
    }

    public TrailParticleEmitter() {
        particles = new java.util.ArrayList<>();
        random = new Random();
        image = "";
        timeSinceLastSpawn = 0;
        sequentialFrameIndex = 0;
        vertices = new java.util.ArrayList<>();

        enabled = 0;
        order = 0;
        layer = "ObjectLayer";
        image = "";
        imageWidth = 0;
        imageHeight = 0;
        visible = true;
        isAnimated = false;
        spriteMode = 0;
        spriteSize = new Vector2(100, 100);
        spriteCount = new Vector2(1, 1);
        spriteFPS = 30;
        spawnRate = 0.25f;
        amount = 1;
        lifetime = 1.0f;
        fade = true;
        scale = 1.0f;
        scaleSpeed = -1.0f;
        scaleVariance = 0.5f;
        rotation = 0.0f;
        rotatationVariance = 0.0f;
        rotationSpeed = 0.0f;
        rotationSpeedVariance = 0.0f;
        rotateWithPlayer = false;
        color = new Color(1.0f, 1.0f, 1.0f, 1.0f);
        alpha = 1.0f;
        offset = new Vector2(0, 0);
        offsetVariance = new Vector2(0, 0);
        force = 0.0f;
        forceVariance = 0.0f;
        direction = new Vector2(0, 0);
        directionVariance = new Vector2(0, 0);
        useWorldAxis = false;
        sameSideUp = false;
        hasGravity = false;
        gravityStrength = new Vector2(0, 0);
    }

    public TrailParticleEmitter(HashMap<String, String> properties, int imageWidth, int imageHeight) {
        this();
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        LoadFromHashMap(properties);
    }

    public void Update(Vector2 playerPosition, Vector2 playVelocity, float deltaTime) {
        timeSinceLastSpawn += deltaTime;

        if (enabled == 0) return;
        if (enabled == 2 && playVelocity.x < Trail.SUPERSPEED_THRESHOLD) return;
        if (enabled == 3 && playVelocity.x >= Trail.SUPERSPEED_THRESHOLD) return;

        if (timeSinceLastSpawn >= spawnRate) {
            timeSinceLastSpawn -= spawnRate;
            for (int i = 0; i < amount; i++) {
                Emit(playerPosition, playVelocity);
            }
        }

        Update(deltaTime);
    }

    private float DegreesToRadians(float degrees) {
        return degrees * (float)Math.PI / 180.0f;
    }

    public void Emit(Vector2 playerPosition, Vector2 playerVelocity) {
        float scaleA = scale + Noise() * scaleVariance;
        float rotationA = rotation + Noise() * rotatationVariance;
        float rotationSpeedA = rotationSpeed + Noise() * rotationSpeedVariance;
        float playerRotation = (float)Math.atan2(playerVelocity.y, playerVelocity.x) + (playerVelocity.x < 0 ? (sameSideUp ? (float)Math.PI : -(float)Math.PI/2.0f) : 0);
        float offsetXA = offset.x + Noise() * offsetVariance.x;
        float offsetYA = offset.y + Noise() * offsetVariance.y;
        float forceA = force + Noise() * forceVariance;
        float directionXA = direction.x + Noise() * directionVariance.x;
        float directionYA = direction.y + Noise() * directionVariance.y;
        Vector2 worldDirection = Vector2.Normalize(new Vector2(directionXA, directionYA));
        Vector2 playerDirection = Vector2.Normalize(playerVelocity); //not really a normal
        // playerNormal = new Vector2(playerNormal.y, -playerNormal.x); //perpendicular to velocity

        rotationA = DegreesToRadians(rotationA);
        rotationSpeedA = DegreesToRadians(rotationSpeedA);

        float initialRotation = rotateWithPlayer ? rotationA + playerRotation : rotationA;

        Vector2 initialPosition = new Vector2(playerPosition.x + offsetXA, playerPosition.y + offsetYA);
        Vector2 initialVelocity = useWorldAxis ? Vector2.Scale(worldDirection, forceA) : Vector2.Scale(playerDirection, forceA);
        Vector2 initialAcceleration = hasGravity ? gravityStrength : new Vector2(0, 0);

        int frameIndex = 0;
        if (spriteMode == 3) { //sequential, so determine frame index at spawn time
            frameIndex = sequentialFrameIndex;
            sequentialFrameIndex = (sequentialFrameIndex + 1) % (int)(spriteCount.x * spriteCount.y);
        }

        TrailParticle newParticle = new TrailParticle(
            image,
            spriteMode,
            (int)spriteCount.x,
            (int)spriteCount.y,
            spriteFPS,
            lifetime,
            fade,
            new Vector2(scaleA, scaleA),
            scaleSpeed,
            initialRotation,
            rotationSpeedA,
            color,
            alpha,
            initialPosition,
            initialVelocity,
            initialAcceleration,
            frameIndex,
            imageWidth,
            imageHeight
        );

        particles.add(newParticle);
    }

    private void Update(float deltaTime) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            TrailParticle particle = particles.get(i);
            particle.Update(deltaTime);
            if (particle.lifetime <= 0) {
                particles.remove(i);
            } else if (particle.scale.x <= 0 || particle.scale.y <= 0) {
                particles.remove(i);
            }
        }

        //update vertex buffer for all particles
        vertices = new java.util.ArrayList<>(particles.size() * 4); //each particle is a quad (4 vertices)
        for (TrailParticle particle : particles) {
            particle.UpdateVertexBuffer(deltaTime);
            vertices.addAll(particle.vertices);
        }
    }

    public void LoadFromHashMap(HashMap<String, String> properties) {
        String enabledString = properties.getOrDefault("Enabled", "NEVER");
        String spriteModeString = properties.getOrDefault("spriteMode", "DEFAULT");

        switch (enabledString.toUpperCase()) {
            case "NEVER" -> enabled = 0;
            case "ALWAYS" -> enabled = 1;
            case "ONLY AT SUPERSPEED" -> enabled = 2;
            case "NOT AT SUPERSPEED" -> enabled = 3;
            default -> throw new IllegalArgumentException("Invalid Enabled value: " + enabledString);
        }

        switch (spriteModeString.toUpperCase()) {
            case "DEFAULT" -> spriteMode = 0;
            case "ANIMATED" -> spriteMode = 1;
            case "RANDOM" -> spriteMode = 2;
            case "SEQUENTIAL" -> spriteMode = 3;
            default -> throw new IllegalArgumentException("Invalid sprite mode value: " + spriteModeString);
        }

        //enabled
        order = Byte.parseByte(properties.getOrDefault("Order", "0"));
        layer = properties.getOrDefault("Layer", "TrailBehindLocalPlayersLayer");
        image = properties.getOrDefault("Image", "");
        visible = Boolean.parseBoolean(properties.getOrDefault("Visible", "TRUE"));
        isAnimated = Boolean.parseBoolean(properties.getOrDefault("isAnimated", "FALSE"));
        //sprite mode
        spriteSize = Vector2.Parse(properties.getOrDefault("SpriteSize", "100,100"));
        spriteCount = Vector2.Parse(properties.getOrDefault("SpriteCount", "1,1"));
        spriteFPS = Integer.parseInt(properties.getOrDefault("FPS", "30"));
        spawnRate = Float.parseFloat(properties.getOrDefault("Spawn Rate", "0.25"));
        amount = Integer.parseInt(properties.getOrDefault("Amount", "1"));
        lifetime = Float.parseFloat(properties.getOrDefault("LifeTime", "1"));
        fade = Boolean.parseBoolean(properties.getOrDefault("FadeOut", "TRUE"));
        scale = Float.parseFloat(properties.getOrDefault("Scale", "1"));
        scaleSpeed = Float.parseFloat(properties.getOrDefault("ScaleSpeed", "-1"));
        scaleVariance = Float.parseFloat(properties.getOrDefault("Scale Variance", "0.5"));
        rotation = Float.parseFloat(properties.getOrDefault("Rotation", "0"));
        rotatationVariance = Float.parseFloat(properties.getOrDefault("Rotation Variance", "0"));
        rotationSpeed = Float.parseFloat(properties.getOrDefault("Rotation Speed", "0"));
        rotationSpeedVariance =
            Float.parseFloat(properties.getOrDefault("Rotation Speed Variance", "0"));
        rotateWithPlayer =
            Boolean.parseBoolean(properties.getOrDefault("Rotate with Player", "FALSE"));
        color = Color.Parse(properties.getOrDefault("Color", "1,1,1"));
        alpha = Float.parseFloat(properties.getOrDefault("Opacity", "1"));
        offset = Vector2.Parse(properties.getOrDefault("Offset", "0,0"));
        offsetVariance =
            Vector2.Parse(properties.getOrDefault("OffsetVariance", "0,0"));
        force = Float.parseFloat(properties.getOrDefault("Force", "0"));
        forceVariance =
            Float.parseFloat(properties.getOrDefault("Force Variance", "0"));
        direction = Vector2.Parse(properties.getOrDefault("Direction", "0,0"));
        directionVariance =
            Vector2.Parse(properties.getOrDefault("Direction Variance", "0,0"));
        useWorldAxis = Boolean.parseBoolean(properties.getOrDefault("Use World Axis", "false"));
        sameSideUp = Boolean.parseBoolean(properties.getOrDefault("Same Side Up", "false"));
        hasGravity = Boolean.parseBoolean(properties.getOrDefault("hasGravity", "false"));
        gravityStrength = Vector2.Parse(properties.getOrDefault("gravity", "0,0"));
        // isBetaTrail = Boolean.parseBoolean(properties.getOrDefault("Is Beta Trail", "false"));
    }
}
