package com.risny.brickbreaker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/**
 * Brick Breaker: entry point and Swing panel.
 * A fixed 120 Hz physics step keeps the game the same speed on every computer;
 * the panel repaints on the Swing timer and scales to any window size.
 */
@SuppressWarnings("serial") // the panel is never serialised
public final class BrickBreaker extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final double STEP = 1.0 / 120;

    private final World world;
    private final Renderer renderer = new Renderer();
    private final HighScores scores = HighScores.inHome();
    private final Sound sound;
    private boolean left, right;
    private long last = System.nanoTime();
    private double acc;

    BrickBreaker() {
        sound = Sound.create();
        world = new World(System.nanoTime(), sound, scores.load());
        setPreferredSize(new Dimension((int) World.W, (int) World.H));
        setBackground(new Color(0xEEF4FF));
        setFocusable(true);
        setCursor(getToolkit().createCustomCursor(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), new Point(), "none"));

        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT, KeyEvent.VK_A -> left = true;
                    case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> right = true;
                    case KeyEvent.VK_SPACE, KeyEvent.VK_ENTER -> world.primaryAction();
                    case KeyEvent.VK_P, KeyEvent.VK_ESCAPE -> world.togglePause();
                    case KeyEvent.VK_M -> sound.toggleMute();
                    default -> { }
                }
            }
            @Override public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT, KeyEvent.VK_A -> left = false;
                    case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> right = false;
                    default -> { }
                }
            }
        });
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { world.movePaddleTo(toWorldX(e.getX())); }
            @Override public void mouseDragged(MouseEvent e) { world.movePaddleTo(toWorldX(e.getX())); }
            @Override public void mousePressed(MouseEvent e) { requestFocusInWindow(); world.primaryAction(); }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        new Timer(1000 / 120, e -> tick()).start();
    }

    private double scale() { return Math.min(getWidth() / World.W, getHeight() / World.H); }
    private double offsetX() { return (getWidth() - World.W * scale()) / 2; }
    private double toWorldX(int px) { return (px - offsetX()) / scale(); }

    private void tick() {
        long now = System.nanoTime();
        acc += Math.min(0.1, (now - last) / 1e9);
        last = now;
        if (left || right) world.movePaddleTo(world.paddle.x + world.paddle.w / 2 + (right ? 1 : -1) * 22);
        World.State before = world.state;
        while (acc >= STEP) { world.update(STEP); acc -= STEP; }
        if (before != world.state && (world.state == World.State.GAME_OVER || world.state == World.State.WON)) scores.save(world.score);
        repaint();
    }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        double s = scale();
        g.translate(offsetX(), (getHeight() - World.H * s) / 2);
        g.scale(s, s);
        g.clipRect(0, 0, (int) World.W, (int) World.H);
        renderer.draw(g, world, sound.muted());
        g.dispose();
    }

    public static void main(String[] args) {
        System.setProperty("sun.java2d.opengl", "true");
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Brick Breaker");
            BrickBreaker game = new BrickBreaker();
            f.setContentPane(game);
            f.pack();
            f.setMinimumSize(new Dimension(640, 480));
            f.setLocationRelativeTo(null);
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.setVisible(true);
            game.requestFocusInWindow();
        });
    }
}
