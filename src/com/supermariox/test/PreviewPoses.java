package com.supermariox.test;

import com.supermariox.graphics.AssetManager;
import com.supermariox.graphics.SpriteSheet;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

public class PreviewPoses {
    public static void main(String[] args) {
        AssetManager am = AssetManager.getInstance();
        BufferedImage sheetImg = am.getImage("mario/mario-1.gif");
        if (sheetImg == null) return;

        SpriteSheet sheet = new SpriteSheet(sheetImg);
        BufferedImage[][] grid = sheet.getGridFrames(10, 10, 100, 100);

        BufferedImage preview = new BufferedImage(700, 220, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = preview.createGraphics();
        g.setColor(new Color(100, 150, 255));
        g.fillRect(0, 0, 700, 220);

        // Standard SMBX Super Mario Poses:
        // [0][4]: Idle Standing
        // [0][5]: Walk Frame 1
        // [1][4]: Walk Frame 2
        // [1][5]: Walk Frame 3
        // [2][5]: Jump
        // [2][4]: Crouch
        // [0][8]: Skid
        Object[][] poses = {
            {"Idle", grid[0][4]},
            {"Run 1", grid[0][5]},
            {"Run 2", grid[1][4]},
            {"Run 3", grid[1][5]},
            {"Jump", grid[2][5]},
            {"Crouch", grid[2][4]},
            {"Skid", grid[0][8]}
        };

        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.setColor(Color.WHITE);

        int x = 20;
        for (Object[] p : poses) {
            String name = (String) p[0];
            BufferedImage frame = (BufferedImage) p[1];
            if (frame != null) {
                g.drawImage(frame, x, 30, 80, 80, null);
            }
            g.drawString(name, x + 10, 140);
            x += 95;
        }

        g.dispose();
        try {
            ImageIO.write(preview, "png", new File("C:/Users/Thean/.gemini/antigravity/brain/5c8235e9-3252-427c-941a-f0ca90480ce0/mario_poses_preview.png"));
            System.out.println("PREVIEW_RENDERED_OK");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
