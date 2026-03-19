package com.meshcomm.data.repository

import com.meshcomm.data.db.AppDatabase
import com.meshcomm.data.db.UserEntity
import com.meshcomm.data.model.User
import com.meshcomm.data.model.UserRole
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepository(private val db: AppDatabase) {
    private val gson = Gson()

    suspend fun saveUser(user: User) {
        db.userDao().insert(user.toEntity())
    }

    suspend fun getUser(userId: String): User? =
        db.userDao().getUser(userId)?.toDomain()

    fun getAllUsers(): Flow<List<User>> =
        db.userDao().getAllUsers().map { list -> list.map { it.toDomain() } }

    private fun User.toEntity() = UserEntity(
        userId = userId,
        name = name,
        role = role.name,
        emergencyContacts = gson.toJson(emergencyContacts)
    )

    private fun UserEntity.toDomain(): User {
        val contacts: List<String> = try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(emergencyContacts, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
        return User(
            userId = userId,
            name = name,
            role = UserRole.valueOf(role),
            emergencyContacts = contacts
        )
    }
}
