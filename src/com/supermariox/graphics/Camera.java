package com.supermariox.graphics;

public class Camera {
    private float x;
    private float y;
    private int viewportWidth;
    private int viewportHeight;

    private float minX = 0;
    private float minY = 0;
    private float maxX = 10000;
    private float maxY = 1000;

    private float lerpSpeed = 0.1f;

    public Camera(int viewportWidth, int viewportHeight) {
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.x = 0;
        this.y = 0;
    }

    public void update(float targetX, float targetY) {
        // Center viewport on target
        float desiredX = targetX - (viewportWidth / 2.0f);
        float desiredY = targetY - (viewportHeight / 1.6f);

        // Smooth camera movement
        x += (desiredX - x) * lerpSpeed;
        y += (desiredY - y) * lerpSpeed;

        // Clamp camera position to level bounds
        if (x < minX) x = minX;
        if (x > maxX - viewportWidth) x = maxX - viewportWidth;
        if (y < minY) y = minY;
        if (y > maxY - viewportHeight) y = maxY - viewportHeight;
    }

    public void setBounds(float minX, float minY, float maxX, float maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public void setViewportSize(int width, int height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public int getViewportWidth() {
        return viewportWidth;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }
}
