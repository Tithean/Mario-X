package com.supermariox.audio;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SoundManager {
    private static SoundManager instance;
    private final Map<String, Clip> soundCache = new HashMap<>();
    private Clip currentMusicClip;
    private boolean soundEnabled = true;

    private SoundManager() {}

    public static synchronized SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    public void playSound(String soundName) {
        if (!soundEnabled) return;
        try {
            File soundFile = new File("src/assets/sound/" + soundName);
            if (!soundFile.exists()) {
                soundFile = new File("assets/sound/" + soundName);
            }
            if (soundFile.exists()) {
                AudioInputStream ais = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                clip.start();
            }
        } catch (Exception e) {
            // Silently handle if audio format (MP3) is not natively supported by Clip without spi
        }
    }

    public void playMusic(String musicName) {
        if (!soundEnabled) return;
        stopMusic();
        try {
            File musicFile = new File("src/assets/music/" + musicName);
            if (!musicFile.exists()) {
                musicFile = new File("assets/music/" + musicName);
            }
            if (musicFile.exists()) {
                AudioInputStream ais = AudioSystem.getAudioInputStream(musicFile);
                currentMusicClip = AudioSystem.getClip();
                currentMusicClip.open(ais);
                currentMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
                currentMusicClip.start();
            }
        } catch (Exception e) {
            // Silently handle MP3 fallback
        }
    }

    public void stopMusic() {
        if (currentMusicClip != null && currentMusicClip.isRunning()) {
            currentMusicClip.stop();
            currentMusicClip.close();
            currentMusicClip = null;
        }
    }

    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
        if (!enabled) stopMusic();
    }
}
