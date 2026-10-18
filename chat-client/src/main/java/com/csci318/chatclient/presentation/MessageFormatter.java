package com.csci318.chatclient.presentation;

// Turns a chat message into the small HTML subset a Swing JLabel can render.
// Long messages are wrapped at widthPx; short ones keep their natural width so their bubble stays small.
// The assistant often replies in simple Markdown, so **bold** and "- " / "* " list items are converted.
// Everything else is escaped, so a message can never inject its own markup.
public final class MessageFormatter {

    private static final int SHORT_MESSAGE = 35;   // characters that fit on one line of a bubble

    private MessageFormatter() {}

    public static String toHtml(String text, int widthPx) {
        boolean wrap = text.length() > SHORT_MESSAGE || text.contains("\n");
        StringBuilder html = new StringBuilder(wrap ? "<html><div style='width:" + widthPx + "px'>" : "<html><div>");
        boolean firstLine = true;
        for (String line : escape(text.strip()).split("\n")) {
            String trimmed = line.stripLeading();
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                line = "&bull; " + trimmed.substring(2);
            }
            if (!firstLine) html.append("<br>");
            html.append(line);
            firstLine = false;
        }
        return html.append("</div></html>").toString().replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>");
    }

    static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
