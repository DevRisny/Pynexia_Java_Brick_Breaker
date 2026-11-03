package com.risny.brickbreaker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Saves the best score in the user's home folder (~/.brickbreaker/highscore). Never crashes the game. */
final class HighScores {
    private final Path file;

    HighScores(Path file) { this.file = file; }

    static HighScores inHome() {
        return new HighScores(Path.of(System.getProperty("user.home"), ".brickbreaker", "highscore"));
    }

    int load() {
        try {
            return Math.max(0, Integer.parseInt(Files.readString(file, StandardCharsets.UTF_8).trim()));
        } catch (IOException | NumberFormatException e) {
            return 0;
        }
    }

    void save(int score) {
        try {
            Files.createDirectories(file.getParent());
            if (score > load()) Files.writeString(file, Integer.toString(score), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // read-only home folder: keep playing without saving
        }
    }
}
