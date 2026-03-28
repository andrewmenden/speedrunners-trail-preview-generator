package org.example;

import java.util.Vector;

public class Camera {
    Vector2 position;
    float zoom;

    Camera() {
        position = new Vector2(0, 0);
        zoom = 1.0f;
    }
}
