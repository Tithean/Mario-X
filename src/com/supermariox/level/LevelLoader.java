package com.supermariox.level;

import com.supermariox.audio.BackgroundMusic;
import com.supermariox.enermy.Goomba;
import com.supermariox.items.Item;
import com.supermariox.enermy.KoopaTroopa;
import com.supermariox.enermy.PiranhaPlant;
import com.supermariox.enermy.RedKoopa;
import com.supermariox.graphics.AssetManager;

import java.awt.image.BufferedImage;

public class LevelLoader {

    public static Level createLevel(int levelIndex) {
        switch (levelIndex) {
            case 2:
                return createLevel1_2();
            case 3:
                return createLevel1_3();
            case 1:
            default:
                return createLevel1_1();
        }
    }

    public static Level createLevel1_1() {
        int levelWidth = 3800;
        int levelHeight = 600;
        int tileSize = 32;

        Level level = new Level("1-1", levelWidth, levelHeight);
        level.setMusicTrack(BackgroundMusic.SMB_OVERWORLD);
        AssetManager am = AssetManager.getInstance();

        BufferedImage groundImg = am.getImage("tile/tile-1.gif");
        BufferedImage pipeTopLImg  = am.getImage("block/pipe-top-L.gif");
        BufferedImage pipeTopRImg  = am.getImage("block/pipe-top-R.gif");
        BufferedImage pipeBodyLImg = am.getImage("block/pipe-body-L.gif");
        BufferedImage pipeBodyRImg = am.getImage("block/pipe-body-R.gif");

        int groundY = levelHeight - (tileSize * 3);

        // Floor
        for (int x = 0; x < levelWidth; x += tileSize) {
            if ((x >= 800 && x <= 896) || (x >= 1800 && x <= 1920) || (x >= 2800 && x <= 2912)) {
                continue; // Gap
            }

            for (int y = groundY; y < levelHeight; y += tileSize) {
                level.addTile(new Tile(x, y, tileSize, tileSize, true, groundImg));
            }
        }

        // Pipes
        addPipe(level, 440, groundY, 2, pipeTopLImg, pipeTopRImg, pipeBodyLImg, pipeBodyRImg);
        addPipe(level, 700, groundY, 3, pipeTopLImg, pipeTopRImg, pipeBodyLImg, pipeBodyRImg);
        addPipe(level, 1200, groundY, 4, pipeTopLImg, pipeTopRImg, pipeBodyLImg, pipeBodyRImg);
        addPipe(level, 1600, groundY, 2, pipeTopLImg, pipeTopRImg, pipeBodyLImg, pipeBodyRImg);

        // Blocks
        level.addBlock(new Block(300, groundY - 128, Block.BlockType.QUESTION_COIN));
        level.addBlock(new Block(360, groundY - 128, Block.BlockType.BRICK));
        level.addBlock(new Block(392, groundY - 128, Block.BlockType.QUESTION_MUSHROOM));
        level.addBlock(new Block(424, groundY - 128, Block.BlockType.BRICK));
        level.addBlock(new Block(456, groundY - 128, Block.BlockType.QUESTION_COIN));

        // Floating Brick Row
        for (int bx = 950; bx <= 1100; bx += 32) {
            if (bx == 1014) {
                level.addBlock(new Block(bx, groundY - 128, Block.BlockType.QUESTION_MUSHROOM));
            } else {
                level.addBlock(new Block(bx, groundY - 128, Block.BlockType.BRICK));
            }
        }

        // Pyramid
        buildPyramid(level, 3000, groundY, 5, groundImg);

        // Rich Variety of Enemies (Goombas, Green Koopas, Red Koopas)
        level.addEnemy(new Goomba(400, groundY - 32));
        level.addEnemy(new Goomba(520, groundY - 32));
        level.addEnemy(new Goomba(1020, groundY - 32));
        level.addEnemy(new Goomba(1060, groundY - 32));
        level.addEnemy(new KoopaTroopa(1380, groundY - 44));
        level.addEnemy(new RedKoopa(1750, groundY - 44));
        level.addEnemy(new Goomba(2100, groundY - 32));
        level.addEnemy(new Goomba(2400, groundY - 32));
        level.addEnemy(new RedKoopa(2650, groundY - 44));

        // Coins
        level.addItem(new Item(550, groundY - 64, Item.ItemType.COIN));
        level.addItem(new Item(582, groundY - 64, Item.ItemType.COIN));
        level.addItem(new Item(614, groundY - 64, Item.ItemType.COIN));

        level.setSpawnX(64);
        level.setSpawnY(groundY - 64);
        level.setGoalX(3450);
        level.setGoalY(groundY - 250);

        return level;
    }

