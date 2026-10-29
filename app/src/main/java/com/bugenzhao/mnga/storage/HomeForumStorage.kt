package com.bugenzhao.mnga.storage

import android.content.SharedPreferences
import com.bugenzhao.mnga.protos.datamodel.Forum
import com.bugenzhao.mnga.util.PbJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The user's custom home-tab forum (v1.8.1): when set, the 主页 tab
 * opens this forum's topic list directly as its tab root instead of
 * the default forum-list home.
 *
 * Purely local (a PbJson-serialized [Forum] in the shared "mnga"
 * preferences, same persistence style as [PinnedForumsStorage], its
 * own key — independent of favorites and of the pinned 常逛 list).
 * A null value means "not customized": the home tab keeps the
 * default forum list. A stored entry whose forum id is blank is
 * treated as unset, so a corrupt/stale value falls back to the
 * default home instead of a dead tab.
 */
class HomeForumStorage(private val prefs: SharedPreferences) {

    companion object {
        var shared: HomeForumStorage? = null
    }

    private val key = "homeForum"

    private val _forum = MutableStateFlow<Forum?>(load())
    val forum: StateFlow<Forum?> = _forum

    /** Set (or, with null, clear) the custom home forum. */
    fun set(forum: Forum?) {
        _forum.value = forum?.takeIf { isValid(it) }
        persist()
    }

    private fun load(): Forum? {
        val json = prefs.getString(key, null) ?: return null
        val parsed = runCatching {
            PbJson.mergeFromJson(json, Forum.newBuilder()).build()
        }.getOrNull() ?: return null
        return parsed.takeIf { isValid(it) }
    }

    private fun persist() {
        val current = _forum.value
        prefs.edit().apply {
            if (current == null) remove(key) else putString(key, PbJson.toJson(current))
        }.apply()
    }

    private fun isValid(forum: Forum): Boolean =
        forum.id.hasFid() && forum.id.fid.isNotEmpty() || forum.id.stid.isNotEmpty()
}
