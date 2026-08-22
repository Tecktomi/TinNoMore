package com.tinnomore.data.repository

import com.tinnomore.data.db.dao.UserDao
import com.tinnomore.data.db.entity.User
import kotlinx.coroutines.flow.Flow

class UserRepository(private val dao: UserDao) {

    fun getAllUsers(): Flow<List<User>> = dao.getAllUsers()

    fun getAllPatients(): Flow<List<User>> = dao.getAllPatients()

    suspend fun getUserById(id: Long): User? = dao.getUserById(id)

    suspend fun login(email: String, password: String): User? =
        dao.login(email.trim(), password.trim())

    suspend fun createUser(user: User): Long = dao.insert(user)

    suspend fun updateUser(user: User) = dao.update(user)

    suspend fun deleteUser(user: User) = dao.delete(user)
}
