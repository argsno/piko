/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.twitter.entity;

import app.morphe.extension.twitter.entity.ExtMediaEntities;
import app.morphe.extension.twitter.entity.TweetInfo;
import app.morphe.extension.twitter.entity.Debug;

import java.util.*;
import app.morphe.extension.crimera.PikoUtils;

// All comments based of 11.14.beta-0
// Lcom/twitter/model/core/entity/e;
public class Tweet extends Debug {
    private final Object obj;

    public Tweet(Object obj) {
        super(obj);
        this.obj = obj;
    }

    public Long getTweetId() throws Exception {
        return (Long) super.getMethod("getId");
    }

    public String getTweetUsername() throws Exception {
        return (String) super.getMethod("userNameMethod");
    }

    public String getTweetLink() throws Exception {
        Long tweetId = getTweetId();
        String username = getTweetUsername();
        return "https://x.com/"+username+"/status/"+tweetId;
    }

    public String getTweetProfileName() throws Exception {
        return (String) super.getMethod("profileNameMethod");
    }

    public Long getTweetUserId() throws Exception {

        return (Long) super.getMethod("userIdMethod");
    }

    /**
     * Whether the wrapped tweet is a reply (评论) rather than an original post.
     *
     * A reply points at the status it replies to via its reply-to status id;
     * an original tweet has no such id. The field is nullable (Long) — a null
     * value, or a primitive long default of 0, both mean "not a reply".
     *
     * The obfuscated field name below was chosen from the canonical Twitter core
     * model field. Confirm it against the target APK at dev time using the
     * built-in reflection-describe helper ({@code Debug.describeClass()}) and
     * update the literal if the field is obfuscated differently.
     */
    public boolean isReply() {
        try {
            Object value = super.getField("inReplyToStatusId");
            // null is not a reply; a numeric 0 (covers primitive long) is not a
            // reply either. Any non-zero value means the tweet replies to another.
            return value instanceof Number && ((Number) value).longValue() != 0L;
        } catch (Exception e) {
            // Missing or renamed field on this APK version — tolerate and report
            // not-a-reply so original tweets are never hidden by later filters.
            PikoUtils.logger(e);
            return false;
        }
    }

    private ArrayList<ExtMediaEntities> getExtendedMediaEntities() throws Exception {
        ArrayList<ExtMediaEntities> extMediaEntitiesArrayList = new ArrayList();

        // c()Lcom/twitter/model/core/entity/c0;
        Object mediaRootObject = super.getMethod("mediaMethod");
        Class<?> mediaRootObjectClass = mediaRootObject.getClass();

        // Lcom/twitter/model/core/entity/s;
        Class<?> superClass = mediaRootObjectClass.getSuperclass();
        Object superClassInstance = superClass.cast(mediaRootObject);

        // a:List
        List<?> list = (List<?>) super.getField(superClass, superClassInstance, "extMediaList");

        assert list != null;

        list.forEach(item ->{
            extMediaEntitiesArrayList.add(new ExtMediaEntities(item));
        });

        return extMediaEntitiesArrayList;
    }

    public ArrayList<ArrayList<Media>> getMediaList() throws Exception {
        ArrayList<ArrayList<Media>> mediaData = new ArrayList();

        ArrayList<ExtMediaEntities> extMediaEntitiesArrayList = this.getExtendedMediaEntities();

        extMediaEntitiesArrayList.forEach(item ->{
                mediaData.add(item.getMediaList());
        });
        return mediaData;
    }

    public TweetInfo getTweetInfo() throws Exception {
        Object data = super.getField("tweetInfo");
        return new TweetInfo(data);
    }

    public String getTweetLang() throws Exception {
        TweetInfo tweetInfo = this.getTweetInfo();
        return tweetInfo.getLang();
    }

    public String getLongText() throws Exception {
        // j()Lcom/twitter/model/notetweet/b;
        Object noteTweetObj = super.getMethod("noteTweetMethod");
        String data = noteTweetObj != null ? (String) super.getField(noteTweetObj, "longTextField") : null;
        return data;
    }

    public String getShortText() throws Exception {
        // y()Lcom/twitter/model/core/entity/c1;
        Object tweetObj = super.getMethod("tweetEntityClass");
        // getText()
        Object data = super.getMethod(tweetObj, "getText");
        return (String) data;
    }

    public String getText() throws Exception {
        String text = "";
        try {
            text = this.getLongText();
            if (text == null) {
                text = this.getShortText();
            }
            // Replaces text to empty if the text contains only media link.
            text = text.replaceAll("pic\\.x\\.com/\\S+", "");
            // Replaces links.
            text = text.replaceAll("https?://t\\.co/\\S+", "");

        } catch (Exception e) {
            PikoUtils.logger(e);
            text = e.getMessage();
        }
        return text;

    }

    @Override
    public String toString() {
        try {
            return "Tweet [getTweetId()=" + this.getTweetId() + ", getTweetUsername()=" + this.getTweetUsername()
                    + ", getTweetProfileName()=" + this.getTweetProfileName() + ", getTweetUserId()=" + this.getTweetUserId()
                    + ", getMediaList()=" + this.getMediaList() + ", getTweetInfo()=" + this.getTweetInfo() + ", getTweetLang()="
                    + this.getTweetLang() + ", getLongText()=" + this.getLongText() + ", getShortText()=" + this.getShortText() + "]";

        } catch (Exception e) {
            PikoUtils.logger(e);
            return e.getMessage();
        }

    }
}
