package com.bugenzhao.mnga.storage

import android.content.SharedPreferences
import com.bugenzhao.mnga.protos.datamodel.Forum
import com.bugenzhao.mnga.protos.datamodel.ForumId
import com.bugenzhao.mnga.util.PbJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The user's pinned 「常逛版面」 (custom frequent forums) shown at the
 * top of the 版面 directory tab (v1.8.0).
 *
 * Deliberately separate from [FavoriteForumsStorage]: favorites are the
 * account-level bookmark set (optionally server-synced), while the
 * pinned list is a purely local, user-curated shortcut row — pinning a
 * forum must not favorite it and vice versa. Persistence mirrors the
 * local favorites backend (a PbJson list in the shared "mnga"
 * preferences).
 *
 * First run: the list is uninitialized (the preference key is absent)
 * and the directory screen seeds it once from the current favorites, so
 * the section is useful immediately; from then on it is independent.
 */
class PinnedForumsStorage(private val prefs: SharedPreferences) {

    companion object {
        var shared: PinnedForumsStorage? = null
    }

    private val key = "pinnedForums"

    private val _forums = MutableStateFlow<List<Forum>>(
        PbJson.listFromJson(prefs.getString(key, null)) { Forum.newBuilder() },
    )
    val forums: StateFlow<List<Forum>> = _forums

    /** True once the user (or the first-run seed) has written the list. */
    val isInitialized: Boolean get() = prefs.contains(key)

    /** One-time first-run seed from the given forums (usually favorites). */
    fun seed(forums: List<Forum>) {
        if (isInitialized) return
        _forums.value = forums.distinctBy { forumKey(it.id) }
        persist()
    }

    fun isPinned(id: ForumId): Boolean = _forums.value.any { it.id == id }

    fun add(forum: Forum) {
        if (isPinned(forum.id)) return
        _forums.value = _forums.value + forum
        persist()
    }

    fun remove(id: ForumId) {
        _forums.value = _forums.value.filterNot { it.id == id }
        persist()
    }

    fun toggle(forum: Forum) {
        if (isPinned(forum.id)) remove(forum.id) else add(forum)
    }

    fun move(fromIndex: Int, toIndex: Int) {
        val list = _forums.value.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _forums.value = list
        persist()
    }

    private fun persist() {
        prefs.edit().putString(key, PbJson.listToJson(_forums.value)).apply()
    }

    private fun forumKey(id: ForumId): String =
        if (id.hasFid()) "f${id.fid}" else "st${id.stid}"
}
