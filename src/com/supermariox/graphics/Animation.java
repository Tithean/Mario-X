package com.supermariox.graphics;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class Animation {
    private BufferedImage[] frames;
    private int frameDelay;
    private int frameTimer;
    private int currentFrame;
    private boolean loop;
    private boolean finished;

    public Animation(BufferedImage[] frames, int frameDelay, boolean loop) {
        this.frames = frames != null ? frames : new BufferedImage[0];
        this.frameDelay = Math.max(1, frameDelay);
        this.loop = loop;
        this.frameTimer = 0;
        this.currentFrame = 0;
        this.finished = false;
    }

    public Animation(BufferedImage frame) {
        this(new BufferedImage[]{frame}, 1, true);
    }

    public void update() {
        if (frames.length <= 1 || finished) return;

        frameTimer++;
        if (frameTimer >= frameDelay) {
            frameTimer = 0;
            currentFrame++;
            if (currentFrame >= frames.length) {
                if (loop) {
                    currentFrame = 0;
                } else {
                    currentFrame = frames.length - 1;
                    finished = true;
                }
            }
        }
    }

    public BufferedImage getCurrentFrame() {
        if (frames == null || frames.length == 0) return null;
        if (currentFrame < 0 || currentFrame >= frames.length) return frames[0];
        return frames[currentFrame];
    }

    public void reset() {
        currentFrame = 0;
        frameTimer = 0;
        finished = false;
    }

    public boolean isFinished() {
        return finished;
    }

    public int getCurrentFrameIndex() {
        return currentFrame;
    }

    public void setFrameDelay(int delay) {
        this.frameDelay = Math.max(1, delay);
    }

    public static BufferedImage flipHorizontally(BufferedImage image) {
        if (image == null) return null;
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage flipped = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = flipped.createGraphics();
        g.drawImage(image, w, 0, -w, h, null);
        g.dispose();
        return flipped;
    }
}
