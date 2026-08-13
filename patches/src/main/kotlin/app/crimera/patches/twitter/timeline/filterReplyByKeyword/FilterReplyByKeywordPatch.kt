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

            // JsonTimelineTweet.r() returns a rendered timeline model (r4), not
            // the core Tweet entity. Resolve its polymorphic tweet-result
            // builder to core/b, then inspect core/b.f (core/d) before r4 is
            // constructed. Registers v0-v3 are safe here because the original
            // first instruction initializes v0 from p0.
            method.addInstructionsWithLabels(
                0,
                """
                iget-object v0, p0, $JSON_TIMELINE_TWEET_DESCRIPTOR->a:$TWEET_RESULT_BUILDER_DESCRIPTOR
                invoke-static {v0}, $TWEET_RESULT_DESCRIPTOR->c($TWEET_RESULT_BUILDER_DESCRIPTOR)$CORE_TWEET_DESCRIPTOR
                move-result-object v0
                if-eqz v0, :piko_filter_reply_continue

                iget-object v0, v0, $CORE_TWEET_DESCRIPTOR->f:$CORE_TWEET_INFO_DESCRIPTOR
                if-eqz v0, :piko_filter_reply_continue
                iget-wide v1, v0, $CORE_TWEET_INFO_DESCRIPTOR->o:J

                iget-object v3, v0, $CORE_TWEET_INFO_DESCRIPTOR->t0:$NOTE_TWEET_CONTAINER_DESCRIPTOR
                if-eqz v3, :piko_filter_reply_short_text
                iget-object v3, v3, $NOTE_TWEET_CONTAINER_DESCRIPTOR->c:Lkotlin/o;
                if-eqz v3, :piko_filter_reply_short_text
                invoke-virtual {v3}, Lkotlin/o;->getValue()Ljava/lang/Object;
                move-result-object v3
                if-eqz v3, :piko_filter_reply_short_text
                check-cast v3, $NOTE_TWEET_DESCRIPTOR
                iget-object v3, v3, $NOTE_TWEET_DESCRIPTOR->b:Ljava/lang/String;
                if-nez v3, :piko_filter_reply_check

                :piko_filter_reply_short_text
                iget-object v3, v0, $CORE_TWEET_INFO_DESCRIPTOR->l:$TWEET_TEXT_DESCRIPTOR
                if-nez v3, :piko_filter_reply_get_text
                iget-object v3, v0, $CORE_TWEET_INFO_DESCRIPTOR->k:$TWEET_TEXT_DESCRIPTOR

                :piko_filter_reply_get_text
                if-eqz v3, :piko_filter_reply_continue
                invoke-virtual {v3}, $TWEET_TEXT_DESCRIPTOR->getText()Ljava/lang/CharSequence;
                move-result-object v3

                :piko_filter_reply_check
                invoke-static {v1, v2, v3}, $FILTER_REPLY_CLASS_DESCRIPTOR;->shouldFilter(JLjava/lang/CharSequence;)Z
                move-result v0
                if-eqz v0, :piko_filter_reply_continue
                const/4 v0, 0x0
                return-object v0
                """.trimIndent(),
                ExternalLabel("piko_filter_reply_continue", method.getInstruction(0)),
            )

            enableSettings("filterReplyByKeyword")
        }
    }
