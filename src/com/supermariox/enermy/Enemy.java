package com.supermariox.enermy;

import com.supermariox.player.Player;

public abstract class Enemy extends Entity {
    protected boolean squished = false;
    protected int squishTimer = 0;

    public Enemy(float x, float y, int width, int height) {
        super(x, y, width, height);
        this.velX = -1.2f; // Default walk left
    }

    public abstract void onStomped(Player player);
    public abstract void onHitByShell();

    public void reverseDirection() {
        velX = -velX;
        facingRight = velX > 0;
    }

    public boolean isSquished() { return squished; }
}
