package org.example;

import java.awt.image.BufferedImage;

public class RendererCpu {

    private float EdgeFunction(Vector2 a, Vector2 b, Vector2 c) {
        return (float)((c.x - a.x) * (b.y - a.y) - (c.y - a.y) * (b.x - a.x));
    }

    private float[] CalculateInterpolationFactors(Vertex v1, Vertex v2, Vertex v3, Vector2 p) {
        float area = EdgeFunction(v1.position, v2.position, v3.position);
        float w0 = EdgeFunction(v2.position, v3.position, p) / area;
        float w1 = EdgeFunction(v3.position, v1.position, p) / area;
        float w2 = EdgeFunction(v1.position, v2.position, p) / area;
        return new float[]{w0, w1, w2};
    }

    private int Min(float a, float b, float c) {
        return (int)Math.floor(Math.min(a, Math.min(b, c)));
    }

    private int Max(float a, float b, float c) {
        return (int)Math.ceil(Math.max(a, Math.max(b, c)));
    }

    private boolean IsTopLeftEdge(Vector2 a, Vector2 b) {
        return (a.y < b.y) || (a.y == b.y && a.x > b.x);
    }

    public void DrawTriangle(Vertex v0, Vertex v1, Vertex v2) {
        Vertex sv0 = VertexShader(v0);
        Vertex sv1 = VertexShader(v1);
        Vertex sv2 = VertexShader(v2);

        int minX = Min(sv0.position.x, sv1.position.x, sv2.position.x);
        int maxX = Max(sv0.position.x, sv1.position.x, sv2.position.x);
        int minY = Min(sv0.position.y, sv1.position.y, sv2.position.y);
        int maxY = Max(sv0.position.y, sv1.position.y, sv2.position.y);

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (x < 0 || x >= width || y < 0 || y >= height) {
                    continue;
                }

                Vector2 p = new Vector2(x + 0.5f, y + 0.5f);
                float[] factors = CalculateInterpolationFactors(sv0, sv1, sv2, p);

                boolean e0 = IsTopLeftEdge(sv1.position, sv2.position);
                boolean e1 = IsTopLeftEdge(sv2.position, sv0.position);
                boolean e2 = IsTopLeftEdge(sv0.position, sv1.position);

                boolean inside = (factors[0] > 0 || (factors[0] == 0 && e0)) &&
                                 (factors[1] > 0 || (factors[1] == 0 && e1)) &&
                                 (factors[2] > 0 || (factors[2] == 0 && e2));

                if (!inside) {
                    continue;
                }

                Vector2 normalizedFragCoord = new Vector2(p.x / width, p.y / height);
                Color color = FragmentShader(normalizedFragCoord,
                    new Color(
                        factors[0] * sv0.color.r + factors[1] * sv1.color.r + factors[2] * sv2.color.r,
                        factors[0] * sv0.color.g + factors[1] * sv1.color.g + factors[2] * sv2.color.g,
                        factors[0] * sv0.color.b + factors[1] * sv1.color.b + factors[2] * sv2.color.b,
                        factors[0] * sv0.color.a + factors[1] * sv1.color.a + factors[2] * sv2.color.a
                    ),
                    new Vector2(factors[0] * sv0.textureCoordinate.x + factors[1] * sv1.textureCoordinate.x + factors[2] * sv2.textureCoordinate.x,
                                factors[0] * sv0.textureCoordinate.y + factors[1] * sv1.textureCoordinate.y + factors[2] * sv2.textureCoordinate.y)
                );
                Color existingColor = new Color(
                    ((framebuffer.getRGB(x, y) >> 16) & 0xFF) / 255f,
                    ((framebuffer.getRGB(x, y) >> 8) & 0xFF) / 255f,
                    (framebuffer.getRGB(x, y) & 0xFF) / 255f,
                    ((framebuffer.getRGB(x, y) >> 24) & 0xFF) / 255f
                );
                Color blendedColor = Color.Blend(color, existingColor);
                framebuffer.setRGB(x, y, blendedColor.toRGB());
            }
        }
    }

    public void DrawTriangleStrip(VertexArray vertexArray, int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex - 2; i++) {
            if (i % 2 == 0) {
                DrawTriangle(vertexArray.GetVertex(i), vertexArray.GetVertex(i + 1), vertexArray.GetVertex(i + 2));
            } else {
                DrawTriangle(vertexArray.GetVertex(i), vertexArray.GetVertex(i + 2), vertexArray.GetVertex(i + 1));
            }
        }
    }

    public void DrawTriangleStrip(VertexArray vertexArray) {
        DrawTriangleStrip(vertexArray, 0, vertexArray.vertexCount);
    }

    public void DrawQuads(VertexArray vertexArray) {
        for (int i = 0; i < vertexArray.vertexCount - 3; i += 4) {
            DrawTriangle(vertexArray.GetVertex(i), vertexArray.GetVertex(i + 1), vertexArray.GetVertex(i + 2));
            DrawTriangle(vertexArray.GetVertex(i + 2), vertexArray.GetVertex(i + 3), vertexArray.GetVertex(i + 1));
        }
    }

    private Color SampleTextureNearest(Vector2 textureCoordinate) {
        BufferedImage texture = GetTexture();
        int x = (int)(textureCoordinate.x * (texture.getWidth() - 1));
        int y = (int)(textureCoordinate.y * (texture.getHeight() - 1));
        int rgb = texture.getRGB(x, y);
        return new Color(
            ((rgb >> 16) & 0xFF) / 255f,
            ((rgb >> 8) & 0xFF) / 255f,
            (rgb & 0xFF) / 255f,
            ((rgb >> 24) & 0xFF) / 255f
        );
    }
    
    private Color SampleTextureBilinear(Vector2 textureCoordinate) {
        BufferedImage texture = GetTexture();
        float x = textureCoordinate.x * (texture.getWidth() - 1);
        float y = textureCoordinate.y * (texture.getHeight() - 1);
        int x0 = (int)Math.floor(x);
        int x1 = (int)Math.ceil(x);
        int y0 = (int)Math.floor(y);
        int y1 = (int)Math.ceil(y);

        Color c00 = SampleTextureNearest(new Vector2(x0 / (float)texture.getWidth(), y0 / (float)texture.getHeight()));
        Color c10 = SampleTextureNearest(new Vector2(x1 / (float)texture.getWidth(), y0 / (float)texture.getHeight()));
        Color c01 = SampleTextureNearest(new Vector2(x0 / (float)texture.getWidth(), y1 / (float)texture.getHeight()));
        Color c11 = SampleTextureNearest(new Vector2(x1 / (float)texture.getWidth(), y1 / (float)texture.getHeight()));

        float tx = x - x0;
        float ty = y - y0;

        return new Color(
            c00.r * (1 - tx) * (1 - ty) + c10.r * tx * (1 - ty) + c01.r * (1 - tx) * ty + c11.r * tx * ty,
            c00.g * (1 - tx) * (1 - ty) + c10.g * tx * (1 - ty) + c01.g * (1 - tx) * ty + c11.g * tx * ty,
            c00.b * (1 - tx) * (1 - ty) + c10.b * tx * (1 - ty) + c01.b * (1 - tx) * ty + c11.b * tx * ty,
            c00.a * (1 - tx) * (1 - ty) + c10.a * tx * (1 - ty) + c01.a * (1 - tx) * ty + c11.a * tx * ty
        );
    }

    private Color SampleTexture(Vector2 textureCoordinate) {
        return SampleTextureBilinear(textureCoordinate);
    }

    private Vertex VertexShader(Vertex vertex) {
        if (camera == null) {
            return vertex;
        }
        Vector2 center = new Vector2(width / 2, height / 2);
        Vertex transformedVertex = new Vertex(
            new Vector2(
                (int)((vertex.position.x - camera.position.x - center.x) * camera.zoom + center.x),
                (int)((vertex.position.y - camera.position.y - center.y) * camera.zoom + center.y)
            ),
            vertex.color,
            vertex.textureCoordinate
        );
        return transformedVertex;
    }

    private Color FragmentShader(Vector2 fragCoord, Color color, Vector2 textureCoordinate) {
        return Color.Multiply(color, SampleTexture(textureCoordinate));
    }

    private BufferedImage GetTexture() {
        if (texture == null) {
            // Placeholder for texture loading logic
            texture = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            texture.setRGB(0, 0, 0xFFFFFFFF); // White pixel
        }
        return texture;
    }

    public void SetTexture(BufferedImage texture) {
        this.texture = texture;
    }

    public void Clear(Color clearColor) {
        int rgb = clearColor.toRGB();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                framebuffer.setRGB(x, y, rgb);
            }
        }
    }

    public void Clear(int rgb) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                framebuffer.setRGB(x, y, rgb);
            }
        }
    }

    public void SaveToFile(String filename) {
        try {
            javax.imageio.ImageIO.write(framebuffer, "png", new java.io.File(filename));
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    int width;
    int height;
    BufferedImage texture;
    BufferedImage framebuffer;
    Camera camera;

    RendererCpu(int width, int height) {
        this.width = width;
        this.height = height;
        this.framebuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }
}
