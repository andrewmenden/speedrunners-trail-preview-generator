package org.example;

public class Color {
    float r,g,b,a;

    public Color(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public static Color Multiply(Color a, Color b) {
        return new Color(a.r * b.r, a.g * b.g, a.b * b.b, a.a * b.a);
    }

    public int toRGB() {
        int ia = (int)(a * 255.0);
        int ir = (int)(r * 255.0);
        int ig = (int)(g * 255.0);
        int ib = (int)(b * 255.0);
        return (ia << 24) | (ir << 16) | (ig << 8) | ib;
    }

    static Color Parse(String s) {
        String[] parts = s.split(",");
        if (parts.length == 3) {
            return new Color(Float.parseFloat(parts[0]),
                    Float.parseFloat(parts[1]),
                    Float.parseFloat(parts[2]), 1.0f);
        } else if (parts.length == 4) {
            return new Color(Float.parseFloat(parts[0]),
                    Float.parseFloat(parts[1]),
                    Float.parseFloat(parts[2]),
                    Float.parseFloat(parts[3]));
        } else {
            throw new IllegalArgumentException("Invalid color format: " + s);
        }
    }
}