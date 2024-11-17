package com.example.myapplication

import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class WeatherNotificationManager(val context: Context, private val database: LocalDatabase) {

    private val channelId = "weather_notification_channel"

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

    // 날씨 조건 확인 및 알림
    suspend fun checkWeatherConditions(apiService: WeatherApiService, apiKey: String, region: String) {
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

        Log.d("WeatherCheck", "DB에서 로드된 항목: ${savedWeatherItems.size}")
        savedWeatherItems.forEach {
            Log.d(
                "WeatherCheck",
                "DB 항목: wNo=${it.wNo}, weather=${it.weather}, time=${it.time}, isNotified=${it.isNotified}"
            )
        }

        // API 호출
        val response = withContext(Dispatchers.IO) {
            apiService.getWeatherForecast(region, apiKey).execute()
        }

        if (response.isSuccessful) {
            val forecastList = response.body()?.list ?: return

            Log.d("WeatherCheck", "API 응답에서 가져온 날씨 항목: ${forecastList.size}")
            forecastList.forEach { forecast ->
                val originalDescription = forecast.weather[0].description
                val convertedDescription = convertToCommonWeatherDescription(originalDescription)
                Log.d("WeatherCheck", "API에서 받은 날씨 설명: $originalDescription -> 변환된 설명: $convertedDescription")
            }

            // 각 저장된 알림 항목과 조건 비교
            savedWeatherItems.forEach { savedItem ->
                when (savedItem.time) {
                    "현재 날씨" -> handleImmediateNotification(savedItem, forecastList)
                    "당일 오전 6시" -> handleTodayNotification(savedItem, forecastList)
                    "전날 오후 9시" -> handleTomorrowNotification(savedItem, forecastList)
                }
            }
        } else {
            Log.d("WeatherCheck", "API 호출 실패: 코드=${response.code()}")
        }
    }

    // "현재 날씨" 알림 처리 (5분 오차 포함)
    private suspend fun handleImmediateNotification(
        savedItem: WeatherListItem,
        forecastList: List<Forecast>
    ) {
        val currentTime = System.currentTimeMillis()
        val forecastTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .parse(forecastList[0].dt_txt)?.time ?: return

        // 5분 오차 비교
        if (!isWithinTimeOffset(currentTime, forecastTime)) {
            Log.d("WeatherCheck", "현재 시간과 예보 시간의 오차가 5분 이상으로 무시")
            return
        }

        val forecastDescription = convertToCommonWeatherDescription(
            forecastList[0].weather[0].description.lowercase(Locale.getDefault())
        )

        // 날씨 일치 및 알림 전송
        if (savedItem.weather == forecastDescription && !savedItem.isNotified) {
            sendNotification(savedItem.contents, forecastList[0].weather[0].description)
            updateNotificationStatus(savedItem.wNo, true)
            Log.d("WeatherCheck", "즉시 알림 조건 충족: wNo=${savedItem.wNo}, contents=${savedItem.contents}")
        }
    }

    // "당일 오전 6시" 알림 처리
    private fun handleTodayNotification(savedItem: WeatherListItem, forecastList: List<Forecast>) {
        val today = Calendar.getInstance()
        forecastList.forEach { forecast ->
            val forecastTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .parse(forecast.dt_txt)?.time ?: return@forEach

            // 06:00~23:59 사이 시간만 비교
            val hour = Calendar.getInstance().apply { timeInMillis = forecastTime }.get(Calendar.HOUR_OF_DAY)
            if (hour < 6) return@forEach

            if (isSameDay(today.timeInMillis, forecastTime)) {
                val forecastDescription = convertToCommonWeatherDescription(
                    forecast.weather[0].description.lowercase(Locale.getDefault())
                )
                if (savedItem.weather == forecastDescription) {
                    sendNotification(savedItem.contents, forecast.weather[0].description)
                    Log.d("WeatherCheck", "당일 알림 조건 충족: wNo=${savedItem.wNo}, weather=${savedItem.weather}")
                    return
                }
            }
        }
    }

    // "전날 오후 9시" 알림 처리
    private fun handleTomorrowNotification(savedItem: WeatherListItem, forecastList: List<Forecast>) {
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        forecastList.forEach { forecast ->
            val forecastTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .parse(forecast.dt_txt)?.time ?: return@forEach

            // 21:00~23:59 사이 시간만 비교
            val hour = Calendar.getInstance().apply { timeInMillis = forecastTime }.get(Calendar.HOUR_OF_DAY)
            if (hour < 21) return@forEach

            if (isSameDay(tomorrow.timeInMillis, forecastTime)) {
                val forecastDescription = convertToCommonWeatherDescription(
                    forecast.weather[0].description.lowercase(Locale.getDefault())
                )
                if (savedItem.weather == forecastDescription) {
                    sendNotification(savedItem.contents, forecast.weather[0].description)
                    Log.d("WeatherCheck", "전날 알림 조건 충족: wNo=${savedItem.wNo}, weather=${savedItem.weather}")
                    return
                }
            }
        }
    }

    // 알림 전송
    private fun sendNotification(content: String, weatherDescription: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(context, WeatherActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val smallIconId = getWeatherIconId(weatherDescription)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIconId)
            .setContentTitle("날씨 알림!")
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    // 5분 오차 비교 함수
    private fun isWithinTimeOffset(baseTime: Long, targetTime: Long): Boolean {
        val offsetMillis = 5 * 60 * 1000
        return targetTime in (baseTime - offsetMillis)..(baseTime + offsetMillis)
    }

    // 날씨 아이콘 ID 가져오기
    private fun getWeatherIconId(weatherDescription: String): Int {
        return when (convertToCommonWeatherDescription(weatherDescription)) {
            "clear sky" -> R.drawable.weather_sun_icon
            "partly cloudy" -> R.drawable.weather_suncloud_icon
            "few clouds" -> R.drawable.weather_cloud_icon
            "light rain" -> R.drawable.weather_rain_icon
            "thunderstorm" -> R.drawable.weather_thunder_icon
            "snow" -> R.drawable.weather_snow_icon
            else -> R.drawable.weather_sun_icon
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

    // 알림 상태 업데이트
    private suspend fun updateNotificationStatus(wNo: Long, isNotified: Boolean) {
        withContext(Dispatchers.IO) {
            database.getWeatherTextDao().updateNotificationStatus(wNo, isNotified)
        }
    }

    // 같은 날인지 확인
    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
