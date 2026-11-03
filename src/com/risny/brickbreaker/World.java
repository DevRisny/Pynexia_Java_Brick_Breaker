package com.risny.brickbreaker;

import com.risny.brickbreaker.Entities.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** All game state and rules. No Swing here, so the game can be simulated in tests and rendered headless. */
final class World {
    enum State { MENU, READY, PLAYING, PAUSED, LEVEL_CLEAR, GAME_OVER, WON }

    static final double W = 900, H = 640;
    static final double BALL_R = 8, BASE_SPEED = 420, MAX_SPEED = 720;
    static final double PADDLE_W = 120, PADDLE_H = 16, PADDLE_Y = H - 56;

    final List<Ball> balls = new ArrayList<>();
    final List<Brick> bricks = new ArrayList<>();
    final List<PowerUp> powerUps = new ArrayList<>();
    final List<Particle> particles = new ArrayList<>();
    final List<Popup> popups = new ArrayList<>();
    final Paddle paddle = new Paddle(W / 2 - PADDLE_W / 2, PADDLE_Y, PADDLE_W, PADDLE_H);
    final Random rng;
    final Sound sound;

    State state = State.MENU;
    int level = 0, score = 0, lives = 3, combo = 0, bestCombo = 0, highScore;
    double slowTimer, shake, stateTimer, time;
    String banner = "";

    World(long seed, Sound sound, int highScore) {
        this.rng = new Random(seed);
        this.sound = sound;
        this.highScore = highScore;
    }

    // ---------------------------------------------------------------- flow
    void newGame() {
        level = 0; score = 0; lives = 3; combo = 0; bestCombo = 0;
        loadLevel(0);
    }

    void loadLevel(int l) {
        level = l;
        bricks.clear(); powerUps.clear(); particles.clear(); popups.clear();
        bricks.addAll(Levels.build(l, W, 96));
        paddle.w = PADDLE_W; paddle.wideTimer = 0; slowTimer = 0;
        resetBall();
        banner = "Level " + (l + 1) + " · " + Levels.NAMES[l];
        stateTimer = 1.6;
    }

    void resetBall() {
        balls.clear();
        Ball b = new Ball(paddle.x + paddle.w / 2, paddle.y - BALL_R - 1, BALL_R);
        balls.add(b);
        combo = 0;
        state = State.READY;
    }

    /** Space / click: start, launch, continue, or restart depending on the state. */
    void primaryAction() {
        switch (state) {
            case MENU, GAME_OVER, WON -> newGame();
            case READY -> launch();
            case PAUSED -> state = State.PLAYING;
            case LEVEL_CLEAR -> { if (stateTimer <= 0) loadLevel(level + 1); }
            default -> { }
        }
    }

    void launch() {
        for (Ball b : balls) {
            if (!b.stuck) continue;
            double[] v = Physics.paddleBounce(b.x + (rng.nextDouble() - .5) * 30, paddle.x, paddle.w, speedForLevel(), 45);
            b.vx = v[0]; b.vy = v[1]; b.stuck = false;
        }
        state = State.PLAYING;
        sound.play(Sound.Fx.LAUNCH);
    }

    void togglePause() {
        if (state == State.PLAYING) state = State.PAUSED;
        else if (state == State.PAUSED) state = State.PLAYING;
    }

    double speedForLevel() { return BASE_SPEED + level * 40; }

    void movePaddleTo(double centerX) { paddle.targetX = centerX - paddle.w / 2; }

