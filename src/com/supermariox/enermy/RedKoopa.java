package com.supermariox.enermy;

import com.supermariox.graphics.Animation;
import com.supermariox.graphics.AssetManager;
import com.supermariox.graphics.Camera;
import com.supermariox.graphics.SpriteSheet;
import com.supermariox.player.Player;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class RedKoopa extends Enemy {
    private static final float WALK_SPEED = 1.2f;
    private static final float SHELL_SPEED = 8.5f;

    // After being stomped into a shell, ignore kick inputs for this many frames
    private static final int SHELL_SETTLE_FRAMES = 20;

    private boolean isShell = false;
    private boolean movingShell = false;
    private int shellSettleTimer = 0; // counts down to 0; kick blocked while > 0

    private Animation walkAnimLeft;
    private Animation walkAnimRight;
    private Animation shellAnim;

    public RedKoopa(float x, float y) {
        super(x, y, 32, 44);
        this.velX = -WALK_SPEED;
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
            BufferedImage f2 = (frames.length > 1) ? frames[1] : frames[0];

            if (f1 != null && f2 != null) {
                walkAnimLeft = new Animation(new BufferedImage[]{f1, f2}, 10, true);
                walkAnimRight = new Animation(new BufferedImage[]{
                        Animation.flipHorizontally(f1),
                        Animation.flipHorizontally(f2)
                }, 10, true);
            } else if (f1 != null) {
                walkAnimLeft = new Animation(f1);
                walkAnimRight = new Animation(Animation.flipHorizontally(f1));
            }
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
            // Tick down the settle cooldown
            if (shellSettleTimer > 0) shellSettleTimer--;

            if (movingShell && shellAnim != null) {
                shellAnim.update();
            }
        } else {
            // Guard against velX=0 stuck state for a walking RedKoopa
            if (velX == 0) {
                velX = facingRight ? WALK_SPEED : -WALK_SPEED;
            }

            if (velX > 0 && walkAnimRight != null) {
                walkAnimRight.update();
            } else if (velX < 0 && walkAnimLeft != null) {
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

        if (!isShell) {
            // 1st stomp: turn into a still shell — stays in place
            isShell = true;
            movingShell = false;
            velX = 0;
            height = 32;
            y += 12;
            shellSettleTimer = SHELL_SETTLE_FRAMES;
            player.addScore(100);
        } else {
            // 2nd stomp on shell: RedKoopa dies
            active = false;
            player.addScore(200);
        }
    }

    public void kick(boolean fromLeft) {
        isShell = true;
        movingShell = true;
        shellSettleTimer = 0;
        velX = fromLeft ? SHELL_SPEED : -SHELL_SPEED;
    }

    @Override
    public void onHitByShell() {
        active = false;
    }

    public boolean isShell() { return isShell; }
    public boolean isMovingShell() { return movingShell; }

    /** True while the shell is settling after a stomp — cannot be kicked yet. */
    public boolean isSettling() { return shellSettleTimer > 0; }
}
