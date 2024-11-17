package com.example.myapplication

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    private val apiKey = "74c26aef7529a784cee3247a261edd92"  // API 키를 여기에 입력하세요

    override suspend fun doWork(): Result {
        return try {
            // 사용자가 선택한 지역 정보를 가져옵니다.
            val selectedRegion = sharedPreferences.getString("selectedRegion", "Seoul") ?: "Seoul"

            // 날씨 조건을 확인하고 알림을 보냅니다.
            withContext(Dispatchers.IO) {
                weatherNotificationManager.checkWeatherConditions(apiService, apiKey, selectedRegion)
            }

            // 다음 정각에 맞춰 작업을 예약합니다.
            scheduleNextHourlyNotification()

            Result.success()
        } catch (e: Exception) {
            Log.e("WeatherWorker", "Error in worker: ${e.message}")
            Result.retry()  // 작업 실패 시 재시도합니다.
        }
    }

    // 다음 정각에 맞춰 알림을 예약하는 함수
    private fun scheduleNextHourlyNotification() {
        val initialDelay = calculateInitialDelay()

        // 1시간 후 정각에 작업을 실행하도록 예약합니다.
        val notificationRequest = OneTimeWorkRequestBuilder<WeatherWorker>()
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(notificationRequest)
    }

    // 다음 1시간 정각까지 남은 시간(밀리초)을 계산하는 함수
    private fun calculateInitialDelay(): Long {
        val calendar = Calendar.getInstance()

        // 현재 시각에서 1시간 후의 정각으로 설정합니다.
        calendar.add(Calendar.HOUR_OF_DAY, 1)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // 다음 정각까지의 남은 시간을 계산합니다.
        return calendar.timeInMillis - System.currentTimeMillis()
    }
}
