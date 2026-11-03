package com.risny.brickbreaker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Dependency-free test runner (no JUnit download needed): run with
 *   java -cp out com.risny.brickbreaker.GameTests
 * Exits with status 1 if any test fails, so CI catches it.
 */
public final class GameTests {
    private static int passed;
    private static final List<String> failures = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        test("circle misses a far rectangle", () -> check(Physics.circleRect(0, 0, 5, 20, 20, 10, 10) == null));
        test("circle hitting the top of a rectangle gets an upward normal", () -> {
            Physics.Hit h = Physics.circleRect(15, 17, 5, 10, 20, 30, 10);
            check(h != null && near(h.nx(), 0) && near(h.ny(), -1) && near(h.depth(), 2));
        });
        test("corner hit gives a diagonal normal", () -> {
            Physics.Hit h = Physics.circleRect(7, 7, 5, 10, 10, 10, 10);
            check(h != null && near(h.nx(), -Math.sqrt(.5)) && near(h.ny(), -Math.sqrt(.5)));
        });
        test("centre inside a rectangle is pushed out the nearest side", () -> {
            Physics.Hit h = Physics.circleRect(11, 15, 2, 10, 10, 20, 20);
            check(h != null && h.nx() == -1 && h.ny() == 0 && near(h.depth(), 3));
        });
        test("reflect flips velocity into a surface", () -> {
            double[] v = Physics.reflect(3, 4, 0, -1);
            check(near(v[0], 3) && near(v[1], -4));
        });
        test("reflect ignores a ball already moving away", () -> {
            double[] v = Physics.reflect(3, -4, 0, -1);
            check(near(v[0], 3) && near(v[1], -4));
        });
        test("paddle centre sends the ball straight up at the same speed", () -> {
            double[] v = Physics.paddleBounce(100, 50, 100, 400, 60);
            check(near(v[0], 0) && near(v[1], -400));
        });
        test("paddle edge sends the ball out at the max angle", () -> {
            double[] v = Physics.paddleBounce(150, 50, 100, 400, 60);
            check(near(Math.hypot(v[0], v[1]), 400) && v[0] > 0 && near(Math.toDegrees(Math.atan2(v[0], -v[1])), 60));
        });
        test("nearly flat shots are steepened but keep their speed", () -> {
            double[] v = Physics.enforceMinVertical(400, 10, 0.28);
            check(near(Math.hypot(v[0], v[1]), Math.hypot(400, 10)) && Math.abs(v[1]) >= 0.28 * Math.hypot(400, 10) - 1e-6 && v[0] > 0 && v[1] > 0);
        });
        test("sub-steps stop tunnelling at high speed", () -> check(Physics.substeps(720, 0, 1.0 / 120, 4) == 2 && Physics.substeps(10, 0, 1.0 / 120, 4) == 1));
        test("combo multiplier grows every 3 bricks and caps at x5", () ->
                check(Physics.score(50, 1) == 50 && Physics.score(50, 3) == 100 && Physics.score(50, 6) == 150 && Physics.score(50, 40) == 250));

