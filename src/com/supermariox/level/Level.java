package com.supermariox.level;

import com.supermariox.enermy.Enemy;
import com.supermariox.items.Item;
import com.supermariox.player.Player;
import com.supermariox.graphics.AssetManager;
import com.supermariox.graphics.Camera;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Level {
    private final String name;
    private final int width;
    private final int height;

    private float spawnX = 64;
    private float spawnY = 300;

    private float goalX = 3500;
    private float goalY = 200;

    private final List<Tile> tiles = new ArrayList<>();
    private final List<Block> blocks = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();

    private BufferedImage backgroundImage;
    private String musicTrack = "smb-overworld.mp3";

    public Level(String name, int width, int height) {
        this.name = name;
        this.width = width;
        this.height = height;
        loadBackground("background2/background2-1.gif");
    }

    public String getMusicTrack() {
        return musicTrack;
    }

    public void setMusicTrack(String musicTrack) {
        this.musicTrack = musicTrack;
    }

    public void loadBackground(String relativePath) {
        this.backgroundImage = AssetManager.getInstance().getImage(relativePath);
    }

    public void update(Player player) {
        // Update Blocks
        for (int i = 0; i < blocks.size(); i++) {
            Block b = blocks.get(i);
            b.update();
            if (b.isDestroyed()) {
                blocks.remove(i);
                i--;
            }
        }

        // Update Enemies
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (!e.isActive()) {
                enemies.remove(i);
                i--;
                continue;
            }
            e.update();
        }

        // Update Items
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            if (!item.isActive()) {
                items.remove(i);
                i--;
                continue;
            }
            item.update();
        }
    }

    public void render(Graphics2D g, Camera camera) {
        // 1. Render Parallax Sky/Background
        if (backgroundImage != null) {
            int bgW = backgroundImage.getWidth();
            int bgH = camera.getViewportHeight();
            float parallaxX = camera.getX() * 0.3f;
            int startIdx = (int) (parallaxX / bgW);
            int endIdx = startIdx + (camera.getViewportWidth() / bgW) + 2;

            for (int i = startIdx; i <= endIdx; i++) {
                int drawX = (int) (i * bgW - parallaxX);
                g.drawImage(backgroundImage, drawX, 0, bgW, bgH, null);
            }
        } else {
            g.setColor(new java.awt.Color(107, 140, 255)); // Sky blue
            g.fillRect(0, 0, camera.getViewportWidth(), camera.getViewportHeight());
        }

        // 2. Render Tiles
        for (Tile tile : tiles) {
            tile.render(g, camera);
        }

        // 3. Render Blocks
        for (Block block : blocks) {
            block.render(g, camera);
        }

        // 4. Render Items
        for (Item item : items) {
            item.render(g, camera);
        }

        // 5. Render Enemies
        for (Enemy enemy : enemies) {
            enemy.render(g, camera);
        }

        // 6. Render Goal (Flagpole / Roulette)
        renderGoal(g, camera);
    }

    private void renderGoal(Graphics2D g, Camera camera) {
        int renderX = (int) (goalX - camera.getX());
        int renderY = (int) (goalY - camera.getY());

        g.setColor(java.awt.Color.WHITE);
        g.fillRect(renderX + 12, renderY, 8, 250); // Pole
        g.setColor(java.awt.Color.GREEN);
        g.fillOval(renderX, renderY - 16, 32, 32); // Pole top ball
        g.setColor(java.awt.Color.RED);
        g.fillRect(renderX - 24, renderY + 16, 36, 24); // Flag
    }

    public Rectangle getGoalBounds() {
        return new Rectangle((int) goalX, (int) goalY, 32, 250);
    }

    // Adders
    public void addTile(Tile tile) { tiles.add(tile); }
    public void addBlock(Block block) { blocks.add(block); }
    public void addEnemy(Enemy enemy) { enemies.add(enemy); }
    public void addItem(Item item) { items.add(item); }

    // Getters
    public String getName() { return name; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public float getSpawnX() { return spawnX; }
    public void setSpawnX(float spawnX) { this.spawnX = spawnX; }
    public float getSpawnY() { return spawnY; }
    public void setSpawnY(float spawnY) { this.spawnY = spawnY; }
    public float getGoalX() { return goalX; }
    public void setGoalX(float goalX) { this.goalX = goalX; }
    public float getGoalY() { return goalY; }
    public void setGoalY(float goalY) { this.goalY = goalY; }
    public List<Tile> getTiles() { return tiles; }
    public List<Block> getBlocks() { return blocks; }
    public List<Enemy> getEnemies() { return enemies; }
    public List<Item> getItems() { return items; }
}
