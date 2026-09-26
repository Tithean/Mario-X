package com.supermariox.collision;

import com.supermariox.audio.SoundManager;
import com.supermariox.enermy.Enemy;
import com.supermariox.enermy.Goomba;
import com.supermariox.enermy.KoopaTroopa;
import com.supermariox.enermy.PiranhaPlant;
import com.supermariox.enermy.RedKoopa;
import com.supermariox.items.Item;
import com.supermariox.level.Block;
import com.supermariox.level.Level;
import com.supermariox.level.Tile;
import com.supermariox.player.Player;
import com.supermariox.player.PlayerPower;
import com.supermariox.player.PlayerState;

import java.awt.Rectangle;
import java.util.List;

public class CollisionManager {

    public static void checkAllCollisions(Player player, Level level) {
        if (player.getCurrentState() == PlayerState.DEAD || player.getCurrentState() == PlayerState.VICTORY) {
            return;
        }

        // 1. Player vs Terrain (Tiles & Blocks unified)
        checkPlayerTerrain(player, level);

        // 2. Player vs Items (Coins, Mushrooms, 1-Ups)
        checkPlayerItems(player, level);

        // 3. Player vs Enemies (Goombas, Koopas, Piranhas)
        checkPlayerEnemies(player, level);

        // 4. Enemies vs Level Terrain & Moving Shells
        checkEnemyLevel(level);

        // 5. Items vs Terrain
        checkItemLevel(level);

        // 6. Player vs Goal (Flagpole)
        checkPlayerGoal(player, level);

        // 7. Pit Death Check
        if (player.getY() > level.getHeight() + 100) {
            player.die();
        }
    }

    private static void checkPlayerTerrain(Player player, Level level) {
        player.setOnGround(false);

        // 1. Horizontal Collision Check (Inset top & bottom by 3px to avoid catching ground seams)
        float nextX = player.getX() + player.getVelX();
        Rectangle pBoundsH = new Rectangle((int) nextX, (int) (player.getY() + 3), player.getWidth(), player.getHeight() - 6);

        // Check horizontal tiles
        for (Tile tile : level.getTiles()) {
            if (tile.isSolid() && pBoundsH.intersects(tile.getBounds())) {
                if (player.getVelX() > 0) {
                    player.setX(tile.getX() - player.getWidth());
                } else if (player.getVelX() < 0) {
                    player.setX(tile.getX() + tile.getWidth());
                }
                player.setVelX(0);
                break;
            }
        }

        // Check horizontal blocks if not stopped by tile
        if (player.getVelX() != 0) {
            for (Block block : level.getBlocks()) {
                if (!block.isActive() || block.isDestroyed()) continue;
                Rectangle bBounds = block.getBounds();
                if (pBoundsH.intersects(bBounds)) {
                    if (player.getVelX() > 0) {
                        player.setX(block.getX() - player.getWidth());
                    } else if (player.getVelX() < 0) {
                        player.setX(block.getX() + block.getWidth());
                    }
                    player.setVelX(0);
                    break;
                }
            }
        }

        if (player.getVelX() != 0) {
            player.setX(nextX);
        }

        // 2. Vertical Collision Check
        float nextY = player.getY() + player.getVelY();
        Rectangle pBoundsV = new Rectangle((int) player.getX() + 2, (int) nextY, player.getWidth() - 4, player.getHeight());
        boolean verticalCollided = false;

        // Check vertical tiles
        for (Tile tile : level.getTiles()) {
            if (tile.isSolid() && pBoundsV.intersects(tile.getBounds())) {
                if (player.getVelY() > 0) { // Landing on top of tile
                    player.setY(tile.getY() - player.getHeight());
                    player.setVelY(0);
                    player.setOnGround(true);
                    verticalCollided = true;
                } else if (player.getVelY() < 0) { // Hitting head on ceiling tile
                    player.setY(tile.getY() + tile.getHeight());
                    player.setVelY(0);
                    verticalCollided = true;
                }
                break;
            }
        }

        // Check vertical blocks if not resolved by tile
        if (!verticalCollided) {
            for (Block block : level.getBlocks()) {
                if (!block.isActive() || block.isDestroyed()) continue;

                Rectangle bBounds = block.getBounds();
                if (pBoundsV.intersects(bBounds)) {
                    float playerTop = nextY;
                    float playerBottom = nextY + player.getHeight();
                    float blockBottom = block.getY() + block.getHeight();
                    float blockTop = block.getY();

                    // Hitting block from below
                    if (player.getVelY() < 0 && playerTop <= blockBottom && playerTop >= blockTop - 16) {
                        player.setY(blockBottom);
                        player.setVelY(0);
                        verticalCollided = true;

                        Block.BlockType prevType = block.getType();
                        boolean destroyed = block.bump(player.getCurrentPower() != PlayerPower.SMALL);

                        if (destroyed) {
                            SoundManager.getInstance().playSound("block-smash");
                        } else if (prevType == Block.BlockType.QUESTION_COIN) {
                            player.addCoins(1);
                            SoundManager.getInstance().playSound("coin");
                        } else if (prevType == Block.BlockType.QUESTION_MUSHROOM) {
                            level.addItem(new Item(block.getX(), block.getY(), Item.ItemType.MUSHROOM));
                            SoundManager.getInstance().playSound("mushroom");
                        } else {
                            SoundManager.getInstance().playSound("block-hit");
                        }
                        break;
                    }
                    // Landing on top of block
                    else if (player.getVelY() >= 0 && playerBottom >= blockTop && playerBottom <= blockTop + 16) {
                        player.setY(blockTop - player.getHeight());
                        player.setVelY(0);
                        player.setOnGround(true);
                        verticalCollided = true;
                        break;
                    }
                }
            }
        }

        // Apply vertical movement if not landed or bonked
        if (!verticalCollided && player.getVelY() != 0) {
            player.setY(nextY);
        }

        // 3. Ground Sensor Check (Prevents 1-frame ground flicker on adjacent tiles & blocks)
        if (player.getVelY() >= 0) {
            Rectangle feetSensor = new Rectangle((int) player.getX() + 2, (int) (player.getY() + player.getHeight()), player.getWidth() - 4, 3);
            for (Tile tile : level.getTiles()) {
                if (tile.isSolid() && feetSensor.intersects(tile.getBounds())) {
                    player.setOnGround(true);
                    break;
                }
            }
            if (!player.isOnGround()) {
                for (Block block : level.getBlocks()) {
                    if (block.isActive() && !block.isDestroyed() && feetSensor.intersects(block.getBounds())) {
                        player.setOnGround(true);
                        break;
                    }
                }
            }
        }
    }

