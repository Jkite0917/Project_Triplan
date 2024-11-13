package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class WeatherNotificationManager(val context: Context, val database: LocalDatabase) {

    private val channelId = "weather_notification_channel"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Weather Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notifications for matching weather conditions"
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    suspend fun checkWeatherConditions(apiService: WeatherApiService, apiKey: String, region: String) {
        val savedWeatherItems = database.getWeatherTextDao().getAllWeatherList()

        val response = withContext(Dispatchers.IO) {
            apiService.getWeatherForecast(region, apiKey).execute()
        }

        if (response.isSuccessful) {
            val forecastList = response.body()?.list ?: return

            savedWeatherItems.forEach { savedItem ->
                val matchingForecast = forecastList.find { forecast ->
                    val forecastDescription = convertToCommonWeatherDescription(forecast.weather[0].description.lowercase(Locale.ROOT))
                    Log.d("WeatherAPI", "API forecast weather description: $forecastDescription")

                    val forecastTimeKST = convertToSelectedTimeZone(forecast.dt_txt)
                    val matchesWeather = savedItem.weather == forecastDescription

                    // 알림을 보내야 하는 시간을 비교
                    val matchesTime = when (savedItem.wTime) {
                        "오늘 날씨 알림" -> isScheduledTime(6)
                        "내일 날씨 알림" -> isScheduledTime(21)
                        "즉시 알림" -> true
                        else -> false
                    }

                    Log.d("WeatherCheck", "Comparing saved weather: ${savedItem.weather} with forecast: $forecastDescription at time: $forecastTimeKST")
                    Log.d("WeatherCheck", "matchesWeather: $matchesWeather, matchesTime: $matchesTime")

                    matchesWeather && matchesTime
                }

                if (matchingForecast != null) {
                    Log.d("NotificationCheck", "Matching forecast found: ${matchingForecast.weather[0].description}, sending notification")
                    sendNotification(savedItem.wText, matchingForecast.weather[0].description)
                } else {
                    Log.d("NotificationCheck", "No matching forecast found for saved item: ${savedItem.wText}")
                }
            }
        } else {
            Log.d("WeatherCheck", "API call failed with code: ${response.code()}")
        }
    }

    // 알림을 보내는 메서드
    fun sendNotification(content: String, weatherDescription: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(context, WeatherActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

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

    // 특정 시간 범위에 알림을 보내는 조건을 확인하는 함수 (예: 5분 범위 내)
    private fun isScheduledTime(targetHour: Int, targetMinute: Int = 0, rangeMinutes: Int = 5): Boolean {
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
        }

        // 현재 시간과 목표 시간의 차이 계산 (밀리초 단위)
        val timeDifference = Math.abs(currentTime.timeInMillis - targetTime.timeInMillis)

        // 차이가 rangeMinutes 내라면 true 반환
        return timeDifference <= rangeMinutes * 60 * 1000
    }

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
            else -> "clear sky" // 기본값
        }
    }

    private fun convertToSelectedTimeZone(utcTime: String): String {
        val utcFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        utcFormat.timeZone = TimeZone.getTimeZone("UTC")
        val timeZoneId = "Asia/Seoul"

        val kstFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        kstFormat.timeZone = TimeZone.getTimeZone(timeZoneId)

        val parsedDate = utcFormat.parse(utcTime)
        return kstFormat.format(parsedDate!!)
    }
}
