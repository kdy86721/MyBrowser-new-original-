package com.example

import android.app.Application
import com.example.webview.WebViewWarmupPool

/**
 * Custom Application entry point that pre-warms 1 hidden Chromium WebView on the main looper
 * idle queue during startup to cut cold-start renderer initialization latency.
 */
class BrowserApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WebViewWarmupPool.schedulePrewarm(this)
    }
}
