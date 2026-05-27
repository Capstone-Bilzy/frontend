package com.android.bilzy.domain.repository

import com.android.bilzy.data.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    suspend fun getNotifications(userUid: String): List<Notification>
    fun observeNotifications(userUid: String): Flow<List<Notification>>
    suspend fun saveNotification(notification: Notification): String
    suspend fun markAsRead(id: String)
    suspend fun markAllAsRead(userUid: String)
}
