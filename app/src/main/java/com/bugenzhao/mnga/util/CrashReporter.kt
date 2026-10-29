package com.bugenzhao.mnga.util

import android.content.Context
import android.os.Build
import com.bugenzhao.mnga.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

/**
 * Diagnostic-only crash capture (v1.7.11).
 *
 * The open-topic crash survived the v1.7.10 double guard, so instead of
 * guessing further this build records the real stack: any uncaught
 * exception is appended (full `printStackTrace`, including the whole
 * "Caused by" chain, plus time / app version / Android version / device)
 * to a private file, keeping at most the [MAX_RECORDS] most recent
 * records, and then the previous default handler runs as usual so the
 * process still ends the system way (Bugly keeps working via the chain).
 *
 * On the next launch [pendingCrash] returns the newest record if it has
 * not been shown yet; the root UI displays it in a dialog with a copy
 * button (see `CrashReportDialog`) and [markSeen] suppresses repeats.
 */
object CrashReporter {

    private const val LOG_FILE = "crash_log.txt"
    private const val PREFS = "crash_reporter"
    private const val KEY_SEEN = "seen_millis"
    private const val MAX_RECORDS = 3
    private const val HEADER = "###CRASH "

    /** A crash record that has not been presented to the user yet. */
    data class PendingCrash(val timeMillis: Long, val text: String)

    @Volatile
    private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { appendRecord(appContext, thread, throwable) }
            if (previous != null) {
                previous.uncaughtException(thread, throwable)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                exitProcess(10)
            }
        }
    }

    private fun appendRecord(context: Context, thread: Thread, throwable: Throwable) {
        val now = System.currentTimeMillis()
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val record = buildString {
            appendLine(HEADER + now)
            appendLine(
                "time: " +
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(now)),
            )
            appendLine("app: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("thread: ${thread.name}")
            append(sw.toString())
        }
        val file = File(context.filesDir, LOG_FILE)
        val existing = runCatching { file.readText() }.getOrDefault("")
        val kept = (splitRecords(existing) + record).takeLast(MAX_RECORDS)
        file.writeText(kept.joinToString("\n"))
    }

    /** The newest stored crash if it is newer than the last shown one. */
    fun pendingCrash(context: Context): PendingCrash? {
        val file = File(context.filesDir, LOG_FILE)
        if (!file.exists()) return null
        val last = splitRecords(runCatching { file.readText() }.getOrDefault("")).lastOrNull()
            ?: return null
        val millis = last.lineSequence().firstOrNull()
            ?.removePrefix(HEADER)?.trim()?.toLongOrNull() ?: return null
        val seen = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_SEEN, 0L)
        if (millis <= seen) return null
        // Drop the machine marker line; the body already carries a
        // human-readable time line.
        return PendingCrash(millis, last.substringAfter('\n', last))
    }

    fun markSeen(context: Context, timeMillis: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_SEEN, timeMillis).apply()
    }

    /** Splits file content into records, each starting with a [HEADER] line. */
    private fun splitRecords(content: String): List<String> {
        if (content.isBlank()) return emptyList()
        val result = mutableListOf<String>()
        var current: StringBuilder? = null
        for (line in content.lineSequence()) {
            if (line.startsWith(HEADER)) {
                current?.let { result += it.toString() }
                current = StringBuilder(line)
            } else {
                current?.append('\n')?.append(line)
            }
        }
        current?.let { result += it.toString() }
        return result
    }
}
