package com.supermariox;

import com.supermariox.game.Game;

public class Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            new Game();
        });
    }
}