package com.francis.ocrreader

import android.util.Log

object UiUtils {

    internal fun showLog(TAG: String?, message: String?) {
        Log.v("$TAG", "$message")
    }

    internal fun showErrorLog(TAG: String?, message: String?) {
        Log.e("$TAG", "$message")
    }
}