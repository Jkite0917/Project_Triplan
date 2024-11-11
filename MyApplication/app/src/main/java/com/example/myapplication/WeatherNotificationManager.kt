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
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

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
                    val forecastTimeKST = convertToSelectedTimeZone(forecast.dt_txt)
                    val matchesWeather = savedItem.weather == getWeatherIconId(forecast.weather[0].description)

                    // "오는 순간"의 현재 시간 조건 추가 비교
                    val matchesTime = when (savedItem.wTime) {
                        "오기전날" -> isTomorrow(forecast.dt_txt)
                        "오는순간" -> isCurrentTime(forecast.dt_txt)
                        "하루종일" -> true
                        else -> false
                    }

                    // 로그 추가
                    Log.d("WeatherCheck", "Comparing saved weather: ${savedItem.weather} with forecast: ${forecast.weather[0].description} at time: $forecastTimeKST")
                    Log.d("WeatherCheck", "Current time matches: $matchesTime")

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
        return when (weatherDescription.toLowerCase(Locale.ROOT)) {
            // Clear Sky
            "clear sky" -> R.drawable.weather_sun_icon

            // Partly Cloudy (별도로 유지)
            "partly cloudy" -> R.drawable.weather_suncloud_icon

            // Clouds, Mist, Fog (구름 및 안개 상태 통합)
            "few clouds", "scattered clouds", "broken clouds", "overcast clouds", "mist", "fog", "haze" -> R.drawable.weather_cloud_icon

            // Rain and Drizzle
            "light rain", "moderate rain", "heavy intensity rain", "light intensity drizzle", "drizzle" -> R.drawable.weather_rain_icon

            // Thunderstorm
            "thunderstorm", "thunderstorm with light rain", "thunderstorm with heavy rain" -> R.drawable.weather_thunder_icon

            // Snow
            "light snow", "snow", "heavy snow" -> R.drawable.weather_snow_icon

            // 기본 아이콘 설정
            else -> R.drawable.weather_sun_icon
        }
    }

    // 내일 날짜인지 확인하는 함수
    private fun isTomorrow(forecastDate: String): Boolean {
        val forecastCalendar = Calendar.getInstance().apply {
            time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(forecastDate)!!
        }
        val tomorrowCalendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return forecastCalendar.get(Calendar.YEAR) == tomorrowCalendar.get(Calendar.YEAR) &&
                forecastCalendar.get(Calendar.DAY_OF_YEAR) == tomorrowCalendar.get(Calendar.DAY_OF_YEAR)
    }

    // 현재 시간을 기준으로 알림이 오도록 확인하는 함수
    private fun isCurrentTime(forecastTime: String): Boolean {
        val currentTime = System.currentTimeMillis()
        val forecastDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(forecastTime)

        return forecastDate?.time?.let { forecastTimeMillis ->
            // 현재 시간과 예보 시간이 동일한 3시간 간격 블록에 있는지 확인
            val threeHoursInMillis = 3 * 60 * 60 * 1000 // 3시간(밀리초)
            val difference = Math.abs(currentTime - forecastTimeMillis)

            // 디버그 로그 추가
            Log.d("TimeCheck", "Current time: $currentTime, Forecast time: $forecastTimeMillis, Difference: $difference")

            // 1.5시간 이내일 때 일치한다고 간주
            difference <= threeHoursInMillis / 2
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
