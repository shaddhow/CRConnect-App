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
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat

class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var logoAnimator: ObjectAnimator? = null
    private var pulseAnimator1: ObjectAnimator? = null
    private var pulseAnimator2: ObjectAnimator? = null
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
        logoCard?.let { ViewCompat.setElevation(it, 0f) }
        val pulseView1 = findViewById<View>(R.id.viewPulse1)
        val pulseView2 = findViewById<View>(R.id.viewPulse2)
        val tvSplashTitle = findViewById<TextView>(R.id.tvSplashTitle)
        val tvSplashTagline = findViewById<TextView>(R.id.tvSplashTagline)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvSplashFooter = findViewById<TextView>(R.id.tvSplashFooter)

        setupAppNameSpannable(tvSplashTitle)
        setupTaglineText(tvSplashTagline)

        if (isAnimatorDurationScaleZero()) {
            skipAnimationsToEndState(
                logoCard = logoCard,
                pulseView1 = pulseView1,
                pulseView2 = pulseView2,
                tvTitle = tvSplashTitle,
                tvTagline = tvSplashTagline,
                progressBar = progressBar,
                tvFooter = tvSplashFooter,
            )
        } else {
            setupLogoAnimation(logoCard)
            setupPulseAnimations(pulseView1, pulseView2)
            setupTitleAndTaglineAnimations(tvSplashTitle, tvSplashTagline)
            setupProgressBarAndFooterAnimations(progressBar, tvSplashFooter)

            // Navigate to MainActivity after progress animation completes
            handler.postDelayed(navigateRunnable, 2800)
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
        pulseView1: View?,
        pulseView2: View?,
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
        pulseView1?.alpha = 0f
        pulseView2?.alpha = 0f

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
        logoCard ?: return

        logoCard.scaleX = 0.45f
        logoCard.scaleY = 0.45f
        logoCard.alpha = 0.0f

        val pvhScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.45f, 1.0f)
        val pvhScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.45f, 1.0f)
        val pvhAlpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.0f, 1.0f)

        logoAnimator = ObjectAnimator.ofPropertyValuesHolder(logoCard, pvhScaleX, pvhScaleY, pvhAlpha).apply {
            duration = 900
            startDelay = 100
            interpolator = OvershootInterpolator(1.2f)
            start()
        }
    }

    private fun setupPulseAnimations(view1: View?, view2: View?) {
        if (view1 != null) {
            pulseAnimator1 = createPulseAnimator(view1, startDelayMs = 0)
            pulseAnimator1?.start()
        }
        if (view2 != null) {
            pulseAnimator2 = createPulseAnimator(view2, startDelayMs = 1400)
            pulseAnimator2?.start()
        }
    }

    private fun createPulseAnimator(view: View, startDelayMs: Long): ObjectAnimator {
        view.alpha = 0f
        view.scaleX = 1.0f
        view.scaleY = 1.0f

        val pvhScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.9f)
        val pvhScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.9f)
        val pvhAlpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.65f, 0.0f)

        return ObjectAnimator.ofPropertyValuesHolder(view, pvhScaleX, pvhScaleY, pvhAlpha).apply {
            duration = 2600
            repeatCount = ValueAnimator.INFINITE
            interpolator = DecelerateInterpolator()
            startDelay = startDelayMs
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

        if (progressBar != null) {
            progressBar.max = 100
            progressBar.progress = 0

            progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", 0, 100).apply {
                duration = 2400 // 2.4 seconds
                startDelay = 200 // starts shortly after launch
                interpolator = cubicBezierInterpolator
                start()
            }
        }

        if (tvFooter != null) {
            tvFooter.alpha = 0.0f
            val slideDistancePx = 12f * resources.displayMetrics.density
            tvFooter.translationY = slideDistancePx

            val pvhAlpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.0f, 1.0f)
            val pvhTransY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, slideDistancePx, 0.0f)

            footerAnimator = ObjectAnimator.ofPropertyValuesHolder(tvFooter, pvhAlpha, pvhTransY).apply {
                duration = 600
                startDelay = 800 // Fades in 600ms after the progress bar starts (200ms + 600ms)
                interpolator = PathInterpolator(0.16f, 1.0f, 0.3f, 1.0f)
                start()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(navigateRunnable)
        logoAnimator?.cancel()
        pulseAnimator1?.cancel()
        pulseAnimator2?.cancel()
        titleAnimator?.cancel()
        taglineAnimator?.cancel()
        progressAnimator?.cancel()
        footerAnimator?.cancel()
    }
}