    public static Level createLevel1_2() {
        int levelWidth = 4000;
        int levelHeight = 600;
        int tileSize = 32;

        Level level = new Level("1-2", levelWidth, levelHeight);
        level.setMusicTrack(BackgroundMusic.SMB_UNDERGROUND);
        level.loadBackground("background2/background2-2.gif"); // Underground Cave

        AssetManager am = AssetManager.getInstance();
        BufferedImage caveTileImg = am.getImage("tile/tile-2.gif");
        BufferedImage pipeTopLImg  = am.getImage("block/pipe-top-L.gif");
        BufferedImage pipeTopRImg  = am.getImage("block/pipe-top-R.gif");
        BufferedImage pipeBodyLImg = am.getImage("block/pipe-body-L.gif");
        BufferedImage pipeBodyRImg = am.getImage("block/pipe-body-R.gif");

        int groundY = levelHeight - (tileSize * 3);

        // Subterranean Floor
        for (int x = 0; x < levelWidth; x += tileSize) {
            if ((x >= 1000 && x <= 1120) || (x >= 2200 && x <= 2336)) {
                continue; // Cave Pit
            }
            for (int y = groundY; y < levelHeight; y += tileSize) {
                level.addTile(new Tile(x, y, tileSize, tileSize, true, caveTileImg));
            }

            // Ceiling
            for (int cy = 0; cy < tileSize * 2; cy += tileSize) {
                level.addTile(new Tile(x, cy, tileSize, tileSize, true, caveTileImg));
            }
        }

        // Subterranean Platforms & Blocks
        for (int bx = 400; bx <= 600; bx += 32) {
            level.addBlock(new Block(bx, groundY - 128, Block.BlockType.BRICK));
        }
        level.addBlock(new Block(500, groundY - 224, Block.BlockType.QUESTION_MUSHROOM));

        // Coin vault
        for (int cx = 1300; cx <= 1500; cx += 32) {
            level.addItem(new Item(cx, groundY - 64, Item.ItemType.COIN));
            level.addItem(new Item(cx, groundY - 160, Item.ItemType.COIN));
        }

        addPipe(level, 800, groundY, 3, pipeTopLImg, pipeTopRImg, pipeBodyLImg, pipeBodyRImg);
        addPipe(level, 1800, groundY, 4, pipeTopLImg, pipeTopRImg, pipeBodyLImg, pipeBodyRImg);

        // Enemies
        level.addEnemy(new Goomba(450, groundY - 32));
        level.addEnemy(new Goomba(600, groundY - 32));
        level.addEnemy(new RedKoopa(900, groundY - 44));
        level.addEnemy(new Goomba(1350, groundY - 32));
        level.addEnemy(new Goomba(1450, groundY - 32));
        level.addEnemy(new KoopaTroopa(1700, groundY - 44));
        level.addEnemy(new RedKoopa(2000, groundY - 44));

        level.setSpawnX(64);
        level.setSpawnY(groundY - 64);
        level.setGoalX(3600);
        level.setGoalY(groundY - 250);

        return level;
    }

    public static Level createLevel1_3() {
        int levelWidth = 4200;
        int levelHeight = 600;
        int tileSize = 32;

        Level level = new Level("1-3", levelWidth, levelHeight);
        level.setMusicTrack(BackgroundMusic.SMB3_SKY);
        level.loadBackground("background2/background2-4.gif"); // Athletic Sky

        AssetManager am = AssetManager.getInstance();
        BufferedImage platformImg = am.getImage("tile/tile-3.gif");

        int groundY = levelHeight - (tileSize * 3);

        // Floating Sky Mushroom Platforms
        buildPlatform(level, 0, groundY, 400, platformImg);
        buildPlatform(level, 500, groundY - 64, 300, platformImg);
        buildPlatform(level, 900, groundY - 128, 400, platformImg);
        buildPlatform(level, 1400, groundY, 500, platformImg);
        buildPlatform(level, 2000, groundY - 96, 350, platformImg);
        buildPlatform(level, 2450, groundY - 160, 400, platformImg);
        buildPlatform(level, 2950, groundY, 600, platformImg);

        // Sky Blocks & Coins
        for (int cx = 550; cx <= 750; cx += 32) {
            level.addItem(new Item(cx, groundY - 160, Item.ItemType.COIN));
        }
        level.addBlock(new Block(1000, groundY - 224, Block.BlockType.QUESTION_MUSHROOM));

        // Enemies
        level.addEnemy(new RedKoopa(550, groundY - 108));
        level.addEnemy(new Goomba(1050, groundY - 160));
        level.addEnemy(new Goomba(1150, groundY - 160));
        level.addEnemy(new KoopaTroopa(1600, groundY - 44));
        level.addEnemy(new RedKoopa(2100, groundY - 140));
        level.addEnemy(new Goomba(2500, groundY - 192));

        level.setSpawnX(64);
        level.setSpawnY(groundY - 64);
        level.setGoalX(3300);
        level.setGoalY(groundY - 250);

        return level;
    }

    private static void buildPlatform(Level level, int startX, int y, int width, BufferedImage img) {
        int tileSize = 32;
        for (int x = startX; x < startX + width; x += tileSize) {
            level.addTile(new Tile(x, y, tileSize, tileSize, true, img));
        }
    }

    private static void addPipe(Level level, int x, int groundY, int heightInTiles,
                               BufferedImage topLImg, BufferedImage topRImg,
                               BufferedImage bodyLImg, BufferedImage bodyRImg) {
        int tileSize = 32;
        int topY = groundY - (heightInTiles * tileSize);

        level.addTile(new Tile(x, topY, tileSize, tileSize, true, topLImg));
        level.addTile(new Tile(x + tileSize, topY, tileSize, tileSize, true, topRImg));

        for (int i = 1; i < heightInTiles; i++) {
            int bodyY = topY + (i * tileSize);
            level.addTile(new Tile(x, bodyY, tileSize, tileSize, true, bodyLImg));
            level.addTile(new Tile(x + tileSize, bodyY, tileSize, tileSize, true, bodyRImg));
        }
    }

    private static void buildPyramid(Level level, int startX, int groundY, int height, BufferedImage img) {
        int tileSize = 32;
        for (int row = 0; row < height; row++) {
            int blocksInRow = height - row;
            int rowY = groundY - ((row + 1) * tileSize);
            for (int col = 0; col < blocksInRow; col++) {
                int px = startX + (col * tileSize) + (row * tileSize);
                level.addTile(new Tile(px, rowY, tileSize, tileSize, true, img));
            }
        }
    }
}
