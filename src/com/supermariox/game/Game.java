package com.supermariox.game;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;

public class Game extends JFrame {
    private static Game instance;

    private final GamePanel gamePanel;
    private final GameLoop gameLoop;

    private boolean isFullScreen = false;
    private Dimension windowedSize = new Dimension(GamePanel.PANEL_WIDTH, GamePanel.PANEL_HEIGHT);
    private int windowedX = -1;
    private int windowedY = -1;

    public Game() {
        instance = this;

        setTitle("Super Mario X");
        loadApplicationIcon();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setMinimumSize(new Dimension(480, 270));

        gamePanel = new GamePanel();
        add(gamePanel);
        pack();

        setLocationRelativeTo(null);
        setVisible(true);

        // Ensure keyboard focus is captured immediately
        SwingUtilities.invokeLater(() -> {
            gamePanel.requestFocusInWindow();
        });

        gameLoop = new GameLoop(gamePanel);
        gameLoop.start();
    }

    private void loadApplicationIcon() {
        String[] possiblePaths = {
            "src/assets/logo/super mario Xlogo.png",
            "assets/logo/super mario Xlogo.png",
            "src/assets/logo/super_mario_x_logo.png"
        };
        for (String p : possiblePaths) {
            File f = new File(p);
            if (f.exists()) {
                try {
                    BufferedImage icon = ImageIO.read(f);
                    if (icon != null) {
                        setIconImage(icon);
                        break;
                    }
                } catch (Exception e) {
                    System.err.println("Could not load icon from " + p + ": " + e.getMessage());
                }
            }
        }
    }

    public static Game getInstance() {
        return instance;
    }

    public void setScreenResolution(int width, int height) {
        if (isFullScreen) {
            toggleFullScreen();
        }
        gamePanel.setPreferredSize(new Dimension(width, height));
        pack();
        setLocationRelativeTo(null);
        windowedSize = new Dimension(width, height);
        gamePanel.showToastNotification(String.format("Resolution: %dx%d", width, height));
        gamePanel.requestFocusInWindow();
    }

    public void toggleFullScreen() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();

        if (!isFullScreen) {
            // Switch to Fullscreen
            windowedSize = getSize();
            windowedX = getX();
            windowedY = getY();

            dispose();
            setUndecorated(true);
            setResizable(false);

            if (gd.isFullScreenSupported()) {
                gd.setFullScreenWindow(this);
            } else {
                setExtendedState(JFrame.MAXIMIZED_BOTH);
                setVisible(true);
            }

            isFullScreen = true;
            gamePanel.showToastNotification("Screen Mode: Fullscreen (Press F11 or ESC to Exit)");
        } else {
            // Restore Windowed Mode
            if (gd.isFullScreenSupported() && gd.getFullScreenWindow() == this) {
                gd.setFullScreenWindow(null);
            }

            dispose();
            setUndecorated(false);
            setResizable(true);
            setSize(windowedSize);

            if (windowedX >= 0 && windowedY >= 0) {
                setLocation(windowedX, windowedY);
            } else {
                setLocationRelativeTo(null);
            }

            setVisible(true);
            isFullScreen = false;
            gamePanel.showToastNotification(String.format("Screen Mode: Windowed (%dx%d)", windowedSize.width, windowedSize.height));
        }

        SwingUtilities.invokeLater(() -> {
            gamePanel.requestFocusInWindow();
        });
    }

    public boolean isFullScreen() {
        return isFullScreen;
    }

    public GamePanel getGamePanel() {
        return gamePanel;
    }
}
