package com.belleval.enmerkar.type.diagnostic

import android.app.Application
import android.os.Build
import com.belleval.enmerkar.type.BuildConfig
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * アプリ内診断ログ（メモリ＋ファイル）。未捕捉例外と IME まわりの失敗をユーザー向け画面で確認できるようにする。
 * 入力された文字そのものは記録しない。
 */
object DiagnosticLog {
    private const val MAX_MEMORY_LINES = 4_000
    private const val MAX_FILE_BYTES = 512_000
    private const val FILE_NAME = "diagnostic.log"

    private val dateFormat =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).apply {
            isLenient = false
        }

    private val lock = Any()
    private val deque = ArrayDeque<String>(MAX_MEMORY_LINES)
    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    private var app: Application? = null
    private val handlerInstalled = AtomicBoolean(false)

    fun init(application: Application) {
        synchronized(lock) {
            if (app != null) return
            app = application.applicationContext as Application
            loadFileIntoMemory()
            emitSnapshotLocked()
            i("DiagnosticLog", "init version=${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) sdk=${Build.VERSION.SDK_INT}")
            installUncaughtExceptionHandler()
        }
    }

    private fun logFile(): File? = app?.filesDir?.let { File(it, FILE_NAME) }

    fun i(tag: String, message: String) = append("I", tag, message, null)

    fun w(tag: String, message: String, throwable: Throwable? = null) =
        append("W", tag, message, throwable)

    fun e(tag: String, message: String, throwable: Throwable? = null) =
        append("E", tag, message, throwable)

    private fun append(level: String, tag: String, message: String, throwable: Throwable?) {
        val stamp = dateFormat.format(Date())
        val base = "$stamp $level/$tag: $message"
        val linesToAdd =
            buildList {
                add(base)
                if (throwable != null) {
                    add(throwable.stackTraceToString())
                }
            }
        synchronized(lock) {
            for (line in linesToAdd) {
                deque.addLast(line)
                while (deque.size > MAX_MEMORY_LINES) {
                    deque.removeFirst()
                }
            }
            emitSnapshotLocked()
        }
        persistLines(linesToAdd)
    }

    private fun emitSnapshotLocked() {
        _lines.value = deque.toList()
    }

    private fun persistLines(linesToAdd: List<String>) {
        val f = logFile() ?: return
        try {
            f.appendText(linesToAdd.joinToString("\n", postfix = "\n"))
            trimFileIfNeeded(f)
        } catch (e: IOException) {
            // ファイル書き込み失敗はメモリには残っている
            synchronized(lock) {
                deque.addLast("${dateFormat.format(Date())} E/DiagnosticLog: persist failed: ${e.message}")
                while (deque.size > MAX_MEMORY_LINES) deque.removeFirst()
                emitSnapshotLocked()
            }
        }
    }

    private fun trimFileIfNeeded(f: File) {
        try {
            val len = f.length()
            if (len <= MAX_FILE_BYTES) return
            val text = f.readText()
            val cut = text.takeLast(MAX_FILE_BYTES)
            f.writeText(cut)
        } catch (_: Exception) {
            // 削れなくても継続
        }
    }

    private fun loadFileIntoMemory() {
        val f = logFile() ?: return
        if (!f.exists() || f.length() == 0L) return
        try {
            val text = f.readText()
            val ls = text.trimEnd().lines().filter { it.isNotBlank() }
            synchronized(lock) {
                deque.clear()
                for (line in ls.takeLast(MAX_MEMORY_LINES)) {
                    deque.addLast(line)
                }
            }
        } catch (_: Exception) {
            // 起動は続行
        }
    }

    private fun installUncaughtExceptionHandler() {
        if (!handlerInstalled.compareAndSet(false, true)) return
        val default = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, ex ->
            try {
                e("UncaughtException", "thread=${thread.name}", ex)
                // 同期でファイルへ追記済み
            } finally {
                default?.uncaughtException(thread, ex)
            }
        }
    }

    fun clear() {
        val stamp = dateFormat.format(Date())
        synchronized(lock) {
            deque.clear()
            deque.addLast("$stamp I/DiagnosticLog: log cleared by user")
            emitSnapshotLocked()
        }
        try {
            logFile()?.writeText(deque.last() + "\n")
        } catch (_: Exception) {
            // ignore
        }
    }

    fun dumpSnapshotText(): String = synchronized(lock) { deque.joinToString("\n") }
}