    // ---------------------------------------------------------------- update
    void update(double dt) {
        time += dt;
        stateTimer = Math.max(0, stateTimer - dt);
        shake = Math.max(0, shake - dt * 30);
        updateEffects(dt);
        if (state == State.PAUSED || state == State.MENU || state == State.GAME_OVER || state == State.WON) return;

        // paddle eases towards the target so mouse and keys both feel smooth
        double newX = paddle.x + (paddle.targetX - paddle.x) * Math.min(1, dt * 18);
        paddle.x = Physics.clamp(newX, 8, W - paddle.w - 8);
        if (paddle.wideTimer > 0 && (paddle.wideTimer -= dt) <= 0) {
            double c = paddle.x + paddle.w / 2; paddle.w = PADDLE_W; paddle.x = c - paddle.w / 2;
        }
        if (slowTimer > 0) slowTimer -= dt;

        if (state == State.READY) {
            for (Ball b : balls) { b.x = paddle.x + paddle.w / 2; b.y = paddle.y - b.r - 1; }
            return;
        }
        if (state == State.LEVEL_CLEAR) return;

        double speedScale = slowTimer > 0 ? 0.65 : 1.0;
        for (Iterator<Ball> it = balls.iterator(); it.hasNext(); ) {
            Ball b = it.next();
            int steps = Physics.substeps(b.vx * speedScale, b.vy * speedScale, dt, b.r * 0.5);
            double h = dt / steps;
            boolean lost = false;
            for (int s = 0; s < steps && !lost; s++) lost = stepBall(b, h * speedScale);
            if (lost) it.remove();
        }
        if (balls.isEmpty()) loseLife();

        updatePowerUps(dt);
        if (bricks.stream().noneMatch(Brick::breakable)) levelCleared();
    }

    /** Moves one ball a small step. Returns true if it fell off the bottom. */
    private boolean stepBall(Ball b, double h) {
        b.x += b.vx * h; b.y += b.vy * h;

        // walls
        if (b.x - b.r < 0) { b.x = b.r; b.vx = Math.abs(b.vx); sound.play(Sound.Fx.WALL); }
        if (b.x + b.r > W) { b.x = W - b.r; b.vx = -Math.abs(b.vx); sound.play(Sound.Fx.WALL); }
        if (b.y - b.r < 0) { b.y = b.r; b.vy = Math.abs(b.vy); sound.play(Sound.Fx.WALL); }
        if (b.y - b.r > H) return true;

        // paddle
        if (b.vy > 0) {
            Physics.Hit hit = Physics.circleRect(b.x, b.y, b.r, paddle.x, paddle.y, paddle.w, paddle.h);
            if (hit != null && hit.ny() < 0.2) {
                double[] v = Physics.paddleBounce(b.x, paddle.x, paddle.w, b.speed(), 60);
                b.vx = v[0]; b.vy = v[1]; b.y = paddle.y - b.r - 0.5;
                combo = 0;
                sound.play(Sound.Fx.PADDLE);
                burst(b.x, paddle.y, new Color(0x2563FF), 6, 120);
                return false;
            }
        }

        // bricks: resolve the deepest overlap only, so corner hits do not double-bounce
        Brick best = null; Physics.Hit bestHit = null;
        for (Brick k : bricks) {
            Physics.Hit hit = Physics.circleRect(b.x, b.y, b.r, k.x, k.y, k.w, k.h);
            if (hit != null && (bestHit == null || hit.depth() > bestHit.depth())) { best = k; bestHit = hit; }
        }
        if (best != null) {
            b.x += bestHit.nx() * bestHit.depth(); b.y += bestHit.ny() * bestHit.depth();
            double[] v = Physics.reflect(b.vx, b.vy, bestHit.nx(), bestHit.ny());
            v = Physics.enforceMinVertical(v[0], v[1], 0.28);
            b.vx = v[0]; b.vy = v[1];
            hitBrick(best);
            // speed up gently as the rally goes on
            double sp = Math.min(MAX_SPEED, b.speed() * 1.012);
            double k = sp / b.speed(); b.vx *= k; b.vy *= k;
        }
        return false;
    }

    private void hitBrick(Brick k) {
        k.flash = 0.12;
        if (!k.breakable()) { sound.play(Sound.Fx.STEEL); return; }
        k.hitsLeft--;
        if (k.hitsLeft > 0) { sound.play(Sound.Fx.HIT); burst(k.x + k.w / 2, k.y + k.h / 2, k.color, 6, 140); return; }
        combo++;
        bestCombo = Math.max(bestCombo, combo);
        int pts = Physics.score(k.type.points, combo);
        score += pts;
        bricks.remove(k);
        sound.play(Sound.Fx.BREAK);
        burst(k.x + k.w / 2, k.y + k.h / 2, k.color, 18, 260);
        popups.add(new Popup(k.x + k.w / 2, k.y, "+" + pts + (combo >= 3 ? "  x" + Math.min(5, 1 + combo / 3) : ""), k.color));
        shake = Math.min(6, shake + 2);
        if (rng.nextDouble() < 0.16) {
            PowerType[] all = PowerType.values();
            powerUps.add(new PowerUp(k.x + k.w / 2, k.y + k.h / 2, all[rng.nextInt(all.length)]));
        }
    }

