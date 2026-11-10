package com.example.crconnect;

import android.content.Context;

/**
 * CustomToast delegates to ToastHelper to show WindowManager overlay toasts.
 */
public class CustomToast {

    public enum ToastType {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }

    public static void show(Context context, String message) {
        ToastHelper.show(context, message, (com.example.crconnect.ToastType) null);
    }

    public static void show(Context context, String message, ToastType type) {
        ToastHelper.show(context, message, mapType(type));
    }

    public static void showWithEmoji(Context context, String message, String customEmoji) {
        ToastHelper.show(context, customEmoji, message);
    }

    public static void showSuccess(Context context, String message) {
        ToastHelper.showSuccess(context, "Success", message);
    }

    public static void showError(Context context, String message) {
        ToastHelper.showError(context, "Error", message);
    }

    public static void showWarning(Context context, String message) {
        ToastHelper.showWarning(context, "Warning", message);
    }

    public static void showInfo(Context context, String message) {
        ToastHelper.showInfo(context, "Information", message);
    }

    public static void show(Context context, String message, ToastType type, String customEmoji) {
        if (customEmoji != null && !customEmoji.trim().isEmpty()) {
            ToastHelper.show(context, mapType(type), customEmoji, message);
        } else {
            ToastHelper.show(context, message, mapType(type));
        }
    }

    private static com.example.crconnect.ToastType mapType(ToastType type) {
        if (type == null) return com.example.crconnect.ToastType.INFO;
        switch (type) {
            case SUCCESS:
                return com.example.crconnect.ToastType.SUCCESS;
            case ERROR:
                return com.example.crconnect.ToastType.ERROR;
            case WARNING:
                return com.example.crconnect.ToastType.WARNING;
            case INFO:
            default:
                return com.example.crconnect.ToastType.INFO;
        }
    }
}
