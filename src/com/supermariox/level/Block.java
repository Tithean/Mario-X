package com.supermariox.level;

import com.supermariox.graphics.Animation;
import com.supermariox.graphics.AssetManager;
import com.supermariox.graphics.Camera;
import com.supermariox.graphics.SpriteSheet;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class Block {
    public enum BlockType {
        BRICK,
        QUESTION_COIN,
        QUESTION_MUSHROOM,
        SOLID,
        USED
    }

    private final float x;
    private final float y;
    private final int width = 32;
    private final int height = 32;

    private BlockType type;
    private boolean active = true;
    private boolean destroyed = false;

    // Bounce effect
    private float bounceY = 0;
    private float bounceVelY = 0;
    private boolean bouncing = false;

    private Animation animation;
    private BufferedImage staticImage;

    public Block(float x, float y, BlockType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        loadGraphics();
    }

    private void loadGraphics() {
        AssetManager am = AssetManager.getInstance();

        if (type == BlockType.QUESTION_COIN || type == BlockType.QUESTION_MUSHROOM) {
            BufferedImage sheetImg = am.getImage("block/block-4.gif");
            if (sheetImg != null) {
                SpriteSheet sheet = new SpriteSheet(sheetImg);
                BufferedImage[] frames = sheet.getVerticalFrames(32, 32, 4);
                animation = new Animation(frames, 8, true);
            }
        } else if (type == BlockType.BRICK) {
            staticImage = am.getImage("block/block-1.gif");
        } else if (type == BlockType.SOLID) {
            staticImage = am.getImage("block/block-5.gif");
        } else if (type == BlockType.USED) {
            staticImage = am.getImage("block/block-29.gif");
        }
    }

    public void update() {
        if (animation != null && type != BlockType.USED) {
            animation.update();
        }

        // Bounce Physics
        if (bouncing) {
            bounceY += bounceVelY;
            bounceVelY += 0.8f; // Gravity for bounce
            if (bounceY >= 0) {
                bounceY = 0;
                bounceVelY = 0;
                bouncing = false;
            }
        }
    }

    public void render(Graphics2D g, Camera camera) {
        if (destroyed) return;

        int renderX = (int) (x - camera.getX());
        int renderY = (int) (y + bounceY - camera.getY());

        // Culling
        if (renderX + width < 0 || renderX > camera.getViewportWidth() ||
            renderY + height < 0 || renderY > camera.getViewportHeight()) {
            return;
        }

        BufferedImage imgToDraw = staticImage;
        if (type == BlockType.USED) {
            imgToDraw = AssetManager.getInstance().getImage("block/block-29.gif");
        } else if (animation != null) {
            imgToDraw = animation.getCurrentFrame();
        }

        if (imgToDraw != null) {
            g.drawImage(imgToDraw, renderX, renderY, width, height, null);
        } else {
            g.setColor(java.awt.Color.YELLOW);
            g.fillRect(renderX, renderY, width, height);
        }
    }

    public boolean bump(boolean isSuperPlayer) {
        if (bouncing || type == BlockType.USED || type == BlockType.SOLID) {
            return false;
        }

        bouncing = true;
        bounceY = -6.0f;
        bounceVelY = -2.0f;

        if (type == BlockType.BRICK) {
            if (isSuperPlayer) {
                destroyed = true;
                active = false;
                return true; // Destroyed
            }
        } else if (type == BlockType.QUESTION_COIN || type == BlockType.QUESTION_MUSHROOM) {
            type = BlockType.USED;
            loadGraphics();
        }
        return false;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) (y + bounceY), width, height);
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public BlockType getType() { return type; }
    public boolean isActive() { return active; }
    public boolean isDestroyed() { return destroyed; }
}
