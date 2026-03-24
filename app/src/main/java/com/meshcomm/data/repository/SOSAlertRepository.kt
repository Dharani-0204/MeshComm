package com.meshcomm.data.repository

import android.content.Context
import com.google.gson.Gson
import com.meshcomm.data.db.AppDatabase
import com.meshcomm.data.db.SOSAlertEntity
import com.meshcomm.data.model.SOSAlert
import com.meshcomm.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SOSAlertRepository(context: Context) {
    private val db = AppDatabase.get(context)
    private val sosAlertDao = db.sosAlertDao()
    private val gson = Gson()

    suspend fun saveAlert(alert: SOSAlert, profileSnapshot: UserProfile? = null) {
        val entity = SOSAlertEntity(
            alertId = alert.alertId,
            userId = alert.userId,
            userName = alert.userName,
            latitude = alert.latitude,
            longitude = alert.longitude,
            message = alert.message,
            timestamp = alert.timestamp,
            batteryLevel = alert.batteryLevel,
            isActive = alert.isActive,
            profileSnapshot = profileSnapshot?.let { gson.toJson(it) } ?: "",
            syncedWithFirebase = false
        )
        sosAlertDao.insert(entity)
    }

    fun getActiveAlerts(): Flow<List<SOSAlert>> {
        return sosAlertDao.getActiveAlerts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getAllAlerts(): Flow<List<SOSAlert>> {
        return sosAlertDao.getAllAlerts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getRecentAlerts(limit: Int = 50): Flow<List<SOSAlert>> {
        return sosAlertDao.getRecentAlerts(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getAlertsSince(since: Long): Flow<List<SOSAlert>> {
        return sosAlertDao.getAlertsSince(since).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun deactivateAlert(alertId: String) {
        sosAlertDao.updateActiveStatus(alertId, false)
    }

    suspend fun getUnsyncedAlerts(): List<SOSAlert> {
        return sosAlertDao.getUnsyncedAlerts().map { it.toDomain() }
    }

    private fun SOSAlertEntity.toDomain(): SOSAlert {
        val profile = if (profileSnapshot.isNotBlank()) {
            try {
                gson.fromJson(profileSnapshot, UserProfile::class.java)
            } catch (e: Exception) {
                null
            }
        } else null

        return SOSAlert(
            alertId = alertId,
            userId = userId,
            userName = userName,
            latitude = latitude,
            longitude = longitude,
            message = message,
            timestamp = timestamp,
            batteryLevel = batteryLevel,
            isActive = isActive,
            profile = profile
        )
    }
}
