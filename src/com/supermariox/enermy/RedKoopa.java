package com.supermariox.enermy;

import com.supermariox.audio.SoundManager;
import com.supermariox.graphics.Animation;
import com.supermariox.graphics.AssetManager;
import com.supermariox.graphics.Camera;
import com.supermariox.graphics.SpriteSheet;
import com.supermariox.player.Player;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class RedKoopa extends Enemy {
    private boolean isShell = false;
    private boolean movingShell = false;

    private Animation walkAnimLeft;
    private Animation walkAnimRight;
    private Animation shellAnim;

    public RedKoopa(float x, float y) {
        super(x, y, 32, 44);
        this.velX = -1.2f;
        loadGraphics();
    }

    private void loadGraphics() {
        AssetManager am = AssetManager.getInstance();

        // Red Koopa Walking (npc-4.gif)
        BufferedImage walkSheet = am.getImage("npc/npc-4.gif");
        if (walkSheet != null) {
            SpriteSheet sheet = new SpriteSheet(walkSheet);
            BufferedImage[] frames = sheet.getVerticalFrames(32, 48, 4);
            BufferedImage f1 = frames[0];
            BufferedImage f2 = frames[1];

            walkAnimLeft = new Animation(new BufferedImage[]{f1, f2}, 10, true);
            walkAnimRight = new Animation(new BufferedImage[]{
                    Animation.flipHorizontally(f1),
                    Animation.flipHorizontally(f2)
            }, 10, true);
        }

        // Red Koopa Shell (npc-7.gif)
        BufferedImage shellSheet = am.getImage("npc/npc-7.gif");
        if (shellSheet != null) {
            SpriteSheet sheet = new SpriteSheet(shellSheet);
            BufferedImage[] sFrames = sheet.getVerticalFrames(32, 32, 4);
            shellAnim = new Animation(sFrames, 4, true);
        }
    }

    @Override
    public void update() {
        if (!active) return;

        velY += 0.45f;
        if (velY > 10.0f) velY = 10.0f;

        if (isShell) {
            if (movingShell && shellAnim != null) {
                shellAnim.update();
            }
        } else {
            if (velX > 0 && walkAnimRight != null) {
                walkAnimRight.update();
            } else if (velX <= 0 && walkAnimLeft != null) {
                walkAnimLeft.update();
            }
        }
    }

    @Override
    public void render(Graphics2D g, Camera camera) {
        if (!active) return;

        int renderX = (int) (x - camera.getX());
        int renderY = (int) (y - camera.getY());

        if (isShell) {
            BufferedImage frame = (shellAnim != null) ? shellAnim.getCurrentFrame() : null;
            if (frame != null) {
                g.drawImage(frame, renderX, renderY, width, height, null);
            } else {
                g.setColor(java.awt.Color.RED);
                g.fillOval(renderX, renderY, width, height);
            }
        } else {
            Animation anim = (velX > 0) ? walkAnimRight : walkAnimLeft;
            BufferedImage frame = (anim != null) ? anim.getCurrentFrame() : null;
            if (frame != null) {
                g.drawImage(frame, renderX, renderY, width, height, null);
            } else {
                g.setColor(java.awt.Color.RED);
                g.fillRect(renderX, renderY, width, height);
            }
        }
    }

    @Override
    public void onStomped(Player player) {
        player.setVelY(-6.5f);
        player.addScore(100);
        SoundManager.getInstance().playSound("stomped");

        if (!isShell) {
            isShell = true;
            velX = 0;
            height = 32;
            y += 12;
        } else {
            if (movingShell) {
                movingShell = false;
                velX = 0;
            } else {
                kick(player.getX() < x);
            }
        }
    }

    public void kick(boolean fromLeft) {
        isShell = true;
        movingShell = true;
        velX = fromLeft ? 8.5f : -8.5f;
        SoundManager.getInstance().playSound("shell-hit");
    }

    @Override
    public void onHitByShell() {
        active = false;
    }

    public boolean isShell() { return isShell; }
    public boolean isMovingShell() { return movingShell; }
}