    private static void checkPlayerItems(Player player, Level level) {
        Rectangle pBounds = player.getBounds();
        for (Item item : level.getItems()) {
            if (item.isActive() && !item.isSpawning() && pBounds.intersects(item.getBounds())) {
                item.collect(player);
            }
        }
    }

    private static void checkPlayerEnemies(Player player, Level level) {
        Rectangle pBounds = player.getBounds();

        for (Enemy enemy : level.getEnemies()) {
            if (!enemy.isActive() || enemy.isSquished()) continue;

            Rectangle eBounds = enemy.getBounds();
            if (pBounds.intersects(eBounds)) {
                // Piranha Plant: damages player on touch
                if (enemy instanceof PiranhaPlant) {
                    player.takeDamage();
                    continue;
                }

                float playerBottom = player.getY() + player.getHeight();
                float enemyTop = enemy.getY();

                // Stomp Enemy (player falling downward onto enemy top)
                if (player.getVelY() > 0 && playerBottom <= enemyTop + 18) {
                    enemy.onStomped(player);
                } else {
                    // Koopa shell kick
                    if (enemy instanceof KoopaTroopa) {
                        KoopaTroopa koopa = (KoopaTroopa) enemy;
                        if (koopa.isShell() && !koopa.isMovingShell()) {
                            koopa.kick(player.getX() < koopa.getX());
                            SoundManager.getInstance().playSound("shell-hit");
                            continue;
                        }
                    } else if (enemy instanceof RedKoopa) {
                        RedKoopa koopa = (RedKoopa) enemy;
                        if (koopa.isShell() && !koopa.isMovingShell()) {
                            koopa.kick(player.getX() < koopa.getX());
                            SoundManager.getInstance().playSound("shell-hit");
                            continue;
                        }
                    }
                    player.takeDamage();
                }
            }
        }
    }

