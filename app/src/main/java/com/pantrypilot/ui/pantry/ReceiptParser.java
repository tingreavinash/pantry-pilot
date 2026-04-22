package com.pantrypilot.ui.pantry;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses raw OCR text from a grocery receipt into candidate pantry items.
 * <p>
 * Strategy:
 * 1. Split OCR output into lines.
 * 2. Regex-match lines that look like "ITEM_NAME  QTY  PRICE".
 * 3. Fuzzy-match each name against existing pantry items (Levenshtein ≤ 3).
 * 4. Flag matched items as "Update Existing"; unmatched as "Add New".
 */
public class ReceiptParser {

    // Matches: "Milk 2 60.50"  or  "Toor Dal 1 120"
    private static final Pattern ITEM_PATTERN = Pattern.compile(
            "^([A-Za-z][A-Za-z\\s]{2,30})\\s+(\\d+\\.?\\d*)\\s+(\\d+\\.?\\d*)$");

    /**
     * @param ocrText             Raw text from ML Kit text recognition.
     * @param existingPantryNames Names of all current pantry items (for fuzzy matching).
     * @return Parsed items. Empty list signals poor OCR quality — trigger fallback snackbar.
     */
    public static List<ParsedItem> parse(String ocrText, List<String> existingPantryNames) {
        List<ParsedItem> results = new ArrayList<>();
        if (ocrText == null || ocrText.isEmpty()) return results;

        String[] lines = ocrText.split("\\n");
        for (String raw : lines) {
            String line = raw.trim();
            Matcher m = ITEM_PATTERN.matcher(line);
            if (!m.matches()) continue;

            String name = m.group(1).trim();
            double quantity = Double.parseDouble(m.group(2));
            double price = Double.parseDouble(m.group(3));

            ParsedItem item = new ParsedItem(name, quantity, price);

            // Attempt fuzzy match against existing pantry
            String match = bestFuzzyMatch(name, existingPantryNames);
            if (match != null) {
                item.isUpdate = true;
                item.matchedPantryItem = match;
            }
            results.add(item);
        }
        return results;
    }

    /**
     * Returns the best pantry name match within edit distance threshold, or null.
     * Threshold = min(3, targetLength / 3) to avoid false positives on short strings.
     */
    private static String bestFuzzyMatch(String target, List<String> candidates) {
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        String tLower = target.toLowerCase().trim();
        int threshold = Math.min(3, tLower.length() / 3);

        for (String c : candidates) {
            int dist = levenshtein(tLower, c.toLowerCase().trim());
            if (dist < bestDist && dist <= threshold) {
                bestDist = dist;
                best = c;
            }
        }
        return best;
    }

    // ── Levenshtein fuzzy match ───────────────────────────────────────────────

    private static int levenshtein(String a, String b) {
        int la = a.length(), lb = b.length();
        int[][] dp = new int[la + 1][lb + 1];
        for (int i = 0; i <= la; i++) dp[i][0] = i;
        for (int j = 0; j <= lb; j++) dp[0][j] = j;
        for (int i = 1; i <= la; i++) {
            for (int j = 1; j <= lb; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }
        return dp[la][lb];
    }

    public static class ParsedItem {
        public String name;
        public double quantity;
        public double price;
        public boolean selected = true;
        public boolean isUpdate = false;   // true = update existing pantry item
        public String matchedPantryItem;   // name of matched pantry item (if any)

        ParsedItem(String name, double quantity, double price) {
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }
    }
}
