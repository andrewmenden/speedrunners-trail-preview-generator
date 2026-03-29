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

        Vector2 center = new Vector2(2560/2, 1440/2);
        
        RendererGpu gpu = new RendererGpu(2560, 1440);
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

        boolean[] showLayers = {};
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

            Vector2 currentPos = CirclePath(time, 860, center, speedScale[0]);
            Vector2 velocity = VelocityCirclePath(time, 400, speedScale[0]);
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

                        showLayers = new boolean[trail.stripes.size()];
                        for (int j = 0; j < showLayers.length; j++) {
                            showLayers[j] = trail.stripes.get(j).enabled != 0;
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

            if (trail != null) {
                if (!paused) {
                    trail.AddPoint(currentPos, velocity, time);
                    trail.Update(deltaTime, currentPos, velocity);
                }

                ImGui.begin("Trail Information");
                ImGui.text("Trail Name: " + trail.name);
                ImGui.text("Author: " + trail.author);
                ImGui.text("Description: " + trail.description);
                ImGui.text("Last Updated: " + trail.lastUpdated);

                for (int i = 0; i < trail.stripes.size(); i++) {
                    TrailStripe stripe = trail.stripes.get(i);
                    ImGui.textWrapped(stripe.ToString());
                    ImGui.separator();
                }
                ImGui.end();

                ImGui.begin("Layer Visibility");
                for (int i = 0; i < trail.stripes.size(); i++) {
                    TrailStripe stripe = trail.stripes.get(i);
                    String label = String.format("Stripe %d (%s)", i + 1, stripe.image);
                    if (ImGui.checkbox(label, showLayers[i])) {
                        showLayers[i] = !showLayers[i];
                        stripe.visible = showLayers[i];
                    }
                }
                ImGui.end();

                for (TrailParticleEmitter emitter : trail.particles) {
                    gpu.DrawQuads(emitter.vertices, 
                        gpuTextureIds.getOrDefault(emitter.image, -1));
                }

                for (TrailStripe stripe : trail.stripes) {
                    if (stripe.enabled == 0 || !stripe.visible)
                        continue;
                    gpu.DrawTriangleStrip(stripe.vertices, 
                        gpuTextureIds.getOrDefault(stripe.image, -1));
                }

            }

            ImGui.render();
            imGuiGl3.renderDrawData(ImGui.getDrawData());

            glfwSwapBuffers(window);
        }

        gpu.Dispose();

        glfwDestroyWindow(window);
        glfwTerminate();
    }

    static void Run() {
        // String path = "C:\\Users\\olsud\\Dev\\defaultTrail.srt";
        String path = "C:\\Users\\olsud\\Dev\\ST Goldilocks.srt";
        Trail trail;
        try {
            trail = new Trail(path);
        } catch (IOException e) {
            System.err.println("Error loading trail: " + e.getMessage());
        }
    }

    static Trail LoadTrail(String filename) {
        try {
            return new Trail(filename);
        } catch (IOException e) {
            return null;
        }
    }

    public static void main(String[] args) {
        RunGui();
    }
}