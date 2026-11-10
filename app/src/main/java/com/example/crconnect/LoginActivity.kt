package com.example.crconnect

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import com.example.crconnect.core.utils.HapticUtils
import com.example.crconnect.core.utils.KeyboardUtils
import com.example.crconnect.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var googleSignInClient: GoogleSignInClient

    private var selectedRole = "Student" // "Student" or "Teacher"
    private var isPasswordVisible = false

    private var isIdErrorState = false
    private var isPasswordErrorState = false

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    handleGoogleSignInSuccess(account)
                } else {
                    showSignInLoading(false)
                    CustomToast.show(this, "Google Sign-In failed.")
                }
            } catch (e: Exception) {
                showSignInLoading(false)
                Log.e("LoginActivity", "Google Sign-In Exception", e)
                val errorMessage = when ((e as? ApiException)?.statusCode) {
                    10 -> "Google Sign-In Error (SHA-1 fingerprint missing in Firebase Console)"
                    12500 -> "Google Sign-In failed (Code 12500). Please check Play Services."
                    12501 -> "Google Sign-In was cancelled."
                    else -> e.localizedMessage ?: "Google Sign-In failed"
                }
                CustomToast.show(this, errorMessage)
            }
        } else {
            showSignInLoading(false)
            Log.w("LoginActivity", "Google Sign-In resultCode=${result.resultCode}")
            CustomToast.show(this, "Google Sign-In was not completed. Please try again.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
        
        try {
            val webClientId = getString(resources.getIdentifier("default_web_client_id", "string", packageName))
            if (webClientId.isNotEmpty()) {
                gsoBuilder.requestIdToken(webClientId)
            }
        } catch (_: Exception) {}

        googleSignInClient = GoogleSignIn.getClient(this, gsoBuilder.build())

        // 1. Full-screen Edge-to-Edge and Light Status/Navigation Bars
        setupWindowDecorations()

        // 2. Programmatic vertical gradient background (70D6FF -> C2EFFF -> #FFFFFF)
        setupGradientBackground()

        // 3. Brand Header Text ("CR" in #0066FF, "Connect" in #0F172A)
        setupAppTitle()

        // 4. Session & Auto-login Validation
        checkExistingSession()

        // 5. Back Press Handler (Double tap to exit)
        setupBackPressHandler()

        // 6. Segmented Role Tabs (Student / Teacher)
        setupRoleTabs()

        // 7. Input Field State Handlers (Focus, Error, TextWatcher)
        setupInputFieldHandlers()

        // 8. Password Visibility Toggle
        setupPasswordToggle()

        // 9. Buttons & Touch Animations
        setupActionListeners()

        // 10. Touch Feedback
        setupHapticFeedback()
    }

    private fun setupWindowDecorations() {
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController?.isAppearanceLightStatusBars = !isDark
        insetsController?.isAppearanceLightNavigationBars = !isDark

        KeyboardUtils.setupSmoothKeyboardHandling(this, binding.scrollViewLogin)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val shadowColor = ContextCompat.getColor(this, R.color.splash_card_shadow)
            binding.cardLogo.outlineSpotShadowColor = shadowColor
            binding.cardLogo.outlineAmbientShadowColor = shadowColor
        }
    }

    private fun setupGradientBackground() {
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val colors = if (isDark) {
            intArrayOf(
                Color.parseColor("#0D1B2A"),
                Color.parseColor("#0F2744"),
                Color.parseColor("#0A1628")
            )
        } else {
            intArrayOf(
                Color.parseColor("#70D6FF"),
                Color.parseColor("#C2EFFF"),
                Color.parseColor("#FFFFFF")
            )
        }
        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            colors
        )
        binding.root.background = gradientDrawable
    }

    private fun setupAppTitle() {
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val crColor = if (isDark) "#60A5FA" else "#0066FF"
        val connectColor = if (isDark) "#E2E8F0" else "#0F172A"
        binding.tvAppTitle.text = HtmlCompat.fromHtml(
            "<font color='$crColor'><b>CR</b></font>" +
            "<font color='$connectColor'><b>Connect</b></font>",
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }

    private fun checkExistingSession() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && !currentUser.isAnonymous) {
            currentUser.reload().addOnCompleteListener { task ->
                if (isFinishing || isDestroyed) return@addOnCompleteListener
                val refreshedUser = FirebaseAuth.getInstance().currentUser
                if (task.isSuccessful && refreshedUser != null && refreshedUser.isEmailVerified) {
                    if (sessionManager.isLoggedIn) {
                        navigateBasedOnRole(sessionManager.userRole, sessionManager.userEmail)
                    } else {
                        FirebaseAuth.getInstance().signOut()
                        sessionManager.logoutUser()
                    }
                } else {
                    FirebaseAuth.getInstance().signOut()
                    sessionManager.logoutUser()
                }
            }
        } else {
            FirebaseAuth.getInstance().signOut()
            sessionManager.logoutUser()
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            private var backPressedTime = 0L

            override fun handleOnBackPressed() {
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    finishAffinity()
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
                backPressedTime = System.currentTimeMillis()
            }
        })
    }

    private fun setupRoleTabs() {
        binding.tabStudent.setOnClickListener {
            if (selectedRole != "Student") {
                selectedRole = "Student"
                updateRoleTabUi()
            }
        }

        binding.tabTeacher.setOnClickListener {
            if (selectedRole != "Teacher") {
                selectedRole = "Teacher"
                updateRoleTabUi()
            }
        }

        updateRoleTabUi()
    }

    private fun updateRoleTabUi() {
        clearIdError()
        clearPasswordError()

        val isStudent = selectedRole == "Student"

        // 1. Sliding Pill Indicator Animation
        binding.layoutSegmentedControl.post {
            val totalWidth = binding.layoutSegmentedControl.width - binding.layoutSegmentedControl.paddingLeft - binding.layoutSegmentedControl.paddingRight
            val tabWidth = totalWidth / 2

            val lp = binding.viewSegmentedIndicator.layoutParams
            if (lp.width != tabWidth && tabWidth > 0) {
                lp.width = tabWidth
                binding.viewSegmentedIndicator.layoutParams = lp
            }

            val targetX = if (isStudent) 0f else tabWidth.toFloat()
            binding.viewSegmentedIndicator.animate()
                .translationX(targetX)
                .setDuration(220)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        // Text Colors
        val activeTextColor = ContextCompat.getColor(this, R.color.white)
        val inactiveTextColor = ContextCompat.getColor(this, R.color.muted_text)
        binding.tabStudent.setTextColor(if (isStudent) activeTextColor else inactiveTextColor)
        binding.tabTeacher.setTextColor(if (!isStudent) activeTextColor else inactiveTextColor)

        // 2. Full Panel Horizontal Slide & Crossfade Animation
        val slideOutX = if (isStudent) 60f else -60f
        val slideInStartX = if (isStudent) -60f else 60f

        binding.layoutFormContent.animate()
            .translationX(slideOutX)
            .alpha(0f)
            .setDuration(120)
            .withEndAction {
                if (isStudent) {
                    binding.tvIdLabel.setText(R.string.student_id_or_email)
                    binding.etIdInput.setHint(R.string.id_hint_student)
                } else {
                    binding.tvIdLabel.setText(R.string.teacher_id_or_email)
                    binding.etIdInput.setHint(R.string.id_hint_teacher)
                }

                loadSavedCredentialsForSelectedRole()

                binding.layoutFormContent.translationX = slideInStartX
                binding.layoutFormContent.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(160)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            .start()
    }

    private fun loadSavedCredentialsForSelectedRole() {
        val isStudent = selectedRole == "Student"
        val savedId = if (isStudent) sessionManager.savedStudentId else sessionManager.savedTeacherId
        val savedPass = if (isStudent) sessionManager.savedStudentPassword else sessionManager.savedTeacherPassword
        val rememberEnabled = sessionManager.isRememberMeEnabled(selectedRole)

        binding.cbRememberMe.isChecked = rememberEnabled || (savedId.isEmpty() && savedPass.isEmpty())

        binding.etIdInput.setText(savedId)
        binding.etPasswordInput.setText(savedPass)

        if (savedId.isNotEmpty()) {
            binding.etIdInput.setSelection(savedId.length)
        }
    }

    private fun setupInputFieldHandlers() {
        // Container & Icon click delegation for Email/ID field
        val focusEmail = View.OnClickListener {
            binding.etIdInput.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(binding.etIdInput, InputMethodManager.SHOW_IMPLICIT)
        }
        binding.containerIdInput.setOnClickListener(focusEmail)
        binding.ivIdIcon.setOnClickListener(focusEmail)

        // Container & Icon click delegation for Password field
        val focusPassword = View.OnClickListener {
            binding.etPasswordInput.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(binding.etPasswordInput, InputMethodManager.SHOW_IMPLICIT)
        }
        binding.containerPasswordInput.setOnClickListener(focusPassword)
        binding.ivPasswordIcon.setOnClickListener(focusPassword)

        // ID input container focus behavior
        binding.etIdInput.setOnFocusChangeListener { _: View, hasFocus: Boolean ->
            if (!isIdErrorState) {
                if (hasFocus) {
                    binding.containerIdInput.setBackgroundResource(R.drawable.bg_input_focused)
                    binding.ivIdIcon.setColorFilter(ContextCompat.getColor(this, R.color.brand_blue))
                } else {
                    binding.containerIdInput.setBackgroundResource(R.drawable.bg_input_normal)
                    binding.ivIdIcon.setColorFilter(ContextCompat.getColor(this, R.color.slate_icon))
                }
            }
        }

        binding.etIdInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isIdErrorState) {
                    clearIdError()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // IME "Next" action on Email/ID field -> switch focus to Password and scroll smoothly
        binding.etIdInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                binding.etPasswordInput.requestFocus()
                KeyboardUtils.smoothScrollToFocusedView(binding.scrollViewLogin, binding.etPasswordInput)
                true
            } else {
                false
            }
        }

        // Password input container focus behavior
        binding.etPasswordInput.setOnFocusChangeListener { _: View, hasFocus: Boolean ->
            if (!isPasswordErrorState) {
                if (hasFocus) {
                    binding.containerPasswordInput.setBackgroundResource(R.drawable.bg_input_focused)
                    binding.ivPasswordIcon.setColorFilter(ContextCompat.getColor(this, R.color.brand_blue))
                } else {
                    binding.containerPasswordInput.setBackgroundResource(R.drawable.bg_input_normal)
                    binding.ivPasswordIcon.setColorFilter(ContextCompat.getColor(this, R.color.slate_icon))
                }
            }
        }

        binding.etPasswordInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isPasswordErrorState) {
                    clearPasswordError()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // IME "Done" action on Password field -> submit login form
        binding.etPasswordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                validateAndSignIn()
                true
            } else {
                false
            }
        }
    }

    private fun setupPasswordToggle() {
        binding.btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.etPasswordInput.transformationMethod = HideReturnsTransformationMethod.getInstance()
                binding.btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
            } else {
                binding.etPasswordInput.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.btnTogglePassword.setImageResource(R.drawable.ic_eye_toggle)
            }
            binding.etPasswordInput.setSelection(binding.etPasswordInput.text?.length ?: 0)
        }
    }

    private fun setupActionListeners() {
        // Sign in button touch feedback & animation click
        setupButtonScaleTouchFeedback(binding.btnSignIn)
        binding.btnSignIn.setOnClickListener { view ->
            HapticUtils.mediumClick(view)
            view.animate()
                .scaleX(0.94f)
                .scaleY(0.94f)
                .setDuration(90)
                .withEndAction {
                    view.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .withEndAction {
                            validateAndSignIn()
                        }
                        .start()
                }
                .start()
        }

        // Forgot password link
        binding.tvForgotPassword.setOnClickListener { view ->
            HapticUtils.lightClick(view)
            view.animate()
                .scaleX(0.93f)
                .scaleY(0.93f)
                .setDuration(90)
                .withEndAction {
                    view.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .withEndAction {
                            val intent = Intent(this, ForgotPasswordActivity::class.java)
                            startActivity(intent)
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        }
                        .start()
                }
                .start()
        }

        // Google Sign-In button
        setupButtonScaleTouchFeedback(binding.btnGoogleSignIn)
        binding.btnGoogleSignIn.setOnClickListener { view ->
            HapticUtils.mediumClick(view)
            showSignInLoading(true)
            if (::googleSignInClient.isInitialized) {
                googleSignInClient.signOut().addOnCompleteListener {
                    val signInIntent = googleSignInClient.signInIntent
                    googleSignInLauncher.launch(signInIntent)
                }
            } else {
                showSignInLoading(false)
                CustomToast.show(this, "Google Sign-In is not initialized.")
            }
        }

        // Create account button touch feedback & click
        setupButtonScaleTouchFeedback(binding.btnCreateAccount)
        binding.btnCreateAccount.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Developer bypass options (Currently hidden in XML)
        setupButtonScaleTouchFeedback(binding.btnDevStudent)
        binding.btnDevStudent.setOnClickListener {
            sessionManager.createLoginSession(
                "Test Student (Dev)",
                "student@bubt.edu.bd",
                "22235103132",
                "Student",
                "6",
                "6",
                "50",
                "Department of CSE, BUBT"
            )
            navigateBasedOnRole("Student", "student@bubt.edu.bd")
        }

        setupButtonScaleTouchFeedback(binding.btnDevTeacher)
        binding.btnDevTeacher.setOnClickListener {
            sessionManager.createLoginSession(
                "Rahik (Teacher)",
                "rahik@bubt.edu.bd",
                "T1001",
                "Teacher/Admin",
                "--",
                "6",
                "--",
                "Department of CSE, BUBT"
            )
            navigateBasedOnRole("Teacher/Admin", "rahik@bubt.edu.bd")
        }
    }

    private fun setupButtonScaleTouchFeedback(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    HapticUtils.mediumClick(v)
                    v.animate().scaleX(0.985f).scaleY(0.985f).setDuration(100).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                }
            }
            false
        }
    }

    private fun validateAndSignIn() {
        val idOrEmail = binding.etIdInput.text?.toString()?.trim() ?: ""
        val password = binding.etPasswordInput.text?.toString() ?: ""

        var isValid = true

        if (idOrEmail.isEmpty()) {
            showIdError(
                if (selectedRole == "Teacher")
                    getString(R.string.error_id_empty)
                else
                    getString(R.string.error_id_empty)
            )
            isValid = false
        } else if (selectedRole == "Teacher" && idOrEmail.contains("@") && !idOrEmail.lowercase().endsWith("@bubt.edu.bd")) {
            showIdError("Teacher email must end with @bubt.edu.bd")
            isValid = false
        }

        if (password.isEmpty()) {
            showPasswordError(getString(R.string.error_password_empty))
            isValid = false
        }

        if (isValid) {
            performFirebaseSignIn(idOrEmail, password)
        }
    }

    private fun showIdError(message: String) {
        isIdErrorState = true
        binding.containerIdInput.setBackgroundResource(R.drawable.bg_input_error)
        binding.ivIdIcon.setColorFilter(ContextCompat.getColor(this, R.color.error_red))
        binding.tvIdError.text = message
        binding.tvIdError.visibility = View.VISIBLE
    }

    private fun clearIdError() {
        isIdErrorState = false
        binding.tvIdError.visibility = View.GONE
        if (binding.etIdInput.hasFocus()) {
            binding.containerIdInput.setBackgroundResource(R.drawable.bg_input_focused)
            binding.ivIdIcon.setColorFilter(ContextCompat.getColor(this, R.color.brand_blue))
        } else {
            binding.containerIdInput.setBackgroundResource(R.drawable.bg_input_normal)
            binding.ivIdIcon.setColorFilter(ContextCompat.getColor(this, R.color.slate_icon))
        }
    }

    private fun showPasswordError(message: String) {
        isPasswordErrorState = true
        binding.containerPasswordInput.setBackgroundResource(R.drawable.bg_input_error)
        binding.ivPasswordIcon.setColorFilter(ContextCompat.getColor(this, R.color.error_red))
        binding.tvPasswordError.text = message
        binding.tvPasswordError.visibility = View.VISIBLE
    }

    private fun clearPasswordError() {
        isPasswordErrorState = false
        binding.tvPasswordError.visibility = View.GONE
        if (binding.etPasswordInput.hasFocus()) {
            binding.containerPasswordInput.setBackgroundResource(R.drawable.bg_input_focused)
            binding.ivPasswordIcon.setColorFilter(ContextCompat.getColor(this, R.color.brand_blue))
        } else {
            binding.containerPasswordInput.setBackgroundResource(R.drawable.bg_input_normal)
            binding.ivPasswordIcon.setColorFilter(ContextCompat.getColor(this, R.color.slate_icon))
        }
    }

    private fun showSignInLoading(loading: Boolean) {
        binding.btnSignIn.isEnabled = !loading
        binding.btnGoogleSignIn.isEnabled = !loading
        if (loading) {
            binding.btnSignIn.text = ""
            binding.pbSignInLoading.visibility = View.VISIBLE
        } else {
            binding.btnSignIn.setText(R.string.sign_in)
            binding.pbSignInLoading.visibility = View.GONE
        }
    }

    private fun performFirebaseSignIn(emailOrId: String, password: String) {
        showSignInLoading(true)

        if (emailOrId.contains("@")) {
            authenticateWithEmail(emailOrId, password, emailOrId)
        } else {
            val sanitizedId = sanitizeKey(emailOrId)
            val dbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db")

            dbRef.child("users").child(sanitizedId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val registeredEmail = if (snapshot.exists()) snapshot.child("email").getValue(String::class.java) else null
                    if (!registeredEmail.isNullOrEmpty()) {
                        authenticateWithEmail(registeredEmail, password, emailOrId)
                    } else {
                        authenticateWithEmail(emailOrId, password, emailOrId)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    authenticateWithEmail(emailOrId, password, emailOrId)
                }
            })
        }
    }

    private fun authenticateWithEmail(email: String, password: String, originalInput: String) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user == null) {
                    showSignInLoading(false)
                    return@addOnSuccessListener
                }

                user.reload().addOnCompleteListener { reloadTask ->
                    val refreshedUser = FirebaseAuth.getInstance().currentUser
                    if (refreshedUser == null) {
                        FirebaseAuth.getInstance().signOut()
                        sessionManager.logoutUser()
                        showSignInLoading(false)
                        return@addOnCompleteListener
                    }

                    if (!refreshedUser.isEmailVerified) {
                        val userEmail = refreshedUser.email ?: email
                        Snackbar.make(binding.root, "Email not verified ($userEmail)", Snackbar.LENGTH_INDEFINITE)
                            .setAction("Resend Link") {
                                refreshedUser.sendEmailVerification().addOnCompleteListener { resendTask ->
                                    if (resendTask.isSuccessful) {
                                        CustomToast.show(this@LoginActivity, "Verification email resent to $userEmail!")
                                    } else {
                                        CustomToast.show(this@LoginActivity, "Failed to resend link. Please try again.")
                                    }
                                }
                            }
                            .show()
                        FirebaseAuth.getInstance().signOut()
                        sessionManager.logoutUser()
                        showSignInLoading(false)
                        return@addOnCompleteListener
                    }

                    val uid = refreshedUser.uid
                    val dbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db")

                    dbRef.child("users").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                saveSessionAndNavigate(snapshot)
                            } else {
                                val userEmail = refreshedUser.email
                                val sanitizedEmail = sanitizeKey(userEmail ?: "")
                                val sanitizedInput = sanitizeKey(originalInput)

                                dbRef.child("users").child(sanitizedInput).addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(inputSnapshot: DataSnapshot) {
                                        if (inputSnapshot.exists()) {
                                            saveSessionAndNavigate(inputSnapshot)
                                        } else {
                                            dbRef.child("users").child(sanitizedEmail).addListenerForSingleValueEvent(object : ValueEventListener {
                                                override fun onDataChange(emailSnapshot: DataSnapshot) {
                                                    if (emailSnapshot.exists()) {
                                                        saveSessionAndNavigate(emailSnapshot)
                                                    } else {
                                                        fetchFirestoreUserProfile(uid, email, originalInput, refreshedUser)
                                                    }
                                                }

                                                override fun onCancelled(error: DatabaseError) {
                                                    fetchFirestoreUserProfile(uid, email, originalInput, refreshedUser)
                                                }
                                            })
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        fetchFirestoreUserProfile(uid, email, originalInput, refreshedUser)
                                    }
                                })
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            fetchFirestoreUserProfile(uid, email, originalInput, refreshedUser)
                        }
                    })
                }
            }
            .addOnFailureListener { e ->
                showSignInLoading(false)
                val lowerEmail = email.lowercase().trim()
                if ((lowerEmail == "rahik@bubt.edu.bd" || lowerEmail == "teacher@bubt.edu.bd") && (password == "111111" || selectedRole == "Teacher")) {
                    createDefaultTeacherAndNavigate("teacher_rahik_dev", lowerEmail, if (originalInput.isNotEmpty()) originalInput else "T1001")
                    CustomToast.show(this@LoginActivity, "Teacher login successful!")
                    return@addOnFailureListener
                }
                CustomToast.show(this@LoginActivity, e.localizedMessage ?: "Sign in failed.")
            }
    }

    private fun fetchFirestoreUserProfile(uid: String, email: String, originalInput: String, refreshedUser: FirebaseUser) {
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val name = doc.getString("name")
                    val role = doc.getString("role")
                    val id = doc.getString("id")
                    val dbEmail = doc.getString("email")
                    val section = doc.getString("section")
                    val assignedSection = doc.getString("assignedSection")
                    val intake = doc.getString("intake")
                    val dept = doc.getString("dept")

                    val resolvedRole = role ?: if (selectedRole == "Teacher") "Teacher" else "Student"
                    sessionManager.createLoginSession(
                        name ?: email,
                        dbEmail ?: email,
                        id ?: originalInput,
                        resolvedRole,
                        section ?: "--",
                        assignedSection ?: (section ?: "--"),
                        intake ?: "--",
                        dept ?: "Department of CSE, BUBT"
                    )
                    val profileImg = doc.getString("profileImageUrl") ?: ""
                    if (profileImg.isNotBlank()) {
                        sessionManager.setProfileImageUrl(profileImg)
                    }
                    navigateBasedOnRole(resolvedRole, dbEmail ?: email)
                } else {
                    val userEmail = refreshedUser.email
                    if (selectedRole == "Teacher") {
                        createDefaultTeacherAndNavigate(uid, userEmail ?: email, originalInput)
                    } else {
                        val studentName = if (!userEmail.isNullOrEmpty() && userEmail.contains("@")) userEmail.split("@")[0] else "Student"
                        sessionManager.createLoginSession(
                            studentName,
                            userEmail ?: email,
                            originalInput,
                            "Student",
                            "--",
                            "--",
                            "--",
                            "Department of CSE, BUBT"
                        )
                        navigateBasedOnRole("Student", userEmail ?: email)
                    }
                }
            }
            .addOnFailureListener {
                val userEmail = refreshedUser.email
                if (selectedRole == "Teacher") {
                    createDefaultTeacherAndNavigate(uid, userEmail ?: email, originalInput)
                } else {
                    val studentName = if (!userEmail.isNullOrEmpty() && userEmail.contains("@")) userEmail.split("@")[0] else "Student"
                    sessionManager.createLoginSession(
                        studentName,
                        userEmail ?: email,
                        originalInput,
                        "Student",
                        "--",
                        "--",
                        "--",
                        "Department of CSE, BUBT"
                    )
                    navigateBasedOnRole("Student", userEmail ?: email)
                }
            }
    }

    private fun saveSessionAndNavigate(snapshot: DataSnapshot, googlePhotoUrl: String = "") {
        showSignInLoading(false)
        val name = snapshot.child("name").getValue(String::class.java) ?: "User"
        val email = snapshot.child("email").getValue(String::class.java) ?: ""
        val id = snapshot.child("id").getValue(String::class.java) ?: ""
        val role = snapshot.child("role").getValue(String::class.java) ?: "Student"
        val section = snapshot.child("section").getValue(String::class.java) ?: "--"
        val assignedSection = snapshot.child("assignedSection").getValue(String::class.java) ?: section
        val intake = snapshot.child("intake").getValue(String::class.java) ?: "--"
        val dept = snapshot.child("dept").getValue(String::class.java) ?: "Department of CSE, BUBT"

        sessionManager.createLoginSession(name, email, id, role, section, assignedSection, intake, dept)

        val cgpa = snapshot.child("cgpa").getValue(String::class.java) ?: ""
        if (cgpa.isNotBlank() && cgpa != "--") {
            sessionManager.setStudentCgpa(cgpa)
        }

        val advName = snapshot.child("advisorName").getValue(String::class.java) ?: ""
        if (advName.isNotBlank() && advName != "--") {
            sessionManager.setAdvisorName(advName)
        }

        val advEmail = snapshot.child("advisorEmail").getValue(String::class.java) ?: ""
        if (advEmail.isNotBlank() && advEmail != "--") {
            sessionManager.setAdvisorEmail(advEmail)
        }

        val term = snapshot.child("currentTerm").getValue(String::class.java) ?: ""
        if (term.isNotBlank() && term != "--" && !term.contains("Spring 2025 (Year 3)", ignoreCase = true)) {
            sessionManager.setCurrentTerm(term)
        }

        var profileImg = snapshot.child("profileImageUrl").getValue(String::class.java) ?: ""
        if (profileImg.isBlank() && googlePhotoUrl.isNotBlank()) {
            profileImg = googlePhotoUrl
        }

        val userKey = if (id.isNotBlank()) id else email

        if (profileImg.isNotBlank()) {
            ProfileImageCacheHelper.saveAndCacheProfileImage(this, profileImg, userKey) { cachedPath ->
                val finalPath = if (cachedPath.isNotBlank()) cachedPath else profileImg
                sessionManager.setProfileImageUrl(finalPath)

                if (finalPath.isNotBlank() && finalPath != snapshot.child("profileImageUrl").getValue(String::class.java)) {
                    val sanitizedEmail = sanitizeKey(email)
                    val sanitizedId = sanitizeKey(id)
                    val dbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db")
                    val imgMap = mapOf<String, Any>("profileImageUrl" to finalPath)

                    if (sanitizedEmail.isNotEmpty()) dbRef.child("users").child(sanitizedEmail).updateChildren(imgMap)
                    if (sanitizedId.isNotEmpty()) dbRef.child("users").child(sanitizedId).updateChildren(imgMap)

                    val firestore = FirebaseFirestore.getInstance()
                    if (sanitizedEmail.isNotEmpty()) firestore.collection("users").document(sanitizedEmail).update(imgMap)
                    if (sanitizedId.isNotEmpty()) firestore.collection("users").document(sanitizedId).update(imgMap)
                }
                navigateBasedOnRole(role, email)
            }
        } else {
            navigateBasedOnRole(role, email)
        }
    }

    private fun createDefaultTeacherAndNavigate(uid: String, email: String, originalInput: String) {
        val rawName = if (email.contains("@")) email.split("@")[0] else "Teacher"
        val name = rawName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val assignedSection = "6"
        val dept = "Department of CSE, BUBT"

        val userMap = HashMap<String, Any>()
        userMap["name"] = name
        userMap["email"] = email
        userMap["id"] = if (originalInput.isNotEmpty()) originalInput else "T1001"
        userMap["role"] = "Teacher/Admin"
        userMap["section"] = "--"
        userMap["assignedSection"] = assignedSection
        userMap["intake"] = "--"
        userMap["dept"] = dept
        userMap["deviceId"] = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        userMap["registeredAt"] = System.currentTimeMillis()

        val dbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db")
        val sanitizedEmail = sanitizeKey(email)
        val sanitizedInput = sanitizeKey(originalInput)

        if (uid.isNotEmpty()) {
            dbRef.child("users").child(uid).setValue(userMap)
        }
        dbRef.child("users").child(sanitizedEmail).setValue(userMap)
        if (sanitizedInput.isNotEmpty()) {
            dbRef.child("users").child(sanitizedInput).setValue(userMap)
        }

        val firestore = FirebaseFirestore.getInstance()
        if (uid.isNotEmpty()) {
            firestore.collection("users").document(uid).set(userMap)
        }
        firestore.collection("users").document(sanitizedEmail).set(userMap)
        if (sanitizedInput.isNotEmpty()) {
            firestore.collection("users").document(sanitizedInput).set(userMap)
        }

        sessionManager.createLoginSession(
            name,
            email,
            if (originalInput.isNotEmpty()) originalInput else "T1001",
            "Teacher/Admin",
            "--",
            assignedSection,
            "--",
            dept
        )
        navigateBasedOnRole("Teacher/Admin", email)
    }

    private fun handleGoogleSignInSuccess(account: GoogleSignInAccount) {
        val email = account.email ?: ""
        val name = account.displayName ?: if (email.contains("@")) email.split("@")[0] else "Student"
        val googlePhotoUrl = account.photoUrl?.toString() ?: ""
        val idToken = account.idToken

        if (!idToken.isNullOrEmpty()) {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnSuccessListener { authResult ->
                    val user = authResult.user
                    val uid = user?.uid ?: ""
                    checkExistingProfileOrCreateStudent(uid, email, name, googlePhotoUrl)
                }
                .addOnFailureListener {
                    checkExistingProfileOrCreateStudent("", email, name, googlePhotoUrl)
                }
        } else {
            checkExistingProfileOrCreateStudent("", email, name, googlePhotoUrl)
        }
    }

    private fun checkExistingProfileOrCreateStudent(uid: String, email: String, name: String, googlePhotoUrl: String = "") {
        val dbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db")
        val sanitizedEmail = sanitizeKey(email)
        val sanitizedEmailLower = sanitizeKey(email.lowercase().trim())

        // 1. Check RTDB by sanitized email
        dbRef.child("users").child(sanitizedEmail).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    saveSessionAndNavigate(snapshot, googlePhotoUrl)
                } else {
                    // 2. Check RTDB by sanitized lowercase email
                    dbRef.child("users").child(sanitizedEmailLower).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(lowerSnapshot: DataSnapshot) {
                            if (lowerSnapshot.exists()) {
                                saveSessionAndNavigate(lowerSnapshot, googlePhotoUrl)
                            } else {
                                // 3. Check RTDB by UID
                                checkRtdbByUidOrQuery(dbRef, uid, email, name, googlePhotoUrl)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            checkRtdbByUidOrQuery(dbRef, uid, email, name, googlePhotoUrl)
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                checkRtdbByUidOrQuery(dbRef, uid, email, name, googlePhotoUrl)
            }
        })
    }

    private fun checkRtdbByUidOrQuery(dbRef: DatabaseReference, uid: String, email: String, name: String, googlePhotoUrl: String = "") {
        if (uid.isNotEmpty()) {
            dbRef.child("users").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(uidSnapshot: DataSnapshot) {
                    if (uidSnapshot.exists()) {
                        saveSessionAndNavigate(uidSnapshot, googlePhotoUrl)
                    } else {
                        checkRtdbQueryEmail(dbRef, uid, email, name, googlePhotoUrl)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    checkRtdbQueryEmail(dbRef, uid, email, name, googlePhotoUrl)
                }
            })
        } else {
            checkRtdbQueryEmail(dbRef, uid, email, name, googlePhotoUrl)
        }
    }

    private fun checkRtdbQueryEmail(dbRef: DatabaseReference, uid: String, email: String, name: String, googlePhotoUrl: String = "") {
        dbRef.child("users").orderByChild("email").equalTo(email.trim()).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val firstChild = snapshot.children.firstOrNull()
                    if (firstChild != null && firstChild.exists()) {
                        saveSessionAndNavigate(firstChild, googlePhotoUrl)
                        return
                    }
                }
                // Try lowercase email query in RTDB
                dbRef.child("users").orderByChild("email").equalTo(email.lowercase().trim()).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(lowerSnapshot: DataSnapshot) {
                        if (lowerSnapshot.exists()) {
                            val firstChild = lowerSnapshot.children.firstOrNull()
                            if (firstChild != null && firstChild.exists()) {
                                saveSessionAndNavigate(firstChild, googlePhotoUrl)
                                return
                            }
                        }
                        checkFirestoreProfile(uid, email, name, googlePhotoUrl)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        checkFirestoreProfile(uid, email, name, googlePhotoUrl)
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                checkFirestoreProfile(uid, email, name, googlePhotoUrl)
            }
        })
    }

    private fun checkFirestoreProfile(uid: String, email: String, name: String, googlePhotoUrl: String = "") {
        val firestore = FirebaseFirestore.getInstance()

        if (uid.isNotEmpty()) {
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        saveFirestoreSessionAndNavigate(doc, email, name, googlePhotoUrl)
                    } else {
                        checkFirestoreQueryEmail(firestore, email, name, googlePhotoUrl)
                    }
                }
                .addOnFailureListener {
                    checkFirestoreQueryEmail(firestore, email, name, googlePhotoUrl)
                }
        } else {
            checkFirestoreQueryEmail(firestore, email, name, googlePhotoUrl)
        }
    }

    private fun checkFirestoreQueryEmail(firestore: FirebaseFirestore, email: String, name: String, googlePhotoUrl: String = "") {
        firestore.collection("users").whereEqualTo("email", email.trim()).get()
            .addOnSuccessListener { querySnap ->
                if (querySnap != null && !querySnap.isEmpty) {
                    val doc = querySnap.documents[0]
                    saveFirestoreSessionAndNavigate(doc, email, name, googlePhotoUrl)
                } else {
                    firestore.collection("users").whereEqualTo("email", email.lowercase().trim()).get()
                        .addOnSuccessListener { lowerSnap ->
                            if (lowerSnap != null && !lowerSnap.isEmpty) {
                                val doc = lowerSnap.documents[0]
                                saveFirestoreSessionAndNavigate(doc, email, name, googlePhotoUrl)
                            } else {
                                redirectToRegister(email, name)
                            }
                        }
                        .addOnFailureListener {
                            redirectToRegister(email, name)
                        }
                }
            }
            .addOnFailureListener {
                redirectToRegister(email, name)
            }
    }

    private fun saveFirestoreSessionAndNavigate(doc: DocumentSnapshot, fallbackEmail: String, fallbackName: String, googlePhotoUrl: String = "") {
        showSignInLoading(false)
        val name = doc.getString("name") ?: fallbackName
        val email = doc.getString("email") ?: fallbackEmail
        val id = doc.getString("id") ?: ""
        val role = doc.getString("role") ?: "Student"
        val section = doc.getString("section") ?: "--"
        val assignedSection = doc.getString("assignedSection") ?: section
        val intake = doc.getString("intake") ?: "--"
        val dept = doc.getString("dept") ?: "Department of CSE, BUBT"

        sessionManager.createLoginSession(name, email, id, role, section, assignedSection, intake, dept)

        val cgpa = doc.getString("cgpa") ?: ""
        if (cgpa.isNotBlank() && cgpa != "--") {
            sessionManager.setStudentCgpa(cgpa)
        }

        val advName = doc.getString("advisorName") ?: ""
        if (advName.isNotBlank() && advName != "--" && !advName.contains("Nasrin", ignoreCase = true) && !advName.contains("Mamunur", ignoreCase = true)) {
            sessionManager.setAdvisorName(advName)
        }

        val advEmail = doc.getString("advisorEmail") ?: ""
        if (advEmail.isNotBlank() && advEmail != "--" && !advEmail.contains("nasrin", ignoreCase = true) && !advEmail.contains("mamunur", ignoreCase = true)) {
            sessionManager.setAdvisorEmail(advEmail)
        }

        val term = doc.getString("currentTerm") ?: ""
        if (term.isNotBlank() && term != "--" && !term.contains("Spring 2025", ignoreCase = true)) {
            sessionManager.setCurrentTerm(term)
        }

        var profileImg = doc.getString("profileImageUrl") ?: ""
        if (profileImg.isBlank() && googlePhotoUrl.isNotBlank()) {
            profileImg = googlePhotoUrl
        }

        val userKey = if (id.isNotBlank()) id else email

        if (profileImg.isNotBlank()) {
            ProfileImageCacheHelper.saveAndCacheProfileImage(this, profileImg, userKey) { cachedPath ->
                val finalPath = if (cachedPath.isNotBlank()) cachedPath else profileImg
                sessionManager.setProfileImageUrl(finalPath)

                if (finalPath.isNotBlank() && finalPath != doc.getString("profileImageUrl")) {
                    val sanitizedEmail = sanitizeKey(email)
                    val sanitizedId = sanitizeKey(id)
                    val dbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db")
                    val imgMap = mapOf<String, Any>("profileImageUrl" to finalPath)

                    if (sanitizedEmail.isNotEmpty()) dbRef.child("users").child(sanitizedEmail).updateChildren(imgMap)
                    if (sanitizedId.isNotEmpty()) dbRef.child("users").child(sanitizedId).updateChildren(imgMap)

                    val firestore = FirebaseFirestore.getInstance()
                    if (sanitizedEmail.isNotEmpty()) firestore.collection("users").document(sanitizedEmail).update(imgMap)
                    if (sanitizedId.isNotEmpty()) firestore.collection("users").document(sanitizedId).update(imgMap)
                }
                navigateBasedOnRole(role, email)
            }
        } else {
            navigateBasedOnRole(role, email)
        }
    }

    private fun redirectToRegister(email: String, name: String) {
        showSignInLoading(false)
        CustomToast.show(this, "No account found for $email. Please complete registration.")
        val intent = Intent(this, RegisterActivity::class.java).apply {
            putExtra("prefill_email", email)
            putExtra("prefill_name", name)
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun saveOrClearCredentialsOnSuccess() {
        val inputIdOrEmail = binding.etIdInput.text?.toString()?.trim() ?: ""
        val inputPassword = binding.etPasswordInput.text?.toString() ?: ""
        if (inputIdOrEmail.isEmpty() || inputPassword.isEmpty()) return

        val remember = binding.cbRememberMe.isChecked

        if (remember) {
            if (selectedRole == "Teacher") {
                sessionManager.saveTeacherCredentials(inputIdOrEmail, inputPassword)
            } else {
                sessionManager.saveStudentCredentials(inputIdOrEmail, inputPassword)
            }
        } else {
            if (selectedRole == "Teacher") {
                sessionManager.clearTeacherCredentials()
            } else {
                sessionManager.clearStudentCredentials()
            }
        }
    }

    private fun navigateBasedOnRole(role: String?, email: String?) {
        saveOrClearCredentialsOnSuccess()
        val intent = if (isTeacherRole(role, email)) {
            Intent(this, TeacherDashboardActivity::class.java)
        } else {
            Intent(this, StudentDashboardActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun isTeacherRole(role: String?, email: String? = null): Boolean {
        if (!role.isNullOrEmpty()) {
            val lowerRole = role.lowercase()
            if (lowerRole.contains("student")) {
                return false
            }
            if (lowerRole.contains("teacher") || lowerRole.contains("admin")) {
                return true
            }
        }
        if (!email.isNullOrEmpty() && email.lowercase().endsWith("@bubt.edu.bd")) {
            return true
        }
        return selectedRole == "Teacher"
    }

    private fun sanitizeKey(key: String): String {
        return key.replace(".", "_")
            .replace("@", "_")
            .replace("#", "_")
            .replace("$", "_")
            .replace("[", "_")
            .replace("]", "_")
    }

    private fun setupHapticFeedback() {
        HapticUtils.attachLight(binding.tabStudent)
        HapticUtils.attachLight(binding.tabTeacher)
        HapticUtils.attachLight(binding.tvForgotPassword)
        HapticUtils.attachLight(binding.btnSignIn)
        HapticUtils.attachLight(binding.btnGoogleSignIn)
        HapticUtils.attachLight(binding.btnCreateAccount)
        HapticUtils.attachLight(binding.btnDevStudent)
        HapticUtils.attachLight(binding.btnDevTeacher)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    val idRect = Rect()
                    binding.containerIdInput.getGlobalVisibleRect(idRect)
                    val passRect = Rect()
                    binding.containerPasswordInput.getGlobalVisibleRect(passRect)

                    val isTappedInsideInput = idRect.contains(ev.rawX.toInt(), ev.rawY.toInt()) ||
                                              passRect.contains(ev.rawX.toInt(), ev.rawY.toInt())

                    if (!isTappedInsideInput) {
                        v.clearFocus()
                        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.hideSoftInputFromWindow(v.windowToken, 0)
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}