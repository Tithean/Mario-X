package com.supermariox.level;

import com.supermariox.graphics.Camera;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class Tile {
    private final float x;
    private final float y;
    private final int width;
    private final int height;
    private final boolean solid;
    private final BufferedImage image;

    public Tile(float x, float y, int width, int height, boolean solid, BufferedImage image) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.solid = solid;
        this.image = image;
    }

    public void render(Graphics2D g, Camera camera) {
        if (image == null) return;
        int renderX = (int) (x - camera.getX());
        int renderY = (int) (y - camera.getY());

        // Viewport culling check
        if (renderX + width < 0 || renderX > camera.getViewportWidth() ||
            renderY + height < 0 || renderY > camera.getViewportHeight()) {
            return;
        }

        g.drawImage(image, renderX, renderY, width, height, null);
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, width, height);
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean isSolid() { return solid; }
    public BufferedImage getImage() { return image; }
}
