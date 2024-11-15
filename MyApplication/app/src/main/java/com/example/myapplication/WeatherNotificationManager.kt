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

// WeatherNotificationManager 클래스: 날씨 조건에 따른 알림을 관리하는 클래스
class WeatherNotificationManager(val context: Context, val database: LocalDatabase) {

    private val channelId = "weather_notification_channel"

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

    // 날씨 조건을 확인하고 알림 조건이 일치할 때 알림을 보내는 메서드
    suspend fun checkWeatherConditions(apiService: WeatherApiService, apiKey: String, region: String) {
        // WeatherListDao에서 WeatherList 데이터 목록을 가져온 후, WeatherListItem으로 변환
        val savedWeatherItems = database.getWeatherTextDao().getAllWeatherList().map { weather ->
            WeatherListItem(
                wNo = weather.wNo,
                contents = weather.wText,
                weather = weather.weather,
                time = weather.wTime,
                isNotified = weather.isNotified
            )
        }

        val response = withContext(Dispatchers.IO) {
            apiService.getWeatherForecast(region, apiKey).execute()
        }

        if (response.isSuccessful) {
            val forecastList = response.body()?.list ?: return
            val currentTime = System.currentTimeMillis()

            // 오늘과 내일 알림 확인을 위해 설정한 시간이 맞는지 확인
            val isWithin6amRange = isWithinTimeRange(currentTime, 6)  // 6시 ±5분
            val isWithin9pmRange = isWithinTimeRange(currentTime, 21)  // 21시 ±5분

            for (savedItem in savedWeatherItems) {
                when (savedItem.time) {
                    "당일 오전 6시" -> {
                        if (isWithin6amRange) {
                            handleTodayNotification(savedItem, forecastList)
                        }
                    }
                    "전날 오후 9시" -> {
                        if (isWithin9pmRange) {
                            handleTomorrowNotification(savedItem, forecastList)
                        }
                    }
                    "현재 날씨" -> {
                        handleImmediateNotification(savedItem, forecastList, currentTime)
                    }
                }
            }
        } else {
            Log.d("WeatherCheck", "API call failed with code: ${response.code()}")
        }
    }

    // 오늘 날씨 알림 처리
    private suspend fun handleTodayNotification(savedItem: WeatherListItem, forecastList: List<Forecast>) {
        val today = Calendar.getInstance()
        for (forecast in forecastList) {
            val forecastTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(forecast.dt_txt)?.time ?: continue
            if (isSameDay(today.timeInMillis, forecastTime) && savedItem.weather == convertToCommonWeatherDescription(forecast.weather[0].description.lowercase(Locale.ROOT))) {
                if (!savedItem.isNotified) {
                    sendNotification(savedItem.contents, forecast.weather[0].description)
                    updateNotificationStatus(savedItem.wNo, true)
                    break
                }
            }
        }
    }

    // 내일 날씨 알림 처리
    private suspend fun handleTomorrowNotification(savedItem: WeatherListItem, forecastList: List<Forecast>) {
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        for (forecast in forecastList) {
            val forecastTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(forecast.dt_txt)?.time ?: continue
            if (isSameDay(tomorrow.timeInMillis, forecastTime) && savedItem.weather == convertToCommonWeatherDescription(forecast.weather[0].description.lowercase(Locale.ROOT))) {
                if (!savedItem.isNotified) {
                    sendNotification(savedItem.contents, forecast.weather[0].description)
                    updateNotificationStatus(savedItem.wNo, true)
                    break
                }
            }
        }
    }

    // 즉시 알림 처리 (1시간 정각마다 조건 확인)
    private suspend fun handleImmediateNotification(savedItem: WeatherListItem, forecastList: List<Forecast>, currentTime: Long) {
        val currentMinute = Calendar.getInstance().get(Calendar.MINUTE)
        if (currentMinute == 0) {  // 1시간 정각마다 확인
            for (forecast in forecastList) {
                val forecastTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(forecast.dt_txt)?.time ?: continue
                val forecastDescription = convertToCommonWeatherDescription(forecast.weather[0].description.lowercase(Locale.ROOT))
                if (abs(forecastTime - currentTime) <= 1 * 60 * 60 * 1000 && savedItem.weather == forecastDescription) {
                    if (!savedItem.isNotified) {
                        sendNotification(savedItem.contents, forecast.weather[0].description)
                        updateNotificationStatus(savedItem.wNo, true)
                        break
                    }
                }
            }
        }
    }

    // 특정 시간 ±5분 범위에 있는지 확인하는 메서드
    private fun isWithinTimeRange(currentTime: Long, targetHour: Int): Boolean {
        val targetCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val lowerBound = targetCalendar.timeInMillis - 5 * 60 * 1000  // -5분
        val upperBound = targetCalendar.timeInMillis + 5 * 60 * 1000  // +5분
        return currentTime in lowerBound..upperBound
    }

    // 두 시간 값이 같은 날인지 확인하는 메서드
    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    // 알림을 생성하고 전송하는 메서드
    private fun sendNotification(content: String, weatherDescription: String) {
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

    // weather 설명에 따른 아이콘을 가져오는 메서드
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
            else -> "clear sky"
        }
    }

    // 데이터베이스에서 알림 상태를 업데이트하는 메서드
    private suspend fun updateNotificationStatus(wNo: Long, isNotified: Boolean) {
        withContext(Dispatchers.IO) {
            database.getWeatherTextDao().updateNotificationStatus(wNo, isNotified)
        }
    }
}
