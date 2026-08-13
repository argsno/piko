/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.twitter.entity.tweetInfo

import app.crimera.utils.changeFirstString
import app.crimera.utils.changeStringAt
import app.crimera.utils.fieldExtractor
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch

val tweetInfoEntityPatch =
    bytecodePatch(
        description = "For tweet info entity reflection",
    ) {
        execute {
            // Confirmed from Twitter 12.7.1: core/d.o is populated from the
            // in_reply_to_status_id model value and is zero for top-level posts.
            TweetReplyToStatusIdFingerprint.changeFirstString("o")
            TweetInfoTimelineTextFingerprint.apply {
                changeStringAt(0, "t0")
                changeStringAt(1, "c")
                changeStringAt(3, "b")
                changeStringAt(4, "l")
                changeStringAt(5, "k")
            }

            TweetInfoObjectFingerprint.apply {
                val langStrIndex = stringMatches[1].index
                method.apply {
                    val fieldName = getInstruction(langStrIndex + 1).fieldExtractor().name
                    TweetLangFingerprint.changeFirstString(fieldName)
                }
            }
        }
    }
