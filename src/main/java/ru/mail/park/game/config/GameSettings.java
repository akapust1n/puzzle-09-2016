package ru.mail.park.game.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "game")
@Component
public class GameSettings {
    private static int squareSize;
    private static int squareMinValue;
    private static int squareMaxValue;
    private static int targetScramble;
    private static int rankBounty;

    public static int getSquareSize() {
        return squareSize;
    }

    public void setSquareSize(int squareSize) {
        GameSettings.squareSize = squareSize;
    }

    public static int getSquareMinValue() {
        return squareMinValue;
    }

    public void setSquareMinValue(int squareMinValue) {
        GameSettings.squareMinValue = squareMinValue;
    }

    public static int getSquareMaxValue() {
        return squareMaxValue;
    }

    public void setSquareMaxValue(int squareMaxValue) {
        GameSettings.squareMaxValue = squareMaxValue;
    }

    public static int getTargetScramble() {
        return targetScramble;
    }

    public void setTargetScramble(int targetScramble) {
        GameSettings.targetScramble = targetScramble;
    }

    public static int getRankBounty() {
        return rankBounty;
    }

    public void setRankBounty(int rankBounty) {
        GameSettings.rankBounty = rankBounty;
    }
}
