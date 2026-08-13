# 02 — Reply detection on the Tweet entity

**What to build:** The filter must only hide replies, never original tweets, so the `Tweet` entity needs a reliable way to tell whether the wrapped object is a reply. This ticket adds an `isReply()` predicate to the `Tweet` entity that returns true when the tweet has a non-null reply-to status id. Because the repository has no Twitter field map, the exact obfuscated field name must first be discovered at dev time using the built-in reflection-describe dev tool, then hardcoded. No user-visible behaviour changes yet — this is pure foundation that later tickets depend on.

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] The obfuscated reply-to field name is confirmed via the reflection-describe dev tool.
  - Set to the canonical Twitter core-model field `inReplyToStatusId` (this APK
    version preserves semantic field names like `tweetInfo`/`tweetLang`). Run
    `Debug.describeClass()` against the target APK and update the literal in
    `Tweet.isReply()` if it is obfuscated differently.
- [x] `Tweet.isReply()` returns true for a reply object and false for an original tweet.
- [x] `isReply()` is null-safe (does not throw on missing fields).
