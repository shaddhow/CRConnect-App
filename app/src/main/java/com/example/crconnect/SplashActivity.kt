package com.example.crconnect

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.Configuration
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
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var pulseRingAnimatorA: ObjectAnimator? = null
    private var pulseRingAnimatorB: ObjectAnimator? = null
    private var logoAnimator: ObjectAnimator? = null
    private var titleAnimator: ObjectAnimator? = null
    private var taglineAnimator: ObjectAnimator? = null
    private var progressAnimator: ObjectAnimator? = null
    private var progressFadeAnimator: ObjectAnimator? = null
    private var footerAnimator: ObjectAnimator? = null
    private var exitAnimator: ObjectAnimator? = null

    private val fadeOutAndNavigateRunnable = Runnable {
        if (!isFinishing && !isDestroyed) {
            fadeOutSplashContent {
                performNavigation()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

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
                logoCard = logoCard,
                tvTitle = tvSplashTitle,
                tvTagline = tvSplashTagline,
                progressBar = progressBar,
                tvFooter = tvSplashFooter,
            )
        } else {
            startPulseAnimation()
            setupLogoAnimation(logoCard)
            setupTitleAndTaglineAnimations(tvSplashTitle, tvSplashTagline)
            setupProgressBarAndFooterAnimations(progressBar, tvSplashFooter)

            // Triggers smooth content fade-out at 1180ms
            handler.postDelayed(fadeOutAndNavigateRunnable, 1180)
        }
    }

    private fun performNavigation() {
        if (isFinishing || isDestroyed) return

        pulseRingAnimatorA?.cancel()
        pulseRingAnimatorB?.cancel()

        val sessionManager = SessionManager(this@SplashActivity)
        val firebaseUser = FirebaseAuth.getInstance().currentUser

        val targetClass = if (sessionManager.isLoggedIn && firebaseUser != null) {
            val role = sessionManager.userRole
            if ("Teacher".equals(role, ignoreCase = true)) {
                TeacherDashboardActivity::class.java
            } else {
                StudentDashboardActivity::class.java
            }
        } else {
            LoginActivity::class.java
        }

        val intent = Intent(this@SplashActivity, targetClass)
        startActivity(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                R.anim.fade_in,
                R.anim.fade_out,
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
        finish()
    }

    private fun fadeOutSplashContent(onComplete: () -> Unit) {
        val layoutContent = findViewById<View>(R.id.layoutSplashContent)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvFooter = findViewById<TextView>(R.id.tvSplashFooter)

        layoutContent?.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        progressBar?.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        tvFooter?.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        exitAnimator = ObjectAnimator.ofFloat(layoutContent, View.ALPHA, 1.0f, 0.0f).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                val alphaVal = anim.animatedValue as Float
                progressBar?.alpha = alphaVal
                tvFooter?.alpha = alphaVal
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete()
                }
            })
            start()
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
            alpha = 1.0f
            translationY = 0f
        }
        tvFooter?.apply {
            alpha = 1.0f
            translationY = 0f
        }

        performNavigation()
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

            // Cache linear gradient shaders to prevent GC allocations on every animation frame
            val crShader = LinearGradient(
                0f, 0f, crWidth, 0f,
                intArrayOf(crStartColor, crEndColor),
                null, Shader.TileMode.CLAMP,
            )
            val connectShader = LinearGradient(
                crWidth, 0f, totalWidth, 0f,
                intArrayOf(connectStartColor, connectEndColor),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP,
            )

            val spannable = SpannableString(appName)

            val crSpan = object : CharacterStyle(), UpdateAppearance {
                override fun updateDrawState(tp: TextPaint) {
                    tp.shader = crShader
                }
            }

            val connectSpan = object : CharacterStyle(), UpdateAppearance {
                override fun updateDrawState(tp: TextPaint) {
                    tp.shader = connectShader
                }
            }

            spannable.setSpan(crSpan, 0, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(connectSpan, 2, appName.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            tvTitle.text = spannable
        }
    }

    private fun startPulseAnimation() {
        val pulseRingA = findViewById<View>(R.id.pulseRingA) ?: return
        val pulseRingB = findViewById<View>(R.id.pulseRingB) ?: return

        pulseRingA.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        pulseRingB.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        pulseRingAnimatorA = createPulseRingAnimator(pulseRingA, 0)
        pulseRingAnimatorB = createPulseRingAnimator(pulseRingB, 1200)
    }

    private fun createPulseRingAnimator(view: View, delay: Long): ObjectAnimator {
        val pvhScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 2.0f)
        val pvhScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 2.0f)
        val pvhAlpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.65f, 0.0f)

        return ObjectAnimator.ofPropertyValuesHolder(view, pvhScaleX, pvhScaleY, pvhAlpha).apply {
            duration = 2400
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

    private fun setupLogoAnimation(logoCard: View?) {
        logoCard ?: return

        logoCard.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val slideDistancePx = 28f * resources.displayMetrics.density
        val overshootPx = -3f * resources.displayMetrics.density
        val cubicBezier = PathInterpolator(0.16f, 1.0f, 0.3f, 1.0f)

        val kfAlpha0 = Keyframe.ofFloat(0f, 0f)
        val kfAlpha60 = Keyframe.ofFloat(0.6f, 1f)
        val kfAlpha100 = Keyframe.ofFloat(1f, 1f)
        val pvhAlpha = PropertyValuesHolder.ofKeyframe(View.ALPHA, kfAlpha0, kfAlpha60, kfAlpha100)

        val kfScaleX0 = Keyframe.ofFloat(0f, 0.5f)
        val kfScaleX60 = Keyframe.ofFloat(0.6f, 1.04f)
        val kfScaleX100 = Keyframe.ofFloat(1f, 1.0f)
        val pvhScaleX = PropertyValuesHolder.ofKeyframe(View.SCALE_X, kfScaleX0, kfScaleX60, kfScaleX100)

        val kfScaleY0 = Keyframe.ofFloat(0f, 0.5f)
        val kfScaleY60 = Keyframe.ofFloat(0.6f, 1.04f)
        val kfScaleY100 = Keyframe.ofFloat(1f, 1.0f)
        val pvhScaleY = PropertyValuesHolder.ofKeyframe(View.SCALE_Y, kfScaleY0, kfScaleY60, kfScaleY100)

        val kfTransY0 = Keyframe.ofFloat(0f, slideDistancePx)
        val kfTransY60 = Keyframe.ofFloat(0.6f, overshootPx)
        val kfTransY100 = Keyframe.ofFloat(1f, 0f)
        val pvhTransY = PropertyValuesHolder.ofKeyframe(View.TRANSLATION_Y, kfTransY0, kfTransY60, kfTransY100)

        logoAnimator = ObjectAnimator.ofPropertyValuesHolder(logoCard, pvhAlpha, pvhScaleX, pvhScaleY, pvhTransY).apply {
            duration = 500
            startDelay = 50
            interpolator = cubicBezier
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    logoCard.setLayerType(View.LAYER_TYPE_NONE, null)
                }
            })
            start()
        }
    }

    private fun setupTitleAndTaglineAnimations(tvTitle: TextView?, tvTagline: TextView?) {
        val slideDistancePx = 14f * resources.displayMetrics.density
        val cubicBezier = PathInterpolator(0.16f, 1.0f, 0.3f, 1.0f)

        if (tvTitle != null) {
            tvTitle.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            tvTitle.alpha = 0.0f
            tvTitle.translationY = slideDistancePx

            val pvhAlpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.0f, 1.0f)
            val pvhTransY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, slideDistancePx, 0.0f)

            titleAnimator = ObjectAnimator.ofPropertyValuesHolder(tvTitle, pvhAlpha, pvhTransY).apply {
                duration = 400
                startDelay = 180
                interpolator = cubicBezier
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        tvTitle.setLayerType(View.LAYER_TYPE_NONE, null)
                    }
                })
                start()
            }
        }

        if (tvTagline != null) {
            tvTagline.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            tvTagline.alpha = 0.0f
            tvTagline.translationY = slideDistancePx

            val pvhAlpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.0f, 1.0f)
            val pvhTransY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, slideDistancePx, 0.0f)

            taglineAnimator = ObjectAnimator.ofPropertyValuesHolder(tvTagline, pvhAlpha, pvhTransY).apply {
                duration = 350
                startDelay = 280
                interpolator = cubicBezier
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        tvTagline.setLayerType(View.LAYER_TYPE_NONE, null)
                    }
                })
                start()
            }
        }
    }

    private fun setupProgressBarAndFooterAnimations(progressBar: ProgressBar?, tvFooter: TextView?) {
        val slideDistancePx = 12f * resources.displayMetrics.density
        val cubicBezier = PathInterpolator(0.16f, 1.0f, 0.3f, 1.0f)
        val progressInterpolator = DecelerateInterpolator(1.1f)

        if (progressBar != null) {
            progressBar.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            progressBar.alpha = 0.0f
            progressBar.translationY = slideDistancePx
            progressBar.max = 100
            progressBar.progress = 0

            val pvhAlpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.0f, 1.0f)
            val pvhTransY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, slideDistancePx, 0.0f)

            progressFadeAnimator = ObjectAnimator.ofPropertyValuesHolder(progressBar, pvhAlpha, pvhTransY).apply {
                duration = 300
                startDelay = 220
                interpolator = cubicBezier
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        progressBar.setLayerType(View.LAYER_TYPE_NONE, null)
                    }
                })
                start()
            }

            progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", 0, 100).apply {
                duration = 900
                startDelay = 220
                interpolator = progressInterpolator
                start()
            }
        }

        if (tvFooter != null) {
            tvFooter.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            tvFooter.alpha = 0.0f
            tvFooter.translationY = slideDistancePx

            val pvhAlpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.0f, 1.0f)
            val pvhTransY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, slideDistancePx, 0.0f)

            footerAnimator = ObjectAnimator.ofPropertyValuesHolder(tvFooter, pvhAlpha, pvhTransY).apply {
                duration = 350
                startDelay = 380
                interpolator = cubicBezier
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        tvFooter.setLayerType(View.LAYER_TYPE_NONE, null)
                    }
                })
                start()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(fadeOutAndNavigateRunnable)
        pulseRingAnimatorA?.cancel()
        pulseRingAnimatorB?.cancel()
        logoAnimator?.cancel()
        titleAnimator?.cancel()
        taglineAnimator?.cancel()
        progressAnimator?.cancel()
        progressFadeAnimator?.cancel()
        footerAnimator?.cancel()
        exitAnimator?.cancel()
    }
}
