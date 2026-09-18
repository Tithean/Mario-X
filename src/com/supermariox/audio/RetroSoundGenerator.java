package com.supermariox.audio;

import javax.sound.sampled.AudioFormat;
import java.io.ByteArrayOutputStream;
import java.util.Random;

/**
 * Open-source procedural retro sound generator.
 * Synthesizes 100% royalty-free 8-bit / 16-bit sound effects
 * using square waves, triangle waves, and filtered noise.
 */
public class RetroSoundGenerator {
    public static final int SAMPLE_RATE = 44100;
    public static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    private static final Random random = new Random();

    public static byte[] generateSound(String soundType) {
        if (soundType == null) return null;
        String type = soundType.toLowerCase();

        if (type.contains("jump")) {
            return generateJump();
        } else if (type.contains("coin")) {
            return generateCoin();
        } else if (type.contains("stomp")) {
            return generateStomp();
        } else if (type.contains("smash") || type.contains("break")) {
            return generateBlockSmash();
        } else if (type.contains("hit") || type.contains("bump")) {
            return generateBlockHit();
        } else if (type.contains("grow") || type.contains("mushroom") || type.contains("powerup")) {
            return generatePowerup();
        } else if (type.contains("1up") || type.contains("life")) {
            return generateOneUp();
        } else if (type.contains("died") || type.contains("die")) {
            return generateDie();
        } else if (type.contains("pause")) {
            return generatePause();
        } else if (type.contains("select")) {
            return generateSelect();
        } else if (type.contains("win") || type.contains("clear")) {
            return generateVictory();
        } else if (type.contains("shell")) {
            return generateShellHit();
        }
        return null;
    }

    // Classic rising sweep jump sound
    public static byte[] generateJump() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int totalSamples = (int) (SAMPLE_RATE * 0.16); // 160ms
        double startFreq = 150.0;
        double endFreq = 580.0;
        double phase = 0.0;

