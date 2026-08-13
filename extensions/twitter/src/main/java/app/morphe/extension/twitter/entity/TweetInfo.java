/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.twitter.entity;

import app.morphe.extension.twitter.entity.Debug;
import app.morphe.extension.crimera.PikoUtils;

import java.lang.reflect.Method;

// Lcom/twitter/model/core/entity/d;
public class TweetInfo extends Debug {

    private final Object obj;

    public TweetInfo(Object obj) {
        super(obj);
        this.obj = obj;
    }

    public String getLang() throws Exception {
        // y:String
        return (String) super.getField("tweetLang");
    }

    /**
     * ID of the status this tweet replies to, or zero for a top-level tweet.
     * The entity patch replaces the placeholder with the target APK field.
     */
    public long getInReplyToStatusId() throws Exception {
        return (Long) super.getField("inReplyToStatusId");
    }

    public String getTimelineText() throws Exception {
        Object noteTweetContainer = super.getField("noteTweetContainer");
        if (noteTweetContainer != null) {
            Object lazyNoteTweet = super.getField(noteTweetContainer, "lazyNoteTweet");
            if (lazyNoteTweet != null) {
                Method getValue = lazyNoteTweet.getClass().getMethod("getValue");
                getValue.setAccessible(true);
                Object noteTweet = getValue.invoke(lazyNoteTweet);
                if (noteTweet != null) {
                    String noteText = (String) super.getField(noteTweet, "noteTweetText");
                    if (noteText != null) {
                        return noteText;
                    }
                }
            }
        }

        Object textEntity = super.getField("fullTextEntity");
        if (textEntity == null) {
            textEntity = super.getField("fallbackTextEntity");
        }
        if (textEntity == null) {
            return null;
        }

        Method getText = textEntity.getClass().getMethod("getText");
        getText.setAccessible(true);
        CharSequence text = (CharSequence) getText.invoke(textEntity);
        return text == null ? null : text.toString();
    }

    @Override
    public String toString() {
        try {
            return "TweetInfo [getLang()=" + this.getLang() + "]";

        } catch (Exception e) {
            PikoUtils.logger(e);
            return e.getMessage();
        }

    }

}
