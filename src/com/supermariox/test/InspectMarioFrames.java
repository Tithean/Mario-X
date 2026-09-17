package com.supermariox.test;

import com.supermariox.graphics.AssetManager;
import com.supermariox.graphics.SpriteSheet;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class InspectMarioFrames {
    public static void main(String[] args) {
        AssetManager am = AssetManager.getInstance();
        BufferedImage sheetImg = am.getImage("mario/mario-2.gif");
        if (sheetImg == null) {
            System.out.println("mario-2.gif not found");
            return;
        }

        SpriteSheet sheet = new SpriteSheet(sheetImg);
        BufferedImage[][] grid = sheet.getGridFrames(10, 10, 100, 100);

        new File("mario_frames").mkdirs();
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                BufferedImage cell = grid[r][c];
                if (cell != null) {
                    try {
                        ImageIO.write(cell, "png", new File("mario_frames/m2_r" + r + "_c" + c + ".png"));
                    } catch (Exception e) {}
                }
            }
        }
        System.out.println("FRAMES_EXTRACTED_OK");
    }
}
