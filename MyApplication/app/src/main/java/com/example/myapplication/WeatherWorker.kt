package com.example.myapplication

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.WorkInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class WeatherWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    // 데이터베이스 및 관련 객체 초기화
    private val database = LocalDatabase.getDatabase(context) // 로컬 데이터베이스 인스턴스
    private val weatherNotificationManager = WeatherNotificationManager(context, database) // 알림 매니저
    private val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE) // 설정 저장
    private val apiService = ApiClient.weatherApiService // OpenWeather API 서비스
    private val apiKey = "74c26aef7529a784cee3247a261edd92" // OpenWeather API 키

    companion object {
        @Volatile
        private var isRunning = false // 실행 중 여부 플래그
    }

    override suspend fun doWork(): Result {
        if (isRunning) {
            Log.d("WeatherWorker", "이미 실행 중인 작업입니다.")
            return Result.success() // 이미 실행 중이면 새 작업 실행 안 함
        }

        isRunning = true
        try {
            // 사용자 설정에서 지역 정보 가져오기 (기본값: "Seoul")
            val selectedRegion = sharedPreferences.getString("selectedRegion", "Seoul") ?: "Seoul"

            // 현재 시간을 기준으로 자정에 알림 상태 초기화
            val currentTime = System.currentTimeMillis()
            if (isMidnight(currentTime)) {
                resetNotifiedStatus() // 자정에 알림 상태를 초기화
            }

            // API 호출 및 날씨 데이터 처리
            withContext(Dispatchers.IO) {
                weatherNotificationManager.checkWeatherConditions(apiService, apiKey, selectedRegion)
            }

            // 다음 작업 예약
            scheduleNextHourlyNotification()

            Log.d("WeatherWorker", "작업 성공적으로 완료")
            return Result.success()
        } catch (e: Exception) {
            Log.e("WeatherWorker", "Error in worker: ${e.message}")
            return Result.retry() // 작업 실패 시 재시도
        } finally {
            isRunning = false // 작업 종료 후 플래그 해제
        }
    }

    // 다음 정각 1분 전에 작업 예약
    private fun scheduleNextHourlyNotification() {
        val initialDelay = calculateInitialDelay() // 다음 정각까지 남은 시간 계산

        // 예약된 작업 확인
        val workInfos = WorkManager.getInstance(applicationContext)
            .getWorkInfosForUniqueWork("HourlyWeatherNotification")
            .get()

        val isRunning = workInfos.any { it.state == WorkInfo.State.RUNNING }

        if (!isRunning) {
            val notificationRequest = OneTimeWorkRequestBuilder<WeatherWorker>()
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                "HourlyWeatherNotification",
                ExistingWorkPolicy.KEEP, // 기존 작업 유지
                notificationRequest
            )

            Log.d(
                "WeatherWorker",
                "새 작업이 예약되었습니다: ${formatTime(System.currentTimeMillis() + initialDelay)}"
            )
        } else {
            Log.d("WeatherWorker", "이미 실행 중인 작업이 있습니다. 새 작업을 예약하지 않습니다.")
        }
    }

    // 다음 정각 1분 전까지 남은 시간 계산
    private fun calculateInitialDelay(): Long {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))

        // 다음 정각 설정
        calendar.add(Calendar.HOUR_OF_DAY, 1)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // 정각 1분 전 시간 계산
        val nextHour = calendar.timeInMillis
        val oneMinuteBefore = nextHour - 1 * 60 * 1000

        // 현재 시간과 비교하여 남은 시간 반환 (음수 방지)
        val delay = oneMinuteBefore - System.currentTimeMillis()
        return if (delay > 0) delay else 0L
    }


     // 자정(0시)에 알림 상태를 초기화
    private fun isMidnight(currentTime: Long): Boolean {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).apply {
            timeInMillis = currentTime
        }
        // 시간과 분이 0이고 자정을 초과하지 않은 경우 true 반환
        return calendar.get(Calendar.HOUR_OF_DAY) == 0 && calendar.get(Calendar.MINUTE) < 5
    }


     // 하루가 지나면 알림 상태를 초기화하여 동일 조건에 대해 다시 알림을 보낼 수 있도록 설정
    private suspend fun resetNotifiedStatus() {
        withContext(Dispatchers.IO) {
            database.getWeatherTextDao().resetAllNotificationStatus() // 알림 상태 초기화
        }
        Log.d("WeatherWorker", "모든 알림 상태가 초기화되었습니다.")
    }

    // 시간 포맷 함수 (한국 시간 기준)
    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        return sdf.format(Date(timestamp)) // yyyy-MM-dd HH:mm:ss 형식의 문자열 반환
    }
}
