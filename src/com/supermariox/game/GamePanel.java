package com.supermariox.game;

import com.supermariox.audio.LevelSounds;
import com.supermariox.audio.SoundManager;
import com.supermariox.collision.CollisionManager;
import com.supermariox.player.Player;
import com.supermariox.player.PlayerState;
import com.supermariox.graphics.Camera;
import com.supermariox.input.InputHandler;
import com.supermariox.level.Level;
import com.supermariox.level.LevelLoader;
import com.supermariox.ui.GameUI;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

public class GamePanel extends JPanel {
    public static final int PANEL_WIDTH = 960;
    public static final int PANEL_HEIGHT = 540;

    private GameState gameState = GameState.MENU;
    private final InputHandler inputHandler;
    private final Camera camera;
    private final GameUI gameUI;
    private final SoundManager soundManager = SoundManager.getInstance();

    private Player player;
    private Level currentLevel;
    private int currentLevelIndex = 1;

    // Death delay timer: after player dies, wait this many frames before respawning/game-over
    private int deathDelayTimer = 0;
    private static final int DEATH_DELAY_FRAMES = 120; // 2 seconds at 60fps

    // Intro background camera auto-scroll
    private float introCamX = 0;

    public GamePanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        setFocusable(true);

        inputHandler = new InputHandler();
        addKeyListener(inputHandler);

