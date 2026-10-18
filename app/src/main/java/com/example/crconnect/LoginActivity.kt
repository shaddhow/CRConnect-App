package com.example.crconnect

import android.content.Context
import android.content.Intent
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
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.crconnect.core.utils.HapticUtils
import com.example.crconnect.core.utils.KeyboardUtils
import com.example.crconnect.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    private var selectedRole = "Student" // "Student" or "Teacher"
    private var isPasswordVisible = false

    private var isIdErrorState = false
    private var isPasswordErrorState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // 1. Full-screen Edge-to-Edge and Light Status/Navigation Bars
        setupWindowDecorations()

        // 2. Programmatic vertical gradient background (70D6FF -> C2EFFF -> #FFFFFF)
        setupGradientBackground()

        // 3. Brand Header Gradient Text (CRConnect: #0F172A -> #0B3A9E)
        setupHeaderGradientText()

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
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController?.isAppearanceLightStatusBars = true
        insetsController?.isAppearanceLightNavigationBars = true

        KeyboardUtils.setupSmoothKeyboardHandling(this, binding.scrollViewLogin)
    }

    private fun setupGradientBackground() {
        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.parseColor("#70D6FF"),
                Color.parseColor("#C2EFFF"),
                Color.parseColor("#FFFFFF")
            )
        )
        binding.root.background = gradientDrawable
    }

    private fun setupHeaderGradientText() {
        binding.tvAppTitle.post {
            val paint = binding.tvAppTitle.paint
            val width = paint.measureText(binding.tvAppTitle.text.toString())
            if (width > 0f) {
                val shader = LinearGradient(
                    0f, 0f, width, 0f,
                    intArrayOf(Color.parseColor("#0F172A"), Color.parseColor("#0B3A9E")),
                    null,
                    Shader.TileMode.CLAMP
                )
                paint.shader = shader
                binding.tvAppTitle.invalidate()
            }
        }
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
    }

    private fun updateRoleTabUi() {
        clearIdError()
        clearPasswordError()

        if (selectedRole == "Student") {
            binding.tabStudent.setBackgroundResource(R.drawable.bg_segmented_tab_active)
            binding.tabStudent.setTextColor(ContextCompat.getColor(this, R.color.brand_blue))

            binding.tabTeacher.setBackgroundResource(R.drawable.bg_segmented_tab_inactive)
            binding.tabTeacher.setTextColor(ContextCompat.getColor(this, R.color.slate_muted))

            binding.tvIdLabel.setText(R.string.student_id_or_email)
            binding.etIdInput.setHint(R.string.id_hint_student)
        } else {
            binding.tabTeacher.setBackgroundResource(R.drawable.bg_segmented_tab_active)
            binding.tabTeacher.setTextColor(ContextCompat.getColor(this, R.color.brand_blue))

            binding.tabStudent.setBackgroundResource(R.drawable.bg_segmented_tab_inactive)
            binding.tabStudent.setTextColor(ContextCompat.getColor(this, R.color.slate_muted))

            binding.tvIdLabel.setText(R.string.teacher_id_or_email)
            binding.etIdInput.setHint(R.string.id_hint_teacher)
        }
    }

    private fun setupInputFieldHandlers() {
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
        // Sign in button touch feedback & click
        setupButtonScaleTouchFeedback(binding.btnSignIn)
        binding.btnSignIn.setOnClickListener {
            validateAndSignIn()
        }

        // Forgot password link
        binding.tvForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Create account button touch feedback & click
        setupButtonScaleTouchFeedback(binding.btnCreateAccount)
        binding.btnCreateAccount.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Developer bypass options
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
            val intent = Intent(this, StudentDashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        binding.btnDevTeacher.setOnClickListener {
            sessionManager.createLoginSession(
                "Test Teacher (Dev)",
                "teacher@bubt.edu.bd",
                "T1001",
                "Teacher/Admin",
                "--",
                "6",
                "--",
                "Department of CSE, BUBT"
            )
            val intent = Intent(this, TeacherDashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
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

    private fun performFirebaseSignIn(emailOrId: String, password: String) {
        binding.btnSignIn.isEnabled = false

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
                    binding.btnSignIn.isEnabled = false
                    return@addOnSuccessListener
                }

                user.reload().addOnCompleteListener { reloadTask ->
                    val refreshedUser = FirebaseAuth.getInstance().currentUser
                    if (refreshedUser == null) {
                        FirebaseAuth.getInstance().signOut()
                        sessionManager.logoutUser()
                        binding.btnSignIn.isEnabled = true
                        return@addOnCompleteListener
                    }

                    if (!refreshedUser.isEmailVerified) {
                        FirebaseAuth.getInstance().signOut()
                        sessionManager.logoutUser()
                        binding.btnSignIn.isEnabled = true
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
                binding.btnSignIn.isEnabled = false
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

    private fun saveSessionAndNavigate(snapshot: DataSnapshot) {
        val name = snapshot.child("name").getValue(String::class.java) ?: "User"
        val email = snapshot.child("email").getValue(String::class.java) ?: ""
        val id = snapshot.child("id").getValue(String::class.java) ?: ""
        val role = snapshot.child("role").getValue(String::class.java) ?: "Student"
        val section = snapshot.child("section").getValue(String::class.java) ?: "--"
        val assignedSection = snapshot.child("assignedSection").getValue(String::class.java) ?: section
        val intake = snapshot.child("intake").getValue(String::class.java) ?: "--"
        val dept = snapshot.child("dept").getValue(String::class.java) ?: "Department of CSE, BUBT"

        sessionManager.createLoginSession(name, email, id, role, section, assignedSection, intake, dept)
        navigateBasedOnRole(role, email)
    }

    private fun createDefaultTeacherAndNavigate(uid: String, email: String, originalInput: String) {
        val name = if (email.contains("@")) email.split("@")[0] else "Teacher"
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

    private fun navigateBasedOnRole(role: String?, email: String?) {
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
                    v.clearFocus()
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}