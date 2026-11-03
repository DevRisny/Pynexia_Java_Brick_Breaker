package com.risny.brickbreaker;

import com.risny.brickbreaker.Entities.*;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/** Draws the World on any Graphics2D (screen or an off-screen image for screenshots). Light, bright theme. */
final class Renderer {
    private static final Color INK = new Color(0x0F1B3D), INK2 = new Color(0x56607A), PRIMARY = new Color(0x2563FF);
    private static final Font TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 54);
    private static final Font H2 = new Font(Font.SANS_SERIF, Font.BOLD, 28);
    private static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 17);
    private static final Font SMALL = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    private static final Font HUD = new Font(Font.SANS_SERIF, Font.BOLD, 20);

    void draw(Graphics2D g, World w, boolean muted) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // background: soft sky gradient with a dot grid
        g.setPaint(new GradientPaint(0, 0, new Color(0xEEF4FF), (float) World.W, (float) World.H, new Color(0xFDF2F8)));
        g.fillRect(0, 0, (int) World.W, (int) World.H);
        g.setColor(new Color(15, 27, 61, 14));
        for (int x = 20; x < World.W; x += 32) for (int y = 80; y < World.H; y += 32) g.fillOval(x, y, 3, 3);

        double sx = w.shake > 0 ? (w.rng.nextDouble() - .5) * w.shake : 0, sy = w.shake > 0 ? (w.rng.nextDouble() - .5) * w.shake : 0;
        g.translate(sx, sy);
        for (Brick k : w.bricks) drawBrick(g, k);
        for (PowerUp p : w.powerUps) drawPowerUp(g, p);
        drawPaddle(g, w.paddle);
        for (Ball b : w.balls) drawBall(g, b, w.slowTimer > 0);
        for (Particle p : w.particles) {
            float a = (float) Math.max(0, p.life / p.maxLife);
            g.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), (int) (220 * a)));
            g.fill(new RoundRectangle2D.Double(p.x, p.y, p.size, p.size, 2, 2));
        }
        g.setFont(SMALL.deriveFont(15f));
        for (Popup p : w.popups) {
            float a = (float) Math.max(0, Math.min(1, p.life / 0.5));
            g.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), (int) (255 * a)));
            centered(g, p.text, p.x, p.y);
        }
        g.translate(-sx, -sy);

        drawHud(g, w, muted);
        drawOverlay(g, w);
    }

    private void drawBrick(Graphics2D g, Brick k) {
        Color base = k.flash > 0 ? Color.WHITE : k.color;
        if (k.type == BrickType.STRONG && k.hitsLeft == 1 || k.type == BrickType.TOUGH && k.hitsLeft < 3) base = blend(base, Color.WHITE, 0.35);
        RoundRectangle2D r = new RoundRectangle2D.Double(k.x, k.y, k.w, k.h, 10, 10);
        g.setColor(new Color(15, 27, 61, 28));
        g.fill(new RoundRectangle2D.Double(k.x, k.y + 3, k.w, k.h, 10, 10));
        g.setPaint(new GradientPaint((float) k.x, (float) k.y, blend(base, Color.WHITE, 0.25), (float) k.x, (float) (k.y + k.h), base));
        g.fill(r);
        g.setColor(new Color(255, 255, 255, 110));
        g.fill(new RoundRectangle2D.Double(k.x + 4, k.y + 3, k.w - 8, 5, 5, 5));
        if (k.type == BrickType.STEEL) {
            g.setColor(new Color(255, 255, 255, 160));
            for (double rx : new double[] {k.x + 7, k.x + k.w - 11}) g.fill(new Ellipse2D.Double(rx, k.y + k.h / 2 - 2, 4, 4));
        } else if (k.type.hits > 1) {
            g.setColor(new Color(255, 255, 255, 230));
            for (int i = 0; i < k.hitsLeft; i++) g.fill(new Ellipse2D.Double(k.x + k.w / 2 - (k.hitsLeft - 1) * 6 + i * 12 - 3, k.y + k.h / 2 - 1, 6, 6));
            if (k.hitsLeft < k.type.hits) { // crack
                g.setStroke(new BasicStroke(1.6f)); g.setColor(new Color(255, 255, 255, 170));
                g.drawLine((int) (k.x + k.w * .25), (int) k.y + 4, (int) (k.x + k.w * .38), (int) (k.y + k.h - 5));
                g.drawLine((int) (k.x + k.w * .38), (int) (k.y + k.h * .5), (int) (k.x + k.w * .5), (int) (k.y + k.h * .35));
            }
        }
    }

    private void drawPaddle(Graphics2D g, Paddle p) {
        g.setColor(new Color(37, 99, 255, 50));
        g.fill(new RoundRectangle2D.Double(p.x - 2, p.y + 6, p.w + 4, p.h + 4, 20, 20));
        g.setPaint(new GradientPaint((float) p.x, 0, new Color(0x2563FF), (float) (p.x + p.w), 0, new Color(0x7C5CFF)));
        g.fill(new RoundRectangle2D.Double(p.x, p.y, p.w, p.h, p.h, p.h));
        g.setColor(new Color(255, 255, 255, 120));
        g.fill(new RoundRectangle2D.Double(p.x + 8, p.y + 3, p.w - 16, 4, 4, 4));
    }

    private void drawBall(Graphics2D g, Ball b, boolean slow) {
        if (!b.stuck) { // motion trail
            for (int i = 1; i <= 5; i++) {
                double t = i * 0.012;
                g.setColor(new Color(37, 99, 255, 40 - i * 6));
                double r = b.r * (1 - i * 0.12);
                g.fill(new Ellipse2D.Double(b.x - b.vx * t - r, b.y - b.vy * t - r, r * 2, r * 2));
            }
        }
        g.setColor(slow ? new Color(0x10B981) : PRIMARY);
        g.fill(new Ellipse2D.Double(b.x - b.r - 2, b.y - b.r - 2, (b.r + 2) * 2, (b.r + 2) * 2));
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(b.x - b.r + 1, b.y - b.r + 1, (b.r - 1) * 2, (b.r - 1) * 2));
    }

    private void drawPowerUp(Graphics2D g, PowerUp p) {
        g.setColor(p.type.color);
        g.fill(new RoundRectangle2D.Double(p.x - 26, p.y - 11, 52, 22, 22, 22));
        g.setColor(Color.WHITE);
        g.setFont(SMALL);
        String s = switch (p.type) { case WIDE -> "WIDE"; case MULTI -> "x3"; case SLOW -> "SLOW"; case LIFE -> "+1 ♥"; };
        centered(g, s, p.x, p.y + 5);
    }

    private void drawHud(Graphics2D g, World w, boolean muted) {
        g.setColor(new Color(255, 255, 255, 235));
        g.fill(new RoundRectangle2D.Double(12, 12, World.W - 24, 54, 18, 18));
        g.setColor(new Color(0xE6EAF3));
        g.draw(new RoundRectangle2D.Double(12, 12, World.W - 24, 54, 18, 18));
        g.setFont(SMALL); g.setColor(INK2);
        g.drawString("SCORE", 32, 34); g.drawString("LEVEL", 200, 34); g.drawString("BEST", 330, 34);
        g.setFont(HUD); g.setColor(INK);
        g.drawString(String.format("%,d", w.score), 32, 56);
        g.drawString((w.level + 1) + " / " + Levels.count(), 200, 56);
        g.drawString(String.format("%,d", Math.max(w.highScore, w.score)), 330, 56);
        // combo meter
        if (w.combo >= 3) {
            int mult = Math.min(5, 1 + w.combo / 3);
            g.setColor(new Color(0xFFF4DB)); g.fill(new RoundRectangle2D.Double(470, 24, 120, 30, 30, 30));
            g.setColor(new Color(0x92400E)); g.setFont(SMALL.deriveFont(14f)); g.drawString("COMBO x" + mult, 490, 44);
        }
        // active power-ups
        double px = 610;
        g.setFont(SMALL);
        if (w.paddle.wideTimer > 0) { pill(g, px, "WIDE " + (int) Math.ceil(w.paddle.wideTimer) + "s", Entities.PowerType.WIDE.color); px += 92; }
        if (w.slowTimer > 0) { pill(g, px, "SLOW " + (int) Math.ceil(w.slowTimer) + "s", Entities.PowerType.SLOW.color); }
        // lives
        for (int i = 0; i < w.lives; i++) heart(g, World.W - 44 - i * 26, 30);
        if (muted) { g.setColor(INK2); g.setFont(SMALL); g.drawString("MUTED", (int) World.W - 44 - w.lives * 26 - 46, 44); }
    }

    private void drawOverlay(Graphics2D g, World w) {
        switch (w.state) {
            case MENU -> card(g, "Brick Breaker", "Break every brick. Catch power-ups. Keep the combo going.",
                    new String[] {"Move: mouse or ← →", "Launch: Space or click", "Pause: P    Sound: M"}, "Press Space or click to start");
            case READY -> {
                if (w.stateTimer > 0) banner(g, w.banner);
                g.setFont(BODY); g.setColor(INK2);
                centered(g, "Press Space or click to launch", World.W / 2, World.PADDLE_Y - 40);
            }
            case PAUSED -> card(g, "Paused", "Take a breath.", new String[] {"Score " + String.format("%,d", w.score), "Lives " + w.lives}, "Press P or Space to continue");
            case LEVEL_CLEAR -> card(g, "Level clear!", "+" + (500 + w.lives * 100) + " bonus  ·  best combo " + w.bestCombo,
                    new String[] {"Next: " + Levels.NAMES[Math.min(w.level + 1, Levels.count() - 1)]}, "Press Space or click to continue");
            case GAME_OVER -> card(g, "Game over", "Final score " + String.format("%,d", w.score) + (w.score >= w.highScore && w.score > 0 ? "  ·  new best!" : ""),
                    new String[] {"Reached level " + (w.level + 1), "Best combo " + w.bestCombo}, "Press Space or click to play again");
            case WON -> card(g, "You won! 🎉", "All " + Levels.count() + " levels cleared with " + String.format("%,d", w.score) + " points",
                    new String[] {"Best combo " + w.bestCombo, "Lives left " + w.lives}, "Press Space or click to play again");
            default -> { }
        }
    }

    private void card(Graphics2D g, String title, String sub, String[] lines, String cta) {
        g.setColor(new Color(238, 244, 255, 170));
        g.fillRect(0, 0, (int) World.W, (int) World.H);
        double cw = 520, ch = 300, cx = (World.W - cw) / 2, cy = (World.H - ch) / 2 + 20;
        g.setColor(new Color(15, 27, 61, 30));
        g.fill(new RoundRectangle2D.Double(cx, cy + 10, cw, ch, 28, 28));
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Double(cx, cy, cw, ch, 28, 28));
        g.setFont(TITLE.deriveFont(title.length() > 12 ? 44f : 54f)); g.setColor(INK);
        centered(g, title, World.W / 2, cy + 78);
        g.setFont(BODY); g.setColor(INK2);
        centered(g, sub, World.W / 2, cy + 116);
        g.setFont(BODY.deriveFont(Font.BOLD, 16f)); g.setColor(INK);
        for (int i = 0; i < lines.length; i++) centered(g, lines[i], World.W / 2, cy + 160 + i * 26);
        g.setColor(PRIMARY);
        g.fill(new RoundRectangle2D.Double(cx + 90, cy + ch - 64, cw - 180, 44, 22, 22));
        g.setColor(Color.WHITE); g.setFont(BODY.deriveFont(Font.BOLD, 16f));
        centered(g, cta, World.W / 2, cy + ch - 36);
    }

    private void banner(Graphics2D g, String text) {
        g.setFont(H2);
        int tw = g.getFontMetrics().stringWidth(text) + 60;
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Double((World.W - tw) / 2, World.H / 2 + 20, tw, 56, 28, 28));
        g.setColor(INK);
        centered(g, text, World.W / 2, World.H / 2 + 58);
    }

    private void pill(Graphics2D g, double x, String s, Color c) {
        g.setColor(blend(c, Color.WHITE, 0.82)); g.fill(new RoundRectangle2D.Double(x, 24, 84, 30, 30, 30));
        g.setColor(c.darker()); centered(g, s, x + 42, 44);
    }

    private void heart(Graphics2D g, double x, double y) {
        g.setColor(new Color(0xFF5C8A));
        g.fill(new Ellipse2D.Double(x, y, 10, 10));
        g.fill(new Ellipse2D.Double(x + 8, y, 10, 10));
        g.fillPolygon(new int[] {(int) x, (int) x + 18, (int) x + 9}, new int[] {(int) y + 6, (int) y + 6, (int) y + 17}, 3);
    }

    private static void centered(Graphics2D g, String s, double cx, double baseline) {
        g.drawString(s, (float) (cx - g.getFontMetrics().stringWidth(s) / 2.0), (float) baseline);
    }

    private static Color blend(Color a, Color b, double t) {
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t), (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t), (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
