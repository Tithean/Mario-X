package com.supermariox.ui;

import com.supermariox.audio.SoundManager;
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
    private boolean inPauseOptions = false;

    // Options Index:
    // 0: Master Volume, 1: Music Volume, 2: Music Track (Mute),
    // 3: Sound FX Volume, 4: Sound Effects (Mute), 5: Screen Size,
    // 6: Apply Screen Size, 7: Back
    private int optionsIndex = 0;
    private int resolutionChoice = 0; // 0: 960x540 (1x), 1: 1280x720 (1.33x), 2: 1600x900 (1.66x), 3: 1920x1080 (2x), 4: Fullscreen

    // Overlay Menu Navigation Indices
    // Pause menu: 0: CONTINUE, 1: AUDIO & SETTINGS, 2: RESTART, 3: MAIN MENU
    private int pauseMenuIndex = 0;
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
                    if (inPauseOptions) {
                        renderOptionsMenu(g, width, height, true);
                    } else {
                        renderPauseOverlay(g, width, height);
                    }
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

        // LOGO
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
            renderOptionsMenu(g, screenWidth, screenHeight, false);
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

    public void renderOptionsMenu(Graphics2D g, int screenWidth, int screenHeight, boolean isPauseMenu) {
        int cardW = 780;
        int cardH = 430;
        int cardX = (screenWidth - cardW) / 2;
        int cardY = (screenHeight - cardH) / 2;

        // Backdrop tint
        g.setColor(new Color(0, 0, 0, 195));
        g.fillRect(0, 0, screenWidth, screenHeight);

        // Card body
        g.setColor(new Color(15, 18, 30, 245));
        g.fillRoundRect(cardX, cardY, cardW, cardH, 20, 20);

        // Golden borders
        g.setColor(new Color(255, 215, 0));
        g.drawRoundRect(cardX, cardY, cardW, cardH, 20, 20);
        g.drawRoundRect(cardX + 1, cardY + 1, cardW - 2, cardH - 2, 18, 18);
        g.setColor(new Color(255, 215, 0, 80));
        g.drawRoundRect(cardX + 4, cardY + 4, cardW - 8, cardH - 8, 14, 14);

        // Header Title
        String title = isPauseMenu ? "AUDIO & SETTINGS (PAUSED)" : "AUDIO & SCREEN SETTINGS";
        g.setFont(new Font("Arial Black", Font.BOLD, 22));
        g.setColor(Color.BLACK);
        drawCenteredString(g, title, screenWidth + 2, cardY + 36);
        g.setColor(new Color(255, 215, 0));
        drawCenteredString(g, title, screenWidth, cardY + 34);

        // Header Divider Line
        g.setColor(new Color(255, 215, 0, 160));
        g.drawLine(cardX + 40, cardY + 48, cardX + cardW - 40, cardY + 48);

        SoundManager sm = SoundManager.getInstance();
        int startY = cardY + 80;
        int spacing = 36;
        int labelRightX = cardX + 340;
        int controlLeftX = cardX + 365;

        g.setFont(new Font("Monospaced", Font.BOLD, 18));

        for (int i = 0; i < 8; i++) {
            int y = startY + (i * spacing);
            boolean selected = (i == optionsIndex);

            if (selected) {
                g.setColor(new Color(255, 215, 0, 35));
                g.fillRoundRect(cardX + 25, y - 21, cardW - 50, 28, 10, 10);
                g.setColor(new Color(255, 215, 0, 90));
                g.drawRoundRect(cardX + 25, y - 21, cardW - 50, 28, 10, 10);

                g.setColor(new Color(255, 50, 50));
                g.drawString("▶", cardX + 35, y);
            }

            Color labelColor = selected ? Color.YELLOW : Color.WHITE;

            switch (i) {
                case 0: // Master Volume
                    drawOptionLabel(g, "Master Volume", labelRightX, y, labelColor);
                    drawSliderControl(g, controlLeftX, y, sm.getMasterVolume(), sm.isSoundEnabled() || sm.isMusicEnabled(), new Color(255, 180, 0), selected);
                    break;
                case 1: // Music Volume (BGM)
                    drawOptionLabel(g, "Music Volume (BGM)", labelRightX, y, labelColor);
                    drawSliderControl(g, controlLeftX, y, sm.getMusicVolume(), sm.isMusicEnabled(), new Color(0, 210, 255), selected);
                    break;
                case 2: // Music Track Mute
                    drawOptionLabel(g, "Music Track", labelRightX, y, labelColor);
                    drawToggleControl(g, controlLeftX, y, sm.isMusicEnabled(), selected);
                    break;
                case 3: // Gameplay Sound (SFX)
                    drawOptionLabel(g, "Gameplay Sound (SFX)", labelRightX, y, labelColor);
                    drawSliderControl(g, controlLeftX, y, sm.getSoundVolume(), sm.isSoundEnabled(), new Color(80, 240, 80), selected);
                    break;
                case 4: // Sound Effects Mute
                    drawOptionLabel(g, "Sound Effects", labelRightX, y, labelColor);
                    drawToggleControl(g, controlLeftX, y, sm.isSoundEnabled(), selected);
                    break;
                case 5: // Screen Resolution
                    drawOptionLabel(g, "Screen Size", labelRightX, y, labelColor);
                    drawResolutionControl(g, controlLeftX, y, resolutionNames[resolutionChoice], selected);
                    break;
                case 6: // Apply Screen Size Button
                    drawCenteredButton(g, "APPLY SCREEN RESOLUTION", screenWidth, y, selected ? new Color(100, 255, 100) : Color.LIGHT_GRAY, selected);
                    break;
                case 7: // Back Button
                    String backText = isPauseMenu ? "RETURN TO PAUSE MENU" : "RETURN TO MAIN MENU";
                    drawCenteredButton(g, backText, screenWidth, y, selected ? Color.YELLOW : Color.WHITE, selected);
                    break;
            }
        }

        // Footer Navigation Hint
        g.setFont(new Font("Arial", Font.BOLD, 13));
        g.setColor(new Color(190, 190, 210));
        drawCenteredString(g, "[↑/↓] Navigate   •   [←/→] Adjust Volume   •   [ENTER] Select / Toggle   •   [ESC] Back", screenWidth, cardY + cardH - 16);
    }

    private void drawOptionLabel(Graphics2D g, String label, int rightX, int y, Color color) {
        FontMetrics fm = g.getFontMetrics();
        int x = rightX - fm.stringWidth(label);
        g.setColor(Color.BLACK);
        g.drawString(label, x + 2, y + 2);
        g.setColor(color);
        g.drawString(label, x, y);
    }

    private void drawSliderControl(Graphics2D g, int x, int y, float volume, boolean enabled, Color barColor, boolean selected) {
        g.setColor(selected ? Color.YELLOW : Color.GRAY);
        g.drawString("<", x, y);

        int barX = x + 18;
        int barY = y - 14;
        int segW = 13;
        int segH = 14;
        int gap = 2;
        int totalSegments = 10;
        int filledSegments = Math.round(volume * totalSegments);

        for (int s = 0; s < totalSegments; s++) {
            int sx = barX + (s * (segW + gap));
            if (s < filledSegments && enabled) {
                g.setColor(barColor);
                g.fillRect(sx, barY, segW, segH);
            } else {
                g.setColor(new Color(40, 45, 60));
                g.fillRect(sx, barY, segW, segH);
            }
            g.setColor(new Color(70, 75, 90));
            g.drawRect(sx, barY, segW, segH);
        }

        int rightArrowX = barX + (totalSegments * (segW + gap)) + 6;
        g.setColor(selected ? Color.YELLOW : Color.GRAY);
        g.drawString(">", rightArrowX, y);

        int textX = rightArrowX + 18;
        int percent = Math.round(volume * 100);
        String pctStr = enabled ? String.format("%3d%%", percent) : "MUTED";
        Color pctColor = enabled ? (selected ? Color.WHITE : Color.LIGHT_GRAY) : new Color(255, 100, 100);
        g.setColor(Color.BLACK);
        g.drawString(pctStr, textX + 1, y + 1);
        g.setColor(pctColor);
        g.drawString(pctStr, textX, y);
    }

    private void drawToggleControl(Graphics2D g, int x, int y, boolean enabled, boolean selected) {
        g.setColor(selected ? Color.YELLOW : Color.GRAY);
        g.drawString("<", x, y);

        String status = enabled ? "ENABLED" : "MUTED";
        Color statusColor = enabled ? new Color(100, 255, 100) : new Color(255, 100, 100);

        int textX = x + 25;
        g.setColor(Color.BLACK);
        g.drawString(status, textX + 1, y + 1);
        g.setColor(statusColor);
        g.drawString(status, textX, y);

        g.setColor(selected ? Color.YELLOW : Color.GRAY);
        g.drawString(">", x + 150, y);
    }

    private void drawResolutionControl(Graphics2D g, int x, int y, String resName, boolean selected) {
        g.setColor(selected ? Color.YELLOW : Color.GRAY);
        g.drawString("<", x, y);

        int textX = x + 16;
        g.setColor(Color.BLACK);
        g.drawString(resName, textX + 1, y + 1);
        g.setColor(selected ? Color.CYAN : Color.LIGHT_GRAY);
        g.drawString(resName, textX, y);

        FontMetrics fm = g.getFontMetrics();
        int rw = fm.stringWidth(resName);
        g.setColor(selected ? Color.YELLOW : Color.GRAY);
        g.drawString(">", textX + rw + 8, y);
    }

    private void drawCenteredButton(Graphics2D g, String text, int screenWidth, int y, Color color, boolean selected) {
        FontMetrics fm = g.getFontMetrics();
        int tx = (screenWidth - fm.stringWidth(text)) / 2;
        g.setColor(Color.BLACK);
        g.drawString(text, tx + 2, y + 2);
        g.setColor(color);
        g.drawString(text, tx, y);
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
        drawCenteredString(g, "PAUSED", screenWidth, screenHeight / 2 - 80);

        String[] pauseOptions = {
                "CONTINUE",
                "AUDIO & SETTINGS",
                "RESTART",
                "MAIN MENU"
        };

        g.setFont(menuFont);
        int startY = screenHeight / 2 - 15;
        int spacing = 42;

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
            navigateOptionsUp();
        } else if (inCharacterSelect) {
            characterIndex = (characterIndex - 1 + 5) % 5;
        } else {
            menuIndex = (menuIndex - 1 + 4) % 4;
        }
    }

    public void navigateMenuDown() {
        if (inOptionsMenu) {
            navigateOptionsDown();
        } else if (inCharacterSelect) {
            characterIndex = (characterIndex + 1) % 5;
        } else {
            menuIndex = (menuIndex + 1) % 4;
        }
    }

    public void navigateOptionsUp() {
        optionsIndex = (optionsIndex - 1 + 8) % 8;
    }

    public void navigateOptionsDown() {
        optionsIndex = (optionsIndex + 1) % 8;
    }

    public void navigatePauseUp() {
        pauseMenuIndex = (pauseMenuIndex - 1 + 4) % 4;
    }

    public void navigatePauseDown() {
        pauseMenuIndex = (pauseMenuIndex + 1) % 4;
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
        SoundManager sm = SoundManager.getInstance();
        switch (optionsIndex) {
            case 0: // Master Volume
                sm.adjustMasterVolume(-0.10f);
                sm.playSound("coin");
                break;
            case 1: // Music Volume (BGM)
                sm.adjustMusicVolume(-0.10f);
                break;
            case 2: // Music Track Mute
                sm.setMusicEnabled(!sm.isMusicEnabled());
                sm.playSound("level-select");
                break;
            case 3: // Gameplay Sound (SFX)
                sm.adjustSoundVolume(-0.10f);
                sm.playSound("coin");
                break;
            case 4: // Sound Effects Mute
                sm.setSoundEnabled(!sm.isSoundEnabled());
                sm.playSound("level-select");
                break;
            case 5: // Screen Size Choice
                resolutionChoice = (resolutionChoice - 1 + resolutionNames.length) % resolutionNames.length;
                sm.playSound("level-select");
                break;
        }
    }

    public void navigateOptionsRight() {
        SoundManager sm = SoundManager.getInstance();
        switch (optionsIndex) {
            case 0: // Master Volume
                sm.adjustMasterVolume(0.10f);
                sm.playSound("coin");
                break;
            case 1: // Music Volume (BGM)
                sm.adjustMusicVolume(0.10f);
                break;
            case 2: // Music Track Mute
                sm.setMusicEnabled(!sm.isMusicEnabled());
                sm.playSound("level-select");
                break;
            case 3: // Gameplay Sound (SFX)
                sm.adjustSoundVolume(0.10f);
                sm.playSound("coin");
                break;
            case 4: // Sound Effects Mute
                sm.setSoundEnabled(!sm.isSoundEnabled());
                sm.playSound("level-select");
                break;
            case 5: // Screen Size Choice
                resolutionChoice = (resolutionChoice + 1) % resolutionNames.length;
                sm.playSound("level-select");
                break;
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
    public boolean isInPauseOptions() { return inPauseOptions; }
    public void setInPauseOptions(boolean inPauseOptions) { this.inPauseOptions = inPauseOptions; }
    public int getOptionsIndex() { return optionsIndex; }
    public void setOptionsIndex(int optionsIndex) { this.optionsIndex = optionsIndex; }
    public int getResolutionChoice() { return resolutionChoice; }

    public int getPauseMenuIndex() { return pauseMenuIndex; }
    public void setPauseMenuIndex(int idx) { this.pauseMenuIndex = idx; }

    public int getGameOverMenuIndex() { return gameOverMenuIndex; }
    public void setGameOverMenuIndex(int idx) { this.gameOverMenuIndex = idx; }

    public int getVictoryMenuIndex() { return victoryMenuIndex; }
    public void setVictoryMenuIndex(int idx) { this.victoryMenuIndex = idx; }
}
