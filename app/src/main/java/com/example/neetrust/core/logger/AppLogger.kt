// [KATALOG] shared.core.logger
package com.example.neetrust.core.logger

import android.util.Log

/**
 * Logger terstruktur per modul untuk NeetRust.
 */
class AppLogger(private val tag: String) {

    fun d(message: String) {
        Log.d("NeetRust-$tag", message)
    }

    fun i(message: String) {
        Log.i("NeetRust-$tag", message)
    }

    fun w(message: String) {
        Log.w("NeetRust-$tag", message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e("NeetRust-$tag", message, throwable)
    }

    companion object {
        fun of(moduleName: String): AppLogger = AppLogger(moduleName)
    }
}
