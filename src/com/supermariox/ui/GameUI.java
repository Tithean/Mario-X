package com.supermariox.ui;

import com.supermariox.game.Game;
import com.supermariox.game.GameState;
import com.supermariox.player.Player;
import com.supermariox.player.PlayerState;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

public class GameUI {
    private final Font logoSuperFont = new Font("Arial Black", Font.BOLD, 38);
    private final Font logoMarioFont = new Font("Arial Black", Font.BOLD, 54);
    private final Font menuFont = new Font("Monospaced", Font.BOLD, 24);
    private final Font hudFont = new Font("Monospaced", Font.BOLD, 18);
    private final Font subFont = new Font("Arial", Font.BOLD, 18);
    private final Font toastFont = new Font("Arial", Font.BOLD, 16);

    private int menuIndex = 0; // 0: 1 Player, 1: 2 Player, 2: Battle, 3: Options
    private int characterIndex = 0; // 0: Mario, 1: Luigi, 2: Peach, 3: Toad, 4: Link
    private boolean inCharacterSelect = false;
    private boolean inOptionsMenu = false;

    private int optionsIndex = 0; // 0: Screen Resolution / Fullscreen, 1: Apply, 2: Back
    private int resolutionChoice = 0; // 0: 960x540 (1x), 1: 1280x720 (1.33x), 2: 1600x900 (1.66x), 3: 1920x1080 (2x), 4: Fullscreen

    // Overlay Menu Navigation Indices
    private int pauseMenuIndex = 0;    // 0: CONTINUE, 1: RESTART, 2: MAIN MENU
    private int gameOverMenuIndex = 0; // 0: RETRY, 1: MAIN MENU
    private int victoryMenuIndex = 0;  // 0: NEXT LEVEL, 1: REPLAY, 2: MAIN MENU

    private final String[] resolutionNames = {
            "960 x 540  (Native 1x)",
            "1280 x 720 (HD 1.33x)",
            "1600 x 900 (HD+ 1.66x)",
            "1920 x 1080 (FHD 2x)",
            "FULLSCREEN"
    };

    private int timeRemaining = 300;
    private int timeTimer = 0;
    private String currentLevelName = "1-1";

    private String toastMessage = null;
    private int toastTimer = 0;

    public void showToast(String message) {
        this.toastMessage = message;
        this.toastTimer = 150; // 2.5 seconds at 60fps
    }

    public void update(GameState state, Player player) {
        if (toastTimer > 0) {
            toastTimer--;
            if (toastTimer == 0) {
                toastMessage = null;
            }
        }

        if (state == GameState.PLAYING && player.getCurrentState() != PlayerState.VICTORY) {
            timeTimer++;
            if (timeTimer >= 60) {
                timeTimer = 0;
                if (timeRemaining > 0) {
                    timeRemaining--;
                } else {
                    player.die();
                }
            }
        }
    }

    public void render(Graphics2D g, GameState state, Player player, int width, int height) {
        switch (state) {
            case MENU:
                renderSMBXTitleMenu(g, width, height);
                break;
            case PLAYING:
            case PAUSED:
                renderSMBXHUD(g, player, width);
                if (state == GameState.PAUSED) {
                    renderPauseOverlay(g, width, height);
                }
                break;
            case GAME_OVER:
                renderGameOverOverlay(g, width, height);
                break;
            case VICTORY:
                renderVictoryOverlay(g, width, height);
                break;
        }

        // Render Top Notification Toast
        if (toastMessage != null) {
            renderToast(g, width);
        }
    }

    private void renderToast(Graphics2D g, int screenWidth) {
        g.setFont(toastFont);
        FontMetrics fm = g.getFontMetrics(toastFont);
        int tw = fm.stringWidth(toastMessage) + 36;
        int th = 32;
        int tx = (screenWidth - tw) / 2;
        int ty = 66;

        g.setColor(new Color(0, 0, 0, 210));
        g.fillRoundRect(tx, ty, tw, th, 12, 12);
        g.setColor(Color.YELLOW);
        g.drawRoundRect(tx, ty, tw, th, 12, 12);

        g.setColor(Color.WHITE);
        drawCenteredString(g, toastMessage, screenWidth, ty + 22);
    }

