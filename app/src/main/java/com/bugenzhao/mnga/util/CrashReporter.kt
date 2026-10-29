package com.bugenzhao.mnga.util

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.bugenzhao.mnga.BuildConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.system.exitProcess

/**
 * Diagnostic-only crash capture (v1.7.11; system exit records added in v1.7.12).
 *
 * Two capture channels feed one record file ([LOG_FILE], records separated
 * by [HEADER] lines, at most [MAX_RECORDS] kept):
 *
 * 1. JVM channel (v1.7.11): an uncaught-exception handler appends the full
 *    `printStackTrace` (whole "Caused by" chain, plus time / app version /
 *    Android version / device) and then chains to the previous default
 *    handler so the process still ends the system way (Bugly keeps working).
 *
 * 2. Exit-record channel (v1.7.12): the v1.7.11 build proved the open-topic
 *    crash bypasses the JVM channel entirely (no dialog after a reproduced
 *    crash), which points at a native crash (e.g. a stack-overflow SIGSEGV
 *    in the Rust parser) that no Java handler can ever see. So on the next
 *    launch, [pendingCrash] also sweeps
 *    [ActivityManager.getHistoricalProcessExitReasons] (API 30+) for
 *    CRASH / CRASH_NATIVE / ANR exits and converts each into a record in
 *    the same format, including the system trace (a tombstone for native
 *    crashes). A separate ingestion marker ([KEY_EXIT_SEEN]) tracks which
 *    exit records have been converted, independent of the display marker
 *    ([KEY_SEEN]). A REASON_CRASH exit whose timestamp lands within
 *    [DEDUP_WINDOW_MS] of a JVM record is the same crash seen twice and is
 *    skipped (the JVM copy is at least as complete); native / ANR exits
 *    have no JVM twin and are always kept. Every record labels its source.
 *
 * On the next launch [pendingCrash] returns the newest record if it has
 * not been shown yet; the root UI displays it in a dialog with a copy
 * button (see `CrashReportDialog`) and [markSeen] suppresses repeats.
 */
object CrashReporter {

    private const val LOG_FILE = "crash_log.txt"
    private const val PREFS = "crash_reporter"
    private const val KEY_SEEN = "seen_millis"
    private const val KEY_EXIT_SEEN = "exit_seen_millis"
    private const val MAX_RECORDS = 3
    private const val HEADER = "###CRASH "

    /** Same-crash window between a JVM record and its REASON_CRASH exit twin. */
    private const val DEDUP_WINDOW_MS = 5_000L

    /** Tombstones can be huge; the signal / backtrace head is what matters. */
    private const val MAX_TRACE_BYTES = 512 * 1024

    /** A crash record that has not been presented to the user yet. */
    data class PendingCrash(val timeMillis: Long, val text: String)

    @Volatile
    private var installed = false

    @Volatile
    private var exitSwept = false

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
            appendLine("source: jvm (uncaught exception handler)")
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
        // Fold any new system exit records (native crashes the JVM channel
        // cannot see) into the record file first, same moment as the check.
        runCatching { sweepExitReasons(context) }
        val file = File(context.filesDir, LOG_FILE)
        if (!file.exists()) return null
        val last = splitRecords(runCatching { file.readText() }.getOrDefault("")).lastOrNull()
            ?: return null
        val millis = recordMillis(last) ?: return null
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

