package org.example;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

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

    public void DrawTriangleStrip(VertexArray vertexArray, int textureId, int startVertex, int endVertex) {
        if (vertexArray.vertexCount < 3) {
            return;
        }

        float halfWidth = width / 2.0f;
        float halfHeight = height / 2.0f;

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexArray.GetVertices(), GL_DYNAMIC_DRAW);

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
        glUniform1f(glGetUniformLocation(shaderProgram, "worldWidth"), width);
        glUniform1f(glGetUniformLocation(shaderProgram, "worldHeight"), height);
        glDrawArrays(GL_TRIANGLE_STRIP, startVertex, endVertex - startVertex);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void DrawTriangleStrip(VertexArray vertexArray, int textureId) {
        DrawTriangleStrip(vertexArray, textureId, 0, vertexArray.vertexCount);
    }

    public void DrawQuads(VertexArray vertexArray, int textureId) {
        if (vertexArray.vertexCount < 4) {
            return;
        }

        float halfWidth = width / 2.0f;
        float halfHeight = height / 2.0f;

        int[] indices = new int[vertexArray.vertexCount / 4 * 6];
        for (int i = 0; i < vertexArray.vertexCount / 4; i++) {
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
        glBufferData(GL_ARRAY_BUFFER, vertexArray.GetVertices(), GL_DYNAMIC_DRAW);
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
        glUniform1f(glGetUniformLocation(shaderProgram, "worldWidth"), width);
        glUniform1f(glGetUniformLocation(shaderProgram, "worldHeight"), height);
        glDrawElements(GL_TRIANGLES, indices.length, GL_UNSIGNED_INT, 0);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void DrawLines(VertexArray vertexArray, int textureId) {
        if (vertexArray.vertexCount < 2) {
            return;
        }

        float halfWidth = width / 2.0f;
        float halfHeight = height / 2.0f;

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexArray.GetVertices(), GL_DYNAMIC_DRAW);

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
        glUniform1f(glGetUniformLocation(shaderProgram, "worldWidth"), width);
        glUniform1f(glGetUniformLocation(shaderProgram, "worldHeight"), height);
        glDrawArrays(GL_LINE_LOOP, 0, vertexArray.vertexCount);

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

        uniform float worldWidth;
        uniform float worldHeight;

        out vec4 vertexColor;
        out vec2 texCoord;

        void main() {
            // Convert from world coordinates to normalized device coordinates
            vec2 pos = aPos;
            pos.x = (pos.x / worldWidth) * 2.0 - 1.0;
            pos.y = (pos.y / worldHeight) * 2.0 - 1.0;

            pos = (pos - cameraPosition) * cameraZoom;
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