    private void updatePowerUps(double dt) {
        for (Iterator<PowerUp> it = powerUps.iterator(); it.hasNext(); ) {
            PowerUp p = it.next();
            p.y += p.vy * dt;
            if (p.y > H + 20) { it.remove(); continue; }
            if (p.y + 10 >= paddle.y && p.y - 10 <= paddle.y + paddle.h && p.x >= paddle.x - 10 && p.x <= paddle.x + paddle.w + 10) {
                apply(p.type);
                popups.add(new Popup(p.x, paddle.y - 16, p.type.label, p.type.color));
                it.remove();
            }
        }
    }

    void apply(PowerType t) {
        sound.play(Sound.Fx.POWER);
        switch (t) {
            case WIDE -> { double c = paddle.x + paddle.w / 2; paddle.w = PADDLE_W * 1.6; paddle.x = c - paddle.w / 2; paddle.wideTimer = 12; }
            case SLOW -> slowTimer = 8;
            case LIFE -> lives = Math.min(5, lives + 1);
            case MULTI -> {
                List<Ball> extra = new ArrayList<>();
                for (Ball b : balls) {
                    if (b.stuck) continue;
                    for (int s : new int[] {-1, 1}) {
                        Ball n = new Ball(b.x, b.y, b.r);
                        double a = Math.toRadians(25 * s), c = Math.cos(a), sn = Math.sin(a);
                        n.vx = b.vx * c - b.vy * sn; n.vy = b.vx * sn + b.vy * c; n.stuck = false;
                        extra.add(n);
                    }
                }
                balls.addAll(extra.subList(0, Math.min(extra.size(), Math.max(0, 6 - balls.size()))));
            }
        }
    }

    private void loseLife() {
        lives--;
        shake = 8;
        sound.play(Sound.Fx.LOSE);
        paddle.w = PADDLE_W; paddle.wideTimer = 0; slowTimer = 0; powerUps.clear();
        if (lives <= 0) { state = State.GAME_OVER; highScore = Math.max(highScore, score); return; }
        resetBall();
    }

    private void levelCleared() {
        score += 500 + lives * 100;
        highScore = Math.max(highScore, score);
        sound.play(Sound.Fx.CLEAR);
        if (level + 1 >= Levels.count()) { state = State.WON; return; }
        state = State.LEVEL_CLEAR;
        stateTimer = 1.0;
    }

    // ---------------------------------------------------------------- effects
    private void burst(double x, double y, Color c, int n, double speed) {
        for (int i = 0; i < n; i++) {
            double a = rng.nextDouble() * Math.PI * 2, s = speed * (0.3 + rng.nextDouble() * 0.7);
            particles.add(new Particle(x, y, Math.cos(a) * s, Math.sin(a) * s - 60, 0.4 + rng.nextDouble() * 0.5, 3 + rng.nextDouble() * 4, c));
        }
    }

    private void updateEffects(double dt) {
        for (Iterator<Particle> it = particles.iterator(); it.hasNext(); ) {
            Particle p = it.next();
            p.vy += 600 * dt; p.x += p.vx * dt; p.y += p.vy * dt; p.life -= dt;
            if (p.life <= 0) it.remove();
        }
        for (Iterator<Popup> it = popups.iterator(); it.hasNext(); ) {
            Popup p = it.next();
            p.y -= 40 * dt; p.life -= dt;
            if (p.life <= 0) it.remove();
        }
        for (Brick k : bricks) k.flash = Math.max(0, k.flash - dt);
    }

    int breakableLeft() { return (int) bricks.stream().filter(Brick::breakable).count(); }
}