    /**
     * Converts new crash-type entries from the system's historical exit
     * reasons into records in [LOG_FILE]. Runs at most once per process;
     * a no-op below API 30. Ingestion progress is tracked separately from
     * the display marker so the two channels never suppress each other.
     */
    private fun sweepExitReasons(context: Context) {
        if (exitSwept) return
        exitSwept = true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        sweepExitReasonsApi30(context)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun sweepExitReasonsApi30(context: Context) {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
        val infos = runCatching {
            activityManager.getHistoricalProcessExitReasons(context.packageName, 0, 10)
        }.getOrDefault(emptyList())
        if (infos.isEmpty()) return

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val exitSeen = prefs.getLong(KEY_EXIT_SEEN, 0L)
        val fresh = infos.filter {
            it.timestamp > exitSeen &&
                (it.reason == ApplicationExitInfo.REASON_CRASH ||
                    it.reason == ApplicationExitInfo.REASON_CRASH_NATIVE ||
                    it.reason == ApplicationExitInfo.REASON_ANR)
        }.sortedBy { it.timestamp }
        if (fresh.isEmpty()) return

        val file = File(context.filesDir, LOG_FILE)
        var records = splitRecords(runCatching { file.readText() }.getOrDefault(""))
        val knownMillis = records.mapNotNull { recordMillis(it) }
        var maxProcessed = exitSeen
        for (info in fresh) {
            maxProcessed = maxOf(maxProcessed, info.timestamp)
            // A plain Java crash is reported by both channels; the JVM
            // record is already in the file and is at least as complete,
            // so skip its exit-record twin when the timestamps line up.
            val duplicatedByJvm = info.reason == ApplicationExitInfo.REASON_CRASH &&
                knownMillis.any { abs(it - info.timestamp) <= DEDUP_WINDOW_MS }
            if (duplicatedByJvm) continue
            records = records + exitRecord(info)
        }
        val merged = records
            .sortedBy { recordMillis(it) ?: Long.MIN_VALUE }
            .takeLast(MAX_RECORDS)
        file.writeText(merged.joinToString("\n"))
        prefs.edit().putLong(KEY_EXIT_SEEN, maxProcessed).apply()
    }

    /** Formats one system exit record like a JVM crash record. */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun exitRecord(info: ApplicationExitInfo): String {
        val trace = readTrace(info)
        return buildString {
            appendLine(HEADER + info.timestamp)
            appendLine(
                "time: " +
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
                        .format(Date(info.timestamp)),
            )
            appendLine("app: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine(
                "source: system exit record " +
                    "(ActivityManager.getHistoricalProcessExitReasons)",
            )
            appendLine("process: ${info.processName}")
            appendLine(
                "exit reason: ${reasonName(info.reason)} (${info.reason}), " +
                    "status: ${info.status}",
            )
            appendLine("description: ${info.description ?: "(none)"}")
            if (trace != null) {
                appendLine("trace:")
                append(trace)
            } else {
                // Some OEMs hand out no trace stream; keep the metadata
                // rather than dropping the exit silently.
                appendLine("trace: (system provided no trace stream)")
                append("exitInfo: $info")
            }
        }
    }

    /** Reads the system trace (tombstone / stack text), capped in size. */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun readTrace(info: ApplicationExitInfo): String? {
        val stream = runCatching { info.traceInputStream }.getOrNull() ?: return null
        return runCatching {
            stream.use { input ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(16 * 1024)
                var truncated = false
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    val room = MAX_TRACE_BYTES - out.size()
                    if (read <= room) {
                        out.write(buffer, 0, read)
                    } else {
                        if (room > 0) out.write(buffer, 0, room)
                        truncated = true
                        break
                    }
                }
                val text = out.toString(Charsets.UTF_8.name())
                if (truncated) {
                    text + "\n[trace truncated at $MAX_TRACE_BYTES bytes]"
                } else {
                    text
                }
            }
        }.getOrNull()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun reasonName(reason: Int): String = when (reason) {
        ApplicationExitInfo.REASON_CRASH -> "REASON_CRASH"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "REASON_CRASH_NATIVE"
        ApplicationExitInfo.REASON_ANR -> "REASON_ANR"
        ApplicationExitInfo.REASON_SIGNALED -> "REASON_SIGNALED"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "REASON_LOW_MEMORY"
        ApplicationExitInfo.REASON_EXIT_SELF -> "REASON_EXIT_SELF"
        else -> "REASON_$reason"
    }

    private fun recordMillis(record: String): Long? =
        record.lineSequence().firstOrNull()
            ?.removePrefix(HEADER)?.trim()?.toLongOrNull()

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
