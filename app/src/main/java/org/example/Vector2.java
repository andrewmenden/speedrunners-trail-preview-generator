package org.example;

public class Vector2 {
    float x, y;

    public void Set(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vector2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    static Vector2 Normalize(Vector2 x) {
        float length = Length(x);
        if (length == 0) {
            return new Vector2(0,0);
        } else {
            return new Vector2(x.x / length, x.y / length);
        }
    }

    static float Length(Vector2 x) {
        return (float)Math.sqrt(x.x * x.x + x.y * x.y);
    }

    static Vector2 Sum(Vector2... a) {
        float sumX = 0;
        float sumY = 0;
        for (Vector2 v : a) {
            sumX += v.x;
            sumY += v.y;
        }
        return new Vector2(sumX, sumY);
    }

    static Vector2 Subtract(Vector2 a, Vector2 b) {
        return new Vector2(a.x - b.x, a.y - b.y);
    }

    static Vector2 Scale(Vector2 a, float scalar) {
        return new Vector2(a.x * scalar, a.y * scalar);
    }

    static Vector2 Rotate(Vector2 a, float angle) {
        float cos = (float)Math.cos(angle);
        float sin = (float)Math.sin(angle);
        return new Vector2(a.x * cos - a.y * sin, a.x * sin + a.y * cos);
    }

    static Vector2 Transform(Vector2 vec, Vector2 normal) {
        float angle = (float)Math.atan2(normal.y, normal.x);
        return Rotate(vec, angle);
    }

    static float Distance(Vector2 a, Vector2 b) {
        return Length(Subtract(a, b));
    }

    static Vector2 Parse(String s) {
        String[] parts = s.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid Vector2 format: " + s);
        }
        return new Vector2(Float.parseFloat(parts[0]), Float.parseFloat(parts[1]));
    }
}