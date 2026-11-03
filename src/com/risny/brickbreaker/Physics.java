package com.risny.brickbreaker;

/**
 * Pure collision maths, kept free of Swing so it can be unit tested.
 * All shapes are axis-aligned rectangles (x, y = top-left) and circles (cx, cy, r).
 */
public final class Physics {
    private Physics() {}

    /** Result of a circle-rectangle test: the push-out normal and depth. */
    public record Hit(double nx, double ny, double depth) {}

    /**
     * Circle vs rectangle. Returns null when there is no overlap, otherwise the normal
     * pointing from the rectangle towards the circle centre and how deep they overlap.
     */
    public static Hit circleRect(double cx, double cy, double r, double rx, double ry, double rw, double rh) {
        double closestX = clamp(cx, rx, rx + rw);
        double closestY = clamp(cy, ry, ry + rh);
        double dx = cx - closestX, dy = cy - closestY;
        double d2 = dx * dx + dy * dy;
        if (d2 > r * r) return null;
        if (d2 > 1e-9) {
            double d = Math.sqrt(d2);
            return new Hit(dx / d, dy / d, r - d);
        }
        // centre is inside the rectangle: push out along the smallest overlap axis
        double left = cx - rx, right = rx + rw - cx, top = cy - ry, bottom = ry + rh - cy;
        double m = Math.min(Math.min(left, right), Math.min(top, bottom));
        if (m == left) return new Hit(-1, 0, left + r);
        if (m == right) return new Hit(1, 0, right + r);
        if (m == top) return new Hit(0, -1, top + r);
        return new Hit(0, 1, bottom + r);
    }

    /** Reflect velocity (vx, vy) about a unit normal, only if moving into the surface. */
    public static double[] reflect(double vx, double vy, double nx, double ny) {
        double dot = vx * nx + vy * ny;
        if (dot >= 0) return new double[] {vx, vy};
        return new double[] {vx - 2 * dot * nx, vy - 2 * dot * ny};
    }

    /**
     * Paddle bounce: where the ball lands on the paddle sets its angle.
     * Centre sends it straight up, the edges send it out at up to maxAngle degrees.
     * Speed is preserved.
     */
    public static double[] paddleBounce(double ballX, double paddleX, double paddleW, double speed, double maxAngleDeg) {
        double rel = clamp((ballX - (paddleX + paddleW / 2)) / (paddleW / 2), -1, 1);
        double angle = Math.toRadians(rel * maxAngleDeg);
        return new double[] {speed * Math.sin(angle), -speed * Math.cos(angle)};
    }

    /** Never let the ball travel almost horizontally, or a rally could last forever. */
    public static double[] enforceMinVertical(double vx, double vy, double minRatio) {
        double speed = Math.hypot(vx, vy);
        double minVy = speed * minRatio;
        if (Math.abs(vy) >= minVy) return new double[] {vx, vy};
        double sy = vy < 0 ? -1 : 1;
        double nvy = sy * minVy;
        double nvx = Math.signum(vx == 0 ? 1 : vx) * Math.sqrt(Math.max(0, speed * speed - nvy * nvy));
        return new double[] {nvx, nvy};
    }

    /** Number of sub-steps so the ball never moves more than maxStep pixels at once (no tunnelling). */
    public static int substeps(double vx, double vy, double dt, double maxStep) {
        return Math.max(1, (int) Math.ceil(Math.hypot(vx, vy) * dt / maxStep));
    }

    public static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    /** Score for a brick: base points times a combo multiplier that grows with consecutive hits (max x5). */
    public static int score(int basePoints, int combo) {
        return basePoints * Math.min(5, 1 + combo / 3);
    }
}