    private void renderSMBXTitleMenu(Graphics2D g, int screenWidth, int screenHeight) {
        // 1. Top Stage Curtain Trim
        g.setColor(new Color(220, 110, 0));
        g.fillRect(0, 0, screenWidth, 18);
        g.setColor(new Color(255, 200, 0));
        g.fillRect(0, 18, screenWidth, 6);

        // 2. SMBX Title Logo: "SUPER MARIO BROS. X"
        int logoY = 100;

        g.setFont(logoSuperFont);
        g.setColor(Color.BLACK);
        drawCenteredString(g, "SUPER", screenWidth + 4, logoY + 4);
        g.setColor(new Color(255, 40, 40));
        drawCenteredString(g, "SUPER", screenWidth, logoY);

        g.setFont(logoMarioFont);
        g.setColor(Color.BLACK);
        drawCenteredString(g, "MARIO BROS.", screenWidth + 6, logoY + 54);
        g.setColor(new Color(60, 220, 60));
        drawCenteredString(g, "MARIO BROS.", screenWidth, logoY + 50);

        g.setFont(new Font("Arial Black", Font.BOLD, 68));
        g.setColor(Color.BLACK);
        drawCenteredString(g, "X", screenWidth + 6, logoY + 120);
        g.setColor(new Color(255, 215, 0));
        drawCenteredString(g, "X", screenWidth, logoY + 116);

        // 3. Render Menus
        if (inOptionsMenu) {
            renderOptionsMenu(g, screenWidth, screenHeight);
        } else if (inCharacterSelect) {
            renderCharacterSelect(g, screenWidth, screenHeight);
        } else {
            renderMenuOptions(g, screenWidth, screenHeight);
        }
    }

    private void renderMenuOptions(Graphics2D g, int screenWidth, int screenHeight) {
        g.setFont(menuFont);
        String[] options = {
                "1 PLAYER GAME",
                "2 PLAYER GAME",
                "BATTLE GAME",
                "SCREEN & OPTIONS"
        };

        int startY = 300;
        int spacing = 40;

        for (int i = 0; i < options.length; i++) {
            int y = startY + (i * spacing);

            g.setColor(Color.BLACK);
            drawCenteredString(g, options[i], screenWidth + 4, y + 4);

            if (i == menuIndex) {
                g.setColor(Color.YELLOW);
                drawCenteredString(g, options[i], screenWidth, y);

                FontMetrics fm = g.getFontMetrics(menuFont);
                int textW = fm.stringWidth(options[i]);
                int cursorX = (screenWidth - textW) / 2 - 36;
                g.setColor(new Color(255, 50, 50));
                g.drawString("▶", cursorX, y);
            } else {
                g.setColor(Color.WHITE);
                drawCenteredString(g, options[i], screenWidth, y);
            }
        }
    }

    private void renderOptionsMenu(Graphics2D g, int screenWidth, int screenHeight) {
        g.setFont(menuFont);
        g.setColor(Color.YELLOW);
        drawCenteredString(g, "--- SCREEN & OPTIONS ---", screenWidth, 290);

        int startY = 340;
        int spacing = 38;

        // Option 0: Screen Resolution / Fullscreen
        String resLabel = "< Size: " + resolutionNames[resolutionChoice] + " >";
        int y0 = startY;
        g.setColor(Color.BLACK);
        drawCenteredString(g, resLabel, screenWidth + 3, y0 + 3);
        if (optionsIndex == 0) {
            g.setColor(Color.CYAN);
            drawCenteredString(g, resLabel, screenWidth, y0);
            FontMetrics fm = g.getFontMetrics(menuFont);
            int textW = fm.stringWidth(resLabel);
            g.setColor(Color.YELLOW);
            g.drawString("▶", (screenWidth - textW) / 2 - 28, y0);
        } else {
            g.setColor(Color.WHITE);
            drawCenteredString(g, resLabel, screenWidth, y0);
        }

        // Option 1: Apply Resolution
        String applyLabel = "APPLY SELECTED SCREEN SIZE";
        int y1 = startY + spacing;
        g.setColor(Color.BLACK);
        drawCenteredString(g, applyLabel, screenWidth + 3, y1 + 3);
        if (optionsIndex == 1) {
            g.setColor(new Color(100, 255, 100));
            drawCenteredString(g, applyLabel, screenWidth, y1);
            FontMetrics fm = g.getFontMetrics(menuFont);
            int textW = fm.stringWidth(applyLabel);
            g.setColor(Color.YELLOW);
            g.drawString("▶", (screenWidth - textW) / 2 - 28, y1);
        } else {
            g.setColor(Color.LIGHT_GRAY);
            drawCenteredString(g, applyLabel, screenWidth, y1);
        }

        // Option 2: Back
        String backLabel = "BACK TO MAIN MENU";
        int y2 = startY + (spacing * 2);
        g.setColor(Color.BLACK);
        drawCenteredString(g, backLabel, screenWidth + 3, y2 + 3);
        if (optionsIndex == 2) {
            g.setColor(Color.YELLOW);
            drawCenteredString(g, backLabel, screenWidth, y2);
            FontMetrics fm = g.getFontMetrics(menuFont);
            int textW = fm.stringWidth(backLabel);
            g.setColor(Color.YELLOW);
            g.drawString("▶", (screenWidth - textW) / 2 - 28, y2);
        } else {
            g.setColor(Color.WHITE);
            drawCenteredString(g, backLabel, screenWidth, y2);
        }
    }

