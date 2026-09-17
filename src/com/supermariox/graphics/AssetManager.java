package com.supermariox.graphics;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AssetManager {
    private static AssetManager instance;

    private final String basePath;
    private final Map<String, BufferedImage> imageCache = new HashMap<>();

    private AssetManager() {
        // Resolve assets directory path
        File testPath = new File("src/assets/graphics");
        if (testPath.exists()) {
            basePath = "src/assets/graphics/";
        } else {
            File testPath2 = new File("assets/graphics");
            if (testPath2.exists()) {
                basePath = "assets/graphics/";
            } else {
                basePath = "src/assets/graphics/";
            }
        }
    }

    public static synchronized AssetManager getInstance() {
        if (instance == null) {
            instance = new AssetManager();
        }
        return instance;
    }

    public BufferedImage getImage(String relativePath) {
        if (imageCache.containsKey(relativePath)) {
            return imageCache.get(relativePath);
        }

        BufferedImage img = loadImageWithMask(relativePath);
        if (img != null) {
            imageCache.put(relativePath, img);
        } else {
            System.err.println("[AssetManager] Failed to load image: " + relativePath);
            img = createPlaceholderImage(32, 32, Color.MAGENTA);
            imageCache.put(relativePath, img);
        }
        return img;
    }

    public BufferedImage loadImageWithMask(String relativePath) {
        File baseFile = new File(basePath + relativePath);
        if (!baseFile.exists()) {
            // Try direct path if absolute or relative from project root
            baseFile = new File(relativePath);
            if (!baseFile.exists()) {
                return null;
            }
        }

        try {
            BufferedImage baseImg = ImageIO.read(baseFile);
            if (baseImg == null) return null;

            // Check if mask file exists (e.g. npc-1.gif -> npc-1m.gif)
            String pathStr = baseFile.getAbsolutePath();
            String maskPathStr;
            if (pathStr.endsWith(".gif")) {
                maskPathStr = pathStr.substring(0, pathStr.length() - 4) + "m.gif";
            } else if (pathStr.endsWith(".png")) {
                maskPathStr = pathStr.substring(0, pathStr.length() - 4) + "m.png";
            } else {
                maskPathStr = pathStr + "m";
            }

            File maskFile = new File(maskPathStr);
            if (maskFile.exists()) {
                BufferedImage maskImg = ImageIO.read(maskFile);
                if (maskImg != null) {
                    return applySMBXMask(baseImg, maskImg);
                }
            }

            // Fallback: If base image is INT_ARGB or standard index GIF with transparency
            if (baseImg.getType() == BufferedImage.TYPE_INT_ARGB) {
                return baseImg;
            }

            // Convert to TYPE_INT_ARGB and key out black if it's a solid black background without alpha
            return convertToARGB(baseImg, true);

        } catch (IOException e) {
            System.err.println("[AssetManager] IOException reading " + relativePath + ": " + e.getMessage());
            return null;
        }
    }

    private BufferedImage applySMBXMask(BufferedImage base, BufferedImage mask) {
        int w = base.getWidth();
        int h = base.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        int mw = mask.getWidth();
        int mh = mask.getHeight();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int baseRgb = base.getRGB(x, y);

                int alpha = 255;
                if (x < mw && y < mh) {
                    int maskRgb = mask.getRGB(x, y);
                    int maskR = (maskRgb >> 16) & 0xFF;
                    int maskG = (maskRgb >> 8) & 0xFF;
                    int maskB = maskRgb & 0xFF;

                    int maskBrightness = (maskR + maskG + maskB) / 3;
                    alpha = 255 - maskBrightness;
                }

                if (alpha < 20) {
                    alpha = 0;
                }

                int r = (baseRgb >> 16) & 0xFF;
                int g = (baseRgb >> 8) & 0xFF;
                int b = baseRgb & 0xFF;

                int finalRgb = (alpha << 24) | (r << 16) | (g << 8) | b;
                result.setRGB(x, y, finalRgb);
            }
        }
        return result;
    }

    private BufferedImage convertToARGB(BufferedImage src, boolean keyBlack) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage argb = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = argb.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return argb;
    }

    public BufferedImage createPlaceholderImage(int width, int height, Color color) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, width - 1, height - 1);
        g.dispose();
        return img;
    }

    public SpriteSheet getSpriteSheet(String relativePath) {
        BufferedImage img = getImage(relativePath);
        return new SpriteSheet(img);
    }
}
