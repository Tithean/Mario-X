package com.supermariox.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;

/**
 * SoundManager handles sound effects (SFX) and background music (BGM)
 * for Super Mario X.
 *
 * Supports package resources in com.supermariox.audio.sounds,
 * file assets (WAV / MP3), and procedural open-source retro sound synthesis.
 */
public class SoundManager {
    private static SoundManager instance;

    // Cache decoded PCM byte data for zero-latency SFX playback
    private static class CachedSound {
        final byte[] pcmData;
        final AudioFormat format;

        CachedSound(byte[] pcmData, AudioFormat format) {
            this.pcmData = pcmData;
            this.format = format;
        }
    }

    private final Map<String, CachedSound> soundCache = new ConcurrentHashMap<>();
    private final ExecutorService sfxPool = Executors.newFixedThreadPool(8);

    private Clip currentMusicClip;
    private String currentMusicName = null;
    private long musicPausePosition = 0;

    // Audio volume levels (0.00f to 1.00f)
    // Default background music is set to 0.35f (comfortable ambient level)
    // Gameplay SFX is set to 0.80f (clear, crisp, and punchy)
    private boolean soundEnabled = true;
    private boolean musicEnabled = true;
    private float masterVolume = 0.80f;
    private float soundVolume = 0.80f;
    private float musicVolume = 0.35f;

    private SoundManager() {
        loadSettings();
        // Pre-cache primary sound effects asynchronously on startup
        sfxPool.submit(this::preloadCommonSounds);
    }

    public static synchronized SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    private void preloadCommonSounds() {
        String[] commonSFX = {
            "player-jump", "coin", "stomped", "block-hit", "block-smash",
            "mushroom", "player-grow", "player-shrink", "player-died",
            "player-died2", "shell-hit", "1up", "level-select", "pause",
            "level-win", "game-beat"
        };
        for (String s : commonSFX) {
            getOrLoadSound(s);
        }
    }

