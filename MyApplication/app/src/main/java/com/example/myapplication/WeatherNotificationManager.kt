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

            // SharedPreferences 준비
            val prefs = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE)
            val lastNotifiedWeather = prefs.getString("lastWeather", "")
            val lastNotifiedTime = prefs.getString("lastTime", "")

            savedWeatherItems.forEach { savedItem ->
                val matchingForecast = forecastList.find { forecast ->
                    val forecastDescription = forecast.weather[0].description.lowercase(Locale.ROOT)
                    val forecastTimeKST = convertToSelectedTimeZone(forecast.dt_txt)

                    val matchesWeather = savedItem.weather == forecastDescription

                    // 오는순간 조건: 시간 비교 없이, 이전과 동일한 알림인지 확인
                    val shouldNotify = savedItem.wTime == "오는순간" &&
                            (lastNotifiedWeather != forecastDescription || lastNotifiedTime != forecastTimeKST)

                    if (shouldNotify && matchesWeather) {
                        // 알림을 보내고 상태 업데이트
                        sendNotification(savedItem.wText, forecastDescription)
                        prefs.edit().putString("lastWeather", forecastDescription).apply()
                        prefs.edit().putString("lastTime", forecastTimeKST).apply()
                        true
                    } else {
                        false
                    }
                }

                // 다른 조건들: 하루종일, 오기전날 조건은 기존 로직대로 시간을 확인하며 알림
                if (savedItem.wTime != "오는순간" && matchingForecast != null) {
                    // 기존 로직으로 알림을 처리
                    sendNotification(savedItem.wText, matchingForecast.weather[0].description)
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

    private fun convertToSelectedTimeZone(utcTime: String): String {
        val utcFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        utcFormat.timeZone = TimeZone.getTimeZone("UTC")

        val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        sharedPreferences.getString("selectedRegion", "Seoul") ?: "Seoul"
        val timeZoneId = "Asia/Seoul"

        val kstFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        kstFormat.timeZone = TimeZone.getTimeZone(timeZoneId)

        val parsedDate = utcFormat.parse(utcTime)
        return kstFormat.format(parsedDate!!)
    }
}
