package com.example.myapplication

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class WeatherWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val database = LocalDatabase.getDatabase(context)
    private val weatherNotificationManager = WeatherNotificationManager(context, database)
    private val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
    private val apiService = ApiClient.weatherApiService
    private val apiKey = "74c26aef7529a784cee3247a261edd92"

    override suspend fun doWork(): Result {
        return try {
            val selectedRegion = sharedPreferences.getString("selectedRegion", "Seoul") ?: "Seoul"

            // 날씨 조건 확인 및 알림
            withContext(Dispatchers.IO) {
                weatherNotificationManager.checkWeatherConditions(apiService, apiKey, selectedRegion)
            }

            // 다음 정각 3분 전에 작업 예약
            scheduleNextHourlyNotification()

            Result.success()
        } catch (e: Exception) {
            Log.e("WeatherWorker", "Error in worker: ${e.message}")
            Result.retry() // 작업 실패 시 재시도
        }
    }

    /**
     * 다음 정각 3분 전에 작업 예약
     */
    private fun scheduleNextHourlyNotification() {
        val initialDelay = calculateInitialDelay()

        // 작업 예약
        val notificationRequest = OneTimeWorkRequestBuilder<WeatherWorker>()
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(notificationRequest)
        Log.d(
            "WeatherWorker",
            "다음 작업 예약: ${formatTime(System.currentTimeMillis() + initialDelay)} (현재 시간: ${formatTime(System.currentTimeMillis())})"
        )
    }

    /**
     * 다음 정각 3분 전까지 남은 시간 계산 (한국 시간 기준)
     */
    private fun calculateInitialDelay(): Long {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))

        // 다음 정각 설정
        calendar.add(Calendar.HOUR_OF_DAY, 1)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // 정각 3분 전 계산
        val nextHour = calendar.timeInMillis
        val threeMinutesBefore = nextHour - 3 * 60 * 1000

        // 현재 시간과 비교하여 남은 시간 반환 (음수 방지)
        val delay = threeMinutesBefore - System.currentTimeMillis()
        return if (delay > 0) delay else 0L
    }

    /**
     * 시간 포맷 함수 (한국 시간 기준)
     */
    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        return sdf.format(Date(timestamp))
    }
}
