package com.bugenzhao.mnga.storage

import android.content.SharedPreferences

/**
 * Local reading progress for favorited topics: topicId -> floor.
 *
 * Remembers where the user left off reading a favorited thread, so reopening
 * it jumps straight back to that floor. Stored locally in SharedPreferences
 * (no server round-trip, works offline).
 *
 * v1: initial implementation.
 */
class ReadingProgressStorage(private val prefs: SharedPreferences) {
    companion object {
        lateinit var shared: ReadingProgressStorage

        private const val PREFIX = "reading_progress_"
        private const val KEY_INDEX = "reading_progress_index"
        private const val MAX_ENTRIES = 300
    }

    /** Returns the saved floor for [topicId], or null if none. */
    fun getFloor(topicId: String): Int? {
        if (topicId.isEmpty()) return null
        return prefs.getInt(PREFIX + topicId, -1).takeIf { it >= 0 }
    }

    /** Saves the floor the user left off at for [topicId]. */
    fun setFloor(topicId: String, floor: Int) {
        if (topicId.isEmpty() || floor < 0) return
        val editor = prefs.edit().putInt(PREFIX + topicId, floor)
        // Keep an insertion-order index so we can prune oldest entries.
        val index = prefs.getString(KEY_INDEX, "").orEmpty()
            .split(",")
            .filter { it.isNotEmpty() && it != topicId }
            .toMutableList()
        index.add(topicId)
        while (index.size > MAX_ENTRIES) {
            val oldest = index.removeAt(0)
            editor.remove(PREFIX + oldest)
        }
        editor.putString(KEY_INDEX, index.joinToString(","))
        editor.apply()
    }

    /** Drops the saved progress for [topicId]. */
    fun clear(topicId: String) {
        if (topicId.isEmpty()) return
        prefs.edit().remove(PREFIX + topicId).apply()
    }
}
