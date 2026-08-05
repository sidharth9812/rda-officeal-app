package com.example.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class RDAFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "RDAMessagingService"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token generated: $token")
        saveTokenToFirestore(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title 
            ?: remoteMessage.data["title"] 
            ?: "RDA Physical Academy"

        val body = remoteMessage.notification?.body 
            ?: remoteMessage.data["message"] 
            ?: remoteMessage.data["body"] 
            ?: "You have a new update or notice."

        val type = remoteMessage.data["type"] ?: "NOTICE"

        NotificationHelper.createNotificationChannels(applicationContext)

        if (type == "UPDATE") {
            val version = remoteMessage.data["version"] ?: "Latest"
            NotificationHelper.showUpdateNotification(applicationContext, version, body)
        } else {
            NotificationHelper.showNoticeNotification(applicationContext, title, body)
        }
    }

    private fun saveTokenToFirestore(token: String) {
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val userMap = mapOf(
                "fcmToken" to token,
                "fcmTokenUpdatedAt" to System.currentTimeMillis()
            )
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .update(userMap)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update FCM token in Firestore: ${e.message}")
        }
    }
}
