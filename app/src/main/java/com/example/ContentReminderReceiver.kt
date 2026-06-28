package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class ContentReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val platform = intent.getStringExtra("platform") ?: "Media Sosial"
        val slot = intent.getStringExtra("slot") ?: "Jadwal"
        val caption = intent.getStringExtra("caption") ?: ""
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "otokreator_reminder_channel"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Pengingat Posting OtoKreator",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel untuk notifikasi pengingat posting media sosial"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Waktunya Posting di $platform! ⏰")
            .setContentText("Slot: $slot. Ketuk untuk membuka caption & posting.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Slot: $slot\n\n$caption"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
