package com.pantrypilot.ui.pantry;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiptParser {

    private static final Pattern LINE_PATTERN = Pattern.compile(
            "^([A-Za-z][A-Za-z\\s]{2,30})\\s+(\\d+\\.?\\d*)\\s+(\\d+\\.?\\d*)$");

    public static List<ParsedItem> parse(String ocrText, List<String> pantryNames) {
        List<ParsedItem> results = new ArrayList<>();
        if (ocrText == null || ocrText.isEmpty()) return results;

        for (String raw : ocrText.split("\\n")) {
            Matcher m = LINE_PATTERN.matcher(raw.trim());
            if (!m.matches()) continue;
            String name = m.group(1).trim();
            double quantity = Double.parseDouble(m.group(2));
            double price = Double.parseDouble(m.group(3));
            ParsedItem item = new ParsedItem(name, quantity, price);
            String match = bestMatch(name, pantryNames);
            if (match != null) {
                item.isUpdate = true;
                item.matchedName = match;
            }
            results.add(item);
        }
        return results;
    }

    private static String bestMatch(String target, List<String> candidates) {
        String tLow = target.toLowerCase().trim();
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        int threshold = Math.min(3, tLow.length() / 3);
        for (String c : candidates) {
            int d = levenshtein(tLow, c.toLowerCase().trim());
            if (d < bestDist && d <= threshold) {
                bestDist = d;
                best = c;
            }
        }
        return best;
    }

    private static int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++)
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        return dp[a.length()][b.length()];
    }

    public static class ParsedItem {
        public String name;
        public double quantity;
        public double price;
        public boolean selected = true;
        public boolean isUpdate = false;
        public String matchedName;

        ParsedItem(String name, double quantity, double price) {
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }
    }
}
