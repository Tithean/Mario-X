package com.supermariox.input;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InputHandler extends KeyAdapter {
    private final Set<Integer> activeKeys = ConcurrentHashMap.newKeySet();
    private final Set<Integer> justPressedKeys = ConcurrentHashMap.newKeySet();

    public boolean isKeyDown(int keyCode) {
        return activeKeys.contains(keyCode);
    }

    public boolean isKeyJustPressed(int keyCode) {
        if (justPressedKeys.contains(keyCode)) {
            justPressedKeys.remove(keyCode);
            return true;
        }
        return false;
    }

    public boolean isLeft() {
        return isKeyDown(KeyEvent.VK_LEFT) || isKeyDown(KeyEvent.VK_A);
    }

    public boolean isRight() {
        return isKeyDown(KeyEvent.VK_RIGHT) || isKeyDown(KeyEvent.VK_D);
    }

    public boolean isJumpHeld() {
        return isKeyDown(KeyEvent.VK_SPACE) || isKeyDown(KeyEvent.VK_UP) ||
               isKeyDown(KeyEvent.VK_W) || isKeyDown(KeyEvent.VK_Z);
    }

    /**
     * Returns true if Space or Z was just pressed (gameplay jump).
     * Does NOT consume VK_UP/VK_W so arrow key menu navigation still works.
     */
    public boolean isJumpJustPressed() {
        boolean pressed = false;
        if (justPressedKeys.contains(KeyEvent.VK_SPACE)) { justPressedKeys.remove(KeyEvent.VK_SPACE); pressed = true; }
        if (justPressedKeys.contains(KeyEvent.VK_Z)) { justPressedKeys.remove(KeyEvent.VK_Z); pressed = true; }
        return pressed;
    }

//    public boolean isCrouch() {
//        return isKeyDown(KeyEvent.VK_DOWN) || isKeyDown(KeyEvent.VK_S);
//    }

    public boolean isRunOrFire() {
        return isKeyDown(KeyEvent.VK_SHIFT) || isKeyDown(KeyEvent.VK_X) || isKeyDown(KeyEvent.VK_J);
    }

    public boolean isPauseJustPressed() {
        boolean pressed = false;
        if (justPressedKeys.contains(KeyEvent.VK_P)) { justPressedKeys.remove(KeyEvent.VK_P); pressed = true; }
        if (justPressedKeys.contains(KeyEvent.VK_ESCAPE)) { justPressedKeys.remove(KeyEvent.VK_ESCAPE); pressed = true; }
        return pressed;
    }

    /**
     * Returns true if ENTER or SPACE was just pressed (menu confirm).
     * Does NOT consume UP/W so those remain available for navigation.
     */
    public boolean isStartJustPressed() {
        boolean pressed = false;
        if (justPressedKeys.contains(KeyEvent.VK_ENTER)) { justPressedKeys.remove(KeyEvent.VK_ENTER); pressed = true; }
        if (justPressedKeys.contains(KeyEvent.VK_SPACE)) { justPressedKeys.remove(KeyEvent.VK_SPACE); pressed = true; }
        return pressed;
    }

    public boolean isRestartJustPressed() {
        return isKeyJustPressed(KeyEvent.VK_R);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (!activeKeys.contains(code)) {
            activeKeys.add(code);
            justPressedKeys.add(code);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        activeKeys.remove(code);
        justPressedKeys.remove(code);
    }

    public void clear() {
        activeKeys.clear();
        justPressedKeys.clear();
    }
}
