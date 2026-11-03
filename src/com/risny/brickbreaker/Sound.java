package com.risny.brickbreaker;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.util.EnumMap;
import java.util.Map;

/**
 * Tiny synthesised sound effects (no audio files needed).
 * If the machine has no audio device, every call is a silent no-op.
 */
final class Sound {
    enum Fx { LAUNCH, PADDLE, WALL, HIT, BREAK, STEEL, POWER, LOSE, CLEAR }

    private final Map<Fx, Clip> clips = new EnumMap<>(Fx.class);
    private boolean muted;

    static Sound silent() { Sound s = new Sound(); s.muted = true; return s; }

    static Sound create() {
        Sound s = new Sound();
        try {
            s.load(Fx.LAUNCH, tone(520, 880, 0.10, 0.25));
            s.load(Fx.PADDLE, tone(330, 330, 0.05, 0.25));
            s.load(Fx.WALL, tone(220, 220, 0.03, 0.15));
            s.load(Fx.HIT, tone(600, 600, 0.04, 0.2));
            s.load(Fx.BREAK, tone(760, 1200, 0.07, 0.25));
            s.load(Fx.STEEL, tone(150, 120, 0.06, 0.3));
            s.load(Fx.POWER, tone(660, 1320, 0.18, 0.25));
            s.load(Fx.LOSE, tone(400, 120, 0.35, 0.3));
            s.load(Fx.CLEAR, tone(523, 1047, 0.35, 0.25));
        } catch (Exception | LinkageError e) {
            s.muted = true;           // no audio device: run silently
        }
        return s;
    }

    private void load(Fx fx, byte[] pcm) throws Exception {
        Clip c = AudioSystem.getClip();
        c.open(new AudioFormat(44100, 16, 1, true, false), pcm, 0, pcm.length);
        clips.put(fx, c);
    }

    /** A short sine sweep from f0 to f1 Hz with a fade-out. */
    private static byte[] tone(double f0, double f1, double seconds, double volume) {
        int n = (int) (44100 * seconds);
        byte[] out = new byte[n * 2];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            phase += 2 * Math.PI * (f0 + (f1 - f0) * t) / 44100;
            short v = (short) (Math.sin(phase) * volume * (1 - t) * Short.MAX_VALUE);
            out[2 * i] = (byte) v;
            out[2 * i + 1] = (byte) (v >> 8);
        }
        return out;
    }

    void play(Fx fx) {
        if (muted) return;
        Clip c = clips.get(fx);
        if (c == null) return;
        c.stop();
        c.setFramePosition(0);
        c.start();
    }

    boolean toggleMute() { muted = !muted; return muted; }
    boolean muted() { return muted; }
}
