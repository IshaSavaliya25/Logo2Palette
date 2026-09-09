package com.example.logo2palette.model

import java.io.Serializable

data class User(
    val id: String,
    val name: String,
    val email: String,
    val companyName: String = "",
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
