/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.twitter.patches.filterReply;

import app.morphe.extension.crimera.PikoUtils;
import app.morphe.extension.twitter.Pref;

import java.util.Locale;

/**
 * Runtime filter that hides Twitter replies whose text contains any of the
 * user's configured keywords.
 *
 * The bytecode hook extracts the reply-to status id and visible text from the
 * parsed timeline model before its rendered timeline item is constructed. This
 * class only owns the preference gate and keyword decision, keeping obfuscated
 * Twitter model details out of the runtime extension.
 */
@SuppressWarnings("unused")
public class FilterReply {

    /**
     * Decide whether a parsed tweet should be dropped from the timeline.
     *
     * @param inReplyToStatusId zero for an original tweet, otherwise the id of
     *                          the tweet being replied to
     * @param text note-tweet text when present, otherwise the regular tweet text
     * @return {@code true} when the timeline item should be hidden
     */
    public static boolean shouldFilter(long inReplyToStatusId, CharSequence text) {
        try {
            return Pref.filterReplyByKeyword()
                    && inReplyToStatusId != 0L
                    && matchesAnyKeyword(text == null ? null : text.toString(), Pref.filterReplyKeywords());
        } catch (Exception e) {
            PikoUtils.logger(e);
            return false;
        }
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
