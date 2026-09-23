package com.supermariox.audio;

import java.util.HashSet;
import java.util.Set;

/** Coordinates music, sound effects, volume, and audio resource cleanup. */
public final class SoundManager {
    private static final float DEFAULT_VOLUME = 1.0f;

    private final Set<AudioPlayer.Playback> activeEffects = new HashSet<>();
    private AudioPlayer.Playback currentMusic;
    private boolean soundEnabled = true;
    private boolean musicEnabled = true;
    private float soundVolume = DEFAULT_VOLUME;
    private float musicVolume = DEFAULT_VOLUME;

    private SoundManager() {
    }

    private static class InstanceHolder {
        private static final SoundManager INSTANCE = new SoundManager();
    }

    public static SoundManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public synchronized void playSound(String soundName) {
        if (!soundEnabled) {
            return;
        }

        try {
            AudioPlayer.Playback playback = AudioPlayer.play("sound", soundName, false, soundVolume);
            activeEffects.add(playback);
            playback.onStopped(() -> closeEffect(playback));
        } catch (Exception exception) {
            reportAudioError("sound", soundName, exception);
        }
    }

    public synchronized void playMusic(String musicName) {
        stopMusic();
        if (!musicEnabled) {
            return;
        }

        try {
            currentMusic = AudioPlayer.play("music", musicName, true, musicVolume);
        } catch (Exception exception) {
            currentMusic = null;
            reportAudioError("music", musicName, exception);
        }
    }

    public synchronized void stopMusic() {
        if (currentMusic != null) {
            currentMusic.close();
            currentMusic = null;
        }
    }

    public synchronized void stopAllSounds() {
        for (AudioPlayer.Playback playback : Set.copyOf(activeEffects)) {
            playback.close();
        }
        activeEffects.clear();
    }

    public synchronized void setSoundEnabled(boolean enabled) {
        // Kept as the master switch for compatibility with the original API.
        soundEnabled = enabled;
        musicEnabled = enabled;
        if (!enabled) {
            stopAllSounds();
            stopMusic();
        }
    }

    public synchronized boolean isSoundEnabled() {
        return soundEnabled;
    }

    public synchronized void setEffectsEnabled(boolean enabled) {
        soundEnabled = enabled;
        if (!enabled) {
            stopAllSounds();
        }
    }

    public synchronized void setMusicEnabled(boolean enabled) {
        musicEnabled = enabled;
        if (!enabled) {
            stopMusic();
        }
    }

    public synchronized boolean isMusicEnabled() {
        return musicEnabled;
    }

    public synchronized void setSoundVolume(float volume) {
        soundVolume = clampVolume(volume);
        for (AudioPlayer.Playback playback : activeEffects) {
            playback.setVolume(soundVolume);
        }
    }

    public synchronized void setMusicVolume(float volume) {
        musicVolume = clampVolume(volume);
        if (currentMusic != null) {
            currentMusic.setVolume(musicVolume);
        }
    }

    private synchronized void closeEffect(AudioPlayer.Playback playback) {
        activeEffects.remove(playback);
        playback.close();
    }

    private static float clampVolume(float volume) {
        return Math.max(0.0f, Math.min(1.0f, volume));
    }

    private static void reportAudioError(String type, String name, Exception exception) {
        System.err.printf("Could not play %s '%s': %s%n", type, name, exception.getMessage());
    }
}
