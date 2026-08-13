/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.twitter.timeline.filterReplyByKeyword

import app.crimera.patches.twitter.misc.settings.settingsPatch
import app.crimera.patches.twitter.utils.Constants.COMPATIBILITY_X
import app.crimera.patches.twitter.utils.Constants.INTEGRATIONS_PACKAGE
import app.crimera.patches.twitter.utils.enableSettings
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val FILTER_REPLY_CLASS_DESCRIPTOR = "$INTEGRATIONS_PACKAGE/patches/filterReply/FilterReply"

/**
 * Same fingerprint family as the hide-hidden-replies patch: the
 * {@code JsonTimelineTweet} parse hook, which yields the core tweet model so
 * the filter can read reply-ness and text via the {@code Tweet} entity.
 */
private object FilterReplyByKeywordFingerprint : Fingerprint(
    definingClass = "Lcom/twitter/model/json/timeline/urt/JsonTimelineTweet;",
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
            val instructions = method.instructions

            // Wrap the object the parse hook is about to return with the filter.
            // The filter returns null to drop a matching reply, or the same
            // object to keep it.
            val returnObject = instructions.last { it.opcode == Opcode.RETURN_OBJECT }
            val returnIndex = returnObject.location.index
            val returnRegister = method.getInstruction<OneRegisterInstruction>(returnIndex).registerA

            method.addInstructions(
                returnIndex,
                """
                invoke-static {v$returnRegister}, $FILTER_REPLY_CLASS_DESCRIPTOR;->filter(Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object v$returnRegister
                """.trimIndent(),
            )

            enableSettings("filterReplyByKeyword")
        }
    }
