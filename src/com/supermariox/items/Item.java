package com.supermariox.items;

import com.supermariox.enermy.Entity;
import com.supermariox.graphics.Animation;
import com.supermariox.graphics.AssetManager;
import com.supermariox.graphics.Camera;
import com.supermariox.graphics.SpriteSheet;
import com.supermariox.player.Player;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class Item extends Entity {
    public enum ItemType {
        MUSHROOM,
        COIN,
        ONE_UP
    }

    private final ItemType type;
    private Animation animation;
    private BufferedImage staticImage;

    // Spawning animation (popping up from block)
    private boolean spawning = true;
    private float spawnTargetY;

    public Item(float x, float y, ItemType type) {
        super(x, y, 32, 32);
        this.type = type;
        this.spawnTargetY = y - 32;
        this.velY = -1.5f;

        if (type == ItemType.MUSHROOM || type == ItemType.ONE_UP) {
            this.velX = 1.2f;
        }

        loadGraphics();
    }

    private void loadGraphics() {
        AssetManager am = AssetManager.getInstance();

        if (type == ItemType.MUSHROOM) {
            staticImage = am.getImage("npc/npc-9.gif");
        } else if (type == ItemType.ONE_UP) {
            staticImage = am.getImage("npc/npc-21.gif");
        } else if (type == ItemType.COIN) {
            BufferedImage sheetImg = am.getImage("npc/npc-10.gif");
            if (sheetImg != null) {
                SpriteSheet sheet = new SpriteSheet(sheetImg);
                BufferedImage[] frames = sheet.getVerticalFrames(28, 32, 4);
                animation = new Animation(frames, 6, true);
            } else {
                staticImage = am.getImage("npc/npc-14.gif");
            }
        }
    }

    @Override
    public void update() {
        if (!active) return;

        // Spawning animation from block
        if (spawning) {
            y += velY;
            if (y <= spawnTargetY) {
                y = spawnTargetY;
                spawning = false;
                velY = 0;
            }
            return;
        }

        // Animated coin update
        if (animation != null) {
            animation.update();
        }

        // Mushroom physics (gravity + wall bounce)
        if (type == ItemType.MUSHROOM || type == ItemType.ONE_UP) {
            velY += 0.45f;
            if (velY > 8.0f) velY = 8.0f;
        }
    }

    @Override
    public void render(Graphics2D g, Camera camera) {
        if (!active) return;

        int renderX = (int) (x - camera.getX());
        int renderY = (int) (y - camera.getY());

        BufferedImage img = staticImage;
        if (animation != null) {
            img = animation.getCurrentFrame();
        }

        if (img != null) {
            g.drawImage(img, renderX, renderY, width, height, null);
        } else {
            g.setColor(type == ItemType.COIN ? java.awt.Color.YELLOW : java.awt.Color.RED);
            g.fillOval(renderX, renderY, width, height);
        }
    }

    public void collect(Player player) {
        if (!active) return;
        active = false;

        if (type == ItemType.COIN) {
            player.addCoins(1);
        } else if (type == ItemType.MUSHROOM) {
            player.grow();
        } else if (type == ItemType.ONE_UP) {
            player.addLife();
        }
    }

    public void reverseDirection() {
        velX = -velX;
    }

    public ItemType getType() { return type; }
    public boolean isSpawning() { return spawning; }
}