    private void renderCharacterSelect(Graphics2D g, int screenWidth, int screenHeight) {
        g.setFont(menuFont);
        g.setColor(Color.YELLOW);
        drawCenteredString(g, "SELECT CHARACTER", screenWidth, 300);

        String[] characters = {"MARIO", "LUIGI", "PEACH", "TOAD", "LINK"};
        int startY = 340;

        for (int i = 0; i < characters.length; i++) {
            int y = startY + (i * 32);

            g.setColor(Color.BLACK);
            drawCenteredString(g, characters[i], screenWidth + 3, y + 3);

            if (i == characterIndex) {
                g.setColor(new Color(100, 255, 100));
                drawCenteredString(g, characters[i], screenWidth, y);

                FontMetrics fm = g.getFontMetrics(menuFont);
                int textW = fm.stringWidth(characters[i]);
                int cursorX = (screenWidth - textW) / 2 - 30;
                g.setColor(Color.YELLOW);
                g.drawString("▶", cursorX, y);
            } else {
                g.setColor(Color.LIGHT_GRAY);
                drawCenteredString(g, characters[i], screenWidth, y);
            }
        }
    }

    private void renderSMBXHUD(Graphics2D g, Player player, int screenWidth) {
        g.setFont(hudFont);

        // HUD Top Box
        g.setColor(new Color(0, 0, 0, 175));
        g.fillRect(10, 8, screenWidth - 20, 50);
        g.setColor(Color.WHITE);
        g.drawRect(10, 8, screenWidth - 20, 50);

        // 1. MARIO Score
        g.setColor(Color.WHITE);
        g.drawString("MARIO", 30, 28);
        g.drawString(String.format("%06d", player.getScore()), 30, 48);

        // 2. COINS Counter
        g.setColor(Color.YELLOW);
        g.drawString(String.format("COINS x%02d", player.getCoins()), 210, 48);

        // 3. WORLD Title
        g.setColor(Color.WHITE);
        g.drawString("WORLD", 380, 28);
        g.drawString(currentLevelName, 395, 48);

        // 4. TIME Remaining
        g.drawString("TIME", 530, 28);
        g.drawString(String.format("%03d", timeRemaining), 535, 48);

        // 5. 3-HEARTS LIFE SYSTEM (Draw 3 Heart Icons)
        int heartsStartX = screenWidth - 240;
        int heartsY = 32;

        g.setColor(Color.WHITE);
        g.drawString("LIVES", heartsStartX, 24);

        int maxHearts = 3;
        int currentLives = player.getLives();

        for (int h = 0; h < maxHearts; h++) {
            int hx = heartsStartX + (h * 24);
            boolean filled = (h < currentLives);
            drawHeart(g, hx, heartsY, filled);
        }

        g.setColor(Color.CYAN);
        g.drawString(String.format("x %d", currentLives), heartsStartX + 80, 48);
    }

