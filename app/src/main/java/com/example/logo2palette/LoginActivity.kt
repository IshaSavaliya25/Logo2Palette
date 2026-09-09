package com.example.logo2palette

import android.content.Intent
import android.os.Bundle
import android.view.View
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
    private lateinit var demoLoginButton: Button
    private lateinit var registerLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        // Check if user is already logged in
        if (UserSessionManager.isLoggedIn(this)) {
            navigateToMain()
            return
        }

        setContentView(R.layout.activity_login)

        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        demoLoginButton = findViewById(R.id.demoLoginButton)
        registerLink = findViewById(R.id.registerLink)

        loginButton.setOnClickListener {
            performLogin()
        }

        demoLoginButton.setOnClickListener {
            performDemoLogin()
        }

        registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun performLogin() {
        val email = emailInput.text.toString()
        val password = passwordInput.text.toString()

        setLoading(true)

        UserSessionManager.login(this, email, password) { result ->
            setLoading(false)
            result.onSuccess { user ->
                Toast.makeText(this, "Welcome back, ${user.name}! 👋", Toast.LENGTH_SHORT).show()
                navigateToMain()
            }.onFailure { ex ->
                Toast.makeText(this, ex.message ?: "Authentication failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun performDemoLogin() {
        setLoading(true)
        UserSessionManager.login(this, "demo", "") { result ->
            setLoading(false)
            result.onSuccess { user ->
                Toast.makeText(this, "Signed in as ${user.name} (Demo)", Toast.LENGTH_SHORT).show()
                navigateToMain()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        loginButton.isEnabled = !loading
        demoLoginButton.isEnabled = !loading
        loginButton.text = if (loading) "Authenticating..." else "Sign In"
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