        for (int i = 0; i < totalSamples; i++) {
            double progress = (double) i / totalSamples;
            double freq = startFreq + (endFreq - startFreq) * Math.pow(progress, 0.8);
            phase += 2.0 * Math.PI * freq / SAMPLE_RATE;

            double volume = 0.35 * (1.0 - (progress * 0.4));
            double sample = (Math.sin(phase) > 0 ? 1.0 : -1.0) * volume; // Square wave

            writeSample(out, sample);
        }
        return out.toByteArray();
    }

    // Classic two-tone coin sound (B5 -> E6)
    public static byte[] generateCoin() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int note1Samples = (int) (SAMPLE_RATE * 0.07); // 70ms
        int note2Samples = (int) (SAMPLE_RATE * 0.28); // 280ms
        double phase = 0.0;

        // Note 1: 987.77 Hz (B5)
        for (int i = 0; i < note1Samples; i++) {
            phase += 2.0 * Math.PI * 987.77 / SAMPLE_RATE;
            double sample = (Math.sin(phase) > 0 ? 0.32 : -0.32);
            writeSample(out, sample);
        }

        // Note 2: 1318.51 Hz (E6) with gradual decay
        phase = 0.0;
        for (int i = 0; i < note2Samples; i++) {
            phase += 2.0 * Math.PI * 1318.51 / SAMPLE_RATE;
            double decay = Math.pow(1.0 - ((double) i / note2Samples), 1.5);
            double sample = (Math.sin(phase) > 0 ? 0.35 : -0.35) * decay;
            writeSample(out, sample);
        }
        return out.toByteArray();
    }

    // Stomp enemy sound (crunchy noise + descending thump)
    public static byte[] generateStomp() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int totalSamples = (int) (SAMPLE_RATE * 0.12);
        double phase = 0.0;

        for (int i = 0; i < totalSamples; i++) {
            double progress = (double) i / totalSamples;
            double decay = 1.0 - progress;
            double freq = 240.0 * (1.0 - progress * 0.7);
            phase += 2.0 * Math.PI * freq / SAMPLE_RATE;

            double tone = Math.sin(phase) * 0.35;
            double noise = (random.nextDouble() * 2.0 - 1.0) * 0.45 * (progress < 0.3 ? 1.0 : (1.0 - progress));
            double sample = (tone + noise) * decay;

            writeSample(out, sample);
        }
        return out.toByteArray();
    }

    // Block hit / bump
    public static byte[] generateBlockHit() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int totalSamples = (int) (SAMPLE_RATE * 0.09);
        double phase = 0.0;

        for (int i = 0; i < totalSamples; i++) {
            double progress = (double) i / totalSamples;
            double freq = 180.0 * (1.0 - progress * 0.5);
            phase += 2.0 * Math.PI * freq / SAMPLE_RATE;

            double decay = Math.pow(1.0 - progress, 2.0);
            double sample = (Math.sin(phase) > 0 ? 0.4 : -0.4) * decay;
            writeSample(out, sample);
        }
        return out.toByteArray();
    }

    // Block smash / brick break
    public static byte[] generateBlockSmash() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int totalSamples = (int) (SAMPLE_RATE * 0.22);

        for (int i = 0; i < totalSamples; i++) {
            double progress = (double) i / totalSamples;
            double decay = Math.pow(1.0 - progress, 1.2);
            double noise = (random.nextDouble() * 2.0 - 1.0) * 0.45;
            double lowThump = Math.sin(2.0 * Math.PI * 110.0 * (double) i / SAMPLE_RATE) * 0.3;
            double sample = (noise + lowThump) * decay;
            writeSample(out, sample);
        }
        return out.toByteArray();
    }

    // Powerup / Mushroom grow arpeggio
    public static byte[] generatePowerup() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        double[] notes = {330.0, 392.0, 659.25, 523.25, 587.33, 783.99}; // E4, G4, E5, C5, D5, G5
        int noteLen = (int) (SAMPLE_RATE * 0.06);

        for (double freq : notes) {
            double phase = 0.0;
            for (int i = 0; i < noteLen; i++) {
                phase += 2.0 * Math.PI * freq / SAMPLE_RATE;
                double decay = 1.0 - ((double) i / noteLen * 0.3);
                double sample = (Math.sin(phase) > 0 ? 0.32 : -0.32) * decay;
                writeSample(out, sample);
            }
        }
        return out.toByteArray();
    }

    // 1-Up chime (classic celebratory 6-note arpeggio)
    public static byte[] generateOneUp() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        double[] notes = {330.0, 392.0, 659.25, 523.25, 587.33, 783.99};
        int noteLen = (int) (SAMPLE_RATE * 0.08);

        for (double freq : notes) {
            double phase = 0.0;
            for (int i = 0; i < noteLen; i++) {
                phase += 2.0 * Math.PI * freq / SAMPLE_RATE;
                double decay = 1.0 - ((double) i / noteLen * 0.2);
                double sample = (Math.sin(phase) > 0 ? 0.34 : -0.34) * decay;
                writeSample(out, sample);
            }
        }
        return out.toByteArray();
    }

    // Player death descending tune
    public static byte[] generateDie() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        double[] notes = {523.25, 493.88, 466.16, 440.0, 392.0, 329.63, 261.63};
        int noteLen = (int) (SAMPLE_RATE * 0.11);

        for (double freq : notes) {
            double phase = 0.0;
            for (int i = 0; i < noteLen; i++) {
                phase += 2.0 * Math.PI * freq / SAMPLE_RATE;
                double decay = 1.0 - ((double) i / noteLen);
                double sample = (Math.sin(phase) > 0 ? 0.32 : -0.32) * decay;
                writeSample(out, sample);
            }
        }
        return out.toByteArray();
    }

    // Shell kick thud
    public static byte[] generateShellHit() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int totalSamples = (int) (SAMPLE_RATE * 0.12);
        double phase = 0.0;

        for (int i = 0; i < totalSamples; i++) {
            double progress = (double) i / totalSamples;
            double freq = 320.0 * (1.0 - progress * 0.7);
            phase += 2.0 * Math.PI * freq / SAMPLE_RATE;
            double decay = Math.pow(1.0 - progress, 2.0);
            double sample = (Math.sin(phase) > 0 ? 0.4 : -0.4) * decay;
            writeSample(out, sample);
        }
        return out.toByteArray();
    }

    // Pause staccato beep
    public static byte[] generatePause() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int part = (int) (SAMPLE_RATE * 0.06);
        for (int repeat = 0; repeat < 2; repeat++) {
            double phase = 0.0;
            for (int i = 0; i < part; i++) {
                phase += 2.0 * Math.PI * 784.0 / SAMPLE_RATE;
                double sample = (Math.sin(phase) > 0 ? 0.3 : -0.3);
                writeSample(out, sample);
            }
        }
        return out.toByteArray();
    }

    // Menu select blip
    public static byte[] generateSelect() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int part = (int) (SAMPLE_RATE * 0.04);
        double phase = 0.0;
        for (int i = 0; i < part; i++) {
            phase += 2.0 * Math.PI * 880.0 / SAMPLE_RATE;
            double sample = (Math.sin(phase) > 0 ? 0.25 : -0.25);
            writeSample(out, sample);
        }
        return out.toByteArray();
    }

    // Victory fanfare
    public static byte[] generateVictory() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        double[] notes = {523.25, 659.25, 783.99, 1046.50};
        int noteLen = (int) (SAMPLE_RATE * 0.15);

        for (double freq : notes) {
            double phase = 0.0;
            for (int i = 0; i < noteLen; i++) {
                phase += 2.0 * Math.PI * freq / SAMPLE_RATE;
                double decay = 1.0 - ((double) i / noteLen * 0.15);
                double sample = (Math.sin(phase) > 0 ? 0.35 : -0.35) * decay;
                writeSample(out, sample);
            }
        }
        return out.toByteArray();
    }

    private static void writeSample(ByteArrayOutputStream out, double sample) {
        sample = Math.max(-1.0, Math.min(1.0, sample));
        short s = (short) (sample * 32767);
        out.write(s & 0xFF);
        out.write((s >> 8) & 0xFF);
    }
}
