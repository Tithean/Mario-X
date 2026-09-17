package com.supermariox.game;

public class GameLoop implements Runnable {
    public static final int TARGET_FPS = 60;
    private static final long NS_PER_TICK = 1000000000L / TARGET_FPS;

    private final GamePanel gamePanel;
    private Thread gameThread;
    private boolean running = false;

    public GameLoop(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        gameThread = new Thread(this, "GameLoopThread");
        gameThread.start();
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double unprocessedTime = 0;

        while (running) {
            long now = System.nanoTime();
            long elapsed = now - lastTime;
            lastTime = now;

            unprocessedTime += elapsed / (double) NS_PER_TICK;

            // Clamp max catch-up ticks to 3 to prevent lag spikes or catch-up stutter
            if (unprocessedTime > 3.0) {
                unprocessedTime = 1.0;
            }

            boolean updated = false;
            while (unprocessedTime >= 1.0) {
                gamePanel.update();
                unprocessedTime -= 1.0;
                updated = true;
            }

            if (updated) {
                gamePanel.repaint();
            }

            // High precision frame sleep
            try {
                long frameTime = System.nanoTime() - now;
                long sleepMs = (NS_PER_TICK - frameTime) / 1000000L;
                if (sleepMs > 0) {
                    Thread.sleep(sleepMs);
                } else {
                    Thread.sleep(1);
                }
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }
}
