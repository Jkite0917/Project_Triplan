package com.example.myapplication

import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class WeatherNotificationManager(val context: Context, private val database: LocalDatabase) {

    private val channelId = "weather_notification_channel"
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        createNotificationChannel()
    }

    // 알림 채널 생성
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Weather Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for matching weather conditions"
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // 시간 포맷 함수
    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // 날씨 조건 확인 및 알림
    suspend fun checkWeatherConditions(apiService: WeatherApiService, apiKey: String, region: String) {
        val apiCallTime = System.currentTimeMillis()
        val nextHour = calculateNextHour(apiCallTime) // 다음 정각 계산
        val apiFetchTime = nextHour - 3 * 60 * 1000 // 정각 3분 전

        // API 호출 시점이 아니라면 리턴
        if (apiCallTime < apiFetchTime) {
            Log.d("WeatherCheck", "아직 API 호출 시간이 아님: ${formatTime(apiCallTime)}")
            return
        }

        // API 호출
        val response = withContext(Dispatchers.IO) {
            apiService.getWeatherForecast(region, apiKey).execute()
        }

        if (response.isSuccessful) {
            val forecastList = response.body()?.list ?: return
            Log.d("WeatherCheck", "API 응답에서 가져온 날씨 항목: ${forecastList.size}")

            // DB에서 저장된 알림 항목 불러오기
            val savedWeatherItems = database.getWeatherTextDao().getAllWeatherList().map { weather ->
                WeatherListItem(
                    wNo = weather.wNo,
                    contents = weather.wText,
                    weather = weather.weather,
                    time = weather.wTime,
                    isNotified = weather.isNotified
                )
            }

            // 각 알림 항목에 대해 조건 확인
            savedWeatherItems.forEach { savedItem ->
                when (savedItem.time) {
                    "현재 날씨" -> handleImmediateNotification(savedItem, forecastList)
                    "당일 오전 6시" -> handleTimeNotification(savedItem, forecastList, 6)
                    "전날 오후 9시" -> handleTimeNotification(savedItem, forecastList, 21)
                }
            }
        } else {
            Log.d("WeatherCheck", "API 호출 실패: 코드=${response.code()}")
        }
    }

    // "현재 날씨" 알림 처리 및 상태 업데이트
    private fun handleImmediateNotification(
        savedItem: WeatherListItem,
        forecastList: List<Forecast>
    ) {
        val forecastDescription = convertToCommonWeatherDescription(
            forecastList[0].weather[0].description.lowercase(Locale.getDefault())
        )
        Log.d("WeatherCheck", "예보된 날씨: $forecastDescription, 저장된 날씨: ${savedItem.weather}")

        // 현재 날씨가 저장된 날씨와 다르면 isNotified를 false로 리셋
        if (savedItem.weather != forecastDescription) {
            coroutineScope.launch {
                resetNotificationStatus(savedItem.wNo)
            }
            Log.d("WeatherCheck", "날씨 변경 감지: ${savedItem.weather} -> $forecastDescription, 상태 리셋 완료")
        }

        // 현재 날씨와 조건 일치 시 알림 전송
        if (savedItem.weather == forecastDescription && !savedItem.isNotified && isWithinTimeOffset(
                System.currentTimeMillis(),
                forecastList[0].dt_txt
            )
        ) {
            sendNotification(savedItem.contents)
            coroutineScope.launch {
                updateNotificationStatus(savedItem.wNo, true)
            }
            Log.d("WeatherCheck", "즉시 알림 조건 충족: wNo=${savedItem.wNo}")
        }
    }

    // "당일 오전 6시" 및 "전날 오후 9시" 알림 처리
    private fun handleTimeNotification(
        savedItem: WeatherListItem,
        forecastList: List<Forecast>,
        targetHour: Int
    ) {
        val targetForecasts = forecastList.filter { forecast ->
            val forecastTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .parse(forecast.dt_txt)?.time ?: return@filter false

            val forecastHour = Calendar.getInstance().apply { timeInMillis = forecastTime }.get(Calendar.HOUR_OF_DAY)
            forecastHour == targetHour
        }

        targetForecasts.forEach { forecast ->
            val forecastDescription = convertToCommonWeatherDescription(
                forecast.weather[0].description.lowercase(Locale.getDefault())
            )
            if (savedItem.weather == forecastDescription && !savedItem.isNotified) {
                sendNotification(savedItem.contents)
                coroutineScope.launch {
                    updateNotificationStatus(savedItem.wNo, true)
                }
                Log.d("WeatherCheck", "${targetHour}시 알림 조건 충족: wNo=${savedItem.wNo}")
            }
        }
    }

    // 특정 시간에 대한 오차 범위 확인 (1분으로 줄임)
    private fun isWithinTimeOffset(baseTime: Long, targetTimeString: String): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val targetTime = sdf.parse(targetTimeString)?.time ?: return false
        val offsetMillis = 1 * 60 * 1000 // 1분 오차
        return targetTime in (baseTime - offsetMillis)..(baseTime + offsetMillis)
    }

    // 다음 정각 계산
    private fun calculateNextHour(currentTime: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTime
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.HOUR_OF_DAY, 1)
        }
        return calendar.timeInMillis
    }

    // 알림 전송
    private fun sendNotification(content: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(context, WeatherActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.weather_sun_icon)
            .setContentTitle("날씨 알림!")
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    // 알림 상태 업데이트
    private suspend fun updateNotificationStatus(wNo: Long, isNotified: Boolean) {
        withContext(Dispatchers.IO) {
            database.getWeatherTextDao().updateNotificationStatus(wNo, isNotified)
        }
    }

    // 알림 상태 리셋
    private suspend fun resetNotificationStatus(wNo: Long) {
        withContext(Dispatchers.IO) {
            database.getWeatherTextDao().updateNotificationStatus(wNo, false)
        }
    }

    // 날씨 설명 변환
    private fun convertToCommonWeatherDescription(description: String): String {
        val lowerCaseDescription = description.trim().lowercase(Locale.getDefault())
        return when (lowerCaseDescription) {
            "clear sky" -> "clear sky"
            "few clouds", "scattered clouds" -> "partly cloudy"
            "broken clouds", "overcast clouds", "mist" -> "few clouds"
            "drizzle", "light rain", "moderate rain", "heavy intensity rain" -> "light rain"
            "light thunderstorm", "thunderstorm", "heavy thunderstorm" -> "thunderstorm"
            "light snow", "snow", "heavy snow", "sleet" -> "snow"
            else -> "clear sky"
        }
    }
}
