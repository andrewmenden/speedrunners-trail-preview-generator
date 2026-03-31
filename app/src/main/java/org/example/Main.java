package org.example;
import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import imgui.*;
import imgui.type.*;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class Main {
    static Vector2 CirclePath(float time, float radius, Vector2 center, float speed) {
         time *= speed;
         return Vector2.Sum(
            center,
            new Vector2(
                radius * (float)Math.cos(time),
                radius * (float)Math.sin(time)
            )
        );
    }

    static Vector2 VelocityCirclePath(float time, float radius, float speed) {
         time *= speed;
         return new Vector2(
            -radius * speed * (float)Math.sin(time),
            radius * speed * (float)Math.cos(time)
        );
        //sqrt(d/dx radius*cos(speed*x))^2 + (d/dx radius*sin(speed*x))^2)
        //=sqrt((-radius*speed*sin(speed*x))^2 + (radius*speed*cos(speed*x))^2)
        //=sqrt(radius^2*speed^2*sin^2(speed*x) + radius^2*speed^2*cos^2(speed*x))
        //=sqrt(radius^2*speed^2*(sin^2(speed*x) + cos^2(speed*x)))
        //=sqrt(radius^2*speed^2)
        //=radius*speed
    }

    static Vector2 InfinityPath(float time, float radius, Vector2 center, float speed) {
        time *= speed;
        return Vector2.Sum(
            center,
            new Vector2(
                radius * (float)Math.cos(time) / (1 + (float)Math.sin(time) * (float)Math.sin(time)),
                radius * (float)Math.cos(time) * (float)Math.sin(time) / (1 + (float)Math.sin(time) * (float)Math.sin(time))
            )
        );
    }

    static void FullRender(String trailPath) throws IOException {
        Trail trail = new Trail(trailPath);

        Camera camera = new Camera();
        RendererCpu cpu = new RendererCpu(1024, 512);
        cpu.camera = camera;
        camera.position = new Vector2(-512, -256);
        
        float startTime = 5.2f;
        float endTime = (float)Math.PI * 2.0f - 0.05f;
        int pointCount = 600;
        float timeStep = (endTime - startTime) / (float)pointCount;
        Path pathA = new Path(timeStep);

        float startSpeed = Trail.AFTERIMAGE_THRESHOLD;
        float endSpeed = Trail.SUPERSPEED_THRESHOLD * 1.35f;
        float speedStep = (endSpeed - startSpeed) / (float)pointCount;

        trail.Reset();
        for (int i = 0; i < pointCount; i++) {
            float currentTime = startTime + i * timeStep;
            float currentSpeed = startSpeed + i * speedStep;
            Vector2 position = pathA.InfinityPath(currentTime, 430, new Vector2(0, 0), 3.0f);
            Vector2 velocity = pathA.CalculateVelocity(position);
            velocity = Vector2.Normalize(velocity);
            velocity = Vector2.Scale(velocity, currentSpeed);
            
            trail.Update(timeStep, position, velocity);
        }

        System.out.println("Rendering CPU...");
        if (trail.layers.size() > 0) {
            TrailLayer layer = trail.layers.get(0);
            if (layer.layerType == 0) {
                TrailStripe stripe = (TrailStripe) layer;
                System.out.println("Vertex count: " + stripe.vertexArray.vertexCount);
            }
        }

        cpu.Clear(new Color(0.2f, 0.2f, 0.2f, 1.0f));
        RenderCPU(cpu, trail);
        cpu.SaveToFile("test.png");
    }

    static void RunGui() {
        if (!glfwInit()) {
            throw new IllegalStateException("GLFW init failed");
        }

        long window = glfwCreateWindow(2560, 1440, "Trail Test", NULL, NULL);
        glfwMakeContextCurrent(window);

        GL.createCapabilities();
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glfwSwapInterval(1);

        ImGui.createContext();

        ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
        ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();

        imGuiGlfw.init(window, true);
        imGuiGl3.init("#version 330 core");

        String trailFolder = "C:\\Users\\olsud\\Documents\\SavedGames\\SpeedRunners\\CEngineStorage\\AllPlayers\\Trails\\Local\\";
        String[] trailFiles = new java.io.File(trailFolder).list();
        int selectedTrailIndex = 0;
        Trail trail = null;

        Vector2 center = new Vector2(0,0);
        
        float cpuRenderWidth = 1024;
        float cpuRenderHeight = 512;

        RendererGpu gpu = new RendererGpu(2560, 1440);
        gpu.camera.position = new Vector2(-2560 / 2, -1440 / 2);
        RendererCpu cpu = new RendererCpu((int)cpuRenderWidth, (int)cpuRenderHeight);
        cpu.camera = new Camera();
        cpu.camera.position = new Vector2(-cpuRenderWidth / 2, -cpuRenderHeight / 2);
        Vector2 dragStart = new Vector2(0,0);
        Vector2 cameraStart = new Vector2(0,0);
        boolean[] dragging = {false};

        // scroll behavior
        glfwSetScrollCallback(window, (w, xoffset, yoffset) -> {
            float zoomFactor = 1.0f + (float) yoffset * 0.1f;
            gpu.ZoomCamera(zoomFactor);
        });

        // mouse drag behavior
        glfwSetMouseButtonCallback(window, (w, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                if (action == GLFW_PRESS) {
                    double[] xPos = new double[1];
                    double[] yPos = new double[1];
                    glfwGetCursorPos(window, xPos, yPos);
                    dragStart.x = (float)xPos[0];
                    dragStart.y = (float)yPos[0];
                    cameraStart.x = gpu.camera.position.x;
                    cameraStart.y = gpu.camera.position.y;
                    dragging[0] = true;
                } else if (action == GLFW_RELEASE) {
                    dragging[0] = false;
                }
            }
        });

        glfwSetCursorPosCallback(window, (w, xPos, yPos) -> {
            if (dragging[0]) {
                float deltaX = (float)xPos - dragStart.x;
                float deltaY = (float)yPos - dragStart.y;
                deltaX /= gpu.camera.zoom; // adjust for zoom level
                deltaY /= gpu.camera.zoom;
                gpu.SetCameraPosition(cameraStart.x - deltaX, cameraStart.y - deltaY);
            }
        });

        float lastTime = (float)glfwGetTime();
        float[] speedScale = {1.0f};

        HashMap<String, Integer> gpuTextureIds = new HashMap<>();
        boolean paused = false;

        VertexArray vertexArray = new VertexArray(4);
        vertexArray.AddVertex(new Vertex(new Vector2(-cpuRenderWidth/2, -cpuRenderHeight/2), new Color(1, 0, 0, 1), new Vector2(0, 0)));
        vertexArray.AddVertex(new Vertex(new Vector2(cpuRenderWidth/2, -cpuRenderHeight/2), new Color(1, 0, 0, 1), new Vector2(1, 0)));
        vertexArray.AddVertex(new Vertex(new Vector2(cpuRenderWidth/2, cpuRenderHeight/2), new Color(1, 0, 0, 1), new Vector2(1, 1)));
        vertexArray.AddVertex(new Vertex(new Vector2(-cpuRenderWidth/2, cpuRenderHeight/2), new Color(1, 0, 0, 1), new Vector2(0, 1)));

        Vector2 lastPosition = null;

        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();

            imGuiGlfw.newFrame();
            ImGui.newFrame();
            glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);

            float time = (float)glfwGetTime();
            float deltaTime = time - lastTime;
            lastTime = time;

            ImGui.sliderFloat("Speed Scale", speedScale, 0.1f, 5.0f);
            ImGui.text(String.format("Time: %.2f seconds", time));

            ImGui.text(String.format("Camera Position: (%.2f, %.2f)", gpu.camera.position.x, gpu.camera.position.y));
            ImGui.text(String.format("Camera Zoom: %.2f", gpu.camera.zoom));

            Vector2 currentPos = InfinityPath(time, 430, center, speedScale[0]);
            if (lastPosition == null) {
                lastPosition = currentPos;
            }
            Vector2 velocity = Vector2.Subtract(currentPos, lastPosition);
            velocity = Vector2.Scale(velocity, 1.0f / deltaTime);
            lastPosition = currentPos;
            float speed = Vector2.Length(velocity);

            ImGui.text(String.format("Speed: %.2f", speed));
            if (ImGui.button(paused ? "Resume" : "Pause")) {
                paused = !paused;
            }

            ImGui.begin("Trail Selector");
            if (trailFiles != null) {
                for (int i = 0; i < trailFiles.length; i++) {
                    if (ImGui.selectable(trailFiles[i], selectedTrailIndex == i)) {
                        selectedTrailIndex = i;
                        try {
                            trail = new Trail(trailFolder + trailFiles[i]);
                        } catch (IOException e) {
                            System.err.println("Failed to load trail: " + e.getMessage());
                            trail = null;
                            break;
                        }

                        gpuTextureIds.clear();

                        for (Map.Entry<String, BufferedImage> entry : trail.loadedImages.entrySet()) {
                            int textureId = RendererGpu.CreateTexture(entry.getValue());
                            gpuTextureIds.put(entry.getKey(), textureId);
                        }
                    }
                }
            }
            ImGui.end();

            gpu.DrawLines(vertexArray, -1);
            if (trail != null) {
                if (!paused) {
                    trail.Update(deltaTime, currentPos, velocity);
                }
                if (ImGui.button("render cpu")) {
                    cpu.Clear(new Color(0.2f, 0.2f, 0.2f, 1.0f));
                    RenderCPU(cpu, trail);
                    cpu.SaveToFile("cpu_render.png");
                }

                RenderGPU(gpu, trail, gpuTextureIds);
            }

            ImGui.render();
            imGuiGl3.renderDrawData(ImGui.getDrawData());

            glfwSwapBuffers(window);
        }

        gpu.Dispose();

        glfwDestroyWindow(window);
        glfwTerminate();
    }

    static void RenderCPU(RendererCpu cpu, Trail trail) {
        for (TrailLayer layer : trail.layers) {
            if (layer.layerType == 0) {
                TrailStripe stripe = (TrailStripe) layer;
                if (stripe.enabled == 0 || !stripe.visible)
                    continue;
                cpu.texture = trail.loadedImages.getOrDefault(stripe.image, null);
                for (int i = 0; i < stripe.GetSegmentCount(); i++) {
                    TrailStripe.Segment segment = stripe.GetSegment(i);
                    cpu.DrawTriangleStrip(stripe.vertexArray, segment.startIndex, segment.endIndex);
                }
            } else if (layer.layerType == 1) {
                TrailParticleEmitter emitter = (TrailParticleEmitter) layer;
                if (emitter.enabled == 0 || !emitter.visible)
                    continue;
                cpu.texture = trail.loadedImages.getOrDefault(emitter.image, null);
                cpu.DrawQuads(emitter.vertexArray);
            } else if (layer.layerType == 2) {
                TrailAnimation animation = (TrailAnimation) layer;
                if (animation.enabled == 0 || !animation.visible)
                    continue;
                cpu.texture = trail.loadedImages.getOrDefault(animation.image, null);
                cpu.DrawQuads(animation.vertexArray);
            }
        }
    }

    static void RenderGPU(RendererGpu gpu, Trail trail, HashMap<String, Integer> gpuTextureIds) {
        for (TrailLayer layer : trail.layers) {
            if (layer.layerType == 0) {
                TrailStripe stripe = (TrailStripe) layer;
                if (stripe.enabled == 0 || !stripe.visible)
                    continue;
                for (int i = 0; i < stripe.GetSegmentCount(); i++) {
                    TrailStripe.Segment segment = stripe.GetSegment(i);
                    gpu.DrawTriangleStrip(stripe.vertexArray, gpuTextureIds.getOrDefault(stripe.image, -1),
                        segment.startIndex, segment.endIndex);
                }
            } else if (layer.layerType == 1) {
                TrailParticleEmitter emitter = (TrailParticleEmitter) layer;
                if (emitter.enabled == 0 || !emitter.visible)
                    continue;
                gpu.DrawQuads(emitter.vertexArray, gpuTextureIds.getOrDefault(emitter.image, -1));
            } else if (layer.layerType == 2) {
                TrailAnimation animation = (TrailAnimation) layer;
                if (animation.enabled == 0 || !animation.visible)
                    continue;
                gpu.DrawQuads(animation.vertexArray, gpuTextureIds.getOrDefault(animation.image, -1));
            }
        }
    }

    static void Run() {
        // String path = "C:\\Users\\olsud\\Dev\\defaultTrail.srt";
        String path = "C:\\Users\\olsud\\Documents\\SavedGames\\SpeedRunners\\CEngineStorage\\AllPlayers\\Trails\\Local\\";
        String trailName = "ST Goldilocks.srt";
        Trail trail = null;
        try {
            trail = new Trail(path + trailName);
        } catch (IOException e) {
            System.err.println("Error loading trail: " + e.getMessage());
        }

        if (trail == null) return;

        Camera camera = new Camera();
        RendererCpu cpu = new RendererCpu(2560, 1440);
        cpu.camera = camera;
        Path pathA = new Path(0.016f);

        for (int i = 0; i < 600; i++) {
            Vector2 position = pathA.GetNextPositionCircle(500, 1.0f);
            Vector2 velocity = pathA.CalculateVelocity(position);
            trail.Update(0.016f, position, velocity);
        }

        RenderCPU(cpu, trail);
        cpu.SaveToFile("test.png");
    }

    public static void main(String[] args) {
        try {
            FullRender("C:\\Users\\olsud\\Documents\\SavedGames\\SpeedRunners\\CEngineStorage\\AllPlayers\\Trails\\Local\\Shadow.srt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}