    private AudioInputStream openAudioStream(InputStream is) throws Exception {
        AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(is));
        AudioFormat baseFormat = in.getFormat();
        if (baseFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED &&
            baseFormat.getEncoding() != AudioFormat.Encoding.PCM_UNSIGNED) {
            AudioFormat decodedFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.getSampleRate(),
                16,
                baseFormat.getChannels(),
                baseFormat.getChannels() * 2,
                baseFormat.getSampleRate(),
                false
            );
            return AudioSystem.getAudioInputStream(decodedFormat, in);
        }
        return in;
    }

    /**
     * Decodes and reads an audio file into a raw PCM CachedSound object.
     * Searches in:
     * 1. Package resources: /com/supermariox/audio/sounds/
     * 2. Assets directory: src/assets/sound/ and assets/sound/
     * 3. Open-source procedural sound generator fallback
     */
    private CachedSound getOrLoadSound(String soundName) {
        String key = soundName.toLowerCase();
        if (soundCache.containsKey(key)) {
            return soundCache.get(key);
        }

        String baseName = soundName;
        if (baseName.endsWith(".mp3") || baseName.endsWith(".wav")) {
            baseName = baseName.substring(0, baseName.lastIndexOf('.'));
        }

        // 1. Try package classpath resource in com/supermariox/audio/sounds/
        String[] exts = {".wav", ".mp3"};
        for (String ext : exts) {
            String resPath = "/com/supermariox/audio/sounds/" + baseName + ext;
            InputStream resStream = getClass().getResourceAsStream(resPath);
            if (resStream != null) {
                try (AudioInputStream din = openAudioStream(resStream)) {
                    byte[] pcmBytes = din.readAllBytes();
                    CachedSound cached = new CachedSound(pcmBytes, din.getFormat());
                    soundCache.put(key, cached);
                    return cached;
                } catch (Exception e) {
                    // Try next source
                }
            }
        }

        // 2. Try file system
        File file = resolveAudioFile("sound", soundName);
        if (file != null && file.exists()) {
            try (InputStream fis = new FileInputStream(file);
                 AudioInputStream din = openAudioStream(fis)) {
                byte[] pcmBytes = din.readAllBytes();
                CachedSound cached = new CachedSound(pcmBytes, din.getFormat());
                soundCache.put(key, cached);
                return cached;
            } catch (Exception e) {
                // Fall back to synth
            }
        }

        // 3. Fallback: Synthesize using open-source procedural audio generator
        byte[] synthData = RetroSoundGenerator.generateSound(soundName);
        if (synthData != null && synthData.length > 0) {
            CachedSound cached = new CachedSound(synthData, RetroSoundGenerator.FORMAT);
            soundCache.put(key, cached);
            return cached;
        }

        return null;
    }

    private File resolveAudioFile(String subfolder, String name) {
        String baseName = name;
        if (baseName.endsWith(".mp3") || baseName.endsWith(".wav")) {
            baseName = baseName.substring(0, baseName.lastIndexOf('.'));
        }

        String[] extensions = {".wav", ".mp3"};
        String[] rootPaths = {"src/assets/" + subfolder + "/", "assets/" + subfolder + "/"};

        for (String ext : extensions) {
            for (String root : rootPaths) {
                File f = new File(root + baseName + ext);
                if (f.exists() && f.isFile()) {
                    return f;
                }
            }
        }

        for (String root : rootPaths) {
            File f = new File(root + name);
            if (f.exists() && f.isFile()) {
                return f;
            }
        }

        return null;
    }

    /**
     * Play a sound effect asynchronously with zero game loop latency.
     */
    public void playSound(String soundName) {
        if (!soundEnabled || soundVolume <= 0.001f || masterVolume <= 0.001f) return;

        sfxPool.submit(() -> {
            try {
                CachedSound cached = getOrLoadSound(soundName);
                if (cached == null || cached.pcmData.length == 0) return;

                Clip clip = AudioSystem.getClip();
                clip.open(cached.format, cached.pcmData, 0, cached.pcmData.length);

                applyVolume(clip, masterVolume * soundVolume);

                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });

                clip.start();
            } catch (Exception e) {
                // Silently handle any audio device hiccups
            }
        });
    }

    /**
     * Play looping background music.
     */
    public synchronized void playMusic(String musicName) {
        if (musicName == null) {
            stopMusic();
            return;
        }

        if (currentMusicClip != null && currentMusicClip.isRunning() && musicName.equalsIgnoreCase(this.currentMusicName)) {
            return;
        }

        this.currentMusicName = musicName;
        this.musicPausePosition = 0;

        if (!musicEnabled) {
            stopMusicOnly();
            return;
        }

        stopMusicOnly();

        sfxPool.submit(() -> {
            try {
                String baseName = musicName;
                if (baseName.endsWith(".mp3") || baseName.endsWith(".wav")) {
                    baseName = baseName.substring(0, baseName.lastIndexOf('.'));
                }

                AudioInputStream din = null;

                // 1. Try package resource in /com/supermariox/audio/sounds/
                String[] exts = {".wav", ".mp3"};
                for (String ext : exts) {
                    InputStream is = getClass().getResourceAsStream("/com/supermariox/audio/sounds/" + baseName + ext);
                    if (is != null) {
                        try {
                            din = openAudioStream(is);
                            break;
                        } catch (Exception ignored) {}
                    }
                }

                // 2. Try file system
                if (din == null) {
                    File musicFile = resolveAudioFile("music", musicName);
                    if (musicFile != null && musicFile.exists()) {
                        din = openAudioStream(new FileInputStream(musicFile));
                    }
                }

                if (din == null) return;

                synchronized (SoundManager.this) {
                    if (!musicEnabled || currentMusicName == null || !musicName.equalsIgnoreCase(currentMusicName)) {
                        try { din.close(); } catch (Exception ignored) {}
                        return;
                    }

                    stopMusicOnly();
                    currentMusicClip = AudioSystem.getClip();
                    currentMusicClip.open(din);
                    applyVolume(currentMusicClip, masterVolume * musicVolume);
                    currentMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
                    currentMusicClip.start();
                }
            } catch (Exception e) {
                System.err.println("Could not load music track: " + musicName + " (" + e.getMessage() + ")");
            }
        });
    }

    private void stopMusicOnly() {
        if (currentMusicClip != null) {
            try {
                currentMusicClip.stop();
                currentMusicClip.close();
            } catch (Exception ignored) {}
            currentMusicClip = null;
        }
        musicPausePosition = 0;
    }

    public synchronized void stopMusic() {
        stopMusicOnly();
        currentMusicName = null;
    }

    public synchronized void pauseMusic() {
        if (currentMusicClip != null && currentMusicClip.isRunning()) {
            musicPausePosition = currentMusicClip.getMicrosecondPosition();
            currentMusicClip.stop();
        }
    }

    public synchronized void resumeMusic() {
        if (musicEnabled && currentMusicClip != null && !currentMusicClip.isRunning()) {
            currentMusicClip.setMicrosecondPosition(musicPausePosition);
            applyVolume(currentMusicClip, masterVolume * musicVolume);
            currentMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
            currentMusicClip.start();
        }
    }

    /**
     * Apply gain in decibels using a quadratic perceptual taper curve.
     * This ensures volume adjustments feel natural and even across the 0-100% range.
     */
    private void applyVolume(Clip clip, float volumeLevel) {
        try {
            if (clip != null && clip.isOpen() && clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                if (volumeLevel <= 0.001f) {
                    gainControl.setValue(gainControl.getMinimum());
                } else {
                    float effectiveVol = volumeLevel * volumeLevel;
                    float dB = (float) (Math.log10(effectiveVol) * 20.0);
                    dB = Math.max(gainControl.getMinimum(), Math.min(dB, gainControl.getMaximum()));
                    gainControl.setValue(dB);
                }
            }
        } catch (Exception ignored) {}
    }

    // Settings / Toggles
    public boolean isSoundEnabled() { return soundEnabled; }
    public synchronized void setSoundEnabled(boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
        saveSettings();
    }

    public boolean isMusicEnabled() { return musicEnabled; }
    public synchronized void setMusicEnabled(boolean musicEnabled) {
        this.musicEnabled = musicEnabled;
        if (!musicEnabled) {
            if (currentMusicClip != null && currentMusicClip.isOpen()) {
                applyVolume(currentMusicClip, 0f);
            }
        } else {
            if (currentMusicClip != null && currentMusicClip.isOpen()) {
                applyVolume(currentMusicClip, masterVolume * musicVolume);
                if (!currentMusicClip.isRunning()) {
                    currentMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
                    currentMusicClip.start();
                }
            } else if (currentMusicName != null) {
                playMusic(currentMusicName);
            }
        }
        saveSettings();
    }

    public synchronized void setMasterVolume(float masterVolume) {
        this.masterVolume = Math.max(0f, Math.min(1f, masterVolume));
        if (currentMusicClip != null && currentMusicClip.isOpen()) {
            float vol = musicEnabled ? (this.masterVolume * musicVolume) : 0f;
            applyVolume(currentMusicClip, vol);
        }
        saveSettings();
    }

    public synchronized void setSoundVolume(float soundVolume) {
        this.soundVolume = Math.max(0f, Math.min(1f, soundVolume));
        saveSettings();
    }

    public synchronized void setMusicVolume(float musicVolume) {
        this.musicVolume = Math.max(0f, Math.min(1f, musicVolume));
        if (currentMusicClip != null && currentMusicClip.isOpen()) {
            float vol = musicEnabled ? (masterVolume * this.musicVolume) : 0f;
            applyVolume(currentMusicClip, vol);
        }
        saveSettings();
    }

    public synchronized void adjustMasterVolume(float delta) {
        setMasterVolume(Math.round((masterVolume + delta) * 10.0f) / 10.0f);
    }

    public synchronized void adjustMusicVolume(float delta) {
        setMusicVolume(Math.round((musicVolume + delta) * 10.0f) / 10.0f);
    }

    public synchronized void adjustSoundVolume(float delta) {
        setSoundVolume(Math.round((soundVolume + delta) * 10.0f) / 10.0f);
    }

    public float getMasterVolume() { return masterVolume; }
    public float getSoundVolume() { return soundVolume; }
    public float getMusicVolume() { return musicVolume; }

    /**
     * Load persisted settings from settings.properties or Java Preferences.
     */
    private void loadSettings() {
        try {
            File propFile = new File("settings.properties");
            if (propFile.exists()) {
                Properties props = new Properties();
                try (FileInputStream in = new FileInputStream(propFile)) {
                    props.load(in);
                    masterVolume = Float.parseFloat(props.getProperty("master_volume", "0.80"));
                    soundVolume = Float.parseFloat(props.getProperty("sound_volume", "0.80"));
                    musicVolume = Float.parseFloat(props.getProperty("music_volume", "0.35"));
                    soundEnabled = Boolean.parseBoolean(props.getProperty("sound_enabled", "true"));
                    musicEnabled = Boolean.parseBoolean(props.getProperty("music_enabled", "true"));
                    return;
                }
            }

            Preferences prefs = Preferences.userNodeForPackage(SoundManager.class);
            masterVolume = prefs.getFloat("master_volume", 0.80f);
            soundVolume = prefs.getFloat("sound_volume", 0.80f);
            musicVolume = prefs.getFloat("music_volume", 0.35f);
            soundEnabled = prefs.getBoolean("sound_enabled", true);
            musicEnabled = prefs.getBoolean("music_enabled", true);
        } catch (Throwable ignored) {
            masterVolume = 0.80f;
            soundVolume = 0.80f;
            musicVolume = 0.35f;
            soundEnabled = true;
            musicEnabled = true;
        }
    }

    /**
     * Persist settings to both local properties file and Java Preferences.
     */
    public synchronized void saveSettings() {
        try {
            File propFile = new File("settings.properties");
            Properties props = new Properties();
            props.setProperty("master_volume", String.valueOf(masterVolume));
            props.setProperty("sound_volume", String.valueOf(soundVolume));
            props.setProperty("music_volume", String.valueOf(musicVolume));
            props.setProperty("sound_enabled", String.valueOf(soundEnabled));
            props.setProperty("music_enabled", String.valueOf(musicEnabled));
            try (FileOutputStream out = new FileOutputStream(propFile)) {
                props.store(out, "Super Mario X Sound Configuration");
            }

            Preferences prefs = Preferences.userNodeForPackage(SoundManager.class);
            prefs.putFloat("master_volume", masterVolume);
            prefs.putFloat("sound_volume", soundVolume);
            prefs.putFloat("music_volume", musicVolume);
            prefs.putBoolean("sound_enabled", soundEnabled);
            prefs.putBoolean("music_enabled", musicEnabled);
            prefs.flush();
        } catch (Throwable ignored) {}
    }
}
