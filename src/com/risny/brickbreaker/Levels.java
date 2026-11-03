package com.risny.brickbreaker;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Level layouts drawn as text: '.' empty, '1' normal, '2' strong, '3' tough, '#' steel (unbreakable).
 * Each row gets its own colour, so the layouts read like pictures.
 */
final class Levels {
    private Levels() {}

    static final String[][] MAPS = {
        { // 1 Warm-up
            "..........",
            "1111111111",
            "1111111111",
            "1111111111",
            "1111111111",
        },
        { // 2 Pyramid
            "....22....",
            "...2112...",
            "..211112..",
            ".21111112.",
            "2111111112",
        },
        { // 3 Fortress
            "#22222222#",
            "#21111112#",
            "#21.33.12#",
            "#21111112#",
            "##..11..##",
        },
        { // 4 Diamond
            "....33....",
            "...3223...",
            "..321123..",
            "...3223...",
            "#...33...#",
        },
        { // 5 Final wall
            "3333333333",
            "2#2#22#2#2",
            "1111111111",
            "2222222222",
            "#1#1##1#1#",
        },
    };
    static final String[] NAMES = {"Warm-up", "Pyramid", "Fortress", "Diamond", "Final wall"};

    static final Color[] ROW_COLORS = {
        new Color(0xFF5C8A), new Color(0xFF8A4C), new Color(0xF5B700), new Color(0x10B981), new Color(0x2563FF), new Color(0x7C5CFF),
    };
    static final Color STEEL = new Color(0x9AA6BF);

    static int count() { return MAPS.length; }

    static List<Entities.Brick> build(int level, double width, double top) {
        String[] map = MAPS[level];
        int cols = map[0].length();
        double gap = 6, side = 24;
        double bw = (width - side * 2 - gap * (cols - 1)) / cols, bh = 26;
        List<Entities.Brick> out = new ArrayList<>();
        for (int r = 0; r < map.length; r++) {
            for (int c = 0; c < cols; c++) {
                char ch = map[r].charAt(c);
                if (ch == '.') continue;
                Entities.BrickType t = switch (ch) {
                    case '2' -> Entities.BrickType.STRONG;
                    case '3' -> Entities.BrickType.TOUGH;
                    case '#' -> Entities.BrickType.STEEL;
                    default -> Entities.BrickType.NORMAL;
                };
                Color col = t == Entities.BrickType.STEEL ? STEEL : ROW_COLORS[(r + level) % ROW_COLORS.length];
                out.add(new Entities.Brick(side + c * (bw + gap), top + r * (bh + gap), bw, bh, t, col));
            }
        }
        return out;
    }
}
