package com.example.crconnect

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat

class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var logoAnimator: ObjectAnimator? = null
    private var titleAnimator: ObjectAnimator? = null
    private var taglineAnimator: ObjectAnimator? = null
    private var progressAnimator: ObjectAnimator? = null
    private var footerAnimator: ObjectAnimator? = null

    private val navigateRunnable = Runnable {
        if (!isFinishing && !isDestroyed) {
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide action bar if present
        supportActionBar?.hide()

        // Edge-to-Edge full screen: fully transparent status bar and navigation bar so gradient fills edge to edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true

        setContentView(R.layout.activity_splash)

        val logoCard = findViewById<View>(R.id.cardSplashLogo)

        val tvSplashTitle = findViewById<TextView>(R.id.tvSplashTitle)
        val tvSplashTagline = findViewById<TextView>(R.id.tvSplashTagline)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvSplashFooter = findViewById<TextView>(R.id.tvSplashFooter)

        setupAppNameSpannable(tvSplashTitle)
        setupTaglineText(tvSplashTagline)

        if (isAnimatorDurationScaleZero()) {
            skipAnimationsToEndState(
                logoCard = logoCard,
                tvTitle = tvSplashTitle,
                tvTagline = tvSplashTagline,
                progressBar = progressBar,
                tvFooter = tvSplashFooter,
            )
        } else {
            setupLogoAnimation(logoCard)
            setupTitleAndTaglineAnimations(tvSplashTitle, tvSplashTagline)
            setupProgressBarAndFooterAnimations(progressBar, tvSplashFooter)

            // Navigate to MainActivity after progress bar animation completes (1000ms delay + 2600ms duration)
            handler.postDelayed(navigateRunnable, 3600)
        }
    }

    private fun isAnimatorDurationScaleZero(): Boolean {
        val durationScale = try {
            Settings.Global.getFloat(
                contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f,
            )
        } catch (_: Exception) {
            1.0f
        }
        val areAnimatorsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ValueAnimator.areAnimatorsEnabled()
        } else {
            true
        }
        return (durationScale == 0f) || (!areAnimatorsEnabled)
    }

    private fun skipAnimationsToEndState(
        logoCard: View?,
        tvTitle: TextView?,
        tvTagline: TextView?,
        progressBar: ProgressBar?,
        tvFooter: TextView?,
    ) {
        logoCard?.apply {
            scaleX = 1.0f
            scaleY = 1.0f
            alpha = 1.0f
            translationY = 0f
        }

        tvTitle?.apply {
            alpha = 1.0f
            translationY = 0f
        }
        tvTagline?.apply {
            alpha = 1.0f
            translationY = 0f
        }
        progressBar?.apply {
            max = 100
            progress = 100
        }
        tvFooter?.apply {
            alpha = 1.0f
            translationY = 0f
        }

        handler.post(navigateRunnable)
    }

    private fun setupAppNameSpannable(tvTitle: TextView?) {
        tvTitle ?: return
        val appName = "CRConnect"
        val spannable = SpannableString(appName)
        val colorBlue = ContextCompat.getColor(this, R.color.brand_blue) // #0066FF
        val colorDark = ContextCompat.getColor(this, R.color.slate_dark) // #0F172A

        spannable.setSpan(
            ForegroundColorSpan(colorBlue),
            0,
            2,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            ForegroundColorSpan(colorDark),
            2,
            appName.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tvTitle.text = spannable
    }

    private fun setupTaglineText(tvTagline: TextView?) {
        tvTagline ?: return
        tvTagline.text = getString(R.string.app_subtitle) // "BUBT Department Network"
    }

    private fun setupLogoAnimation(logoCard: View?) {
        if (logoCard == null) return

        val slideDistancePx = 32f * resources.displayMetrics.density
        val cubicInterpolator = OvershootInterpolator(1.1f)

        logoCard.scaleX = 0.45f
        logoCard.scaleY = 0.45f
        logoCard.alpha = 0.0f
        logoCard.translationY = slideDistancePx

        val pvhScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.45f, 1.0f)
        val pvhScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.45f, 1.0f)
        val pvhAlpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.0f, 1.0f)
        val pvhTransY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, slideDistancePx, 0.0f)

        logoAnimator = ObjectAnimator.ofPropertyValuesHolder(logoCard, pvhScaleX, pvhScaleY, pvhAlpha, pvhTransY).apply {
            duration = 1000
            startDelay = 100
            interpolator = cubicInterpolator
            start()
        }
    }

    private fun setupTitleAndTaglineAnimations(tvTitle: View?, tvTagline: View?) {
        val slideDistancePx = 16f * resources.displayMetrics.density
        val cubicBezierInterpolator = PathInterpolator(0.16f, 1.0f, 0.3f, 1.0f)

        if (tvTitle != null) {
            tvTitle.alpha = 0.0f
            tvTitle.translationY = slideDistancePx

            val pvhAlpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.0f, 1.0f)
            val pvhTransY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, slideDistancePx, 0.0f)

            titleAnimator = ObjectAnimator.ofPropertyValuesHolder(tvTitle, pvhAlpha, pvhTransY).apply {
                duration = 750
                startDelay = 600
                interpolator = cubicBezierInterpolator
                start()
            }
        }

        if (tvTagline != null) {
            tvTagline.alpha = 0.0f
            tvTagline.translationY = slideDistancePx

            val pvhAlpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.0f, 1.0f)
            val pvhTransY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, slideDistancePx, 0.0f)

            taglineAnimator = ObjectAnimator.ofPropertyValuesHolder(tvTagline, pvhAlpha, pvhTransY).apply {
                duration = 700
                startDelay = 800 // staggered 200ms after title (600ms + 200ms)
                interpolator = cubicBezierInterpolator
                start()
            }
        }
    }

    private fun setupProgressBarAndFooterAnimations(progressBar: ProgressBar?, tvFooter: TextView?) {
        val cubicBezierInterpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)
        val slideDistancePx = 16f * resources.displayMetrics.density

        if (progressBar != null) {
            progressBar.alpha = 0.0f
            progressBar.translationY = slideDistancePx
            progressBar.max = 100
            progressBar.progress = 0

            // Fade-in and slide-up animation for progress bar container
            val pvhAlpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.0f, 1.0f)
            val pvhTransY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, slideDistancePx, 0.0f)
            ObjectAnimator.ofPropertyValuesHolder(progressBar, pvhAlpha, pvhTransY).apply {
                duration = 600
                startDelay = 1000
                interpolator = PathInterpolator(0.16f, 1.0f, 0.3f, 1.0f)
                start()
            }

            // Fill animation for progress bar
            progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", 0, 100).apply {
                duration = 2600
                startDelay = 1000
                interpolator = cubicBezierInterpolator
                start()
            }
        }

        if (tvFooter != null) {
            tvFooter.alpha = 0.0f
            tvFooter.translationY = slideDistancePx

            val pvhAlpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.0f, 1.0f)
            val pvhTransY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, slideDistancePx, 0.0f)

            footerAnimator = ObjectAnimator.ofPropertyValuesHolder(tvFooter, pvhAlpha, pvhTransY).apply {
                duration = 500
                startDelay = 1600
                interpolator = PathInterpolator(0.16f, 1.0f, 0.3f, 1.0f)
                start()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(navigateRunnable)
        logoAnimator?.cancel()
        titleAnimator?.cancel()
        taglineAnimator?.cancel()
        progressAnimator?.cancel()
        footerAnimator?.cancel()
    }
}
