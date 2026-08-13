/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.twitter.timeline.filterReplyByKeyword

import app.crimera.patches.twitter.entity.tweet.tweetEntityPatch
import app.crimera.patches.twitter.entity.tweetInfo.tweetInfoEntityPatch
import app.crimera.patches.twitter.misc.settings.settingsPatch
import app.crimera.patches.twitter.utils.Constants.COMPATIBILITY_X
import app.crimera.patches.twitter.utils.Constants.PATCHES_DESCRIPTOR
import app.crimera.patches.twitter.utils.enableSettings
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val FILTER_REPLY_CLASS_DESCRIPTOR = "$PATCHES_DESCRIPTOR/filterReply/FilterReply"
private const val JSON_TIMELINE_TWEET_DESCRIPTOR = "Lcom/twitter/model/json/timeline/urt/JsonTimelineTweet;"
private const val TWEET_RESULT_BUILDER_DESCRIPTOR = "Lcom/twitter/model/core/i0\$a;"
private const val TWEET_RESULT_DESCRIPTOR = "Lcom/twitter/model/core/i0;"
private const val CORE_TWEET_DESCRIPTOR = "Lcom/twitter/model/core/b;"

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
        dependsOn(settingsPatch, tweetEntityPatch, tweetInfoEntityPatch)

        execute {
            val method = FilterReplyByKeywordFingerprint.method
            val implementation =
                method.implementation
                    ?: throw PatchException("JsonTimelineTweet.r() has no implementation")
            val receiverMove =
                method.instructions.firstOrNull() as? TwoRegisterInstruction
                    ?: throw PatchException("JsonTimelineTweet.r() has no receiver move")
            val firstBodyInstruction =
                method.instructions.getOrNull(1) as? ReferenceInstruction
                    ?: throw PatchException("JsonTimelineTweet.r() has no expected first body instruction")
            val firstBodyRegisters = firstBodyInstruction as? TwoRegisterInstruction
            val firstBodyField = firstBodyInstruction.reference as? FieldReference
            val parameterRegister = implementation.registerCount - 1

            if (
                receiverMove.opcode != Opcode.MOVE_OBJECT_FROM16 ||
                receiverMove.registerA != 0 ||
                receiverMove.registerB != parameterRegister ||
                firstBodyInstruction.opcode != Opcode.IGET_OBJECT ||
                firstBodyRegisters == null ||
                firstBodyRegisters.registerA != 1 ||
                firstBodyRegisters.registerB != 0 ||
                firstBodyField == null ||
                firstBodyField.definingClass != JSON_TIMELINE_TWEET_DESCRIPTOR ||
                firstBodyField.name != "a" ||
                firstBodyField.type != TWEET_RESULT_BUILDER_DESCRIPTOR
            ) {
                throw PatchException(
                    "Unexpected JsonTimelineTweet.r() receiver layout; refusing unsafe reply-filter injection",
                )
            }

            // The original first instruction normalizes high p0 into v0. Run it
            // before the hook, then use v1 only after confirming the layout. The
            // original body initializes v1 before its first read on continuation.
            method.addInstructionsWithLabels(
                1,
                """
                iget-object v1, v0, $JSON_TIMELINE_TWEET_DESCRIPTOR->a:$TWEET_RESULT_BUILDER_DESCRIPTOR
                invoke-static {v1}, $TWEET_RESULT_DESCRIPTOR->c($TWEET_RESULT_BUILDER_DESCRIPTOR)$CORE_TWEET_DESCRIPTOR
                move-result-object v1
                if-eqz v1, :piko_filter_reply_continue
                invoke-static {v1}, $FILTER_REPLY_CLASS_DESCRIPTOR;->filter(Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object v1
                if-nez v1, :piko_filter_reply_continue
                return-object v1
                """.trimIndent(),
                ExternalLabel("piko_filter_reply_continue", method.getInstruction(1)),
            )

            enableSettings("filterReplyByKeyword")
        }
    }
