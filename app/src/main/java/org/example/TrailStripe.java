package org.example;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Vector;

public class TrailStripe {

    class TrailStripeVertex {
        Vector2 position;
        float lifetime;
    }

    //settings
    byte enabled; //0 = never, 1 = always, 2 = only at superspeed, 3 = not at superspeed
    byte order;
    String layer; //TODO
    String image;
    boolean visible;
    float lifetime;
    Color color;
    boolean taper;
    boolean fade;
    float fadeOutSpeed; //TODO
    float width;
    Vector2 offset;
    boolean invertOffset; //TODO
    boolean flipHorizontal;
    boolean flipVertical;
    boolean sameSideUp; //TODO
    float noiseAmplitude;
    float waveAmplitude;
    float waveFrequency;
    float wavePhaseOffset;

    Vector<Vertex> vertices;
    Vector<TrailStripeVertex> trailVertices;
    BufferedImage texture;

    TrailStripe() {
        this.trailVertices = new Vector<>();
        this.vertices = new Vector<>();

        enabled = 0;
        order = 0;
        layer = "ObjectLayer";
        image = "";
        visible = true;
        lifetime = 2.0f;
        color = new Color(1.0f, 1.0f, 1.0f, 1.0f);
        taper = false;
        fade = false;
        fadeOutSpeed = 1.0f;
        width = 40.0f;
        offset = new Vector2(0, 0);
        invertOffset = true;
        flipHorizontal = false;
        flipVertical = false;
        sameSideUp = false;
        noiseAmplitude = 0;
        waveAmplitude = 0;
        waveFrequency = 0;
        wavePhaseOffset = 0;
    }

    public float Noise(float x) {
        return (float)Math.sin(x);
    }

    public void AddPoint(Vector2 position, Vector2 velocity, float gameTime) {
        TrailStripeVertex newVertex = new TrailStripeVertex();
        newVertex.lifetime = this.lifetime;

        float sin = this.waveAmplitude
                * (float)Math.sin((this.waveFrequency * gameTime + this.wavePhaseOffset));
        Vector2 normal = Vector2.Normalize(velocity);
        Vector2 adjustedOffset = Vector2.Transform(offset, normal);
        normal = new Vector2(normal.y, -normal.x);

        Vector2 adjusted = Vector2.Sum(
                position,
                adjustedOffset,
                Vector2.Scale(normal, sin));
        newVertex.position = adjusted;

        trailVertices.add(newVertex);
    }

    private Vector2 CalculateNormal(int index) {
        Vector2 previous = trailVertices.get(Math.max(0, index - 1)).position;
        Vector2 next = trailVertices.get(Math.min(trailVertices.size() - 1, index + 1)).position;
        Vector2 velocity = Vector2.Normalize(Vector2.Subtract(next, previous));
        return new Vector2(velocity.y, -velocity.x);
    }

    public void UpdateVertexBuffer() {
        vertices = new Vector<>(trailVertices.size() * 2);

        float length = 0;
        for (int i = 0; i < trailVertices.size() - 1; i++) {
            length += Vector2.Distance(trailVertices.get(i).position, trailVertices.get(i + 1).position);
        }

        if (length == 0) {
            return;
        }

        float accumulatedLength = 0;
        for (int i = 0; i < trailVertices.size(); i++) {
            float t = accumulatedLength / length;
            accumulatedLength += (i < trailVertices.size() - 1)
                    ? Vector2.Distance(trailVertices.get(i).position, trailVertices.get(i + 1).position)
                    : 0;
            float effectiveWidth = this.width;
            if (this.taper) {
                effectiveWidth *= (t);
            }
            float effectiveAlpha = this.color.a;
            if (this.fade) {
                effectiveAlpha *= (t);
            }
            float u = (float)i / (trailVertices.size() - 1);
            if (flipHorizontal) {
                u = 1 - u;
            }
            float v = flipVertical ? 1 : 0;

            Vector2 normal = CalculateNormal(i);
            TrailStripeVertex vertex = trailVertices.get(i);

            float noise = Noise(vertex.position.x);
            Vector2 offset = Vector2.Scale(normal, noise * this.noiseAmplitude);

            Vector2 v1 = Vector2.Sum(
                    vertex.position,
                    Vector2.Scale(normal, effectiveWidth / 2f),
                    offset);

            Vector2 v2 = Vector2.Sum(
                    vertex.position,
                    Vector2.Scale(normal, -effectiveWidth / 2f),
                    offset);

            Color color = new Color(this.color.r, this.color.g, this.color.b,
                    effectiveAlpha);

            Vertex v1Vertex = new Vertex(v1, color, new Vector2(u, v));
            Vertex v2Vertex = new Vertex(v2, color, new Vector2(u, 1 - v));

            vertices.add(v1Vertex);
            vertices.add(v2Vertex);
        }
    }

