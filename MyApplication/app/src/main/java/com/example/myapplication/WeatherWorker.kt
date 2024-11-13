package com.example.myapplication

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            // 저장된 지역을 가져와 알림을 위한 날씨 조건 확인
            val selectedRegion = sharedPreferences.getString("selectedRegion", "Seoul") ?: "Seoul"
            withContext(Dispatchers.IO) {
                weatherNotificationManager.checkWeatherConditions(apiService, apiKey, selectedRegion)
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("WeatherWorker", "Error in worker: ${e.message}")
            Result.retry()
        }
    }
}
