package com.example.crconnect

import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.animation.PathInterpolatorCompat
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import kotlin.math.roundToInt

/**
 * ToastType defines notification categories for ToastHelper.
 */
enum class ToastType {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

/**
 * ToastHelper displays custom toast notifications attached to the Window via WindowManager.
 *
 * Features:
 * - Appears above all views, dialogs, and bottom sheets via WindowManager
 * - Accepts Activity or Application context
 * - Callable from any Activity, Fragment, or ViewModel
 * - Smooth spring slide-up entrance animation & shrink progress bar
 * - Light and Dark theme adaptive glassmorphic styling
 */
object ToastHelper {

    private const val TAG_DISMISSING = "CRCONNECT_TOAST_DISMISSING"
    private const val AUTO_DISMISS_MS = 3200L
    private const val MAX_TOASTS = 3
    private const val BOTTOM_OFFSET_DP = 28f
    private const val GAP_DP = 10f
    private const val MAX_WIDTH_DP = 336f

    private var currentActivityRef: WeakReference<Activity>? = null
    private var isInitialized = false
    private val containerMap = WeakHashMap<Activity, LinearLayout>()

    /**
     * Initializes Activity lifecycle tracking for ToastHelper.
     * Called automatically or explicitly from App.onCreate().
     */
    @JvmStatic
    fun init(application: Application) {
        if (isInitialized) return
        isInitialized = true
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                currentActivityRef = WeakReference(activity)
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityDestroyed(activity: Activity) {
                if (currentActivityRef?.get() === activity) {
                    currentActivityRef = null
                }
                removeToastContainer(activity)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                currentActivityRef = WeakReference(activity)
            }

            override fun onActivityStarted(activity: Activity) {
                currentActivityRef = WeakReference(activity)
            }

            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        })
    }

    /**
     * Primary static helper method:
     * ToastHelper.show(context, ToastType.SUCCESS, "Published to dashboard", "shaddhow is now visible to voters");
     */
    @JvmStatic
    @JvmOverloads
    fun show(
        context: Context?,
        toastType: ToastType,
        title: String?,
        body: String? = null
    ) {
        if (context == null) return
        ensureInitialized(context)

        val resolvedActivity = resolveActivity(context)
        val finalTitle = title ?: getDefaultTitle(toastType)
        val finalBody = body ?: ""

        if (resolvedActivity == null || resolvedActivity.isFinishing || resolvedActivity.isDestroyed) {
            showFallbackToast(context, finalTitle, finalBody)
            return
        }

        resolvedActivity.runOnUiThread {
            displayWindowManagerToast(resolvedActivity, toastType, finalTitle, finalBody)
        }
    }

    /**
     * Helper overload accepting title and body (defaulting to ToastType.INFO).
     */
    @JvmStatic
    fun show(context: Context?, title: String?, body: String?) {
        show(context, ToastType.INFO, title, body)
    }

    /**
     * Overload accepting single message and optional type.
     */
    @JvmStatic
    @JvmOverloads
    fun show(context: Context?, message: String?, toastType: ToastType? = null) {
        if (context == null || message.isNullOrBlank()) return
        val detectedType = toastType ?: detectType(message)
        show(context, detectedType, getDefaultTitle(detectedType), message)
    }

    @JvmStatic
    @JvmOverloads
    fun showSuccess(context: Context?, title: String?, body: String? = null) {
        show(context, ToastType.SUCCESS, title ?: "Success", body)
    }

    @JvmStatic
    @JvmOverloads
    fun showInfo(context: Context?, title: String?, body: String? = null) {
        show(context, ToastType.INFO, title ?: "Information", body)
    }

    @JvmStatic
    @JvmOverloads
    fun showWarning(context: Context?, title: String?, body: String? = null) {
        show(context, ToastType.WARNING, title ?: "Warning", body)
    }

    @JvmStatic
    @JvmOverloads
    fun showError(context: Context?, title: String?, body: String? = null) {
        show(context, ToastType.ERROR, title ?: "Error", body)
    }

    private fun ensureInitialized(context: Context) {
        if (!isInitialized) {
            (context.applicationContext as? Application)?.let { init(it) }
        }
    }

    private fun resolveActivity(context: Context?): Activity? {
        if (context == null) return currentActivityRef?.get()
        var curr: Context? = context
        while (curr is ContextWrapper) {
            if (curr is Activity) return curr
            val base = curr.baseContext
            if (base === curr || base == null) break
            curr = base
        }
        return currentActivityRef?.get()
    }

    private fun displayWindowManagerToast(
        activity: Activity,
        toastType: ToastType,
        title: String,
        body: String
    ) {
        try {
            var container = containerMap[activity]

            if (container == null || container.parent == null) {
                container = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                    isClickable = false
                    isFocusable = false

                    layoutTransition = LayoutTransition().apply {
                        setDuration(LayoutTransition.CHANGE_APPEARING, 200)
                        setDuration(LayoutTransition.CHANGE_DISAPPEARING, 200)
                        setDuration(LayoutTransition.CHANGING, 200)
                    }
                }

                containerMap[activity] = container

                val wmParams = WindowManager.LayoutParams().apply {
                    width = WindowManager.LayoutParams.MATCH_PARENT
                    height = WindowManager.LayoutParams.WRAP_CONTENT
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    y = dpToPx(activity, BOTTOM_OFFSET_DP)
                    format = PixelFormat.TRANSLUCENT
                    flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                    this.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
                    token = activity.window?.decorView?.windowToken
                }

                val windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                if (windowManager != null && wmParams.token != null) {
                    windowManager.addView(container, wmParams)
                } else if (activity.window != null) {
                    activity.window.decorView.post {
                        val token = activity.window?.decorView?.windowToken
                        if (token != null && windowManager != null && container.parent == null) {
                            wmParams.token = token
                            try {
                                windowManager.addView(container, wmParams)
                            } catch (_: Exception) {
                                attachToDecorView(activity, container)
                            }
                        } else if (container.parent == null) {
                            attachToDecorView(activity, container)
                        }
                    }
                } else {
                    showFallbackToast(activity, title, body)
                    return
                }
            }

            // Enforce max 3 stacked toasts
            while (container.childCount >= MAX_TOASTS) {
                val oldestView = container.getChildAt(0)
                container.removeView(oldestView)
            }

            val isDarkMode = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val toastView = createToastView(activity, toastType, title, body, isDarkMode)

            val childParams = LinearLayout.LayoutParams(
                dpToPx(activity, MAX_WIDTH_DP).coerceAtMost((activity.resources.displayMetrics.widthPixels * 0.92f).toInt()),
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }

            container.addView(toastView, childParams)
            updateStackMargins(container)

            // Entrance animation (spring slide up + scale + alpha)
            toastView.translationY = dpToPx(activity, 18f).toFloat()
            toastView.scaleX = 0.94f
            toastView.scaleY = 0.94f
            toastView.alpha = 0f

            val springInterpolator = PathInterpolatorCompat.create(0.34f, 1.4f, 0.64f, 1.0f)
            toastView.animate()
                .translationY(0f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(340)
                .setInterpolator(springInterpolator)
                .start()

            // Auto dismiss & manual click dismiss
            val handler = Handler(Looper.getMainLooper())
            val dismissRunnable = Runnable { dismissToast(activity, container, toastView) }

            toastView.setOnClickListener {
                handler.removeCallbacks(dismissRunnable)
                dismissToast(activity, container, toastView)
            }

            val closeBtn = toastView.findViewById<View>(R.id.toastCloseBtn)
            closeBtn?.setOnClickListener {
                handler.removeCallbacks(dismissRunnable)
                dismissToast(activity, container, toastView)
            }

            handler.postDelayed(dismissRunnable, AUTO_DISMISS_MS)

        } catch (_: Exception) {
            showFallbackToast(activity, title, body)
        }
    }

    private fun attachToDecorView(activity: Activity, container: LinearLayout) {
        try {
            val root = (activity.findViewById<ViewGroup>(android.R.id.content) ?: activity.window?.decorView as? ViewGroup)
            if (root != null && container.parent == null) {
                val params = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    bottomMargin = dpToPx(activity, BOTTOM_OFFSET_DP)
                }
                root.addView(container, params)
            }
        } catch (_: Exception) {}
    }

    private fun createToastView(
        context: Context,
        toastType: ToastType,
        title: String,
        body: String,
        isDarkMode: Boolean
    ): FrameLayout {
        val rootFrame = LayoutInflater.from(context).inflate(R.layout.toast_custom, null, false) as FrameLayout
        val style = getToastStyle(context, toastType)

        // Light mode card background override if needed
        if (!isDarkMode) {
            val lightCardBg = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf("#FAF5F7FA".toColorInt(), "#F0E8EEF8".toColorInt())
            ).apply {
                cornerRadius = dpToPx(context, 18f).toFloat()
                setStroke(dpToPx(context, 1f), "#14000000".toColorInt())
            }
            rootFrame.background = lightCardBg
            rootFrame.elevation = dpToPx(context, 8f).toFloat()
        }

        // Icon Container
        val iconFrame = rootFrame.findViewById<FrameLayout>(R.id.iconContainer)
        val iconBg = GradientDrawable(GradientDrawable.Orientation.TL_BR, style.iconBgGradient).apply {
            cornerRadius = dpToPx(context, 12f).toFloat()
            setStroke(dpToPx(context, 1f), style.iconStrokeColor)
        }
        iconFrame.background = iconBg

        val iconView = rootFrame.findViewById<ImageView>(R.id.toastIcon)
        iconView.setImageResource(style.iconRes)
        iconView.setColorFilter(style.iconTintColor)

        // Text
        val titleTextView = rootFrame.findViewById<TextView>(R.id.toastTitle)
        titleTextView.text = title
        titleTextView.setTextColor(style.titleColor)

        val bodyTextView = rootFrame.findViewById<TextView>(R.id.toastBody)
        if (body.isNotBlank()) {
            bodyTextView.text = body
            bodyTextView.visibility = View.VISIBLE
            bodyTextView.setTextColor(if (isDarkMode) "#4A6080".toColorInt() else "#8898B0".toColorInt())
        } else {
            bodyTextView.visibility = View.GONE
        }

        // Close Icon
        val closeButtonIcon = rootFrame.findViewById<ImageView>(R.id.toastCloseIcon)
        closeButtonIcon.setColorFilter(if (isDarkMode) "#2D4060".toColorInt() else "#C0CCE0".toColorInt())

        // Progress Bar Animation
        val progressBarView = rootFrame.findViewById<View>(R.id.toastProgressBar)
        progressBarView.background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            style.barColors
        ).apply {
            cornerRadii = floatArrayOf(
                0f, 0f, 0f, 0f,
                dpToPx(context, 2f).toFloat(), dpToPx(context, 2f).toFloat(),
                dpToPx(context, 16f).toFloat(), dpToPx(context, 16f).toFloat()
            )
        }

        progressBarView.post {
            progressBarView.pivotX = 0f
            val barAnim = ObjectAnimator.ofFloat(progressBarView, "scaleX", 1f, 0f)
            barAnim.duration = AUTO_DISMISS_MS
            barAnim.interpolator = LinearInterpolator()
            barAnim.start()
        }

        return rootFrame
    }

    private fun updateStackMargins(container: LinearLayout) {
        val childCount = container.childCount
        for (i in 0 until childCount) {
            val child = container.getChildAt(i)
            val params = child.layoutParams as? LinearLayout.LayoutParams ?: continue
            params.bottomMargin = if (i == childCount - 1) 0 else dpToPx(container.context, GAP_DP)
            child.layoutParams = params
        }
    }

    private fun dismissToast(activity: Activity, container: LinearLayout, toastView: View) {
        if (toastView.tag == TAG_DISMISSING) return
        toastView.tag = TAG_DISMISSING

        toastView.animate()
            .translationY(dpToPx(toastView.context, 18f).toFloat())
            .scaleX(0.94f)
            .scaleY(0.94f)
            .alpha(0f)
            .setDuration(220)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                try {
                    container.removeView(toastView)
                    updateStackMargins(container)
                    if (container.childCount == 0) {
                        removeToastContainer(activity)
                    }
                } catch (_: Exception) {}
            }
            .start()
    }

    private fun removeToastContainer(activity: Activity) {
        val container = containerMap.remove(activity) ?: return
        try {
            if (container.parent != null) {
                val windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                if (container.isAttachedToWindow && windowManager != null) {
                    windowManager.removeViewImmediate(container)
                } else if (container.parent is ViewGroup) {
                    (container.parent as ViewGroup).removeView(container)
                }
            }
        } catch (_: Exception) {}
    }

    private class ToastStyle(
        val iconBgGradient: IntArray,
        val iconStrokeColor: Int,
        val iconRes: Int,
        val titleColor: Int,
        val barColors: IntArray,
        val iconTintColor: Int
    )

    private fun getToastStyle(context: Context, toastType: ToastType): ToastStyle {
        return when (toastType) {
            ToastType.SUCCESS -> ToastStyle(
                iconBgGradient = intArrayOf(
                    ContextCompat.getColor(context, R.color.toast_success_icon_bg_start),
                    ContextCompat.getColor(context, R.color.toast_success_icon_bg_end)
                ),
                iconStrokeColor = ContextCompat.getColor(context, R.color.toast_success_icon_border),
                iconRes = R.drawable.ic_toast_check,
                titleColor = ContextCompat.getColor(context, R.color.toast_success_title_color),
                barColors = intArrayOf(
                    ContextCompat.getColor(context, R.color.toast_success_progress_bar_start),
                    ContextCompat.getColor(context, R.color.toast_success_progress_bar_end)
                ),
                iconTintColor = ContextCompat.getColor(context, R.color.toast_success_icon_color)
            )
            ToastType.INFO -> ToastStyle(
                iconBgGradient = intArrayOf(
                    ContextCompat.getColor(context, R.color.toast_info_icon_bg_start),
                    ContextCompat.getColor(context, R.color.toast_info_icon_bg_end)
                ),
                iconStrokeColor = ContextCompat.getColor(context, R.color.toast_info_icon_border),
                iconRes = R.drawable.ic_toast_vote,
                titleColor = ContextCompat.getColor(context, R.color.toast_info_title_color),
                barColors = intArrayOf(
                    ContextCompat.getColor(context, R.color.toast_info_progress_bar_start),
                    ContextCompat.getColor(context, R.color.toast_info_progress_bar_end)
                ),
                iconTintColor = ContextCompat.getColor(context, R.color.toast_info_icon_color)
            )
            ToastType.WARNING -> ToastStyle(
                iconBgGradient = intArrayOf(
                    ContextCompat.getColor(context, R.color.toast_warning_icon_bg_start),
                    ContextCompat.getColor(context, R.color.toast_warning_icon_bg_end)
                ),
                iconStrokeColor = ContextCompat.getColor(context, R.color.toast_warning_icon_border),
                iconRes = R.drawable.ic_toast_clock,
                titleColor = ContextCompat.getColor(context, R.color.toast_warning_title_color),
                barColors = intArrayOf(
                    ContextCompat.getColor(context, R.color.toast_warning_progress_bar_start),
                    ContextCompat.getColor(context, R.color.toast_warning_progress_bar_end)
                ),
                iconTintColor = ContextCompat.getColor(context, R.color.toast_warning_icon_color)
            )
            ToastType.ERROR -> ToastStyle(
                iconBgGradient = intArrayOf(
                    ContextCompat.getColor(context, R.color.toast_error_icon_bg_start),
                    ContextCompat.getColor(context, R.color.toast_error_icon_bg_end)
                ),
                iconStrokeColor = ContextCompat.getColor(context, R.color.toast_error_icon_border),
                iconRes = R.drawable.ic_toast_shield,
                titleColor = ContextCompat.getColor(context, R.color.toast_error_title_color),
                barColors = intArrayOf(
                    ContextCompat.getColor(context, R.color.toast_error_progress_bar_start),
                    ContextCompat.getColor(context, R.color.toast_error_progress_bar_end)
                ),
                iconTintColor = ContextCompat.getColor(context, R.color.toast_error_icon_color)
            )
        }
    }

    private fun detectType(message: String): ToastType {
        val lower = message.lowercase()
        return when {
            lower.contains("fail") || lower.contains("error") || lower.contains("cannot") ||
                    lower.contains("invalid") || lower.contains("denied") || lower.contains("missing") ||
                    lower.contains("wrong") || lower.contains("problem") -> ToastType.ERROR

            lower.contains("success") || lower.contains("updated") || lower.contains("saved") ||
                    lower.contains("deleted") || lower.contains("reset") || lower.contains("assigned") ||
                    lower.contains("published") || lower.contains("resent") || lower.contains("added") ||
                    lower.contains("connected") -> ToastType.SUCCESS

            lower.contains("locked") || lower.contains("warning") || lower.contains("closed") || lower.contains("ended") ||
                    lower.contains("required") || lower.contains("no candidates") || lower.contains("not found") ||
                    lower.contains("please try again") || lower.contains("permission") -> ToastType.WARNING

            else -> ToastType.INFO
        }
    }

    private fun getDefaultTitle(toastType: ToastType): String {
        return when (toastType) {
            ToastType.SUCCESS -> "Success"
            ToastType.INFO -> "Information"
            ToastType.WARNING -> "Notice"
            ToastType.ERROR -> "Error"
        }
    }

    private fun showFallbackToast(context: Context, title: String, body: String) {
        val text = if (title.isNotBlank() && body.isNotBlank()) "$title: $body" else title.ifBlank { body }
        try {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context.applicationContext, text, Toast.LENGTH_SHORT).show()
            }
        } catch (_: Exception) {}
    }

    private fun dpToPx(context: Context, dp: Float): Int {
        return (dp * context.resources.displayMetrics.density).roundToInt()
    }
}
