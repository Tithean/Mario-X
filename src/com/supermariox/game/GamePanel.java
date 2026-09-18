package com.supermariox.game;

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
        SoundManager.getInstance().playMusic("smb3-overworld.mp3");
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

        if (gameState == GameState.PLAYING) {
            SoundManager.getInstance().playMusic(currentLevel.getMusicTrack());
        }
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

        if (gameState == GameState.PLAYING) {
            SoundManager.getInstance().playMusic(currentLevel.getMusicTrack());
        }
    }

    public void nextLevel() {
        int nextIdx = (currentLevelIndex % 3) + 1;
        gameState = GameState.PLAYING;
        initGame(nextIdx);
    }

    public void returnToHomeScreen() {
        gameState = GameState.MENU;
        gameUI.setInCharacterSelect(false);
        gameUI.setInOptionsMenu(false);
        gameUI.setInPauseOptions(false);
        SoundManager.getInstance().playMusic("smb3-overworld.mp3");
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

        // Global Audio Hotkeys: M to Toggle Music, - / [ for Volume Down, + / ] for Volume Up
        if (inputHandler.isKeyJustPressed(KeyEvent.VK_M)) {
            SoundManager sm = SoundManager.getInstance();
            sm.setMusicEnabled(!sm.isMusicEnabled());
            String status = sm.isMusicEnabled() ? "Music: " + Math.round(sm.getMusicVolume() * 100) + "%" : "Music: MUTED";
            showToastNotification(status);
            sm.playSound("level-select");
        } else if (inputHandler.isKeyJustPressed(KeyEvent.VK_MINUS) || inputHandler.isKeyJustPressed(KeyEvent.VK_OPEN_BRACKET)) {
            SoundManager sm = SoundManager.getInstance();
            sm.adjustMasterVolume(-0.10f);
            showToastNotification(String.format("Master Volume: %d%%", Math.round(sm.getMasterVolume() * 100)));
            sm.playSound("coin");
        } else if (inputHandler.isKeyJustPressed(KeyEvent.VK_EQUALS) || inputHandler.isKeyJustPressed(KeyEvent.VK_CLOSE_BRACKET) || inputHandler.isKeyJustPressed(KeyEvent.VK_ADD)) {
            SoundManager sm = SoundManager.getInstance();
            sm.adjustMasterVolume(0.10f);
            showToastNotification(String.format("Master Volume: %d%%", Math.round(sm.getMasterVolume() * 100)));
            sm.playSound("coin");
        }

        // Handle Global ESC / Home Screen Return
        if (inputHandler.isKeyJustPressed(KeyEvent.VK_ESCAPE)) {
            if (gameState == GameState.MENU) {
                if (gameUI.isInOptionsMenu()) {
                    gameUI.setInOptionsMenu(false);
                    SoundManager.getInstance().playSound("level-select");
                    return;
                } else if (gameUI.isInCharacterSelect()) {
                    gameUI.setInCharacterSelect(false);
                    SoundManager.getInstance().playSound("level-select");
                    return;
                }
            } else if (gameState == GameState.PAUSED) {
                if (gameUI.isInPauseOptions()) {
                    gameUI.setInPauseOptions(false);
                    SoundManager.getInstance().playSound("level-select");
                    return;
                }
                gameState = GameState.PLAYING;
                SoundManager.getInstance().resumeMusic();
                SoundManager.getInstance().playSound("pause");
                return;
            } else if (gameState == GameState.GAME_OVER || gameState == GameState.VICTORY) {
                returnToHomeScreen();
                return;
            } else if (gameState == GameState.PLAYING) {
                gameState = GameState.PAUSED;
                SoundManager.getInstance().pauseMusic();
                SoundManager.getInstance().playSound("pause");
                return;
            }
        }

        // Handle Global Pause (P key)
        if (inputHandler.isKeyJustPressed(KeyEvent.VK_P)) {
            if (gameState == GameState.PLAYING) {
                gameState = GameState.PAUSED;
                SoundManager.getInstance().pauseMusic();
                SoundManager.getInstance().playSound("pause");
            } else if (gameState == GameState.PAUSED) {
                if (gameUI.isInPauseOptions()) {
                    gameUI.setInPauseOptions(false);
                }
                gameState = GameState.PLAYING;
                SoundManager.getInstance().resumeMusic();
                SoundManager.getInstance().playSound("pause");
            }
        }

        // Handle Restart (R key)
        if (inputHandler.isRestartJustPressed()) {
            SoundManager.getInstance().playSound("coin");
            initGame(currentLevelIndex);
            gameState = GameState.PLAYING;
            return;
        }

        switch (gameState) {
            case MENU:
                if (inputHandler.isKeyJustPressed(KeyEvent.VK_UP) || inputHandler.isKeyJustPressed(KeyEvent.VK_W)) {
                    gameUI.navigateMenuUp();
                    SoundManager.getInstance().playSound("level-select");
                }
                if (inputHandler.isKeyJustPressed(KeyEvent.VK_DOWN) || inputHandler.isKeyJustPressed(KeyEvent.VK_S)) {
                    gameUI.navigateMenuDown();
                    SoundManager.getInstance().playSound("level-select");
                }
                if (inputHandler.isKeyJustPressed(KeyEvent.VK_LEFT) || inputHandler.isKeyJustPressed(KeyEvent.VK_A)) {
                    gameUI.navigateOptionsLeft();
                    SoundManager.getInstance().playSound("level-select");
                }
                if (inputHandler.isKeyJustPressed(KeyEvent.VK_RIGHT) || inputHandler.isKeyJustPressed(KeyEvent.VK_D)) {
                    gameUI.navigateOptionsRight();
                    SoundManager.getInstance().playSound("level-select");
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
                } else if (player.getCurrentState() == PlayerState.DEAD) {
                    // Wait for death animation to finish before transitioning
                    deathDelayTimer++;
                    if (deathDelayTimer >= DEATH_DELAY_FRAMES) {
                        deathDelayTimer = 0;
                        if (player.getLives() > 0) {
                            player.respawn(currentLevel.getSpawnX(), currentLevel.getSpawnY());
                            SoundManager.getInstance().playMusic(currentLevel.getMusicTrack());
                        } else {
                            gameState = GameState.GAME_OVER;
                            SoundManager.getInstance().stopMusic();
                            SoundManager.getInstance().playSound("player-died2");
                        }
                    }
                } else {
                    deathDelayTimer = 0; // Reset if not dead
                }
                break;

            case PAUSED:
                gameUI.update(gameState, player);
                if (gameUI.isInPauseOptions()) {
                    if (inputHandler.isKeyJustPressed(KeyEvent.VK_UP) || inputHandler.isKeyJustPressed(KeyEvent.VK_W)) {
                        gameUI.navigateOptionsUp();
                        SoundManager.getInstance().playSound("level-select");
                    }
                    if (inputHandler.isKeyJustPressed(KeyEvent.VK_DOWN) || inputHandler.isKeyJustPressed(KeyEvent.VK_S)) {
                        gameUI.navigateOptionsDown();
                        SoundManager.getInstance().playSound("level-select");
                    }
                    if (inputHandler.isKeyJustPressed(KeyEvent.VK_LEFT) || inputHandler.isKeyJustPressed(KeyEvent.VK_A)) {
                        gameUI.navigateOptionsLeft();
                    }
                    if (inputHandler.isKeyJustPressed(KeyEvent.VK_RIGHT) || inputHandler.isKeyJustPressed(KeyEvent.VK_D)) {
                        gameUI.navigateOptionsRight();
                    }
                    if (inputHandler.isStartJustPressed() || inputHandler.isJumpJustPressed()) {
                        handlePauseOptionsSelect();
                    }
                    break;
                }

                if (inputHandler.isKeyJustPressed(KeyEvent.VK_UP) || inputHandler.isKeyJustPressed(KeyEvent.VK_W)) {
                    gameUI.navigatePauseUp();
                    SoundManager.getInstance().playSound("level-select");
                }
                if (inputHandler.isKeyJustPressed(KeyEvent.VK_DOWN) || inputHandler.isKeyJustPressed(KeyEvent.VK_S)) {
                    gameUI.navigatePauseDown();
                    SoundManager.getInstance().playSound("level-select");
                }
                if (inputHandler.isStartJustPressed() || inputHandler.isJumpJustPressed()) {
                    SoundManager.getInstance().playSound("coin");
                    int pSel = gameUI.getPauseMenuIndex();
                    if (pSel == 0) { // CONTINUE
                        gameState = GameState.PLAYING;
                        SoundManager.getInstance().resumeMusic();
                    } else if (pSel == 1) { // AUDIO & SETTINGS
                        gameUI.setInPauseOptions(true);
                    } else if (pSel == 2) { // RESTART
                        initGame(currentLevelIndex);
                        gameState = GameState.PLAYING;
                    } else if (pSel == 3) { // MAIN MENU
                        returnToHomeScreen();
                    }
                }
                break;

            case GAME_OVER:
                gameUI.update(gameState, player);
                if (inputHandler.isKeyJustPressed(KeyEvent.VK_UP) || inputHandler.isKeyJustPressed(KeyEvent.VK_W)) {
                    gameUI.navigateGameOverUp();
                    SoundManager.getInstance().playSound("level-select");
                }
                if (inputHandler.isKeyJustPressed(KeyEvent.VK_DOWN) || inputHandler.isKeyJustPressed(KeyEvent.VK_S)) {
                    gameUI.navigateGameOverDown();
                    SoundManager.getInstance().playSound("level-select");
                }
                if (inputHandler.isStartJustPressed() || inputHandler.isJumpJustPressed()) {
                    SoundManager.getInstance().playSound("coin");
                    int goSel = gameUI.getGameOverMenuIndex();
                    if (goSel == 0) { // RETRY
                        startNewGame();
                        gameState = GameState.PLAYING;
                    } else if (goSel == 1) { // MAIN MENU
                        returnToHomeScreen();
                    }
                }
                break;

            case VICTORY:
                gameUI.update(gameState, player);
                if (inputHandler.isKeyJustPressed(KeyEvent.VK_UP) || inputHandler.isKeyJustPressed(KeyEvent.VK_W)) {
                    gameUI.navigateVictoryUp();
                    SoundManager.getInstance().playSound("level-select");
                }
                if (inputHandler.isKeyJustPressed(KeyEvent.VK_DOWN) || inputHandler.isKeyJustPressed(KeyEvent.VK_S)) {
                    gameUI.navigateVictoryDown();
                    SoundManager.getInstance().playSound("level-select");
                }
                if (inputHandler.isStartJustPressed() || inputHandler.isJumpJustPressed()) {
                    SoundManager.getInstance().playSound("coin");
                    int vicSel = gameUI.getVictoryMenuIndex();
                    if (vicSel == 0) { // NEXT LEVEL
                        nextLevel();
                    } else if (vicSel == 1) { // REPLAY
                        initGame(currentLevelIndex);
                        gameState = GameState.PLAYING;
                    } else if (vicSel == 2) { // MAIN MENU
                        returnToHomeScreen();
                    }
                }
                break;
        }
    }

    private void handleMenuSelect() {
        SoundManager sm = SoundManager.getInstance();

        if (gameUI.isInOptionsMenu()) {
            int opt = gameUI.getOptionsIndex();
            if (opt == 0) {
                sm.playSound("coin");
            } else if (opt == 1 || opt == 2) {
                sm.setMusicEnabled(!sm.isMusicEnabled());
                sm.playSound("level-select");
            } else if (opt == 3) {
                sm.playSound("coin");
            } else if (opt == 4) {
                sm.setSoundEnabled(!sm.isSoundEnabled());
                sm.playSound("level-select");
            } else if (opt == 5 || opt == 6) {
                gameUI.applySelectedResolution();
                sm.playSound("coin");
            } else if (opt == 7) {
                gameUI.setInOptionsMenu(false);
                sm.playSound("coin");
            }
            return;
        }

        sm.playSound("coin");

        if (gameUI.isInCharacterSelect()) {
            startNewGame();
            gameState = GameState.PLAYING;
            gameUI.setInCharacterSelect(false);
            SoundManager.getInstance().playMusic(currentLevel.getMusicTrack());
            return;
        }

        int sel = gameUI.getMenuIndex();
        if (sel == 0) { // 1 Player Game -> Character Select
            gameUI.setInCharacterSelect(true);
        } else if (sel == 3) { // Screen & Options
            gameUI.setInOptionsMenu(true);
        } else {
            startNewGame();
            gameState = GameState.PLAYING;
            SoundManager.getInstance().playMusic(currentLevel.getMusicTrack());
        }
    }

    private void handlePauseOptionsSelect() {
        SoundManager sm = SoundManager.getInstance();
        int opt = gameUI.getOptionsIndex();
        if (opt == 0) {
            sm.playSound("coin");
        } else if (opt == 1 || opt == 2) {
            sm.setMusicEnabled(!sm.isMusicEnabled());
            sm.playSound("level-select");
        } else if (opt == 3) {
            sm.playSound("coin");
        } else if (opt == 4) {
            sm.setSoundEnabled(!sm.isSoundEnabled());
            sm.playSound("level-select");
        } else if (opt == 5 || opt == 6) {
            gameUI.applySelectedResolution();
            sm.playSound("coin");
        } else if (opt == 7) {
            gameUI.setInPauseOptions(false);
            sm.playSound("coin");
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

