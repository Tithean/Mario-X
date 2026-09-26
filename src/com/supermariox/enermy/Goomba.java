package com.supermariox.enermy;

import com.supermariox.graphics.Animation;
import com.supermariox.graphics.AssetManager;
import com.supermariox.graphics.Camera;
import com.supermariox.graphics.SpriteSheet;
import com.supermariox.player.Player;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class Goomba extends Enemy {
    private static final float WALK_SPEED = 1.2f;

    private Animation walkAnim;
    private BufferedImage squishedImg;

    public Goomba(float x, float y) {
        super(x, y, 32, 32);
        this.velX = -WALK_SPEED; // Walk left by default
        loadGraphics();
    }

    private void loadGraphics() {
        AssetManager am = AssetManager.getInstance();
        BufferedImage sheetImg = am.getImage("npc/npc-1.gif");

        if (sheetImg != null) {
            SpriteSheet sheet = new SpriteSheet(sheetImg);
            // npc-1.gif has 2 walk frames stacked vertically, each 32x32
            BufferedImage[] frames = sheet.getVerticalFrames(32, 32, 2);
            if (frames[0] != null && frames[1] != null) {
                walkAnim = new Animation(frames, 10, true);
            } else if (frames[0] != null) {
                walkAnim = new Animation(frames[0]);
            }

            // Squished frame: a thin flat sprite at the bottom of the sheet (row 2, height 16)
            if (sheetImg.getHeight() >= 80) {
                squishedImg = sheet.getSprite(0, 64, 32, 16);
            }
            // Fallback: squash first walk frame to half height
            if (squishedImg == null && frames[0] != null) {
                squishedImg = frames[0];
            }
        }
    }

    @Override
    public void update() {
        if (!active) return;

        if (squished) {
            squishTimer++;
            if (squishTimer >= 20) {
                active = false;
            }
            return;
        }

        // Keep walk speed constant — velX should never be 0 for a live Goomba.
        // This guards against any sticking bug that zeroes out velX without reversing.
        if (velX == 0) {
            velX = facingRight ? WALK_SPEED : -WALK_SPEED;
        }

        // Apply gravity
        velY += 0.45f;
        if (velY > 10.0f) velY = 10.0f;

        // Update walk animation
        if (walkAnim != null) {
            walkAnim.update();
        }
    }

    @Override
    public void render(Graphics2D g, Camera camera) {
        if (!active) return;

        int renderX = (int) (x - camera.getX());
        int renderY = (int) (y - camera.getY());

        if (squished) {
            if (squishedImg != null) {
                g.drawImage(squishedImg, renderX, renderY + 16, 32, 16, null);
            } else {
                g.setColor(java.awt.Color.ORANGE);
                g.fillRect(renderX, renderY + 16, 32, 16);
            }
            return;
        }

        if (walkAnim != null && walkAnim.getCurrentFrame() != null) {
            g.drawImage(walkAnim.getCurrentFrame(), renderX, renderY, width, height, null);
        } else {
            g.setColor(java.awt.Color.ORANGE);
            g.fillRect(renderX, renderY, width, height);
        }
    }

    @Override
    public void onStomped(Player player) {
        if (squished) return;
        squished = true;
        velX = 0;
        velY = 0;
        height = 16;
        y += 16;
        player.addScore(100);
        player.setVelY(-6.5f); // Player bounces up
    }

    @Override
    public void onHitByShell() {
        active = false;
    }
}
