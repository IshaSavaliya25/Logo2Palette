package com.example.logo2palette.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.logo2palette.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object UserSessionManager {

    private const val PREF_NAME = "logo2palette_user_session"
    private const val KEY_CURRENT_USER_ID = "current_user_id"
    private const val KEY_USERS_LIST = "registered_users_json"
    private const val KEY_PASSWORDS = "passwords_json"
    private const val KEY_LOGGED_IN_USER = "logged_in_user_details"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private fun getFirebaseAuth(): FirebaseAuth? {
        return try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    fun isLoggedIn(context: Context): Boolean {
        val auth = getFirebaseAuth()
        if (auth?.currentUser != null) {
            return true
        }
        return getCurrentUserId(context) != null
    }

    fun getCurrentUserId(context: Context): String? {
        val firebaseUser = getFirebaseAuth()?.currentUser
        if (firebaseUser != null) {
            return firebaseUser.uid
        }
        return getPrefs(context).getString(KEY_CURRENT_USER_ID, null)
    }

    fun getCurrentUser(context: Context): User? {
        val userId = getCurrentUserId(context) ?: return null
        
        // Check cached user in shared preferences first
        val cachedUserJson = getPrefs(context).getString(KEY_LOGGED_IN_USER, null)
        if (cachedUserJson != null) {
            try {
                val obj = JSONObject(cachedUserJson)
                if (obj.getString("id") == userId) {
                    return User(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        email = obj.getString("email"),
                        companyName = obj.optString("companyName", "Brand Designer"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val firebaseUser = getFirebaseAuth()?.currentUser
        if (firebaseUser != null) {
            return User(
                id = firebaseUser.uid,
                name = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "Brand Designer",
                email = firebaseUser.email ?: "",
                companyName = "Brand Designer"
            )
        }

        val users = getAllUsers(context)
        return users.find { it.id == userId }
    }

    /**
     * Async registration with Firebase Authentication and Cloud Firestore fallback
     */
    fun register(
        context: Context,
        name: String,
        email: String,
        pass: String,
        company: String,
        onResult: (Result<User>) -> Unit
    ) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isEmpty() || pass.length < 4) {
            onResult(Result.failure(Exception("Please enter a valid email and a password of at least 4 characters")))
            return
        }

        val auth = getFirebaseAuth()
        if (auth != null) {
            auth.createUserWithEmailAndPassword(cleanEmail, pass)
                .addOnSuccessListener { authResult ->
                    val firebaseUser = authResult.user
                    val uid = firebaseUser?.uid ?: UUID.randomUUID().toString()
                    val newUser = User(
                        id = uid,
                        name = name.ifBlank { "Brand Designer" },
                        email = cleanEmail,
                        companyName = company.ifBlank { "Logo2Palette User" }
                    )

                    // Save to Firestore
                    val firestore = getFirestore()
                    if (firestore != null) {
                        val userMap = hashMapOf(
                            "id" to newUser.id,
                            "name" to newUser.name,
                            "email" to newUser.email,
                            "companyName" to newUser.companyName,
                            "createdAt" to newUser.createdAt
                        )
                        firestore.collection("users").document(uid).set(userMap)
                    }

                    saveUserLocally(context, newUser)
                    loginDirect(context, newUser)
                    onResult(Result.success(newUser))
                }
                .addOnFailureListener { ex ->
                    // Fallback to local registration if Firebase fails or is not configured
                    val localResult = registerLocally(context, name, cleanEmail, pass, company)
                    if (localResult.isSuccess) {
                        onResult(localResult)
                    } else {
                        onResult(Result.failure(ex))
                    }
                }
        } else {
            // Local storage fallback
            onResult(registerLocally(context, name, cleanEmail, pass, company))
        }
    }

    /**
     * Async login with Firebase Authentication
     */
    fun login(
        context: Context,
        email: String,
        pass: String,
        onResult: (Result<User>) -> Unit
    ) {
        val cleanEmail = email.trim().lowercase()

        val auth = getFirebaseAuth()
        if (auth != null && pass.isNotEmpty()) {
            auth.signInWithEmailAndPassword(cleanEmail, pass)
                .addOnSuccessListener { authResult ->
                    val firebaseUser = authResult.user
                    val uid = firebaseUser?.uid ?: ""

                    // Fetch profile details from Firestore
                    val firestore = getFirestore()
                    if (firestore != null && uid.isNotEmpty()) {
                        firestore.collection("users").document(uid).get()
                            .addOnSuccessListener { doc ->
                                val user = if (doc.exists()) {
                                    User(
                                        id = uid,
                                        name = doc.getString("name") ?: "Brand Designer",
                                        email = doc.getString("email") ?: cleanEmail,
                                        companyName = doc.getString("companyName") ?: "Brand Designer",
                                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                    )
                                } else {
                                    User(
                                        id = uid,
                                        name = firebaseUser?.displayName ?: cleanEmail.substringBefore("@"),
                                        email = cleanEmail,
                                        companyName = "Brand Designer"
                                    )
                                }
                                saveUserLocally(context, user)
                                loginDirect(context, user)
                                onResult(Result.success(user))
                            }
                            .addOnFailureListener {
                                val user = User(
                                    id = uid,
                                    name = cleanEmail.substringBefore("@"),
                                    email = cleanEmail,
                                    companyName = "Brand Designer"
                                )
                                saveUserLocally(context, user)
                                loginDirect(context, user)
                                onResult(Result.success(user))
                            }
                    } else {
                        val user = User(
                            id = uid,
                            name = cleanEmail.substringBefore("@"),
                            email = cleanEmail,
                            companyName = "Brand Designer"
                        )
                        saveUserLocally(context, user)
                        loginDirect(context, user)
                        onResult(Result.success(user))
                    }
                }
                .addOnFailureListener { ex ->
                    // Fallback to local authentication check
                    val localResult = loginLocally(context, cleanEmail, pass)
                    if (localResult.isSuccess) {
                        onResult(localResult)
                    } else {
                        onResult(Result.failure(ex))
                    }
                }
        } else {
            onResult(loginLocally(context, cleanEmail, pass))
        }
    }

    fun logout(context: Context) {
        try {
            getFirebaseAuth()?.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        getPrefs(context).edit().remove(KEY_CURRENT_USER_ID).remove(KEY_LOGGED_IN_USER).apply()
    }

    private fun loginDirect(context: Context, user: User) {
        saveUserLocally(context, user)
        getPrefs(context).edit().putString(KEY_CURRENT_USER_ID, user.id).apply()
    }

    private fun saveUserLocally(context: Context, user: User) {
        val obj = JSONObject().apply {
            put("id", user.id)
            put("name", user.name)
            put("email", user.email)
            put("companyName", user.companyName)
            put("createdAt", user.createdAt)
        }
        getPrefs(context).edit().putString(KEY_LOGGED_IN_USER, obj.toString()).apply()

        val users = getAllUsers(context).toMutableList()
        users.removeAll { it.id == user.id || it.email == user.email }
        users.add(user)
        saveAllUsers(context, users)
    }

    private fun registerLocally(
        context: Context,
        name: String,
        email: String,
        pass: String,
        company: String
    ): Result<User> {
        val users = getAllUsers(context).toMutableList()
        if (users.any { it.email.equals(email, ignoreCase = true) }) {
            return Result.failure(Exception("An account with this email already exists"))
        }

        val newUser = User(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Brand Designer" },
            email = email,
            companyName = company.ifBlank { "Logo2Palette User" }
        )

        users.add(newUser)
        saveAllUsers(context, users)

        val passMap = getPasswordMap(context)
        passMap[email] = pass
        savePasswordMap(context, passMap)

        loginDirect(context, newUser)
        return Result.success(newUser)
    }

    private fun loginLocally(context: Context, email: String, pass: String): Result<User> {
        val users = getAllUsers(context)
        val user = users.find { it.email.equals(email, ignoreCase = true) }
            ?: return Result.failure(Exception("No account found with this email"))

        val passMap = getPasswordMap(context)
        val storedPass = passMap[email]

        if (storedPass != pass) {
            return Result.failure(Exception("Incorrect password"))
        }

        loginDirect(context, user)
        return Result.success(user)
    }

    private fun getAllUsers(context: Context): List<User> {
        val jsonStr = getPrefs(context).getString(KEY_USERS_LIST, "[]") ?: "[]"
        val list = mutableListOf<User>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    User(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        email = obj.getString("email"),
                        companyName = obj.optString("companyName", "Brand Designer"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveAllUsers(context: Context, users: List<User>) {
        val array = JSONArray()
        for (u in users) {
            val obj = JSONObject().apply {
                put("id", u.id)
                put("name", u.name)
                put("email", u.email)
                put("companyName", u.companyName)
                put("createdAt", u.createdAt)
            }
            array.put(obj)
        }
        getPrefs(context).edit().putString(KEY_USERS_LIST, array.toString()).apply()
    }

    private fun getPasswordMap(context: Context): MutableMap<String, String> {
        val jsonStr = getPrefs(context).getString(KEY_PASSWORDS, "{}") ?: "{}"
        val map = mutableMapOf<String, String>()
        try {
            val obj = JSONObject(jsonStr)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = obj.getString(key)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    private fun savePasswordMap(context: Context, map: Map<String, String>) {
        val obj = JSONObject()
        for ((k, v) in map) {
            obj.put(k, v)
        }
        getPrefs(context).edit().putString(KEY_PASSWORDS, obj.toString()).apply()
    }
}
