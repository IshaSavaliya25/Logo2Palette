package com.example.logo2palette

import android.content.Intent
import android.os.Bundle
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
        setContentView(R.layout.activity_register)

        nameInput = findViewById(R.id.nameInput)
        companyInput = findViewById(R.id.companyInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        registerButton = findViewById(R.id.registerButton)
        loginLink = findViewById(R.id.loginLink)

        registerButton.setOnClickListener {
            performRegistration()
        }

        loginLink.setOnClickListener {
            finish()
        }
    }

    private fun performRegistration() {
        val name = nameInput.text.toString()
        val company = companyInput.text.toString()
        val email = emailInput.text.toString()
        val password = passwordInput.text.toString()

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
                Toast.makeText(this, ex.message ?: "Registration failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        registerButton.isEnabled = !loading
        registerButton.text = if (loading) "Creating Account..." else "Create Account"
    }
}
