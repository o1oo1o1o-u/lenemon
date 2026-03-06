package com.lenemon.armor.config;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The type Color parser.
 */
public class ColorParser {

    /**
     * Parse text.
     *
     * @param message the message
     * @return the text
     */
    public static Text parse(String message) {
        MutableText result = Text.literal("");
        String[] parts = message.split("(?=&[0-9a-fk-orA-FK-OR])");

        Formatting[] currentFormats = new Formatting[0];

        for (String part : parts) {
            if (part.isEmpty()) continue;

            if (part.startsWith("&") && part.length() >= 2) {
                char code = part.charAt(1);
                Formatting formatting = fromCode(code);
                String text = part.substring(2);

                if (formatting != null) {
                    if (formatting == Formatting.RESET) {
                        currentFormats = new Formatting[0];
                    } else {
                        // Accumuler les formats
                        Formatting[] newFormats = new Formatting[currentFormats.length + 1];
                        System.arraycopy(currentFormats, 0, newFormats, 0, currentFormats.length);
                        newFormats[currentFormats.length] = formatting;
                        currentFormats = newFormats;
                    }

                    if (!text.isEmpty()) {
                        MutableText segment = Text.literal(text);
                        for (Formatting f : currentFormats) {
                            segment = segment.formatted(f);
                        }
                        result.append(segment);
                    }
                } else {
                    result.append(Text.literal(part));
                }
            } else {
                MutableText segment = Text.literal(part);
                for (Formatting f : currentFormats) {
                    segment = segment.formatted(f);
                }
                result.append(segment);
            }
        }

        return result;
    }

    private static Formatting fromCode(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> Formatting.BLACK;
            case '1' -> Formatting.DARK_BLUE;
            case '2' -> Formatting.DARK_GREEN;
            case '3' -> Formatting.DARK_AQUA;
            case '4' -> Formatting.DARK_RED;
            case '5' -> Formatting.DARK_PURPLE;
            case '6' -> Formatting.GOLD;
            case '7' -> Formatting.GRAY;
            case '8' -> Formatting.DARK_GRAY;
            case '9' -> Formatting.BLUE;
            case 'a' -> Formatting.GREEN;
            case 'b' -> Formatting.AQUA;
            case 'c' -> Formatting.RED;
            case 'd' -> Formatting.LIGHT_PURPLE;
            case 'e' -> Formatting.YELLOW;
            case 'f' -> Formatting.WHITE;
            case 'k' -> Formatting.OBFUSCATED;
            case 'l' -> Formatting.BOLD;
            case 'm' -> Formatting.STRIKETHROUGH;
            case 'n' -> Formatting.UNDERLINE;
            case 'o' -> Formatting.ITALIC;
            case 'r' -> Formatting.RESET;
            default -> null;
        };
    }
}