package com.belleval.enmerkar.type

import android.app.Application
import com.belleval.enmerkar.type.diagnostic.DiagnosticLog
import com.belleval.enmerkar.type.sumerian.SumerianTransliteration
import kotlin.concurrent.thread

class UrukImeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // IME 初回表示で main thread が詰まらないよう、重い初期化は先行してバックグラウンドへ逃がす。
        thread(name = "UrukImeWarmup", isDaemon = true) {
            runCatching { DiagnosticLog.init(this) }
            runCatching { SumerianTransliteration.readingToCodepoint.size }
            runCatching { cuneiformCodePoints(0x12000..0x123FF).size }
            runCatching { cuneiformCodePoints(0x12400..0x1247F).size }
        }
    }
}
