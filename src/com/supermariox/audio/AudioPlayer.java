package com.supermariox.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Low-level audio backend with Java Sound and FFplay MP3 fallback support. */
public final class AudioPlayer {
    private static final String SOURCE_ASSET_ROOT = "src/assets/";
    private static final String PACKAGED_ASSET_ROOT = "assets/";

    private AudioPlayer() {
    }

    static Playback play(String folder, String fileName, boolean loop, float volume) throws Exception {
        File audioFile = findAudioFile(folder, fileName);
        if (audioFile == null) {
            throw new IOException("Audio file not found: " + folder + "/" + fileName);
        }

        try {
            return playWithJavaSound(audioFile, loop, volume);
        } catch (UnsupportedAudioFileException exception) {
            return playWithFfplay(audioFile, loop, volume);
        }
    }

    private static Playback playWithJavaSound(File file, boolean loop, float volume) throws Exception {
        // Step 1: open the raw (possibly compressed MP3) stream
        AudioInputStream rawStream = AudioSystem.getAudioInputStream(file);
        AudioFormat rawFormat = rawStream.getFormat();

        // Step 2: if not already PCM, transcode to PCM_SIGNED so Clip can handle it
        // (mp3spi returns MPEG1L3 with NOT_SPECIFIED frame size — Clip can't use that directly)
        AudioInputStream pcmStream;
        if (rawFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED
                && rawFormat.getEncoding() != AudioFormat.Encoding.PCM_UNSIGNED) {
            AudioFormat pcmFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    rawFormat.getSampleRate(),
                    16,
                    rawFormat.getChannels(),
                    rawFormat.getChannels() * 2,
                    rawFormat.getSampleRate(),
                    false);
            pcmStream = AudioSystem.getAudioInputStream(pcmFormat, rawStream);
        } else {
            pcmStream = rawStream;
        }

        // Step 3: load fully into a Clip
        Clip clip = AudioSystem.getClip();
        try {
            clip.open(pcmStream);
        } catch (Exception exception) {
            clip.close();
            pcmStream.close();
            throw exception;
        }

        setClipVolume(clip, volume);
        if (loop) {
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
        clip.start();
        return new Playback(clip, null);
    }

    private static Playback playWithFfplay(File file, boolean loop, float volume) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("ffplay");
        command.add("-nodisp");
        command.add("-loglevel");
        command.add("quiet");
        command.add("-volume");
        command.add(Integer.toString(Math.round(clampVolume(volume) * 100.0f)));
        if (loop) {
            command.add("-loop");
            command.add("0");
        } else {
            command.add("-autoexit");
        }
        command.add(file.getAbsolutePath());

        Process process = new ProcessBuilder(command)
                .redirectInput(ProcessBuilder.Redirect.PIPE)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        return new Playback(null, process);
    }

    private static void setClipVolume(Clip clip, float volume) {
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float safeVolume = clampVolume(volume);
        float decibels = safeVolume == 0.0f
                ? gain.getMinimum()
                : (float) (20.0 * Math.log10(safeVolume));
        gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
    }

    private static float clampVolume(float volume) {
        return Math.max(0.0f, Math.min(1.0f, volume));
    }

    private static File findAudioFile(String folder, String fileName) throws IOException {
        validatePathPart(folder, "folder");
        validatePathPart(fileName, "file name");

        File sourceFile = new File(SOURCE_ASSET_ROOT + folder, fileName);
        if (sourceFile.isFile()) {
            return sourceFile;
        }
        File packagedFile = new File(PACKAGED_ASSET_ROOT + folder, fileName);
        return packagedFile.isFile() ? packagedFile : null;
    }

    private static void validatePathPart(String value, String label) throws IOException {
        if (value == null || value.isBlank() || value.contains("..")
                || value.contains("/") || value.contains("\\")) {
            throw new IOException("Invalid audio " + label + ": " + value);
        }
    }

    static final class Playback {
        private final Clip clip;
        private final Process process;

        private Playback(Clip clip, Process process) {
            this.clip = clip;
            this.process = process;
        }

        void close() {
            if (clip != null) {
                clip.stop();
                clip.close();
            }
            if (process != null) {
                process.destroy();
            }
        }

        void onStopped(Runnable callback) {
            if (clip != null) {
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        callback.run();
                    }
                });
                return;
            }

            Thread waiter = new Thread(() -> {
                try {
                    process.waitFor();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } finally {
                    callback.run();
                }
            }, "AudioPlaybackCleanup");
            waiter.setDaemon(true);
            waiter.start();
        }

        void setVolume(float volume) {
            if (clip != null) {
                setClipVolume(clip, volume);
            }
            // FFplay volume is applied when its process starts.
        }
    }
}
