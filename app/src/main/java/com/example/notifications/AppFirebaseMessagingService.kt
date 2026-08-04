package com.example.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "New FCM registration token: $token")
        // Store or send token to repository if backend syncing is enabled
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        val title = remoteMessage.notification?.title 
            ?: remoteMessage.data["title"] 
            ?: "RDA Physical Academy"

        val body = remoteMessage.notification?.body 
            ?: remoteMessage.data["body"] 
            ?: "New notification received."

        val type = remoteMessage.data["type"] ?: "general"

        if (type == "update" || title.contains("Update", ignoreCase = true)) {
            NotificationHelper.showUpdateNotification(applicationContext, title, body)
        } else {
            NotificationHelper.showGeneralNotification(applicationContext, title, body)
        }
    }
}
