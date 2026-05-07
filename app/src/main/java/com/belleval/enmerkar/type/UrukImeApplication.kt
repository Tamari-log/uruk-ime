package com.belleval.enmerkar.type

import android.app.Application
import com.belleval.enmerkar.type.diagnostic.DiagnosticLog

class UrukImeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DiagnosticLog.init(this)
    }
}
