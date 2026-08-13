/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.twitter.timeline.filterReplyByKeyword

import app.crimera.patches.twitter.misc.settings.settingsPatch
import app.crimera.patches.twitter.utils.Constants.COMPATIBILITY_X
import app.crimera.patches.twitter.utils.Constants.PATCHES_DESCRIPTOR
import app.crimera.patches.twitter.utils.enableSettings
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel

private const val FILTER_REPLY_CLASS_DESCRIPTOR = "$PATCHES_DESCRIPTOR/filterReply/FilterReply"
private const val JSON_TIMELINE_TWEET_DESCRIPTOR = "Lcom/twitter/model/json/timeline/urt/JsonTimelineTweet;"
private const val TWEET_RESULT_BUILDER_DESCRIPTOR = "Lcom/twitter/model/core/i0\$a;"
private const val TWEET_RESULT_DESCRIPTOR = "Lcom/twitter/model/core/i0;"
private const val CORE_TWEET_DESCRIPTOR = "Lcom/twitter/model/core/b;"
private const val CORE_TWEET_INFO_DESCRIPTOR = "Lcom/twitter/model/core/d;"
private const val TWEET_TEXT_DESCRIPTOR = "Lcom/twitter/model/core/entity/h1;"
private const val NOTE_TWEET_CONTAINER_DESCRIPTOR = "Lcom/twitter/model/notetweet/c;"
private const val NOTE_TWEET_DESCRIPTOR = "Lcom/twitter/model/notetweet/a;"

/**
 * Converts a parsed timeline tweet into its rendered timeline item. The method
 * has multiple return-object instructions, including a final null fallback, so
 * filtering must happen at the method entry rather than around the last return.
 */
private object FilterReplyByKeywordFingerprint : Fingerprint(
    definingClass = JSON_TIMELINE_TWEET_DESCRIPTOR,
    name = "r",
    returnType = "Ljava/lang/Object;",
)

@Suppress("unused")
val filterReplyByKeywordPatch =
    bytecodePatch(
        name = "Filter replies by keyword",
        description = "Hide replies whose text contains any of the user's configured keywords.",
    ) {
        compatibleWith(COMPATIBILITY_X)
        dependsOn(settingsPatch)

        execute {
            val method = FilterReplyByKeywordFingerprint.method

            // JsonTimelineTweet.r() has 39 registers in the supported build, so
            // p0 is v38 and cannot be encoded directly by iget-object (format
            // 22c only has four bits per register). Let the original
            // move-object/from16 v0, p0 run first, keep v0 as the receiver for
            // the original body, and use v1-v4 as scratch registers. The
            // original body initializes those registers before reading them.
            method.addInstructionsWithLabels(
                1,
                """
                iget-object v1, v0, $JSON_TIMELINE_TWEET_DESCRIPTOR->a:$TWEET_RESULT_BUILDER_DESCRIPTOR
                invoke-static {v1}, $TWEET_RESULT_DESCRIPTOR->c($TWEET_RESULT_BUILDER_DESCRIPTOR)$CORE_TWEET_DESCRIPTOR
                move-result-object v1
                if-eqz v1, :piko_filter_reply_continue

                iget-object v1, v1, $CORE_TWEET_DESCRIPTOR->f:$CORE_TWEET_INFO_DESCRIPTOR
                if-eqz v1, :piko_filter_reply_continue
                iget-wide v2, v1, $CORE_TWEET_INFO_DESCRIPTOR->o:J

                iget-object v4, v1, $CORE_TWEET_INFO_DESCRIPTOR->t0:$NOTE_TWEET_CONTAINER_DESCRIPTOR
                if-eqz v4, :piko_filter_reply_short_text
                iget-object v4, v4, $NOTE_TWEET_CONTAINER_DESCRIPTOR->c:Lkotlin/o;
                if-eqz v4, :piko_filter_reply_short_text
                invoke-virtual {v4}, Lkotlin/o;->getValue()Ljava/lang/Object;
                move-result-object v4
                if-eqz v4, :piko_filter_reply_short_text
                check-cast v4, $NOTE_TWEET_DESCRIPTOR
                iget-object v4, v4, $NOTE_TWEET_DESCRIPTOR->b:Ljava/lang/String;
                if-nez v4, :piko_filter_reply_check

                :piko_filter_reply_short_text
                iget-object v4, v1, $CORE_TWEET_INFO_DESCRIPTOR->l:$TWEET_TEXT_DESCRIPTOR
                if-nez v4, :piko_filter_reply_get_text
                iget-object v4, v1, $CORE_TWEET_INFO_DESCRIPTOR->k:$TWEET_TEXT_DESCRIPTOR

                :piko_filter_reply_get_text
                if-eqz v4, :piko_filter_reply_continue
                invoke-virtual {v4}, $TWEET_TEXT_DESCRIPTOR->getText()Ljava/lang/CharSequence;
                move-result-object v4

                :piko_filter_reply_check
                invoke-static {v2, v3, v4}, $FILTER_REPLY_CLASS_DESCRIPTOR;->shouldFilter(JLjava/lang/CharSequence;)Z
                move-result v1
                if-eqz v1, :piko_filter_reply_continue
                const/4 v1, 0x0
                return-object v1
                """.trimIndent(),
                ExternalLabel("piko_filter_reply_continue", method.getInstruction(1)),
            )

            enableSettings("filterReplyByKeyword")
        }
    }
