package com.supermariox.player;

import com.supermariox.audio.PlayerSoundEffects;
import com.supermariox.audio.SoundManager;
import com.supermariox.enermy.Entity;
import com.supermariox.graphics.Animation;
import com.supermariox.graphics.AssetManager;
import com.supermariox.graphics.Camera;
import com.supermariox.graphics.SpriteSheet;
import com.supermariox.input.InputHandler;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class Player extends Entity {
    // Movement Physics Tuning
    private static final float ACCELERATION = 0.45f;
    private static final float MAX_WALK_SPEED = 4.0f;
    private static final float MAX_RUN_SPEED = 6.5f;
    private static final float FRICTION = 0.82f;
    private static final float GRAVITY = 0.45f;
    private static final float JUMP_FORCE = -9.8f;
    private static final float MAX_FALL_SPEED = 10.5f;

    // Jump buffer & coyote time
    private int jumpBufferTimer = 0;
    private int coyoteTimer = 0;
    private int maxJumps = 2; // Double jump
    private int jumpsRemaining = 2;

    private PlayerState currentState = PlayerState.IDLE;
    private PlayerPower currentPower = PlayerPower.SUPER; // Super Mario only

    private int lives = 3;
    private int score = 0;
    private int coins = 0;

    private boolean invulnerable = false;
    private int invulnerableTimer = 0;

    // Animations map: Key = Power + "_" + State + "_" + (facingRight ? "R" : "L")
    private final Map<String, Animation> animations = new HashMap<>();

    public Player(float x, float y) {
        super(x, y, 30, 54);
        loadAnimations();
    }

    private void loadAnimations() {
        AssetManager am = AssetManager.getInstance();
        loadPowerAnimations(am, "mario-2.gif", PlayerPower.SUPER);
        loadPowerAnimations(am, "mario-1.gif", PlayerPower.SMALL);
        loadPowerAnimations(am, "mario-3.gif", PlayerPower.FIRE);
    }

    private void loadPowerAnimations(AssetManager am, String sheetName, PlayerPower power) {
        BufferedImage sheetImg = am.getImage("mario/" + sheetName);
        if (sheetImg == null) return;

        SpriteSheet sheet = new SpriteSheet(sheetImg);
        BufferedImage[][] grid = sheet.getGridFrames(10, 10, 100, 100);

        // Standard Super Mario World / SMBX walking & running frames:
        // [0][4] = Idle standing
        // [0][5] = Run frame 1 (stride)
        // [1][4] = Run frame 2 (passing)
        // [1][5] = Run frame 3 (reach)
        // [2][5] = Jump
        // [2][4] = Crouch
        // [0][8] = Skid / Turn
        // [0][9] = Dead
        BufferedImage idleFrame = autoCrop(grid[0][4]);
        BufferedImage run1 = autoCrop(grid[0][5]);
        BufferedImage run2 = autoCrop(grid[1][4]);
        BufferedImage run3 = autoCrop(grid[1][5]);
        BufferedImage jumpFrame = autoCrop(grid[2][5]);
        BufferedImage skidFrame = autoCrop(grid[0][8]);
        BufferedImage crouchFrame = autoCrop(grid[2][4]);
        BufferedImage deadFrame = autoCrop(grid[0][9]);

        // Fallbacks
        if (idleFrame == null) idleFrame = autoCrop(grid[0][5]);
        if (run1 == null) run1 = idleFrame;
        if (run2 == null) run2 = idleFrame;
        if (run3 == null) run3 = idleFrame;
        if (jumpFrame == null) jumpFrame = idleFrame;
        if (skidFrame == null) skidFrame = idleFrame;
        if (crouchFrame == null) crouchFrame = idleFrame;
        if (deadFrame == null) deadFrame = idleFrame;

        // Right animations
        animations.put(power + "_IDLE_R", new Animation(idleFrame));
        animations.put(power + "_RUNNING_R", new Animation(new BufferedImage[]{run1, run2, run3}, 6, true));
        animations.put(power + "_JUMPING_R", new Animation(jumpFrame));
        animations.put(power + "_FALLING_R", new Animation(jumpFrame));
        animations.put(power + "_SKIDDING_R", new Animation(skidFrame));
        animations.put(power + "_CROUCHING_R", new Animation(crouchFrame));
        animations.put(power + "_DEAD_R", new Animation(deadFrame));

        // Left animations (flipped horizontally)
        animations.put(power + "_IDLE_L", new Animation(Animation.flipHorizontally(idleFrame)));
        animations.put(power + "_RUNNING_L", new Animation(new BufferedImage[]{
                Animation.flipHorizontally(run1),
                Animation.flipHorizontally(run2),
                Animation.flipHorizontally(run3)
        }, 6, true));
        animations.put(power + "_JUMPING_L", new Animation(Animation.flipHorizontally(jumpFrame)));
        animations.put(power + "_FALLING_L", new Animation(Animation.flipHorizontally(jumpFrame)));
        animations.put(power + "_SKIDDING_L", new Animation(Animation.flipHorizontally(skidFrame)));
        animations.put(power + "_CROUCHING_L", new Animation(Animation.flipHorizontally(crouchFrame)));
        animations.put(power + "_DEAD_L", new Animation(Animation.flipHorizontally(deadFrame)));
    }

    private BufferedImage autoCrop(BufferedImage cell) {
        if (cell == null) return null;
        int w = cell.getWidth();
        int h = cell.getHeight();

        int minX = w, minY = h, maxX = -1, maxY = -1;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int alpha = (cell.getRGB(x, y) >> 24) & 0xFF;
                if (alpha > 20) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (maxX < minX || maxY < minY) return null;
        return cell.getSubimage(minX, minY, (maxX - minX + 1), (maxY - minY + 1));
    }

    public void handleInput(InputHandler input) {
        if (currentState == PlayerState.DEAD || currentState == PlayerState.VICTORY) return;

        boolean running = input.isRunOrFire();
        float maxSpeed = running ? MAX_RUN_SPEED : MAX_WALK_SPEED;

        // Horizontal Movement
        if (input.isLeft()) {
            velX -= ACCELERATION;
            if (velX < -maxSpeed) velX = -maxSpeed;
            facingRight = false;
        } else if (input.isRight()) {
            velX += ACCELERATION;
            if (velX > maxSpeed) velX = maxSpeed;
            facingRight = true;
        } else {
            velX *= FRICTION;
            if (Math.abs(velX) < 0.1f) velX = 0;
        }

        // Jump Input Check (Buffer jump request if Space/Jump pressed or held)
        if (input.isJumpJustPressed()) {
            jumpBufferTimer = 10;
        }

        // Variable Jump Cut (Releasing jump key early cuts upward velocity)
        if (!input.isJumpHeld() && velY < -3.2f) {
            velY = -3.2f;
        }

        // Crouch
        if (input.isCrouch() && onGround) {
            currentState = PlayerState.CROUCHING;
        }
    }

    @Override
    public void update() {
        if (currentState == PlayerState.DEAD) {
            velY += GRAVITY;
            y += velY;
            return;
        }

        // Update Timers
        if (jumpBufferTimer > 0) jumpBufferTimer--;

        if (onGround) {
            coyoteTimer = 6;
            jumpsRemaining = maxJumps;
        } else if (coyoteTimer > 0) {
            coyoteTimer--;
        }

        // Execute First Jump (Ground / Coyote)
        if (jumpBufferTimer > 0 && coyoteTimer > 0) {
            velY = JUMP_FORCE;
            SoundManager.getInstance().playSound(PlayerSoundEffects.JUMP);
            onGround = false;
            coyoteTimer = 0;
            jumpBufferTimer = 0;
            jumpsRemaining = maxJumps - 1; // Used one jump
        }
        // Execute Double Jump (in air, still has a jump left)
        else if (jumpBufferTimer > 0 && jumpsRemaining > 0 && !onGround && coyoteTimer == 0) {
            velY = JUMP_FORCE * 0.92f; // Slightly smaller double jump
            SoundManager.getInstance().playSound(PlayerSoundEffects.JUMP);
            jumpBufferTimer = 0;
            jumpsRemaining--;
        }

        // Apply Gravity
        velY += GRAVITY;
        if (velY > MAX_FALL_SPEED) velY = MAX_FALL_SPEED;

        // Handle Invulnerability Timer
        if (invulnerable) {
            invulnerableTimer--;
            if (invulnerableTimer <= 0) {
                invulnerable = false;
            }
        }

        // Determine current state for animation
        if (!onGround) {
            if (velY < 0) {
                currentState = PlayerState.JUMPING;
            } else {
                currentState = PlayerState.FALLING;
            }
        } else if (Math.abs(velX) > 0.3f) {
            if ((velX > 0 && !facingRight) || (velX < 0 && facingRight)) {
                currentState = PlayerState.SKIDDING;
            } else {
                currentState = PlayerState.RUNNING;
            }
        } else if (currentState != PlayerState.CROUCHING) {
            currentState = PlayerState.IDLE;
        }

        // Update active animation frame
        Animation anim = getCurrentAnimation();
        if (anim != null) {
            anim.update();
        }

        // Dimensions for Super Mario
        width = 30;
        height = (currentState == PlayerState.CROUCHING) ? 36 : 54;
    }

    @Override
    public void render(Graphics2D g, Camera camera) {
        int renderX = (int) (x - camera.getX());
        int renderY = (int) (y - camera.getY());

        // Flash during invulnerability
        if (invulnerable && (invulnerableTimer / 4) % 2 == 0) {
            return;
        }

        Animation anim = getCurrentAnimation();
        if (anim != null && anim.getCurrentFrame() != null) {
            BufferedImage frame = anim.getCurrentFrame();
            g.drawImage(frame, renderX, renderY, width, height, null);
        } else {
            g.setColor(Color.RED);
            g.fillRect(renderX, renderY, width, height);
        }
    }

    private Animation getCurrentAnimation() {
        String key = currentPower + "_" + currentState + "_" + (facingRight ? "R" : "L");
        Animation anim = animations.get(key);
        if (anim == null) {
            key = currentPower + "_IDLE_" + (facingRight ? "R" : "L");
            anim = animations.get(key);
        }
        return anim;
    }

    public void grow() {
        score += 1000;
        SoundManager.getInstance().playSound(PlayerSoundEffects.GROW);
    }

    public void takeDamage() {
        if (invulnerable || currentState == PlayerState.DEAD) return;
        die();
    }

    public void die() {
        if (currentState == PlayerState.DEAD) return;
        SoundManager.getInstance().playSound(PlayerSoundEffects.DIED);
        currentState = PlayerState.DEAD;
        velY = -9.0f;
        velX = 0;
        lives--;
    }

    public void respawn(float spawnX, float spawnY) {
        this.x = spawnX;
        this.y = spawnY;
        this.velX = 0;
        this.velY = 0;
        this.currentState = PlayerState.IDLE;
        this.currentPower = PlayerPower.SUPER;
        this.width = 30;
        this.height = 54;
        this.jumpsRemaining = maxJumps;
        this.jumpBufferTimer = 0;
        this.coyoteTimer = 0;
        makeInvulnerable(120); // 2 seconds spawn protection
    }

    public void makeInvulnerable(int ticks) {
        invulnerable = true;
        invulnerableTimer = ticks;
    }

    public void addCoins(int amount) {
        coins += amount;
        score += amount * 200;
        if (coins >= 100) {
            coins -= 100;
            lives++;
        }
    }

    public void addScore(int amount) { score += amount; }
    public void addLife() { lives++; }
    public void setScore(int score) { this.score = score; }
    public void setCoins(int coins) { this.coins = coins; }

    public PlayerState getCurrentState() { return currentState; }
    public void setCurrentState(PlayerState state) { this.currentState = state; }

    public PlayerPower getCurrentPower() { return currentPower; }
    public void setCurrentPower(PlayerPower power) { this.currentPower = power; }

    public int getLives() { return lives; }
    public void setLives(int lives) { this.lives = lives; }

    public int getScore() { return score; }
    public int getCoins() { return coins; }
    public boolean isInvulnerable() { return invulnerable; }
}

