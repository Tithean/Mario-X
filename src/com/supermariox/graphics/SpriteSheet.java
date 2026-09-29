package com.supermariox.graphics;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

// Slices individual sprites and animation frames out of a single sheet
public class SpriteSheet {
    private final BufferedImage sheet;

    public SpriteSheet(BufferedImage sheet) {
        this.sheet = sheet;
    }

    // Extracts a sub-rectangle with bounds checking
    public BufferedImage getSprite(int x, int y, int width, int height) {
        if (sheet == null) return null;
        if (x < 0 || y < 0 || x + width > sheet.getWidth() || y + height > sheet.getHeight()) {
            return null;
        }
        return sheet.getSubimage(x, y, width, height);
    }

    // Cuts vertically-stacked frames (e.g. Goomba walk cycle)
    public BufferedImage[] getVerticalFrames(int frameWidth, int frameHeight, int count) {
        if (sheet == null) return new BufferedImage[0];
        BufferedImage[] frames = new BufferedImage[count];
        for (int i = 0; i < count; i++) {
            int y = i * frameHeight;
            if (y + frameHeight <= sheet.getHeight()) {
                frames[i] = getSprite(0, y, frameWidth, frameHeight);
            } else {
                frames[i] = frames[Math.max(0, i - 1)];
            }
        }
        return frames;
    }

    // Cuts horizontally-arranged frames (e.g. Mario run cycle)
    public BufferedImage[] getHorizontalFrames(int frameWidth, int frameHeight, int count) {
        if (sheet == null) return new BufferedImage[0];
        BufferedImage[] frames = new BufferedImage[count];
        for (int i = 0; i < count; i++) {
            int x = i * frameWidth;
            if (x + frameWidth <= sheet.getWidth()) {
                frames[i] = getSprite(x, 0, frameWidth, frameHeight);
            } else {
                frames[i] = frames[Math.max(0, i - 1)];
            }
        }
        return frames;
    }

    // Cuts 2D grid matrix of sprites (rows x cols)
    public BufferedImage[][] getGridFrames(int cols, int rows, int cellWidth, int cellHeight) {
        if (sheet == null) return new BufferedImage[0][0];
        BufferedImage[][] grid = new BufferedImage[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = c * cellWidth;
                int y = r * cellHeight;
                if (x + cellWidth <= sheet.getWidth() && y + cellHeight <= sheet.getHeight()) {
                    grid[r][c] = getSprite(x, y, cellWidth, cellHeight);
                }
            }
        }
        return grid;
    }

    public BufferedImage getSheet() {
        return sheet;
    }
}
