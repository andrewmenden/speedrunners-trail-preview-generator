package org.example;

public class Path {
    private float time;
    private float deltaTime;
    Vector2 previousPosition;

    public Path(float deltaTime) {
        time = 0.0f;
        this.deltaTime = deltaTime;
    }

    public Vector2 CalculateVelocity(Vector2 position) {
        if (previousPosition == null) {
            previousPosition = new Vector2(position.x, position.y);
        }
        Vector2 velocity = new Vector2(position.x - previousPosition.x, position.y - previousPosition.y);
        previousPosition = new Vector2(position.x, position.y);
        return Vector2.Scale(velocity, 1.0f / deltaTime);
    }

    void Reset() {
        time = 0.0f;
        previousPosition = null;
    }

    public Vector2 GetNextPositionCircle(float radius, float speedScale) {
        time += deltaTime;
        float angle = time * speedScale;
        float x = (float) Math.cos(angle) * radius;
        float y = (float) Math.sin(angle) * radius;
        return new Vector2(x, y);
    }

    public Vector2 GetNextPositionLine(float speed) {
        time += deltaTime;
        float x = time * speed;
        return new Vector2(x, 0);
    }

    public Vector2 GetNextPositionAcceleratingLine(float startSpeed, float acceleration) {
        time += deltaTime;
        float x = startSpeed * time + 0.5f * acceleration * time * time;
        return new Vector2(x, 0);
    }

    public Vector2 InfinityPath(float time, float radius, Vector2 center, float speed) {
        time *= speed;
        return Vector2.Sum(
            center,
            new Vector2(
                radius * (float)Math.cos(time) / (1 + (float)Math.sin(time) * (float)Math.sin(time)),
                radius * (float)Math.cos(time) * (float)Math.sin(time) / (1 + (float)Math.sin(time) * (float)Math.sin(time))
            )
        );
    }

}
