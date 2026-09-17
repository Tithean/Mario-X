package com.supermariox.enermy;

import com.supermariox.graphics.Animation;
import com.supermariox.graphics.AssetManager;
import com.supermariox.graphics.Camera;
import com.supermariox.graphics.SpriteSheet;
import com.supermariox.player.Player;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class PiranhaPlant extends Enemy {
    private Animation anim;
    private final float baseOffsetY;
    private float currentOffsetY = 0; // 0 = hidden inside pipe, 32 = fully emerged
    private int stateTimer = 0;
    private boolean emerging = true;

    public PiranhaPlant(float x, float y) {
        super(x, y, 32, 32);
        this.baseOffsetY = y;
        this.velX = 0;
        this.velY = 0;
        loadGraphics();
    }

    private void loadGraphics() {
        AssetManager am = AssetManager.getInstance();
        BufferedImage sheetImg = am.getImage("npc/npc-11.gif");

        if (sheetImg != null) {
            SpriteSheet sheet = new SpriteSheet(sheetImg);
            BufferedImage[] frames = sheet.getVerticalFrames(32, 32, 2);
            anim = new Animation(frames, 8, true);
        }
    }

    @Override
    public void update() {
        if (!active) return;

        if (anim != null) {
            anim.update();
        }

        // Emerging & Retracting cycle inside pipe
        stateTimer++;
        if (emerging) {
            if (currentOffsetY < 32) {
                currentOffsetY += 0.8f;
            } else {
                if (stateTimer >= 80) { // Stay emerged for 80 ticks
                    emerging = false;
                    stateTimer = 0;
                }
            }
        } else {
            if (currentOffsetY > 0) {
                currentOffsetY -= 0.8f;
            } else {
                if (stateTimer >= 80) { // Stay hidden inside pipe for 80 ticks
                    emerging = true;
                    stateTimer = 0;
                }
            }
        }

        y = baseOffsetY - currentOffsetY;
    }

    @Override
    public void render(Graphics2D g, Camera camera) {
        if (!active || currentOffsetY <= 2) return;

        int renderX = (int) (x - camera.getX());
        int renderY = (int) (y - camera.getY());

        if (anim != null && anim.getCurrentFrame() != null) {
            g.drawImage(anim.getCurrentFrame(), renderX, renderY, width, (int) currentOffsetY, null);
        } else {
            g.setColor(java.awt.Color.RED);
            g.fillRect(renderX, renderY, width, (int) currentOffsetY);
        }
    }

    @Override
    public void onStomped(Player player) {
        // Piranha Plants CANNOT be stomped! Stomping damages Mario!
        player.takeDamage();
    }

    @Override
    public void onHitByShell() {
        active = false;
    }
}
