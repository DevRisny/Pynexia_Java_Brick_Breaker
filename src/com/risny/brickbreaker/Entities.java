package com.risny.brickbreaker;

import java.awt.Color;

/** Game objects. Plain mutable data: the World updates them, the Renderer draws them. */
final class Entities {
    private Entities() {}

    static final class Ball {
        double x, y, vx, vy;
        final double r;
        boolean stuck = true;          // waiting on the paddle before launch
        Ball(double x, double y, double r) { this.x = x; this.y = y; this.r = r; }
        double speed() { return Math.hypot(vx, vy); }
    }

    static final class Paddle {
        double x, y, w, h, targetX;
        double wideTimer;              // seconds left of the "wide" power-up
        Paddle(double x, double y, double w, double h) { this.x = x; this.y = y; this.w = w; this.h = h; this.targetX = x; }
    }

    enum BrickType {
        NORMAL(1, 50), STRONG(2, 120), TOUGH(3, 200), STEEL(-1, 0);
        final int hits, points;
        BrickType(int hits, int points) { this.hits = hits; this.points = points; }
    }

    static final class Brick {
        final double x, y, w, h;
        final BrickType type;
        final Color color;
        int hitsLeft;
        double flash;                  // short white flash when hit
        Brick(double x, double y, double w, double h, BrickType type, Color color) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.type = type; this.color = color; this.hitsLeft = type.hits;
        }
        boolean breakable() { return type != BrickType.STEEL; }
        boolean alive() { return type == BrickType.STEEL || hitsLeft > 0; }
    }

    enum PowerType {
        WIDE("Wide paddle", new Color(0x2563FF)),
        MULTI("Multi-ball", new Color(0x7C5CFF)),
        SLOW("Slow ball", new Color(0x10B981)),
        LIFE("Extra life", new Color(0xFF5C8A));
        final String label; final Color color;
        PowerType(String label, Color color) { this.label = label; this.color = color; }
    }

    static final class PowerUp {
        double x, y; final PowerType type; final double vy = 140;
        PowerUp(double x, double y, PowerType type) { this.x = x; this.y = y; this.type = type; }
    }

    static final class Particle {
        double x, y, vx, vy, life, maxLife, size; final Color color;
        Particle(double x, double y, double vx, double vy, double life, double size, Color color) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.life = life; this.maxLife = life; this.size = size; this.color = color;
        }
    }

    /** Floating "+150" text after a hit. */
    static final class Popup {
        double x, y, life = 0.8; final String text; final Color color;
        Popup(double x, double y, String text, Color color) { this.x = x; this.y = y; this.text = text; this.color = color; }
    }
}
