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
        try {
            if (!Pref.filterReplyByKeyword()) {
                return itemObject;
            }

            Tweet tweet = new Tweet(itemObject);
            if (tweet.isReply() && matchesAnyKeyword(tweet.getTimelineText(), Pref.filterReplyKeywords())) {
                return null;
            }
        } catch (Exception e) {
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
