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
import kotlin.math.abs

class WeatherNotificationManager(val context: Context, val database: LocalDatabase) {

    private val channelId = "weather_notification_channel"
    private var lastNotifiedWeather: String? = null  // 마지막으로 알림을 보낸 날씨 조건을 저장하는 변수

    init {
        createNotificationChannel()
    }

    // 알림 채널을 생성하는 메서드 (Android 8.0 이상에서 필요)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Weather Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for matching weather conditions"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // 날씨 조건을 확인하고 조건이 일치할 때 알림을 보내는 메서드
    suspend fun checkWeatherConditions(apiService: WeatherApiService, apiKey: String, region: String) {
        // 데이터베이스에서 저장된 사용자 날씨 알림 조건을 가져옴
        val savedWeatherItems = database.getWeatherTextDao().getAllWeatherList()

        // 날씨 API에서 예보 데이터를 가져옴
        val response = withContext(Dispatchers.IO) {
            apiService.getWeatherForecast(region, apiKey).execute()
        }

        if (response.isSuccessful) {
            val forecastList = response.body()?.list ?: return

            // 현재 시간에 가장 가까운 예보 항목을 찾음
            val closestForecast = forecastList.minByOrNull { forecast ->
                val forecastTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(forecast.dt_txt)?.time
                if (forecastTime != null) abs(forecastTime - System.currentTimeMillis()) else Long.MAX_VALUE
            }

            if (closestForecast != null) {
                // 현재 예보의 날씨 설명을 공통 형식으로 변환
                val forecastDescription = convertToCommonWeatherDescription(closestForecast.weather[0].description.lowercase(Locale.ROOT))

                // 즉시 알림 조건 중 일치하는 첫 번째 조건을 찾음
                val matchingItem = savedWeatherItems.firstOrNull { savedItem ->
                    savedItem.wTime == "즉시 알림" && savedItem.weather == forecastDescription
                }

                // 마지막 알림을 보낸 날씨와 다를 경우에만 알림 전송
                matchingItem?.let {
                    if (lastNotifiedWeather != forecastDescription) {
                        sendNotification(it.wText, closestForecast.weather[0].description)
                        lastNotifiedWeather = forecastDescription  // 마지막 알림 날씨 업데이트
                    }
                }
            }
        } else {
            Log.d("WeatherCheck", "API call failed with code: ${response.code()}")
        }
    }

    // 알림을 생성하고 전송하는 메서드
    fun sendNotification(content: String, weatherDescription: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(context, WeatherActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // 날씨 설명에 따라 아이콘을 설정
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

    // 날씨 설명에 따라 알맞은 아이콘을 반환하는 메서드
    private fun getWeatherIconId(weatherDescription: String): Int {
        return when (weatherDescription.lowercase(Locale.ROOT)) {
            "clear sky" -> R.drawable.weather_sun_icon
            "partly cloudy" -> R.drawable.weather_suncloud_icon
            "few clouds", "scattered clouds", "broken clouds", "overcast clouds", "mist", "fog", "haze", "smoke", "dust", "sand", "ash" -> R.drawable.weather_cloud_icon
            "light rain", "moderate rain", "heavy intensity rain", "very heavy rain", "extreme rain",
            "light intensity drizzle", "drizzle", "heavy intensity drizzle", "shower rain", "ragged shower rain" -> R.drawable.weather_rain_icon
            "thunderstorm", "thunderstorm with light rain", "thunderstorm with rain", "thunderstorm with heavy rain",
            "light thunderstorm", "heavy thunderstorm", "ragged thunderstorm" -> R.drawable.weather_thunder_icon
            "light snow", "snow", "heavy snow", "sleet", "light shower sleet", "shower sleet",
            "light rain and snow", "rain and snow", "light shower snow", "shower snow", "heavy shower snow" -> R.drawable.weather_snow_icon
            else -> R.drawable.weather_sun_icon
        }
    }

    // 공통 형식의 날씨 설명으로 변환하는 메서드
    private fun convertToCommonWeatherDescription(description: String): String {
        return when (description) {
            "clear sky" -> "clear sky"
            "partly cloudy" -> "partly cloudy"
            "few clouds", "scattered clouds", "broken clouds", "overcast clouds", "mist", "fog", "haze", "smoke", "dust", "sand", "ash" -> "few clouds"
            "light rain", "moderate rain", "heavy intensity rain", "very heavy rain", "extreme rain",
            "light intensity drizzle", "drizzle", "heavy intensity drizzle", "shower rain", "ragged shower rain" -> "light rain"
            "thunderstorm", "thunderstorm with light rain", "thunderstorm with rain", "thunderstorm with heavy rain",
            "light thunderstorm", "heavy thunderstorm", "ragged thunderstorm" -> "thunderstorm"
            "light snow", "snow", "heavy snow", "sleet", "light shower sleet", "shower sleet",
            "light rain and snow", "rain and snow", "light shower snow", "shower snow", "heavy shower snow" -> "snow"
            else -> "clear sky"  // 기본값
        }
    }
}
