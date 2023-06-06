package com.lucy.test_aura

import android.content.Context
import android.os.Bundle
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private companion object {
        private const val CHANNEL_ID = "boot_channel"
        private const val NOTIFICATION_ID = 1
        private const val WORK_TAG = "boot_worker"
    }

    private lateinit var bootEventTimestamps: MutableList<Long>
    private lateinit var textView : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
         textView = findViewById(R.id.textView)

        bootEventTimestamps = mutableListOf()

        createNotificationChannel()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresCharging(false)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<BootWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WORK_TAG,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )

        updateTextView()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Boot Events",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateTextView() {
        val stringBuilder = StringBuilder()

        if (bootEventTimestamps.isEmpty()) {
            textView.text = "No boots detected"
        } else {
            for ((index, timestamp) in bootEventTimestamps.withIndex()) {
                stringBuilder.append("${index + 1}. Timestamp: $timestamp\n")
            }
            textView.text = stringBuilder.toString()
        }
    }

    inner class BootWorker(appContext: Context, workerParams: WorkerParameters) :
        Worker(appContext, workerParams) {

        override fun doWork(): Result {
            val notificationText = when {
                bootEventTimestamps.isEmpty() -> "No boots detected"
                bootEventTimestamps.size == 1 -> {
                    val timestamp = bootEventTimestamps[0]
                    "The boot was detected with the timestamp = $timestamp"
                }
                else -> {
                    val lastBootTimestamp = bootEventTimestamps.last()
                    val prevBootTimestamp = bootEventTimestamps[bootEventTimestamps.size - 2]
                    val timeDelta = lastBootTimestamp - prevBootTimestamp
                    "Last boots time delta = $timeDelta"
                }
            }

            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle("Boot Event")
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)

            return Result.success()
        }
    }
}
