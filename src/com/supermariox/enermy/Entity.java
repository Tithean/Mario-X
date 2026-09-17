package com.supermariox.enermy;

import com.supermariox.graphics.Camera;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public abstract class Entity {
    protected float x;
    protected float y;
    protected float velX;
    protected float velY;
    protected int width;
    protected int height;
    protected boolean active = true;
    protected boolean onGround = false;
    protected boolean facingRight = true;

    public Entity(float x, float y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void update();

    public abstract void render(Graphics2D g, Camera camera);

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, width, height);
    }

    public Rectangle getPredictiveBounds(float deltaX, float deltaY) {
        return new Rectangle((int) (x + deltaX), (int) (y + deltaY), width, height);
    }

    // Getters and Setters
    public float getX() { return x; }
    public void setX(float x) { this.x = x; }

    public float getY() { return y; }
    public void setY(float y) { this.y = y; }

    public float getVelX() { return velX; }
    public void setVelX(float velX) { this.velX = velX; }

    public float getVelY() { return velY; }
    public void setVelY(float velY) { this.velY = velY; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isOnGround() { return onGround; }
    public void setOnGround(boolean onGround) { this.onGround = onGround; }

    public boolean isFacingRight() { return facingRight; }
    public void setFacingRight(boolean facingRight) { this.facingRight = facingRight; }
}
