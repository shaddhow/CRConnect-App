package com.example.crconnect

import android.content.Context

/**
 * ToastManager delegates to ToastHelper to display WindowManager floating overlay toast notifications.
 */
object ToastManager {

    @JvmStatic
    @JvmOverloads
    fun show(context: Context?, message: String?, type: ToastType? = null) {
        ToastHelper.show(context, message, type)
    }

    @JvmStatic
    fun showSuccess(context: Context?, message: String?) {
        ToastHelper.showSuccess(context, "Success", message)
    }

    @JvmStatic
    fun showSuccess(context: Context?, title: String?, message: String?) {
        ToastHelper.showSuccess(context, title, message)
    }

    @JvmStatic
    fun showError(context: Context?, message: String?) {
        ToastHelper.showError(context, "Error", message)
    }

    @JvmStatic
    fun showError(context: Context?, title: String?, message: String?) {
        ToastHelper.showError(context, title, message)
    }

    @JvmStatic
    fun showWarning(context: Context?, message: String?) {
        ToastHelper.showWarning(context, "Warning", message)
    }

    @JvmStatic
    fun showWarning(context: Context?, title: String?, message: String?) {
        ToastHelper.showWarning(context, title, message)
    }

    @JvmStatic
    fun showInfo(context: Context?, message: String?) {
        ToastHelper.showInfo(context, "Information", message)
    }

    @JvmStatic
    fun showInfo(context: Context?, title: String?, message: String?) {
        ToastHelper.showInfo(context, title, message)
    }

    @JvmStatic
    @JvmOverloads
    fun show(context: Context?, title: String?, message: String?, type: ToastType = ToastType.INFO) {
        ToastHelper.show(context, type, title, message)
    }
}
