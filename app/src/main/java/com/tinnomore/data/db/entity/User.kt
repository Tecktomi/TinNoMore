package com.tinnomore.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole { PATIENT, SPECIALIST, ADMIN }

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val rut: String,
    val email: String,
    val password: String,
    val role: UserRole
)