        test("every level is a full rectangle and has breakable bricks", () -> {
            for (int l = 0; l < Levels.count(); l++) {
                String[] m = Levels.MAPS[l];
                for (String row : m) check(row.length() == m[0].length());
                List<Entities.Brick> b = Levels.build(l, World.W, 96);
                check(b.stream().anyMatch(Entities.Brick::breakable));
                for (Entities.Brick k : b) check(k.x >= 0 && k.x + k.w <= World.W);
            }
        });
        test("new game starts on level 1 with 3 lives and the ball on the paddle", () -> {
            World w = new World(1, Sound.silent(), 0);
            w.primaryAction();
            check(w.state == World.State.READY && w.lives == 3 && w.level == 0 && w.balls.size() == 1 && w.balls.get(0).stuck);
        });
        test("launch sends the ball upwards", () -> {
            World w = new World(1, Sound.silent(), 0);
            w.primaryAction(); w.primaryAction();
            check(w.state == World.State.PLAYING && w.balls.get(0).vy < 0 && !w.balls.get(0).stuck);
        });
        test("missing the ball costs a life and resets", () -> {
            World w = new World(1, Sound.silent(), 0);
            w.primaryAction(); w.primaryAction();
            Entities.Ball b = w.balls.get(0);
            b.x = 30; b.y = World.H - 10; b.vx = 0; b.vy = 500; w.paddle.x = 700; w.paddle.targetX = 700;
            for (int i = 0; i < 30; i++) w.update(1.0 / 120);
            check(w.lives == 2 && w.state == World.State.READY);
        });
        test("pause freezes the ball", () -> {
            World w = new World(1, Sound.silent(), 0);
            w.primaryAction(); w.primaryAction(); w.togglePause();
            double y = w.balls.get(0).y;
            for (int i = 0; i < 60; i++) w.update(1.0 / 120);
            check(w.state == World.State.PAUSED && w.balls.get(0).y == y);
        });
        test("the ball never leaves the play area sideways or through the top", () -> {
            World w = new World(7, Sound.silent(), 0);
            w.primaryAction(); w.primaryAction();
            for (int i = 0; i < 120 * 30; i++) {
                if (!w.balls.isEmpty()) w.movePaddleTo(w.balls.get(0).x);
                w.update(1.0 / 120);
                for (Entities.Ball b : w.balls) check(b.x >= b.r - 0.01 && b.x <= World.W - b.r + 0.01 && b.y >= b.r - 0.01);
                if (w.state == World.State.READY) w.primaryAction();
            }
        });
        test("an autopilot clears level 1 and scores points", () -> {
            World w = new World(3, Sound.silent(), 0);
            w.primaryAction(); w.primaryAction();
            for (int i = 0; i < 120 * 240 && w.level == 0 && w.state != World.State.LEVEL_CLEAR; i++) {
                if (!w.balls.isEmpty()) w.movePaddleTo(w.balls.get(0).x + Math.sin(i * 0.01) * 30);
                w.update(1.0 / 120);
                if (w.state == World.State.READY) w.primaryAction();
                if (w.state == World.State.GAME_OVER) { w.primaryAction(); w.primaryAction(); }
            }
            check(w.state == World.State.LEVEL_CLEAR && w.score > 2000 && w.breakableLeft() == 0);
        });
        test("multi-ball adds two balls per moving ball", () -> {
            World w = new World(1, Sound.silent(), 0);
            w.primaryAction(); w.primaryAction();
            w.apply(Entities.PowerType.MULTI);
            check(w.balls.size() == 3);
        });
        test("wide paddle grows and returns to normal size", () -> {
            World w = new World(1, Sound.silent(), 0);
            w.primaryAction(); w.primaryAction();
            w.apply(Entities.PowerType.WIDE);
            check(w.paddle.w > World.PADDLE_W);
            w.paddle.wideTimer = 0.001; w.update(1.0 / 120);
            check(near(w.paddle.w, World.PADDLE_W));
        });
        test("steel bricks cannot be broken", () -> {
            World w = new World(1, Sound.silent(), 0);
            w.loadLevel(2);
            long steel = w.bricks.stream().filter(k -> !k.breakable()).count();
            check(steel > 0 && w.bricks.stream().filter(k -> !k.breakable()).allMatch(Entities.Brick::alive));
        });
        test("high score is saved and only goes up", () -> {
            Path dir = Files.createTempDirectory("bb");
            HighScores hs = new HighScores(dir.resolve("hs"));
            check(hs.load() == 0);
            hs.save(1200); hs.save(800);
            check(hs.load() == 1200);
            Files.writeString(dir.resolve("hs"), "not a number");
            check(hs.load() == 0);
        });

        System.out.printf("%n%d passed, %d failed%n", passed, failures.size());
        failures.forEach(f -> System.out.println("  FAIL " + f));
        if (!failures.isEmpty()) System.exit(1);
    }

    interface Body { void run() throws Exception; }

    private static void test(String name, Body body) {
        try { body.run(); passed++; System.out.println("  ok   " + name); }
        catch (Throwable t) { failures.add(name + " -> " + t); System.out.println("  FAIL " + name); }
    }

    private static void check(boolean condition) { if (!condition) throw new AssertionError("check failed"); }
    private static boolean near(double a, double b) { return Math.abs(a - b) < 1e-6; }
}
