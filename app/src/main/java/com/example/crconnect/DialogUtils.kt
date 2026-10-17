@file:JvmName("DialogUtils")
package com.example.crconnect

import android.content.Context
import androidx.fragment.app.FragmentActivity

@JvmOverloads
fun showConfirmDialog(
    context: Context,
    title: String,
    message: String,
    confirmText: String,
    isDanger: Boolean = true,
    onConfirm: Runnable
) {
    if (context is FragmentActivity) {
        val sheet = LogoutBottomSheet.newInstance(
            title,
            message,
            confirmText,
            onConfirm
        )
        sheet.showAllowingStateLoss(context.supportFragmentManager, LogoutBottomSheet.TAG)
    }
}

fun showConfirmDialog(
    context: Context,
    title: String,
    message: String,
    confirmText: String,
    isDanger: Boolean = true,
    onConfirm: () -> Unit
) {
    showConfirmDialog(context, title, message, confirmText, isDanger, Runnable { onConfirm() })
}
