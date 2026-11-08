package com.example.crconnect

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.PathInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import android.content.res.Configuration

class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var pulseAnimator1: ObjectAnimator? = null
    private var pulseAnimator2: ObjectAnimator? = null
    private var pulseRingAnimatorA: ObjectAnimator? = null
    private var pulseRingAnimatorB: ObjectAnimator? = null
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
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
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

        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !isDark
        insetsController.isAppearanceLightNavigationBars = !isDark

        setContentView(R.layout.activity_splash)

        startPulseAnimation()

        val viewPulse1 = findViewById<View>(R.id.viewPulse1)
        val viewPulse2 = findViewById<View>(R.id.viewPulse2)
        val logoCard = findViewById<View>(R.id.cardSplashLogo)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && logoCard != null) {
            val shadowColor = ContextCompat.getColor(this, R.color.splash_card_shadow)
            logoCard.outlineSpotShadowColor = shadowColor
            logoCard.outlineAmbientShadowColor = shadowColor
        }
        val tvSplashTitle = findViewById<TextView>(R.id.tvSplashTitle)
        val tvSplashTagline = findViewById<TextView>(R.id.tvSplashTagline)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvSplashFooter = findViewById<TextView>(R.id.tvSplashFooter)

        setupAppNameSpannable(tvSplashTitle)

        if (isAnimatorDurationScaleZero()) {
            skipAnimationsToEndState(
                viewPulse1 = viewPulse1,
                viewPulse2 = viewPulse2,
                logoCard = logoCard,
                tvTitle = tvSplashTitle,
                tvTagline = tvSplashTagline,
                progressBar = progressBar,
                tvFooter = tvSplashFooter,
            )
        } else {
            setupPulseAnimations(viewPulse1, viewPulse2)
            setupLogoAnimation(logoCard)
            setupTitleAndTaglineAnimations(tvSplashTitle, tvSplashTagline)
            setupProgressBarAndFooterAnimations(progressBar, tvSplashFooter)

            // Fast navigation to MainActivity (1400ms total splash delay)
            handler.postDelayed(navigateRunnable, 1400)
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
        viewPulse1: View?,
        viewPulse2: View?,
        logoCard: View?,
        tvTitle: TextView?,
        tvTagline: TextView?,
        progressBar: ProgressBar?,
        tvFooter: TextView?,
    ) {
        viewPulse1?.alpha = 0.0f
        viewPulse2?.alpha = 0.0f

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
            alpha = 1.0f
            translationY = 0f
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
        tvTitle.post {
            val paint = tvTitle.paint
            val crWidth = paint.measureText("CR")
            val totalWidth = paint.measureText(appName)

            val crStartColor = ContextCompat.getColor(this, R.color.splash_title_cr)
            val crEndColor = ContextCompat.getColor(this, R.color.splash_title_cr_end)
            val connectStartColor = ContextCompat.getColor(this, R.color.splash_title_connect)
            val connectEndColor = ContextCompat.getColor(this, R.color.splash_title_connect_end)

            val spannable = SpannableString(appName)

            val crSpan = object : CharacterStyle(), UpdateAppearance {
                override fun updateDrawState(tp: TextPaint) {
                    tp.shader = LinearGradient(
                        0f, 0f, crWidth, 0f,
                        intArrayOf(crStartColor, crEndColor),
                        null, Shader.TileMode.CLAMP
                    )
                }
            }

            val connectSpan = object : CharacterStyle(), UpdateAppearance {
                override fun updateDrawState(tp: TextPaint) {
                    tp.shader = LinearGradient(
                        crWidth, 0f, totalWidth, 0f,
                        intArrayOf(connectStartColor, connectEndColor),
                        floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
                    )
                }
            }

            spannable.setSpan(crSpan, 0, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(connectSpan, 2, appName.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            tvTitle.text = spannable
        }
    }

    private fun setupPulseAnimations(viewPulse1: View?, viewPulse2: View?) {
        if (viewPulse1 == null || viewPulse2 == null) return

        val decelerate = DecelerateInterpolator()

        // Pulse View 1: delay 0ms, duration 2800ms
        viewPulse1.scaleX = 1.0f
        viewPulse1.scaleY = 1.0f
        viewPulse1.alpha = 0.65f

        val pvhScaleX1 = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.9f)
        val pvhScaleY1 = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.9f)
        val pvhAlpha1 = PropertyValuesHolder.ofFloat(View.ALPHA, 0.65f, 0.0f)

        pulseAnimator1 = ObjectAnimator.ofPropertyValuesHolder(viewPulse1, pvhScaleX1, pvhScaleY1, pvhAlpha1).apply {
            duration = 2800
            startDelay = 0
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = decelerate
            start()
        }

        // Pulse View 2: delay 1400ms, duration 2800ms
        viewPulse2.scaleX = 1.0f
        viewPulse2.scaleY = 1.0f
        viewPulse2.alpha = 0.65f

        val pvhScaleX2 = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.9f)
        val pvhScaleY2 = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.9f)
        val pvhAlpha2 = PropertyValuesHolder.ofFloat(View.ALPHA, 0.65f, 0.0f)

        pulseAnimator2 = ObjectAnimator.ofPropertyValuesHolder(viewPulse2, pvhScaleX2, pvhScaleY2, pvhAlpha2).apply {
            duration = 2800
            startDelay = 1400
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = decelerate
            start()
        }
    }

    private fun setupLogoAnimation(logoCard: View?) {
        logoCard ?: return

        val slideDistancePx = 32f * resources.displayMetrics.density
        val overshootPx = -4f * resources.displayMetrics.density
        val cubicBezier = PathInterpolator(0.16f, 1.0f, 0.3f, 1.0f)

        // Keyframes for Alpha (0% -> 0.0, 60% -> 1.0, 100% -> 1.0)
        val kfAlpha0 = Keyframe.ofFloat(0f, 0f)
        val kfAlpha60 = Keyframe.ofFloat(0.6f, 1f)
        val kfAlpha100 = Keyframe.ofFloat(1f, 1f)
        val pvhAlpha = PropertyValuesHolder.ofKeyframe(View.ALPHA, kfAlpha0, kfAlpha60, kfAlpha100)

        // Keyframes for ScaleX (0% -> 0.45, 60% -> 1.05, 100% -> 1.0)
        val kfScaleX0 = Keyframe.ofFloat(0f, 0.45f)
        val kfScaleX60 = Keyframe.ofFloat(0.6f, 1.05f)
        val kfScaleX100 = Keyframe.ofFloat(1f, 1.0f)
        val pvhScaleX = PropertyValuesHolder.ofKeyframe(View.SCALE_X, kfScaleX0, kfScaleX60, kfScaleX100)

        // Keyframes for ScaleY (0% -> 0.45, 60% -> 1.05, 100% -> 1.0)
        val kfScaleY0 = Keyframe.ofFloat(0f, 0.45f)
        val kfScaleY60 = Keyframe.ofFloat(0.6f, 1.05f)
        val kfScaleY100 = Keyframe.ofFloat(1f, 1.0f)
        val pvhScaleY = PropertyValuesHolder.ofKeyframe(View.SCALE_Y, kfScaleY0, kfScaleY60, kfScaleY100)

        // Keyframes for TranslationY (0% -> 32dp, 60% -> -4dp, 100% -> 0dp)
        val kfTransY0 = Keyframe.ofFloat(0f, slideDistancePx)
        val kfTransY60 = Keyframe.ofFloat(0.6f, overshootPx)
        val kfTransY100 = Keyframe.ofFloat(1f, 0f)
        val pvhTransY = PropertyValuesHolder.ofKeyframe(View.TRANSLATION_Y, kfTransY0, kfTransY60, kfTransY100)

        logoAnimator = ObjectAnimator.ofPropertyValuesHolder(logoCard, pvhAlpha, pvhScaleX, pvhScaleY, pvhTransY).apply {
            duration = 1000
            startDelay = 100
            interpolator = cubicBezier
            start()
        }
    }

    private fun setupTitleAndTaglineAnimations(tvTitle: TextView?, tvTagline: TextView?) {
        val slideDistancePx = 16f * resources.displayMetrics.density
        val cubicBezier = PathInterpolator(0.16f, 1.0f, 0.3f, 1.0f)

        if (tvTitle != null) {
            tvTitle.alpha = 0.0f
            tvTitle.translationY = slideDistancePx

            val pvhAlpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.0f, 1.0f)
            val pvhTransY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, slideDistancePx, 0.0f)

            titleAnimator = ObjectAnimator.ofPropertyValuesHolder(tvTitle, pvhAlpha, pvhTransY).apply {
                duration = 750
                startDelay = 600
                interpolator = cubicBezier
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
                startDelay = 800
                interpolator = cubicBezier
                start()
            }
        }
    }

    private fun setupProgressBarAndFooterAnimations(progressBar: ProgressBar?, tvFooter: TextView?) {
        val slideDistancePx = 16f * resources.displayMetrics.density
        val cubicBezier = PathInterpolator(0.16f, 1.0f, 0.3f, 1.0f)
        val progressInterpolator = PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f)

        if (progressBar != null) {
            progressBar.alpha = 0.0f
            progressBar.translationY = slideDistancePx
            progressBar.max = 100
            progressBar.progress = 0

            val pvhAlpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.0f, 1.0f)
            val pvhTransY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, slideDistancePx, 0.0f)

            ObjectAnimator.ofPropertyValuesHolder(progressBar, pvhAlpha, pvhTransY).apply {
                duration = 600
                startDelay = 1000
                interpolator = cubicBezier
                start()
            }

            progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", 0, 100).apply {
                duration = 1000
                startDelay = 400
                interpolator = progressInterpolator
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
                interpolator = cubicBezier
                start()
            }
        }
    }

    private fun startPulseAnimation() {
        val pulseRingA = findViewById<View>(R.id.pulseRingA) ?: return
        val pulseRingB = findViewById<View>(R.id.pulseRingB) ?: return

        pulseRingAnimatorA = createPulseRingAnimator(pulseRingA, 0)
        pulseRingAnimatorB = createPulseRingAnimator(pulseRingB, 1400)
    }

    private fun createPulseRingAnimator(view: View, delay: Long): ObjectAnimator {
        val pvhScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 2.2f)
        val pvhScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 2.2f)
        val pvhAlpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.7f, 0.0f)

        return ObjectAnimator.ofPropertyValuesHolder(view, pvhScaleX, pvhScaleY, pvhAlpha).apply {
            duration = 2800
            startDelay = delay
            interpolator = DecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!isFinishing && !isDestroyed) {
                        startDelay = 0
                        start()
                    }
                }
            })
            start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(navigateRunnable)
        pulseRingAnimatorA?.cancel()
        pulseRingAnimatorB?.cancel()
        pulseAnimator1?.cancel()
        pulseAnimator2?.cancel()
        logoAnimator?.cancel()
        titleAnimator?.cancel()
        taglineAnimator?.cancel()
        progressAnimator?.cancel()
        footerAnimator?.cancel()
    }
}
