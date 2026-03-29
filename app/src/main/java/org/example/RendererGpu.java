package org.example;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.List;

import imgui.ImGui;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class RendererGpu {
    int width;
    int height;

    private int shaderProgram;
    private int vao;
    private int vbo;
    private int ebo;

    Camera camera;

    public RendererGpu(int width, int height) {
        this.width = width;
        this.height = height;
        this.camera = new Camera();

        // Initialize OpenGL resources
        InitializeShaders();
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        // Position attribute
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 8 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // Color attribute
        glVertexAttribPointer(1, 4, GL_FLOAT, false, 8 * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);

        // Texture coordinate attribute
        glVertexAttribPointer(2, 2, GL_FLOAT, false, 8 * Float.BYTES, 6 * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void DrawTriangleStrip(List<Vertex> vertices, int textureId) {
        if (vertices.isEmpty()) return;
        float[] vertexData = new float[vertices.size() * 8];

        float halfWidth = width / 2.0f;
        float halfHeight = height / 2.0f;

        for (int i = 0; i < vertices.size(); i++) {
            Vertex vertex = vertices.get(i);
            vertexData[i * 8] = (float) vertex.position.x / halfWidth - 1.0f; // Convert to NDC
            vertexData[i * 8 + 1] = (float) vertex.position.y / halfHeight - 1.0f; // Convert to NDC
            vertexData[i * 8 + 2] = vertex.color.r;
            vertexData[i * 8 + 3] = vertex.color.g;
            vertexData[i * 8 + 4] = vertex.color.b;
            vertexData[i * 8 + 5] = vertex.color.a;
            vertexData[i * 8 + 6] = (float) vertex.textureCoordinate.x;
            vertexData[i * 8 + 7] = (float) vertex.textureCoordinate.y;
        }

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexData, GL_DYNAMIC_DRAW);

        glUseProgram(shaderProgram);
        float camX = (float) camera.position.x / halfWidth;
        float camY = (float) camera.position.y / halfHeight;
        glUniform2f(glGetUniformLocation(shaderProgram, "cameraPosition"), camX, camY);
        glUniform1f(glGetUniformLocation(shaderProgram, "cameraZoom"), camera.zoom);
        if (textureId != -1) {
            glUniform1i(glGetUniformLocation(shaderProgram, "texture1"), 0);
            glUniform1i(glGetUniformLocation(shaderProgram, "useTexture"), 1);
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, textureId);
        } else {
            glUniform1i(glGetUniformLocation(shaderProgram, "useTexture"), 0);
        }
        glDrawArrays(GL_TRIANGLE_STRIP, 0, vertices.size());

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void DrawQuads(List<Vertex> vertices, int textureId) {
        if (vertices.isEmpty()) return;
        float[] vertexData = new float[vertices.size() * 8];

        float halfWidth = width / 2.0f;
        float halfHeight = height / 2.0f;

        for (int i = 0; i < vertices.size(); i++) {
            Vertex vertex = vertices.get(i);
            vertexData[i * 8] = (float) vertex.position.x / halfWidth - 1.0f;
            vertexData[i * 8 + 1] = (float) vertex.position.y / halfHeight - 1.0f;
            vertexData[i * 8 + 2] = vertex.color.r;
            vertexData[i * 8 + 3] = vertex.color.g;
            vertexData[i * 8 + 4] = vertex.color.b;
            vertexData[i * 8 + 5] = vertex.color.a;
            vertexData[i * 8 + 6] = (float) vertex.textureCoordinate.x;
            vertexData[i * 8 + 7] = (float) vertex.textureCoordinate.y;
        }

        int[] indices = new int[vertices.size() / 4 * 6];
        for (int i = 0; i < vertices.size() / 4; i++) {
            int baseIndex = i * 4;
            indices[i * 6] = baseIndex;
            indices[i * 6 + 1] = baseIndex + 1; 
            indices[i * 6 + 2] = baseIndex + 2;
            indices[i * 6 + 3] = baseIndex + 2;
            indices[i * 6 + 4] = baseIndex + 1;
            indices[i * 6 + 5] = baseIndex + 3;
        }

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexData, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_DYNAMIC_DRAW);

        glUseProgram(shaderProgram);
        float camX = (float) camera.position.x / halfWidth;
        float camY = (float) camera.position.y / halfHeight;
        glUniform2f(glGetUniformLocation(shaderProgram, "cameraPosition"), camX, camY);
        glUniform1f(glGetUniformLocation(shaderProgram, "cameraZoom"), camera.zoom);
        if (textureId != -1) {
            glUniform1i(glGetUniformLocation(shaderProgram, "texture1"), 0);
            glUniform1i(glGetUniformLocation(shaderProgram, "useTexture"), 1);
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, textureId);
        } else {
            glUniform1i(glGetUniformLocation(shaderProgram, "useTexture"), 0);
        }
        glDrawElements(GL_TRIANGLES, indices.length, GL_UNSIGNED_INT, 0);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void DrawPoints(List<Vertex> vertices, float radius) {
        if (vertices.isEmpty()) return;
        int circlePoints = 3;
        float[] vertexData = new float[vertices.size() * 8 * 3];

        float halfWidth = width / 2.0f;
        float halfHeight = height / 2.0f;

        for (int i = 0; i < vertices.size(); i++) {
            Vertex vertex = vertices.get(i);
            for (int j = 0; j < circlePoints; j++) {
                float angle = (float) (j * 2 * Math.PI / circlePoints);
                vertexData[(i * circlePoints + j) * 8] = (float) vertex.position.x / halfWidth - 1.0f + (float) Math.cos(angle) * radius / halfWidth;
                vertexData[(i * circlePoints + j) * 8 + 1] = (float) vertex.position.y / halfHeight - 1.0f + (float) Math.sin(angle) * radius / halfHeight;
                vertexData[(i * circlePoints + j) * 8 + 2] = vertex.color.r;
                vertexData[(i * circlePoints + j) * 8 + 3] = vertex.color.g;
                vertexData[(i * circlePoints + j) * 8 + 4] = vertex.color.b;
                vertexData[(i * circlePoints + j) * 8 + 5] = vertex.color.a;
                vertexData[(i * circlePoints + j) * 8 + 6] = (float) vertex.textureCoordinate.x;
                vertexData[(i * circlePoints + j) * 8 + 7] = (float) vertex.textureCoordinate.y;
            }
        }

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexData, GL_DYNAMIC_DRAW);
        glUseProgram(shaderProgram);
        float camX = (float) camera.position.x / halfWidth;
        float camY = (float) camera.position.y / halfHeight;
        glUniform2f(glGetUniformLocation(shaderProgram, "cameraPosition"), camX, camY);
        glUniform1f(glGetUniformLocation(shaderProgram, "cameraZoom"), camera.zoom);
        glUniform1i(glGetUniformLocation(shaderProgram, "useTexture"), 0);
        glDrawArrays(GL_TRIANGLES, 0, vertices.size() * circlePoints);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public static int CreateTexture(BufferedImage image) {
        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        // Set texture parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        // Upload texture data
        ByteBuffer buffer = ByteBuffer.allocateDirect(image.getWidth() * image.getHeight() * 4);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                buffer.put((byte) ((argb >> 16) & 0xFF)); // Red
                buffer.put((byte) ((argb >> 8) & 0xFF));  // Green
                buffer.put((byte) (argb & 0xFF));         // Blue
                buffer.put((byte) ((argb >> 24) & 0xFF)); // Alpha
            }
        }
        buffer.flip();

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, image.getWidth(), image.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        glBindTexture(GL_TEXTURE_2D, 0);

        return textureId;
    }

    public static void DeleteTexture(int textureId) {
        glDeleteTextures(textureId);
    }

    public void SetCameraPosition(float x, float y) {
        camera.position.x = x;
        camera.position.y = y;
    }

    public void SetCameraZoom(float zoom) {
        camera.zoom = zoom;
    }

    public void MoveCamera(float deltaX, float deltaY) {
        camera.position.x += deltaX;
        camera.position.y += deltaY;
    }

    public void ZoomCamera(float zoomFactor) {
        camera.zoom *= zoomFactor;
    }

    public void Dispose() {
        glDeleteProgram(shaderProgram);
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
    }

    private int CompileShader(String source, int type) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader compilation failed: " + glGetShaderInfoLog(shader));
        }
        return shader;
    }

    private int LinkProgram(int vertexShader, int fragmentShader) {
        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Program linking failed: " + glGetProgramInfoLog(program));
        }
        return program;
    }

    private void InitializeShaders() {
        int vertexShader = CompileShader(vertexShaderSource, GL_VERTEX_SHADER);
        int fragmentShader = CompileShader(fragmentShaderSource, GL_FRAGMENT_SHADER);
        shaderProgram = LinkProgram(vertexShader, fragmentShader);
    }

    private static final String vertexShaderSource = """
        #version 330 core
        layout(location = 0) in vec2 aPos;
        layout(location = 1) in vec4 aColor;
        layout(location = 2) in vec2 aTexCoord;

        uniform vec2 cameraPosition;
        uniform float cameraZoom;

        out vec4 vertexColor;
        out vec2 texCoord;

        void main() {
            vec2 pos = (aPos - cameraPosition) * cameraZoom;
            pos.y = -pos.y; // Invert Y axis for OpenGL
            gl_Position = vec4(pos, 0.0, 1.0);
            vertexColor = aColor;
            texCoord = aTexCoord;
        }
    """;

    private static final String fragmentShaderSource = """
        #version 330 core
        in vec4 vertexColor;
        in vec2 texCoord;

        out vec4 FragColor;

        uniform sampler2D texture1;
        uniform bool useTexture;

        void main() {
            vec4 texColor = useTexture ? texture(texture1, texCoord) : vec4(1.0);
            FragColor = vertexColor * texColor;
        }
    """;
}
