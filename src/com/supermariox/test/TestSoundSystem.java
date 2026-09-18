package com.supermariox.test;

import com.supermariox.audio.SoundManager;
import com.supermariox.level.Level;
import com.supermariox.level.LevelLoader;

public class TestSoundSystem {
    public static void main(String[] args) {
        System.out.println("=== TESTING SOUND SYSTEM ===");
        SoundManager sm = SoundManager.getInstance();

        String[] testSounds = {
            "coin", "player-jump", "stomped", "block-hit", "block-smash",
            "mushroom", "player-grow", "player-died", "shell-hit", "1up",
            "level-select", "pause", "level-win"
        };

        for (String sound : testSounds) {
            System.out.println("Testing SFX: " + sound);
            sm.playSound(sound);
        }

        try {
            Thread.sleep(800);
        } catch (InterruptedException ignored) {}

        System.out.println("Testing Music for Levels...");
        Level l1 = LevelLoader.createLevel(1);
        Level l2 = LevelLoader.createLevel(2);
        Level l3 = LevelLoader.createLevel(3);

        System.out.println("Level 1 music: " + l1.getMusicTrack());
        System.out.println("Level 2 music: " + l2.getMusicTrack());
        System.out.println("Level 3 music: " + l3.getMusicTrack());

        System.out.println("Testing Volume & Sound Controls...");
        System.out.println("Initial Master Volume: " + sm.getMasterVolume());
        System.out.println("Initial Music Volume: " + sm.getMusicVolume());
        System.out.println("Initial Sound Volume: " + sm.getSoundVolume());

        sm.setMusicVolume(0.35f);
        sm.setSoundVolume(0.80f);
        sm.setMasterVolume(0.80f);

        System.out.println("Playing Level 1 music at adjusted volume: " + l1.getMusicTrack());
        sm.playMusic(l1.getMusicTrack());

        try {
            Thread.sleep(400);
            System.out.println("Adjusting music volume in real-time to 20%...");
            sm.adjustMusicVolume(-0.15f);
            Thread.sleep(300);

            System.out.println("Adjusting music volume in real-time to 50%...");
            sm.adjustMusicVolume(0.30f);
            Thread.sleep(300);

            System.out.println("Pausing music...");
            sm.pauseMusic();
            Thread.sleep(200);
            System.out.println("Resuming music...");
            sm.resumeMusic();
            Thread.sleep(400);

            System.out.println("Testing Mute / Unmute...");
            sm.setMusicEnabled(false);
            System.out.println("Music enabled: " + sm.isMusicEnabled());
            Thread.sleep(200);
            sm.setMusicEnabled(true);
            System.out.println("Music enabled: " + sm.isMusicEnabled());
            Thread.sleep(300);

            System.out.println("Stopping music...");
            sm.stopMusic();
        } catch (InterruptedException ignored) {}

        System.out.println("=== ALL AUDIO TESTS COMPLETED SUCCESSFULLY ===");
        System.exit(0);
    }
}
