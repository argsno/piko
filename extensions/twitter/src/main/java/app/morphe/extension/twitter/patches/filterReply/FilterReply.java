/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.twitter.patches.filterReply;

import app.morphe.extension.crimera.PikoUtils;
import app.morphe.extension.twitter.Pref;
import app.morphe.extension.twitter.entity.Tweet;

/**
 * Runtime filter that hides Twitter replies whose text contains any of the
 * user's configured keywords.
 *
 * Mirrors the Instagram {@code FilterStory} shape: a single static
 * {@link #filter(Object)} seam that returns {@code null} to drop an item, or
 * the (unchanged) item to keep it. The decision is kept free of Android /
 * reflection concerns so it stays easy to reason about and test.
 */
@SuppressWarnings("unused")
public class FilterReply {

    /**
     * Decide whether a parsed tweet should be dropped from the timeline.
     *
     * @param itemObject the core tweet model (as produced by the
     *                   {@code JsonTimelineTweet} parse hook)
     * @return {@code null} to hide the item, or {@code itemObject} unchanged to
     *         keep it visible
     */
    public static Object filter(Object itemObject) {
        try {
            // Read the master switch on every call so edits apply without an
            // app restart. When off, nothing is ever hidden.
            if (!Pref.filterReplyByKeyword()) {
                return itemObject;
            }

            Tweet tweet = new Tweet(itemObject);

            // Only replies are filtered; an original tweet is never hidden.
            if (!tweet.isReply()) {
                return itemObject;
            }

            String text = tweet.getText();
            if (matchesAnyKeyword(text, Pref.filterReplyKeywords())) {
                return null;
            }
        } catch (Exception e) {
            // Tolerate reflection / field mismatches: keep the reply rather than
            // crash the parse path.
            PikoUtils.logger(e);
        }
        return itemObject;
    }

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
        String lowerText = text.toLowerCase();
        for (String raw : keywords.split("\n", -1)) {
            String keyword = raw.trim();
            if (!keyword.isEmpty() && lowerText.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
