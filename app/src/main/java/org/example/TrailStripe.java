package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TrailStripe extends TrailLayer {

    class TrailStripeVertex {
        Vector2 position;
        float lifetime;
        boolean isNewStart;
    }

    class Segment {
        int startIndex;
        int endIndex;
    }

    //settings
    byte enabled; //0 = never, 1 = always, 2 = only at superspeed, 3 = not at superspeed
    String layer; //unused
    boolean visible;
    float lifetime;
    Color color;
    float opacity;
    boolean taper;
    boolean fade;
    float fadeOutSpeed; //unused-- likely won't implement since it's honestly uglier than doing nothing
    float width;
    Vector2 offset;
    boolean invertOffset;
    boolean flipHorizontal;
    boolean flipVertical;
    boolean sameSideUp;
    float noiseAmplitude;
    float waveAmplitude;
    float waveFrequency;
    float wavePhaseOffset;

    List<TrailStripeVertex> trailVertices;
    List<Integer> newStartIndices;

    boolean lastPointNotAdded = true;

    TrailStripe() {
        super(0, (byte)0);
        this.trailVertices = new ArrayList<>();
        this.newStartIndices = new ArrayList<>();

        enabled = 0;
        layer = "TrailBehindLocalPlayersLayer";
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
        if (enabled == Trail.ENABLED_NEVER) {
            lastPointNotAdded = true;
            return;
        } else if (enabled == Trail.ENABLED_ONLY_SUPERSPEED && Trail.CalculateSpeed(velocity) < Trail.SUPERSPEED_THRESHOLD) {
            lastPointNotAdded = true;
            return;
        } else if (enabled == Trail.ENABLED_NOT_SUPERSPEED && Trail.CalculateSpeed(velocity) >= Trail.SUPERSPEED_THRESHOLD) {
            lastPointNotAdded = true;
            return;
        } else if (enabled == Trail.ENABLED_ALWAYS && Vector2.Length(velocity) < Trail.AFTERIMAGE_THRESHOLD) {
            lastPointNotAdded = true;
            return;
        }

        TrailStripeVertex newVertex = new TrailStripeVertex();
        newVertex.lifetime = this.lifetime;
        newVertex.isNewStart = false;

        if (lastPointNotAdded) {
            newVertex.isNewStart = true;
        }

        lastPointNotAdded = false;

        float sin = this.waveAmplitude
                * (float)Math.sin((this.waveFrequency * gameTime + this.wavePhaseOffset));
        Vector2 normal = Vector2.Normalize(velocity);
        Vector2 adjustedOffset = new Vector2(offset.x, -offset.y);
        adjustedOffset.y *= !invertOffset && velocity.x > 0 ? -1 : 1;
        adjustedOffset = Vector2.Transform(adjustedOffset, normal);
        normal = new Vector2(normal.y, -normal.x);

        Vector2 adjusted = Vector2.Sum(
                position,
                adjustedOffset,
                Vector2.Scale(normal, sin));
        newVertex.position = adjusted;

        trailVertices.add(newVertex);
    }

    private Vector2 CalculateNormal(int index) {
        Vector2 previous;
        Vector2 next;
        previous = trailVertices.get(Math.max(0, index - 1)).position;
        next = trailVertices.get(Math.min(trailVertices.size() - 1, index + 1)).position;
        if (trailVertices.get(index).isNewStart) {
            previous = trailVertices.get(index).position;
        }
        if (index < trailVertices.size() - 1 && trailVertices.get(index + 1).isNewStart) {
            next = trailVertices.get(index).position;
        }
        Vector2 velocity = Vector2.Normalize(Vector2.Subtract(next, previous));
        return new Vector2(velocity.y, -velocity.x);
    }

    public void UpdateVertexBuffer() {
        vertexArray.SetSize(trailVertices.size() * 2);
        newStartIndices.clear();

        float length = 0;
        for (int i = 0; i < trailVertices.size() - 1; i++) {
            length += Vector2.Distance(trailVertices.get(i).position, trailVertices.get(i + 1).position);
        }

        if (length == 0) {
            return;
        }

        float accumulatedLength = 0;
        for (int i = 0; i < trailVertices.size(); i++) {
            if (trailVertices.get(i).isNewStart) {
                newStartIndices.add(i);
            }
            Vector2 normal = CalculateNormal(i);

            float t = accumulatedLength / length;
            accumulatedLength += (i < trailVertices.size() - 1)
                    ? Vector2.Distance(trailVertices.get(i).position, trailVertices.get(i + 1).position)
                    : 0;
            float effectiveWidth = this.width;
            if (this.taper) {
                effectiveWidth *= (t);
            }
            float effectiveAlpha = this.color.a * this.opacity;
            if (this.fade) {
                effectiveAlpha *= (t);
            }
            float u = (float)i / (trailVertices.size() - 1);
            if (flipHorizontal) {
                u = 1 - u;
            }
            float v = flipVertical ? 1 : 0;

            if (sameSideUp && normal.y > 0) {
                v = 1 - v;
            }

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

            vertexArray.AddVertex(v1Vertex);
            vertexArray.AddVertex(v2Vertex);
        }
    }

    public int GetSegmentCount() {
        return newStartIndices.size();
    }

    public Segment GetSegment(int index) {
        if (index >= newStartIndices.size()) {
            throw new IndexOutOfBoundsException();
        }
        Segment segment = new Segment();
        segment.startIndex = newStartIndices.get(index) * 2; //each vertex has 2 vertices in the vertex array
        if (index == newStartIndices.size() - 1) {
            segment.endIndex = vertexArray.vertexCount;
        } else {
            segment.endIndex = newStartIndices.get(index + 1) * 2;
        }
        return segment;
    }

    @Override
    public void Update(float deltaTime, Vector2 position, Vector2 velocity) {
        for (int i = trailVertices.size() - 1; i >= 0; i--) {
            TrailStripeVertex vertex = trailVertices.get(i);
            vertex.lifetime -= deltaTime;
            if (vertex.lifetime <= 0) {
                if (vertex.isNewStart) {
                    //transfer to next vertex
                    if (i + 1 < trailVertices.size()) {
                        trailVertices.get(i + 1).isNewStart = true;
                    }
                }
                trailVertices.remove(i);
            }
        }
        UpdateVertexBuffer();
    }

    @Override
    public void Reset() {
        trailVertices.clear();
        newStartIndices.clear();
        lastPointNotAdded = true;
    }

    @Override
    public void LoadFromHashMap(HashMap<String, String> properties) {
        layer = properties.getOrDefault("Layer", "TrailBehindLocalPlayersLayer");

        switch (properties.getOrDefault("Enabled", "NEVER").toUpperCase()) {
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

        order = Byte.parseByte(properties.getOrDefault("Order", "0"));
        image = properties.getOrDefault("Image", "");
        visible = properties.getOrDefault("Visible", "TRUE").equalsIgnoreCase("TRUE");
        lifetime = Float.parseFloat(properties.getOrDefault("LifeTime", "2"));
        color = Color.Parse(properties.getOrDefault("Color", "1,1,1"));
        opacity = Float.parseFloat(properties.getOrDefault("Opacity", "1"));
        taper = properties.getOrDefault("Taper", "FALSE").equalsIgnoreCase("TRUE");
        fade = properties.getOrDefault("FadeOut", "FALSE").equalsIgnoreCase("TRUE");
        fadeOutSpeed = Float.parseFloat(properties.getOrDefault("FadeOut Speed", "1"));
        width = Float.parseFloat(properties.getOrDefault("Size", "40"));
        offset = Vector2.Parse(properties.getOrDefault("OffsetVector", "0,0"));
        invertOffset = properties.getOrDefault("Invert Offset", "TRUE").equalsIgnoreCase("TRUE");
        flipHorizontal = properties.getOrDefault("Flip Horizontally", "FALSE").equalsIgnoreCase("TRUE");
        flipVertical = properties.getOrDefault("Flip Vertically", "FALSE").equalsIgnoreCase("TRUE");
        sameSideUp = properties.getOrDefault("Force right side Up", "FALSE").equalsIgnoreCase("TRUE");
        noiseAmplitude = Float.parseFloat(properties.getOrDefault("Noise", "0"));
        waveAmplitude = Float.parseFloat(properties.getOrDefault("Sinewave Amplitude", "0"));
        waveFrequency = Float.parseFloat(properties.getOrDefault("Sinewave Frequency", "0"));
        wavePhaseOffset = Float.parseFloat(properties.getOrDefault("Sine Phase Offset", "0"));
    }
}