package com.example.logo2palette

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.logo2palette.utils.UserSessionManager

class RegisterActivity : AppCompatActivity() {

    private lateinit var nameInput: EditText
    private lateinit var companyInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var registerButton: Button
    private lateinit var loginLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        // Ensure screenshot and screen recording are allowed
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setRecentsScreenshotEnabled(true)
        }

        setContentView(R.layout.activity_register)

        nameInput = findViewById(R.id.nameInput)
        companyInput = findViewById(R.id.companyInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        passwordInput.transformationMethod = PasswordTransformationMethod.getInstance()
        registerButton = findViewById(R.id.registerButton)
        loginLink = findViewById(R.id.loginLink)

        registerButton.setOnClickListener {
            performRegistration()
        }

        loginLink.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun performRegistration() {
        val name = nameInput.text.toString().trim()
        val company = companyInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        UserSessionManager.register(this, name, email, password, company) { result ->
            setLoading(false)
            result.onSuccess { user ->
                Toast.makeText(this, "Account created! Welcome, ${user.name} 🎉", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }.onFailure { ex ->
                val rawMsg = ex.message ?: ""
                val friendlyMsg = when {
                    rawMsg.contains("already in use", ignoreCase = true) ->
                        "This email is already registered. Please go back and sign in."
                    rawMsg.contains("badly formatted", ignoreCase = true) ->
                        "Please enter a valid email address."
                    else -> rawMsg.ifBlank { "Registration failed. Please try again." }
                }
                Toast.makeText(this, friendlyMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        registerButton.isEnabled = !loading
        registerButton.text = if (loading) "Creating Account..." else "Create Account"
    }
}