        // Mouse listener for window focus and menu click interaction
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (gameState == GameState.MENU) {
                    handleMenuSelect();
                } else if (gameState == GameState.GAME_OVER || gameState == GameState.VICTORY) {
                    nextLevel();
                }
            }
        });

        camera = new Camera(PANEL_WIDTH, PANEL_HEIGHT);
        gameUI = new GameUI();

        startNewGame();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        requestFocusInWindow();
    }

    public void showToastNotification(String message) {
        gameUI.showToast(message);
    }

    public void startNewGame() {
        this.currentLevelIndex = 1;
        this.deathDelayTimer = 0;
        currentLevel = LevelLoader.createLevel(1);

        player = new Player(currentLevel.getSpawnX(), currentLevel.getSpawnY());
        player.setLives(3); // Start with 3 lives

        camera.setBounds(0, 0, currentLevel.getWidth(), currentLevel.getHeight());
        gameUI.resetTime();
        gameUI.setLevelName(currentLevel.getName());
    }

    public void initGame(int levelIndex) {
        this.currentLevelIndex = levelIndex;
        this.deathDelayTimer = 0;
        currentLevel = LevelLoader.createLevel(levelIndex);

        int prevLives = (player != null) ? player.getLives() : 3;
        int prevScore = (player != null) ? player.getScore() : 0;
        int prevCoins = (player != null) ? player.getCoins() : 0;

        player = new Player(currentLevel.getSpawnX(), currentLevel.getSpawnY());
        player.setLives(prevLives);
        player.setScore(prevScore);
        player.setCoins(prevCoins);

        camera.setBounds(0, 0, currentLevel.getWidth(), currentLevel.getHeight());
        gameUI.resetTime();
        gameUI.setLevelName(currentLevel.getName());
    }

    public void nextLevel() {
        int nextIdx = (currentLevelIndex % 3) + 1;
        initGame(nextIdx);
        gameState = GameState.PLAYING;
        playCurrentLevelMusic();
    }

    public void returnToHomeScreen() {
        soundManager.stopMusic();
        gameState = GameState.MENU;
        gameUI.setInCharacterSelect(false);
        gameUI.setInOptionsMenu(false);
        gameUI.setInWorldsMenu(false);
    }

    public void update() {
        // Global Screen Hotkeys: F11 for Fullscreen, F1-F4 for Screen Sizes
        Game game = Game.getInstance();
        if (inputHandler.isKeyJustPressed(KeyEvent.VK_F11)) {
            if (game != null) game.toggleFullScreen();
        } else if (inputHandler.isKeyJustPressed(KeyEvent.VK_F1)) {
            if (game != null) game.setScreenResolution(960, 540);
        } else if (inputHandler.isKeyJustPressed(KeyEvent.VK_F2)) {
            if (game != null) game.setScreenResolution(1280, 720);
        } else if (inputHandler.isKeyJustPressed(KeyEvent.VK_F3)) {
            if (game != null) game.setScreenResolution(1600, 900);
        } else if (inputHandler.isKeyJustPressed(KeyEvent.VK_F4)) {
            if (game != null) game.setScreenResolution(1920, 1080);
        }

        // Handle Global ESC / Home Screen Return
        if (inputHandler.isKeyJustPressed(KeyEvent.VK_ESCAPE)) {
            if (gameState == GameState.MENU) {
                if (gameUI.isInOptionsMenu()) {
                    gameUI.setInOptionsMenu(false);
                    return;
                } else if (gameUI.isInWorldsMenu()) {
                    gameUI.setInWorldsMenu(false);
                    return;
                } else if (gameUI.isInCharacterSelect()) {
                    gameUI.setInCharacterSelect(false);
                    return;
                }
            } else if (gameState == GameState.PAUSED || gameState == GameState.GAME_OVER || gameState == GameState.VICTORY) {
                returnToHomeScreen();
                return;
            } else if (gameState == GameState.PLAYING) {
                gameState = GameState.PAUSED;
                soundManager.playSound(LevelSounds.PAUSE);
                return;
            }
        }

        // Handle Global Pause (P key)
        if (inputHandler.isKeyJustPressed(KeyEvent.VK_P)) {
            if (gameState == GameState.PLAYING) {
                gameState = GameState.PAUSED;
                soundManager.playSound(LevelSounds.PAUSE);
            } else if (gameState == GameState.PAUSED) {
                gameState = GameState.PLAYING;
                soundManager.playSound(LevelSounds.PAUSE);
            }
        }

        // Handle Restart (R key)
        if (inputHandler.isRestartJustPressed()) {
            initGame(currentLevelIndex);
            gameState = GameState.PLAYING;
            playCurrentLevelMusic();
            return;
        }

        switch (gameState) {
            case MENU:
                if (inputHandler.isKeyJustPressed(KeyEvent.VK_UP) || inputHandler.isKeyJustPressed(KeyEvent.VK_W)) {
                    gameUI.navigateMenuUp();
                }
                if (inputHandler.isKeyJustPressed(KeyEvent.VK_DOWN) || inputHandler.isKeyJustPressed(KeyEvent.VK_S)) {
                    gameUI.navigateMenuDown();
                }
                if (inputHandler.isKeyJustPressed(KeyEvent.VK_LEFT) || inputHandler.isKeyJustPressed(KeyEvent.VK_A)) {
                    gameUI.navigateOptionsLeft();
                }
                if (inputHandler.isKeyJustPressed(KeyEvent.VK_RIGHT) || inputHandler.isKeyJustPressed(KeyEvent.VK_D)) {
                    gameUI.navigateOptionsRight();
                }
                if (inputHandler.isStartJustPressed() || inputHandler.isJumpJustPressed()) {
                    handleMenuSelect();
                }

                // Smooth Auto-scroll Intro Level Background
                introCamX += 0.8f;
                if (introCamX > currentLevel.getWidth() - PANEL_WIDTH) {
                    introCamX = 0;
                }
                camera.update(introCamX + PANEL_WIDTH / 2.0f, 300);
                currentLevel.update(player);
                gameUI.update(gameState, player);
                break;

            case PLAYING:
                player.handleInput(inputHandler);
                player.update();
                currentLevel.update(player);
                CollisionManager.checkAllCollisions(player, currentLevel);
                camera.update(player.getX() + player.getWidth() / 2.0f, player.getY() + player.getHeight() / 2.0f);
                gameUI.update(gameState, player);

                // State transitions
                if (player.getCurrentState() == PlayerState.VICTORY) {
                    gameState = GameState.VICTORY;
                    soundManager.stopMusic();
                    soundManager.playSound(LevelSounds.WIN);
                } else if (player.getCurrentState() == PlayerState.DEAD) {
                    // Wait for death animation to finish before transitioning
                    deathDelayTimer++;
                    if (deathDelayTimer >= DEATH_DELAY_FRAMES) {
                        deathDelayTimer = 0;
                        if (player.getLives() > 0) {
                            player.respawn(currentLevel.getSpawnX(), currentLevel.getSpawnY());
                        } else {
                            gameState = GameState.GAME_OVER;
                            soundManager.stopMusic();
                            soundManager.playSound(LevelSounds.GAME_BEAT);
                        }
                    }
                } else {
                    deathDelayTimer = 0; // Reset if not dead
                }
                break;

            case PAUSED:
                gameUI.update(gameState, player);
                if (inputHandler.isKeyJustPressed(KeyEvent.VK_UP) || inputHandler.isKeyJustPressed(KeyEvent.VK_W)) {
                    gameUI.navigatePauseUp();
                }
                if (inputHandler.isKeyJustPressed(KeyEvent.VK_DOWN) || inputHandler.isKeyJustPressed(KeyEvent.VK_S)) {
                    gameUI.navigatePauseDown();
                }
                if (inputHandler.isStartJustPressed() || inputHandler.isJumpJustPressed()) {
                    int pSel = gameUI.getPauseMenuIndex();
                    if (pSel == 0) { // CONTINUE
                        gameState = GameState.PLAYING;
                        soundManager.playSound(LevelSounds.PAUSE);
                    } else if (pSel == 1) { // RESTART
                        initGame(currentLevelIndex);
                        gameState = GameState.PLAYING;
                        playCurrentLevelMusic();
                    } else if (pSel == 2) { // MAIN MENU
                        returnToHomeScreen();
                    }
                }
                break;

            case GAME_OVER:
                gameUI.update(gameState, player);
                if (inputHandler.isKeyJustPressed(KeyEvent.VK_UP) || inputHandler.isKeyJustPressed(KeyEvent.VK_W)) {
                    gameUI.navigateGameOverUp();
                }
                if (inputHandler.isKeyJustPressed(KeyEvent.VK_DOWN) || inputHandler.isKeyJustPressed(KeyEvent.VK_S)) {
                    gameUI.navigateGameOverDown();
                }
                if (inputHandler.isStartJustPressed() || inputHandler.isJumpJustPressed()) {
                    int goSel = gameUI.getGameOverMenuIndex();
                    if (goSel == 0) { // RETRY
                        startNewGame();
                        gameState = GameState.PLAYING;
                        playCurrentLevelMusic();
                    } else if (goSel == 1) { // MAIN MENU
                        returnToHomeScreen();
                    }
                }
                break;

            case VICTORY:
                gameUI.update(gameState, player);
                if (inputHandler.isKeyJustPressed(KeyEvent.VK_UP) || inputHandler.isKeyJustPressed(KeyEvent.VK_W)) {
                    gameUI.navigateVictoryUp();
                }
                if (inputHandler.isKeyJustPressed(KeyEvent.VK_DOWN) || inputHandler.isKeyJustPressed(KeyEvent.VK_S)) {
                    gameUI.navigateVictoryDown();
                }
                if (inputHandler.isStartJustPressed() || inputHandler.isJumpJustPressed()) {
                    int vicSel = gameUI.getVictoryMenuIndex();
                    if (vicSel == 0) { // NEXT LEVEL
                        nextLevel();
                    } else if (vicSel == 1) { // REPLAY
                        initGame(currentLevelIndex);
                        gameState = GameState.PLAYING;
                        playCurrentLevelMusic();
                    } else if (vicSel == 2) { // MAIN MENU
                        returnToHomeScreen();
                    }
                }
                break;
        }
    }

    private void handleMenuSelect() {
        if (gameUI.isInOptionsMenu()) {
            int opt = gameUI.getOptionsIndex();
            if (opt == 0 || opt == 1) {
                gameUI.applySelectedResolution();
            } else if (opt == 2) {
                gameUI.setInOptionsMenu(false);
            }
            return;
        }

        if (gameUI.isInWorldsMenu()) {
            int wSel = gameUI.getWorldsIndex();
            if (wSel >= 0 && wSel <= 2) {
                initGame(wSel + 1);
                gameState = GameState.PLAYING;
                soundManager.playSound(LevelSounds.SELECT);
                playCurrentLevelMusic();
                gameUI.setInWorldsMenu(false);
            } else if (wSel == 3) {
                gameUI.setInWorldsMenu(false);
            }
            return;
        }

        if (gameUI.isInCharacterSelect()) {
            startNewGame();
            gameState = GameState.PLAYING;
            soundManager.playSound(LevelSounds.SELECT);
            playCurrentLevelMusic();
            gameUI.setInCharacterSelect(false);
            return;
        }

        int sel = gameUI.getMenuIndex();
        if (sel == 0) { // START GAME
            gameUI.setInCharacterSelect(true);
        } else if (sel == 1) { // WORLDS
            gameUI.setInWorldsMenu(true);
            soundManager.playSound(LevelSounds.SELECT);
        } else if (sel == 2) { // QUIT
            System.exit(0);
        }
    }

    private void playCurrentLevelMusic() {
        if (currentLevel != null) {
            soundManager.playMusic(currentLevel.getMusicTrack());
        }
    }

    public void renderToGraphics(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (gameState == GameState.MENU) {
            currentLevel.render(g2d, camera);
            gameUI.render(g2d, gameState, player, PANEL_WIDTH, PANEL_HEIGHT);
        } else {
            currentLevel.render(g2d, camera);
            player.render(g2d, camera);
            gameUI.render(g2d, gameState, player, PANEL_WIDTH, PANEL_HEIGHT);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        int winW = getWidth();
        int winH = getHeight();

        // 1. Fill entire window background with black (for letterboxing / pillarboxing)
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, winW, winH);

        // 2. Compute aspect-ratio scaling to fit 960x540 inside current window
        double scale = Math.min((double) winW / PANEL_WIDTH, (double) winH / PANEL_HEIGHT);
        int drawW = (int) (PANEL_WIDTH * scale);
        int drawH = (int) (PANEL_HEIGHT * scale);
        int offsetX = (winW - drawW) / 2;
        int offsetY = (winH - drawH) / 2;

        // 3. Translate and scale graphics context
        AffineTransform oldTx = g2d.getTransform();
        g2d.translate(offsetX, offsetY);
        g2d.scale(scale, scale);

        // 4. Clip to virtual 960x540 viewport and render
        g2d.clipRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        renderToGraphics(g2d);

        g2d.setTransform(oldTx);
        g2d.dispose();
    }

    public InputHandler getInputHandler() {
        return inputHandler;
    }
}

