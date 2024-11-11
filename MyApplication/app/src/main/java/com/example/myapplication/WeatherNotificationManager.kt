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

            val currentTime = Calendar.getInstance()

            savedWeatherItems.forEach { savedItem ->
                val matchingForecast = forecastList.find { forecast ->
                    val forecastTimeKST = convertToSelectedTimeZone(forecast.dt_txt)
                    val matchesWeather = savedItem.weather == getWeatherIconId(forecast.weather[0].description)

                    // 조건에 따라 시간을 설정
                    val matchesTime = when (savedItem.wTime) {
                        "하루종일" -> {
                            // 하루종일은 오전 6시에 알림 설정
                            currentTime.set(Calendar.HOUR_OF_DAY, 6)
                            currentTime.set(Calendar.MINUTE, 0)
                            currentTime.set(Calendar.SECOND, 0)
                            isScheduledTime(forecastTimeKST, currentTime)
                        }
                        "오기전날" -> {
                            // 오기 전날은 오후 9시에 알림 설정
                            currentTime.set(Calendar.HOUR_OF_DAY, 21)
                            currentTime.set(Calendar.MINUTE, 0)
                            currentTime.set(Calendar.SECOND, 0)
                            isScheduledTime(forecastTimeKST, currentTime)
                        }
                        "오는순간" -> isCurrentTime(forecastTimeKST) // 현재 시간과 비교
                        else -> false
                    }

                    Log.d("WeatherCheck", "Comparing saved weather: ${savedItem.weather} with forecast: ${forecast.weather[0].description} at time: $forecastTimeKST")
                    matchesWeather && matchesTime
                }

                if (matchingForecast != null) {
                    Log.d("NotificationCheck", "Matching forecast found: ${matchingForecast.weather[0].description}, sending notification")
                    sendNotification(savedItem.wText, matchingForecast.weather[0].description)
                } else {
                    Log.d("NotificationCheck", "No matching forecast found for saved item: ${savedItem.wText}")
                }
            }
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
        return when (weatherDescription.toLowerCase()) {
            "clear" -> R.drawable.weather_sun_icon
            "clouds" -> R.drawable.weather_cloud_icon
            "rain" -> R.drawable.weather_rain_icon
            "thunderstorm" -> R.drawable.weather_thunder_icon
            "snow" -> R.drawable.weather_snow_icon
            "partly cloudy" -> R.drawable.weather_suncloud_icon
            else -> R.drawable.weather_sun_icon
        }
    }

    // 내일 날짜인지 확인하는 함수
    private fun isScheduledTime(forecastTime: String, scheduledTime: Calendar): Boolean {
        val forecastCalendar = Calendar.getInstance().apply {
            time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(forecastTime)!!
        }
        return forecastCalendar.get(Calendar.HOUR_OF_DAY) == scheduledTime.get(Calendar.HOUR_OF_DAY) &&
                forecastCalendar.get(Calendar.MINUTE) == scheduledTime.get(Calendar.MINUTE)
    }

    // 현재 시간을 기준으로 알림이 오도록 확인하는 함수
    private fun isCurrentTime(forecastTime: String): Boolean {
        val currentTime = System.currentTimeMillis()
        val forecastDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(forecastTime)

        return forecastDate?.time?.let { forecastTimeMillis ->
            // 현재 시간과 예보 시간이 동일한 3시간 간격 블록에 있는지 확인
            val threeHoursInMillis = 3 * 60 * 60 * 1000 // 3시간(밀리초)
            val difference = Math.abs(currentTime - forecastTimeMillis)
            difference <= threeHoursInMillis / 2 // 1.5시간 이내일 때 일치한다고 간주
        } ?: false
    }

    private fun convertToSelectedTimeZone(utcTime: String): String {
        val utcFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        utcFormat.timeZone = TimeZone.getTimeZone("UTC")

        val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val selectedRegion = sharedPreferences.getString("selectedRegion", "Seoul") ?: "Seoul"

        val timeZoneId = "Asia/Seoul"

        val kstFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        kstFormat.timeZone = TimeZone.getTimeZone(timeZoneId)

        val parsedDate = utcFormat.parse(utcTime)
        return kstFormat.format(parsedDate!!)
    }
}