    private static void checkEnemyLevel(Level level) {
        List<Enemy> enemies = level.getEnemies();

        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            // Skip inactive, squished, or stationary pipe enemies
            if (!enemy.isActive() || enemy.isSquished() || enemy instanceof PiranhaPlant) continue;

            // 1. Horizontal Movement & Collision
            float nextX = enemy.getX() + enemy.getVelX();
            Rectangle eBoundsH = new Rectangle((int) nextX, (int) (enemy.getY() + 4), enemy.getWidth(), enemy.getHeight() - 8);
            boolean hitWall = false;

            for (Tile tile : level.getTiles()) {
                if (tile.isSolid() && eBoundsH.intersects(tile.getBounds())) {
                    if (enemy.getVelX() > 0) {
                        enemy.setX(tile.getX() - enemy.getWidth() - 1);
                    } else if (enemy.getVelX() < 0) {
                        enemy.setX(tile.getX() + tile.getWidth() + 1);
                    }
                    enemy.reverseDirection();
                    hitWall = true;
                    break;
                }
            }
            if (!hitWall) {
                for (Block block : level.getBlocks()) {
                    if (block.isActive() && !block.isDestroyed() && eBoundsH.intersects(block.getBounds())) {
                        if (enemy.getVelX() > 0) {
                            enemy.setX(block.getX() - enemy.getWidth() - 1);
                        } else if (enemy.getVelX() < 0) {
                            enemy.setX(block.getX() + block.getWidth() + 1);
                        }
                        enemy.reverseDirection();
                        hitWall = true;
                        break;
                    }
                }
            }
            if (!hitWall) {
                enemy.setX(nextX);
            }

            // Red Koopa ledge smart AI: turn around before falling off platforms (ONLY if not already bounced off a wall)
            if (!hitWall && enemy instanceof RedKoopa && enemy.isOnGround() && !((RedKoopa) enemy).isMovingShell()) {
                float checkEdgeX = enemy.getVelX() > 0 ? enemy.getX() + enemy.getWidth() + 4 : enemy.getX() - 4;
                Rectangle ledgeSensor = new Rectangle((int) checkEdgeX, (int) (enemy.getY() + enemy.getHeight() + 2), 4, 8);
                boolean groundAhead = false;
                for (Tile tile : level.getTiles()) {
                    if (tile.isSolid() && ledgeSensor.intersects(tile.getBounds())) {
                        groundAhead = true;
                        break;
                    }
                }
                if (!groundAhead) {
                    for (Block block : level.getBlocks()) {
                        if (block.isActive() && !block.isDestroyed() && ledgeSensor.intersects(block.getBounds())) {
                            groundAhead = true;
                            break;
                        }
                    }
                }
                if (!groundAhead) {
                    enemy.reverseDirection();
                }
            }

            // 2. Vertical Movement & Collision (Gravity)
            enemy.setOnGround(false);
            float nextY = enemy.getY() + enemy.getVelY();
            int scanHeight = enemy.getHeight() + Math.max(2, (int) Math.ceil(Math.max(0, enemy.getVelY())));
            Rectangle eBoundsV = new Rectangle((int) enemy.getX() + 4, (int) Math.min(enemy.getY(), nextY), enemy.getWidth() - 8, scanHeight);
            boolean landed = false;

            for (Tile tile : level.getTiles()) {
                if (tile.isSolid() && eBoundsV.intersects(tile.getBounds())) {
                    if (enemy.getVelY() >= 0) {
                        enemy.setY(tile.getY() - enemy.getHeight());
                        enemy.setVelY(0);
                        enemy.setOnGround(true);
                        landed = true;
                        break;
                    }
                }
            }
            if (!landed) {
                for (Block block : level.getBlocks()) {
                    if (block.isActive() && !block.isDestroyed() && eBoundsV.intersects(block.getBounds())) {
                        if (enemy.getVelY() >= 0) {
                            enemy.setY(block.getY() - enemy.getHeight());
                            enemy.setVelY(0);
                            enemy.setOnGround(true);
                            landed = true;
                            break;
                        }
                    }
                }
            }
            if (!landed) {
                enemy.setY(nextY);
            }

            // Enemy Ground Sensor (Prevents 1-frame ground flicker)
            if (enemy.getVelY() >= 0 && !enemy.isOnGround()) {
                Rectangle feetSensor = new Rectangle((int) enemy.getX() + 4, (int) (enemy.getY() + enemy.getHeight()), enemy.getWidth() - 8, 3);
                for (Tile tile : level.getTiles()) {
                    if (tile.isSolid() && feetSensor.intersects(tile.getBounds())) {
                        enemy.setOnGround(true);
                        break;
                    }
                }
                if (!enemy.isOnGround()) {
                    for (Block block : level.getBlocks()) {
                        if (block.isActive() && !block.isDestroyed() && feetSensor.intersects(block.getBounds())) {
                            enemy.setOnGround(true);
                            break;
                        }
                    }
                }
            }

            // 3. Moving Shell Collisions
            boolean isShellMoving = false;
            if (enemy instanceof KoopaTroopa && ((KoopaTroopa) enemy).isShell() && ((KoopaTroopa) enemy).isMovingShell()) {
                isShellMoving = true;
            } else if (enemy instanceof RedKoopa && ((RedKoopa) enemy).isShell() && ((RedKoopa) enemy).isMovingShell()) {
                isShellMoving = true;
            }

            if (isShellMoving) {
                for (int j = 0; j < enemies.size(); j++) {
                    if (i == j) continue;
                    Enemy other = enemies.get(j);
                    if (other.isActive() && enemy.getBounds().intersects(other.getBounds())) {
                        other.onHitByShell();
                        SoundManager.getInstance().playSound("shell-hit");
                    }
                }
            } else {
                // 4. Enemy vs Enemy Collision (Walking enemies bounce off each other smoothly)
                for (int j = i + 1; j < enemies.size(); j++) {
                    Enemy other = enemies.get(j);
                    if (!other.isActive() || other.isSquished() || other instanceof PiranhaPlant) continue;

                    boolean otherShell = (other instanceof KoopaTroopa && ((KoopaTroopa) other).isMovingShell())
                            || (other instanceof RedKoopa && ((RedKoopa) other).isMovingShell());
                    if (otherShell) continue;

                    if (enemy.getBounds().intersects(other.getBounds())) {
                        if (enemy.getX() < other.getX()) {
                            enemy.setX(other.getX() - enemy.getWidth() - 1);
                            if (enemy.getVelX() > 0) enemy.reverseDirection();
                            if (other.getVelX() < 0) other.reverseDirection();
                        } else {
                            enemy.setX(other.getX() + other.getWidth() + 1);
                            if (enemy.getVelX() < 0) enemy.reverseDirection();
                            if (other.getVelX() > 0) other.reverseDirection();
                        }
                    }
                }
            }

            // Pit death check for enemies
            if (enemy.getY() > level.getHeight() + 100) {
                enemy.setActive(false);
            }
        }
    }

    private static void checkItemLevel(Level level) {
        for (Item item : level.getItems()) {
            if (!item.isActive() || item.isSpawning()) continue;

            item.setX(item.getX() + item.getVelX());
            Rectangle iBoundsH = item.getBounds();

            for (Tile tile : level.getTiles()) {
                if (tile.isSolid() && iBoundsH.intersects(tile.getBounds())) {
                    item.reverseDirection();
                    break;
                }
            }

            item.setY(item.getY() + item.getVelY());
            Rectangle iBoundsV = item.getBounds();

            for (Tile tile : level.getTiles()) {
                if (tile.isSolid() && iBoundsV.intersects(tile.getBounds())) {
                    if (item.getVelY() > 0) {
                        item.setY(tile.getY() - item.getHeight());
                        item.setVelY(0);
                        item.setOnGround(true);
                    }
                    break;
                }
            }
        }
    }

    private static void checkPlayerGoal(Player player, Level level) {
        if (player.getBounds().intersects(level.getGoalBounds())) {
            if (player.getCurrentState() != PlayerState.VICTORY) {
                player.setCurrentState(PlayerState.VICTORY);
                player.setVelX(0);
                player.setVelY(0);
                SoundManager.getInstance().stopMusic();
                SoundManager.getInstance().playSound("level-win");
            }
        }
    }
}

