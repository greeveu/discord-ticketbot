package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.interactions.Interaction;

import java.awt.*;

public abstract class AbstractCommand implements Interaction {

    Color getColor(String colorFormat) {
        Color color = new Color(63, 226, 69, 255);
        switch (colorFormat) {
            case "BLACK" -> color = Color.BLACK;
            case "BLUE" -> color = Color.BLUE;
            case "CYAN" -> color = Color.CYAN;
            case "DARK_GRAY" -> color = Color.DARK_GRAY;
            case "GRAY" -> color = Color.GRAY;
            case "GREEN" -> color = Color.GREEN;
            case "LIGHT_GRAY" -> color = Color.LIGHT_GRAY;
            case "MAGENTA" -> color = Color.MAGENTA;
            case "ORANGE" -> color = Color.ORANGE;
            case "PINK" -> color = Color.PINK;
            case "RED" -> color = Color.RED;
            case "WHITE" -> color = Color.WHITE;
            case "YELLOW" -> color = Color.YELLOW;
        }
        return color;
    }
}