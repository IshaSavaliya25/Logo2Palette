package com.example.logo2palette

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.logo2palette.utils.UserSessionManager

class LoginActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var registerLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        // Ensure screenshot and screen recording are allowed
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setRecentsScreenshotEnabled(true)
        }

        // Check if user is already logged in
        if (UserSessionManager.isLoggedIn(this)) {
            navigateToMain()
            return
        }

        setContentView(R.layout.activity_login)

        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        passwordInput.transformationMethod = PasswordTransformationMethod.getInstance()
        loginButton = findViewById(R.id.loginButton)
        registerLink = findViewById(R.id.registerLink)

        loginButton.setOnClickListener {
            performLogin()
        }

        registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun performLogin() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter your email and password", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        UserSessionManager.login(this, email, password) { result ->
            setLoading(false)
            result.onSuccess { user ->
                Toast.makeText(this, "Welcome back, ${user.name}! 👋", Toast.LENGTH_SHORT).show()
                navigateToMain()
            }.onFailure { ex ->
                val rawMsg = ex.message ?: ""
                val friendlyMsg = when {
                    rawMsg.contains("credential", ignoreCase = true) || 
                    rawMsg.contains("password", ignoreCase = true) || 
                    rawMsg.contains("user", ignoreCase = true) ||
                    rawMsg.contains("malformed", ignoreCase = true) ||
                    rawMsg.contains("expired", ignoreCase = true) ->
                        "Invalid credentials. If you haven't registered yet, tap 'Sign Up' below."
                    rawMsg.contains("network", ignoreCase = true) ->
                        "Network error. Please check your internet connection."
                    else -> rawMsg.ifBlank { "Authentication failed. Please try again." }
                }
                Toast.makeText(this, friendlyMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        loginButton.isEnabled = !loading
        loginButton.text = if (loading) "Authenticating..." else "Sign In"
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
