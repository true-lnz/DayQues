package ru.lansonz.dayquestion.utils

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import ru.lansonz.dayquestion.ui.activity.question.QuestionViewModel
import java.util.*

class NotificationService {//: Service() {
/*
    companion object {
        const val TAG = "NotificationService"
        const val NOTIFICATION_CHANNEL_ID = "NotificationChannel"
        const val NOTIFICATION_ID = 12345
        const val INTERVAL_MILLIS: Long = 15 * 60 * 1000 // 15 minutes in milliseconds
    }

    private lateinit var alarmManager: AlarmManager
    private lateinit var alarmIntent: PendingIntent

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        // Initialize AlarmManager and PendingIntent for repeating tasks
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java)
        alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0)

        // Schedule repeating tasks with AlarmManager
        scheduleAlarm()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        // Return START_STICKY to ensure the service keeps running
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        // Cancel alarm when the service is destroyed
        alarmManager.cancel(alarmIntent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun scheduleAlarm() {
        // Set up repeating alarm with AlarmManager
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.add(Calendar.MILLISECOND, INTERVAL_MILLIS.toInt())

        // Schedule the alarm
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            INTERVAL_MILLIS,
            alarmIntent
        )
    }

    class NotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Alarm received, sending notification")

            // Проверяем, наступило ли указанное время для отправки уведомления
            val currentTime = Calendar.getInstance()
            val notificationTime = Calendar.getInstance().apply {
                // Устанавливаем время для отправки уведомления (11:00 утра)
                set(Calendar.HOUR_OF_DAY, 11)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            if (currentTime.timeInMillis < notificationTime.timeInMillis) {
                // Если текущее время меньше времени для отправки, игнорируем
                return
            }

            // Проверяем, был ли уже отправлен вопрос для текущего дня
            val prefs = Prefs.getInstance(context!!)
            val lastQuestionDate = prefs.getLastQuestionDate()
            val currentDate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            if (lastQuestionDate == currentDate.timeInMillis) {
                // Если вопрос уже был отправлен сегодня, игнорируем
                return
            }

            // Если текущее время больше времени для отправки и вопрос еще не был отправлен сегодня,
            // отправляем уведомление и сохраняем дату отправки вопроса
            val questionViewModel = ViewModelProvider(MyApplication.getInstance()).get(QuestionViewModel::class.java)
            questionViewModel.getQuestion(MyApplication.currentUser!!.userID)
            //prefs.saveLastQuestionDate(currentDate.timeInMillis)
        }
    }*/
}