    private void drawHeart(Graphics2D g, int x, int y, boolean filled) {
        if (filled) {
            g.setColor(new Color(255, 45, 45)); // Vibrant red
            g.fillOval(x, y - 8, 9, 9);
            g.fillOval(x + 7, y - 8, 9, 9);
            int[] px = {x, x + 16, x + 8};
            int[] py = {y - 2, y - 2, y + 9};
            g.fillPolygon(px, py, 3);
        } else {
            g.setColor(new Color(80, 80, 80)); // Empty heart outline/dark gray
            g.drawOval(x, y - 8, 9, 9);
            g.drawOval(x + 7, y - 8, 9, 9);
            int[] px = {x, x + 16, x + 8};
            int[] py = {y - 2, y - 2, y + 9};
            g.drawPolygon(px, py, 3);
        }
    }

    private void renderPauseOverlay(Graphics2D g, int screenWidth, int screenHeight) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, screenWidth, screenHeight);

        g.setFont(logoSuperFont);
        g.setColor(Color.YELLOW);
        drawCenteredString(g, "PAUSED", screenWidth, screenHeight / 2 - 70);

        String[] pauseOptions = {
                "CONTINUE",
                "RESTART",
                "MAIN MENU"
        };

        g.setFont(menuFont);
        int startY = screenHeight / 2;
        int spacing = 44;

        for (int i = 0; i < pauseOptions.length; i++) {
            int y = startY + (i * spacing);

            g.setColor(Color.BLACK);
            drawCenteredString(g, pauseOptions[i], screenWidth + 4, y + 4);

            if (i == pauseMenuIndex) {
                g.setColor(Color.YELLOW);
                drawCenteredString(g, pauseOptions[i], screenWidth, y);

                FontMetrics fm = g.getFontMetrics(menuFont);
                int textW = fm.stringWidth(pauseOptions[i]);
                int cursorX = (screenWidth - textW) / 2 - 32;
                g.setColor(new Color(255, 50, 50));
                g.drawString("▶", cursorX, y);
            } else {
                g.setColor(Color.WHITE);
                drawCenteredString(g, pauseOptions[i], screenWidth, y);
            }
        }
    }

    private void renderGameOverOverlay(Graphics2D g, int screenWidth, int screenHeight) {
        g.setColor(new Color(0, 0, 0, 190));
        g.fillRect(0, 0, screenWidth, screenHeight);

        g.setFont(logoSuperFont);
        g.setColor(Color.RED);
        drawCenteredString(g, "GAME OVER", screenWidth, screenHeight / 2 - 60);

        String[] gameOverOptions = {
                "RETRY",
                "MAIN MENU"
        };

        g.setFont(menuFont);
        int startY = screenHeight / 2 + 10;
        int spacing = 44;

        for (int i = 0; i < gameOverOptions.length; i++) {
            int y = startY + (i * spacing);

            g.setColor(Color.BLACK);
            drawCenteredString(g, gameOverOptions[i], screenWidth + 4, y + 4);

            if (i == gameOverMenuIndex) {
                g.setColor(Color.YELLOW);
                drawCenteredString(g, gameOverOptions[i], screenWidth, y);

                FontMetrics fm = g.getFontMetrics(menuFont);
                int textW = fm.stringWidth(gameOverOptions[i]);
                int cursorX = (screenWidth - textW) / 2 - 32;
                g.setColor(new Color(255, 50, 50));
                g.drawString("▶", cursorX, y);
            } else {
                g.setColor(Color.WHITE);
                drawCenteredString(g, gameOverOptions[i], screenWidth, y);
            }
        }
    }

    private void renderVictoryOverlay(Graphics2D g, int screenWidth, int screenHeight) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, screenWidth, screenHeight);

        g.setFont(logoSuperFont);
        g.setColor(Color.GREEN);
        drawCenteredString(g, "COURSE CLEAR!", screenWidth, screenHeight / 2 - 80);

        g.setFont(subFont);
        g.setColor(Color.YELLOW);
        drawCenteredString(g, "GREAT JOB!", screenWidth, screenHeight / 2 - 40);

        String[] victoryOptions = {
                "NEXT LEVEL",
                "REPLAY",
                "MAIN MENU"
        };

        g.setFont(menuFont);
        int startY = screenHeight / 2 + 10;
        int spacing = 44;

        for (int i = 0; i < victoryOptions.length; i++) {
            int y = startY + (i * spacing);

            g.setColor(Color.BLACK);
            drawCenteredString(g, victoryOptions[i], screenWidth + 4, y + 4);

            if (i == victoryMenuIndex) {
                g.setColor(Color.GREEN);
                drawCenteredString(g, victoryOptions[i], screenWidth, y);

                FontMetrics fm = g.getFontMetrics(menuFont);
                int textW = fm.stringWidth(victoryOptions[i]);
                int cursorX = (screenWidth - textW) / 2 - 32;
                g.setColor(Color.YELLOW);
                g.drawString("▶", cursorX, y);
            } else {
                g.setColor(Color.WHITE);
                drawCenteredString(g, victoryOptions[i], screenWidth, y);
            }
        }
    }

    private void drawCenteredString(Graphics2D g, String text, int width, int y) {
        FontMetrics fm = g.getFontMetrics();
        int x = (width - fm.stringWidth(text)) / 2;
        g.drawString(text, x, y);
    }

    public void navigateMenuUp() {
        if (inOptionsMenu) {
            optionsIndex = (optionsIndex - 1 + 3) % 3;
        } else if (inCharacterSelect) {
            characterIndex = (characterIndex - 1 + 5) % 5;
        } else {
            menuIndex = (menuIndex - 1 + 4) % 4;
        }
    }

    public void navigateMenuDown() {
        if (inOptionsMenu) {
            optionsIndex = (optionsIndex + 1) % 3;
        } else if (inCharacterSelect) {
            characterIndex = (characterIndex + 1) % 5;
        } else {
            menuIndex = (menuIndex + 1) % 4;
        }
    }

    public void navigatePauseUp() {
        pauseMenuIndex = (pauseMenuIndex - 1 + 3) % 3;
    }

    public void navigatePauseDown() {
        pauseMenuIndex = (pauseMenuIndex + 1) % 3;
    }

    public void navigateGameOverUp() {
        gameOverMenuIndex = (gameOverMenuIndex - 1 + 2) % 2;
    }

    public void navigateGameOverDown() {
        gameOverMenuIndex = (gameOverMenuIndex + 1) % 2;
    }

    public void navigateVictoryUp() {
        victoryMenuIndex = (victoryMenuIndex - 1 + 3) % 3;
    }

    public void navigateVictoryDown() {
        victoryMenuIndex = (victoryMenuIndex + 1) % 3;
    }

    public void navigateOptionsLeft() {
        if (inOptionsMenu && optionsIndex == 0) {
            resolutionChoice = (resolutionChoice - 1 + resolutionNames.length) % resolutionNames.length;
        }
    }

    public void navigateOptionsRight() {
        if (inOptionsMenu && optionsIndex == 0) {
            resolutionChoice = (resolutionChoice + 1) % resolutionNames.length;
        }
    }

    public void applySelectedResolution() {
        Game game = Game.getInstance();
        if (game == null) return;

        switch (resolutionChoice) {
            case 0:
                game.setScreenResolution(960, 540);
                break;
            case 1:
                game.setScreenResolution(1280, 720);
                break;
            case 2:
                game.setScreenResolution(1600, 900);
                break;
            case 3:
                game.setScreenResolution(1920, 1080);
                break;
            case 4:
                game.toggleFullScreen();
                break;
        }
    }

    public void resetTime() {
        this.timeRemaining = 300;
        this.timeTimer = 0;
    }

    public void setLevelName(String name) {
        this.currentLevelName = name;
    }

    public int getMenuIndex() { return menuIndex; }
    public int getCharacterIndex() { return characterIndex; }
    public boolean isInCharacterSelect() { return inCharacterSelect; }
    public void setInCharacterSelect(boolean inCharacterSelect) { this.inCharacterSelect = inCharacterSelect; }
    public boolean isInOptionsMenu() { return inOptionsMenu; }
    public void setInOptionsMenu(boolean inOptionsMenu) { this.inOptionsMenu = inOptionsMenu; }
    public int getOptionsIndex() { return optionsIndex; }
    public int getResolutionChoice() { return resolutionChoice; }

    public int getPauseMenuIndex() { return pauseMenuIndex; }
    public void setPauseMenuIndex(int idx) { this.pauseMenuIndex = idx; }

    public int getGameOverMenuIndex() { return gameOverMenuIndex; }
    public void setGameOverMenuIndex(int idx) { this.gameOverMenuIndex = idx; }

    public int getVictoryMenuIndex() { return victoryMenuIndex; }
    public void setVictoryMenuIndex(int idx) { this.victoryMenuIndex = idx; }
}
