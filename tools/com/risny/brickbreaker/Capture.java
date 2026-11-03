package com.risny.brickbreaker;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

/** Headless capture of screenshots and GIF frames for the README (an autopilot plays the game). */
public final class Capture {
    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        out.mkdirs();
        Renderer r = new Renderer();
        World w = new World(11, Sound.silent(), 18450);
        shot(r, w, out, "menu");
        w.primaryAction(); w.update(0.2);
        shot(r, w, out, "ready");
        w.primaryAction();
        int frame = 0;
        double t = 0;
        boolean gotCombo = false, gotPower = false;
        for (int i = 0; i < 120 * 50; i++) {
            if (!w.balls.isEmpty()) {
                Entities.Ball low = w.balls.stream().max((a, b) -> Double.compare(a.y, b.y)).get();
                w.movePaddleTo(low.x + Math.sin(t * 2) * 25);
            }
            if (i == 120 * 4) w.apply(Entities.PowerType.MULTI);
            if (i == 120 * 9) w.apply(Entities.PowerType.WIDE);
            w.update(1.0 / 120); t += 1.0 / 120;
            if (w.state == World.State.READY || w.state == World.State.LEVEL_CLEAR) w.primaryAction();
            if (i % 4 == 0 && i < 120 * 14) { // ~30 fps for 14 s
                save(r, w, new File(out, String.format("f%04d.png", frame++)), 0.5);
            }
            if (!gotCombo && w.combo >= 6 && w.balls.size() > 1) { shot(r, w, out, "combo"); gotCombo = true; }
            if (!gotPower && !w.powerUps.isEmpty() && w.paddle.wideTimer > 0) { shot(r, w, out, "powerups"); gotPower = true; }
        }
        World l3 = new World(5, Sound.silent(), 18450);
        l3.newGame(); l3.loadLevel(2); l3.stateTimer = 0; l3.score = 9350; l3.lives = 2; l3.primaryAction();
        for (int i = 0; i < 120 * 3; i++) { l3.movePaddleTo(l3.balls.get(0).x); l3.update(1.0 / 120); }
        shot(r, l3, out, "fortress");
        l3.togglePause(); shot(r, l3, out, "paused");
        World over = new World(5, Sound.silent(), 18450); over.newGame(); over.score = 21480; over.highScore = 21480; over.bestCombo = 14; over.level = 3; over.state = World.State.GAME_OVER;
        shot(r, over, out, "gameover");
        System.out.println("frames " + frame);
    }

    private static void shot(Renderer r, World w, File dir, String name) throws Exception { save(r, w, new File(dir, name + ".png"), 1.0); }

    private static void save(Renderer r, World w, File f, double scale) throws Exception {
        int W = (int) (World.W * scale * 2), H = (int) (World.H * scale * 2);
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.scale(scale * 2, scale * 2);
        r.draw(g, w, false);
        g.dispose();
        ImageIO.write(img, "png", f);
    }
}
