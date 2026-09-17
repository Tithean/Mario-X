package com.supermariox.collision;

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

        // 1. Player vs Tiles (Ground, Walls, Pipes)
        checkPlayerTiles(player, level);

        // 2. Player vs Blocks (Bricks, Question blocks)
        checkPlayerBlocks(player, level);

        // 3. Player vs Items (Coins, Mushrooms, 1-Ups)
        checkPlayerItems(player, level);

        // 4. Player vs Enemies (Goombas, Koopas, Piranhas)
        checkPlayerEnemies(player, level);

        // 5. Enemies vs Level Terrain & Moving Shells
        checkEnemyLevel(level);

        // 6. Items vs Terrain
        checkItemLevel(level);

        // 7. Player vs Goal (Flagpole)
        checkPlayerGoal(player, level);

        // 8. Pit Death Check
        if (player.getY() > level.getHeight() + 100) {
            player.die();
        }
    }

    private static void checkPlayerTiles(Player player, Level level) {
        player.setOnGround(false);

        // 1. Horizontal Collision Check (Inset top & bottom by 3px to avoid catching ground seams)
        float nextX = player.getX() + player.getVelX();
        Rectangle pBoundsH = new Rectangle((int) nextX, (int) (player.getY() + 3), player.getWidth(), player.getHeight() - 6);

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
        if (player.getVelX() != 0) {
            player.setX(nextX);
        }

        // 2. Vertical Collision Check
        float nextY = player.getY() + player.getVelY();
        Rectangle pBoundsV = new Rectangle((int) player.getX() + 2, (int) nextY, player.getWidth() - 4, player.getHeight());

        for (Tile tile : level.getTiles()) {
            if (tile.isSolid() && pBoundsV.intersects(tile.getBounds())) {
                if (player.getVelY() > 0) { // Landing on top of tile
                    player.setY(tile.getY() - player.getHeight());
                    player.setVelY(0);
                    player.setOnGround(true);
                } else if (player.getVelY() < 0) { // Hitting head on ceiling tile
                    player.setY(tile.getY() + tile.getHeight());
                    player.setVelY(0);
                }
                break;
            }
        }
        if (player.getVelY() != 0 && !player.isOnGround()) {
            player.setY(nextY);
        }

        // 3. Ground Sensor Check (Prevents 1-frame ground flicker on adjacent tiles)
        if (player.getVelY() >= 0) {
            Rectangle feetSensor = new Rectangle((int) player.getX() + 2, (int) (player.getY() + player.getHeight()), player.getWidth() - 4, 3);
            for (Tile tile : level.getTiles()) {
                if (tile.isSolid() && feetSensor.intersects(tile.getBounds())) {
                    player.setOnGround(true);
                    break;
                }
            }
        }
    }

    private static void checkPlayerBlocks(Player player, Level level) {
        // 1. Horizontal block collision
        Rectangle pBoundsH = new Rectangle((int) (player.getX() + player.getVelX()), (int) (player.getY() + 4), player.getWidth(), player.getHeight() - 8);
        for (Block block : level.getBlocks()) {
            if (!block.isActive() || block.isDestroyed()) continue;
            Rectangle bBounds = block.getBounds();
            if (pBoundsH.intersects(bBounds)) {
                if (player.getVelX() > 0) {
                    player.setX(block.getX() - player.getWidth());
                    player.setVelX(0);
                } else if (player.getVelX() < 0) {
                    player.setX(block.getX() + block.getWidth());
                    player.setVelX(0);
                }
            }
        }

        // 2. Vertical block collision
        Rectangle pBounds = player.getBounds();
        for (Block block : level.getBlocks()) {
            if (!block.isActive() || block.isDestroyed()) continue;

            Rectangle bBounds = block.getBounds();
            if (pBounds.intersects(bBounds)) {
                float playerBottom = player.getY() + player.getHeight();
                float playerTop = player.getY();
                float blockBottom = block.getY() + block.getHeight();
                float blockTop = block.getY();

                // Hitting block from below
                if (player.getVelY() < 0 && playerTop <= blockBottom && playerTop >= blockTop - 12) {
                    player.setY(blockBottom);
                    player.setVelY(0);
                    boolean destroyed = block.bump(player.getCurrentPower() != PlayerPower.SMALL);

                    if (!destroyed) {
                        if (block.getType() == Block.BlockType.USED) {
                            level.addItem(new Item(block.getX(), block.getY(), Item.ItemType.MUSHROOM));
                        }
                    }
                }
                // Landing on top of block
                else if (player.getVelY() >= 0 && playerBottom >= blockTop && playerBottom <= blockTop + 16) {
                    player.setY(blockTop - player.getHeight());
                    player.setVelY(0);
                    player.setOnGround(true);
                }
            }

            // Feet sensor for blocks
            if (player.getVelY() >= 0) {
                Rectangle feetSensor = new Rectangle((int) player.getX() + 2, (int) (player.getY() + player.getHeight()), player.getWidth() - 4, 3);
                if (feetSensor.intersects(bBounds)) {
                    player.setOnGround(true);
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
                            continue;
                        }
                    } else if (enemy instanceof RedKoopa) {
                        RedKoopa koopa = (RedKoopa) enemy;
                        if (koopa.isShell() && !koopa.isMovingShell()) {
                            koopa.kick(player.getX() < koopa.getX());
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
                        enemy.setX(tile.getX() - enemy.getWidth());
                    } else if (enemy.getVelX() < 0) {
                        enemy.setX(tile.getX() + tile.getWidth());
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
                            enemy.setX(block.getX() - enemy.getWidth());
                        } else if (enemy.getVelX() < 0) {
                            enemy.setX(block.getX() + block.getWidth());
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

            // Red Koopa ledge smart AI: turn around before falling off platforms
            if (enemy instanceof RedKoopa && enemy.isOnGround() && !((RedKoopa) enemy).isMovingShell()) {
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
            Rectangle eBoundsV = new Rectangle((int) enemy.getX() + 2, (int) nextY, enemy.getWidth() - 4, enemy.getHeight());
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
            player.setCurrentState(PlayerState.VICTORY);
            player.setVelX(0);
            player.setVelY(0);
        }
    }
}

