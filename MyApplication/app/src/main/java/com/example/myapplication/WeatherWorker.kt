package com.example.myapplication

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

// WeatherWorker: 15분마다 실행되며 날씨 조건 확인 및 알림 처리
class WeatherWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    // 로컬 데이터베이스 초기화
    private val database = LocalDatabase.getDatabase(context)

    // WeatherNotificationManager를 통해 알림 처리
    private val weatherNotificationManager = WeatherNotificationManager(context, database)

    // SharedPreferences: 사용자가 선택한 지역 정보를 가져옴
    private val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

    // OpenWeather API 서비스 및 API 키
    private val apiService = ApiClient.weatherApiService
    private val apiKey = "~~~~~~~~~~~~~~~~~~~~~"

    // Worker 실행 시 호출되는 메서드
    override suspend fun doWork(): Result {
        return try {
            // SharedPreferences에서 사용자가 선택한 지역 정보 가져오기 (기본값: "Seoul")
            val selectedRegion = sharedPreferences.getString("selectedRegion", "Seoul") ?: "Seoul"

            // WeatherNotificationManager를 통해 날씨 조건 확인 및 알림 처리
            withContext(Dispatchers.IO) {
                weatherNotificationManager.checkWeatherConditions(apiService, apiKey, selectedRegion)
            }

            // 작업 성공 로그 출력
            Log.d("WeatherWorker", "작업 성공적으로 완료")

            // 다음 실행 예약
            scheduleNextRun()

            // 작업 성공 리턴
            Result.success()
        } catch (e: Exception) {
            // 작업 실패 로그 출력
            Log.e("WeatherWorker", "Error: ${e.message}")

            // 작업 실패 시 재시도
            Result.retry()
        }
    }

    // 다음 실행 예약 메서드
    private fun scheduleNextRun() {
        // 15분 후에 다시 실행되도록 WorkManager 작업 예약
        val workRequest = OneTimeWorkRequestBuilder<WeatherWorker>()
            .setInitialDelay(15, TimeUnit.MINUTES) // 15분 후 실행
            .build()

        // 기존 작업을 대체하며 새 작업을 WorkManager에 등록
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "WeatherWorker", // 고유 작업 이름
            ExistingWorkPolicy.REPLACE, // 기존 작업 대체
            workRequest
        )

        // 작업 예약 로그 출력
        Log.d("WeatherWorker", "다음 작업이 15분 후에 예약되었습니다.")
    }
}