    public void Update(float deltaTime) {
        for (TrailStripeVertex vertex : trailVertices) {
            vertex.lifetime -= deltaTime;
        }
        trailVertices.removeIf(vertex -> vertex.lifetime <= 0);
        UpdateVertexBuffer();
    }

    public String ToString() {
        return String.format(
            "enabled: %d, order: %d, layer: %s, image: %s, visible: %b, lifetime: %f, color: %f, %f, %f, %f, taper: %b, fade: %b, fadeOutSpeed: %f, width: %f, offset: %f, %f, invertOffset: %b, flipHorizontal: %b, flipVertical: %b, sameSideUp: %b, noiseAmplitude: %f, waveAmplitude: %f, waveFrequency: %f, wavePhaseOffset: %f",
            enabled, order, layer, image, visible, lifetime,
            color.r, color.g, color.b, color.a,
            taper, fade, fadeOutSpeed, width,
            offset.x, offset.y, invertOffset, flipHorizontal, flipVertical, sameSideUp,
            noiseAmplitude, waveAmplitude, waveFrequency, wavePhaseOffset
        );
    }

    public void LoadFromHashMap(HashMap<String, String> properties) {
        String enabledString = properties.getOrDefault("Enabled", "NEVER");
        String orderString = properties.getOrDefault("Order", "0");
        layer = properties.getOrDefault("Layer", "ObjectLayer");
        String imageString = properties.getOrDefault("Image", "");
        String visibleString = properties.getOrDefault("Visible", "TRUE");
        String lifeTimeString = properties.getOrDefault("LifeTime", "2");
        String[] colorString = properties.getOrDefault("Color", "1,1,1").split(",");
        String taperString = properties.getOrDefault("Taper", "FALSE");
        String fadeOutString = properties.getOrDefault("FadeOut", "FALSE");
        String fadeOutSpeedString = properties.getOrDefault("FadeOut Speed", "1");
        String opacityString = properties.getOrDefault("Opacity", "1");
        String sizeString = properties.getOrDefault("Size", "40");
        // String offsetString = properties.getOrDefault("Offset", "0"); //unused?
        // String xOffsetString = properties.getOrDefault("X-Offset", "0"); //unused?
        String[] offsetVectorString = properties.getOrDefault("OffsetVector", "0,0").split(",");
        String invertOffsetString = properties.getOrDefault("Invert Offset", "TRUE");
        String flipHorizontallyString = properties.getOrDefault("Flip Horizontally", "FALSE");
        String flipVerticallyString = properties.getOrDefault("Flip Vertically", "FALSE");
        String forceRightSideUpString = properties.getOrDefault("Force right side Up", "FALSE");
        String noiseString = properties.getOrDefault("Noise", "0");
        String sinewaveAmplitudeString = properties.getOrDefault("Sinewave Amplitude", "0");
        String sinewaveFrequencyString = properties.getOrDefault("Sinewave Frequency", "0");
        String sinePhaseOffsetString = properties.getOrDefault("Sine Phase Offset", "0");

        switch (enabledString.toUpperCase()) {
            case "ALWAYS":
                enabled = 1;
                break;
            case "ONLY AT SUPERSPEED":
                enabled = 2;
                break;
            case "NOT AT SUPERSPEED":
                enabled = 3;
                break;
            default:
                enabled = 0;
        }

        order = Byte.parseByte(orderString);
        image = imageString;
        visible = visibleString.equalsIgnoreCase("TRUE");
        lifetime = Float.parseFloat(lifeTimeString);
        color = new Color(Float.parseFloat(colorString[0]), Float.parseFloat(colorString[1]), Float.parseFloat(colorString[2]), Float.parseFloat(opacityString));
        taper = taperString.equalsIgnoreCase("TRUE");
        fade = fadeOutString.equalsIgnoreCase("TRUE");
        fadeOutSpeed = Float.parseFloat(fadeOutSpeedString);
        width = Float.parseFloat(sizeString);
        offset = new Vector2(Float.parseFloat(offsetVectorString[0]), Float.parseFloat(offsetVectorString[1]));
        invertOffset = invertOffsetString.equalsIgnoreCase("TRUE");
        flipHorizontal = flipHorizontallyString.equalsIgnoreCase("TRUE");
        flipVertical = flipVerticallyString.equalsIgnoreCase("TRUE");
        sameSideUp = forceRightSideUpString.equalsIgnoreCase("TRUE");
        noiseAmplitude = Float.parseFloat(noiseString);
        waveAmplitude = Float.parseFloat(sinewaveAmplitudeString);
        waveFrequency = Float.parseFloat(sinewaveFrequencyString);
        wavePhaseOffset = Float.parseFloat(sinePhaseOffsetString);
    }
}