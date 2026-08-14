/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.twitter.patches.filterReply;

import app.morphe.extension.crimera.PikoUtils;
import app.morphe.extension.twitter.Pref;
import app.morphe.extension.twitter.entity.Tweet;

import java.util.Locale;

/**
 * Runtime filter that hides Twitter replies whose text contains any of the
 * user's configured keywords.
 *
 * The bytecode hook supplies the parsed core tweet model before its rendered
 * timeline item is constructed. This class is the single drop/keep seam and
 * keeps Twitter model access behind the Tweet entity.
 */
@SuppressWarnings("unused")
public class FilterReply {

    /**
     * Decide whether a parsed tweet should be dropped from the timeline.
     *
     * @param itemObject parsed core tweet model
     * @return {@code null} to hide the item, otherwise {@code itemObject}
     */
    public static Object filter(Object itemObject) {
        boolean enabled = Pref.filterReplyByKeyword();
        String keywords = Pref.filterReplyKeywords();

        boolean isReply = false;
        long replyToStatusId = 0L;
        int textLength = 0;
        boolean matched = false;
        String decision = "KEEP";

        try {
            if (enabled) {
                Tweet tweet = new Tweet(itemObject);
                isReply = tweet.isReply();
                replyToStatusId = tweet.getInReplyToStatusId();
                String text = tweet.getTimelineText();
                textLength = text != null ? text.length() : 0;
                matched = isReply && matchesAnyKeyword(text, keywords);
                if (matched) {
                    decision = "DROP";
                }
            }
        } catch (Exception e) {
            PikoUtils.logger(e);
            decision = "KEEP";
        }

        // ===== TEMP DEBUG [DEBUG-FRBK] — remove when done =====
        logDebug(enabled, countKeywords(keywords), replyToStatusId, isReply, textLength, matched, decision);
        // ======================================================

        return decision.equals("DROP") ? null : itemObject;
    }

    // ===== TEMP DEBUG [DEBUG-FRBK] — remove when done =====

    private static int countKeywords(String keywords) {
        int count = 0;
        if (keywords != null && !keywords.isEmpty()) {
            for (String raw : keywords.split("\n", -1)) {
                if (!raw.trim().isEmpty()) count++;
            }
        }
        return count;
    }

    private static void logDebug(boolean enabled, int keywordCount, long replyToStatusId,
                                 boolean isReply, int textLength, boolean matched, String decision) {
        PikoUtils.logger("[DEBUG-FRBK] entered=true enabled=" + enabled
                + " keywordCount=" + keywordCount
                + " replyToStatusId=" + replyToStatusId
                + " isReply=" + isReply
                + " textLength=" + textLength
                + " matched=" + matched
                + " decision=" + decision);
    }

    // ======================================================

    /**
     * Pure, dependency-free keyword match: case-insensitive substring
     * ({@code contains}), OR across keywords, null/empty-safe. Keywords are
     * taken one per line.
     *
     * @return {@code true} if {@code text} contains any non-empty keyword
     */
    static boolean matchesAnyKeyword(String text, String keywords) {
        if (text == null || text.isEmpty() || keywords == null || keywords.isEmpty()) {
            return false;
        }
        String lowerText = text.toLowerCase(Locale.ROOT);
        for (String raw : keywords.split("\n", -1)) {
            String keyword = raw.trim();
            if (!keyword.isEmpty() && lowerText.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
