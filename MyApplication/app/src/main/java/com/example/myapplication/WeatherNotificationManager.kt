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
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // 날씨 조건 확인 및 알림
    suspend fun checkWeatherConditions(apiService: WeatherApiService, apiKey: String, region: String) {
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

            // 모든 날씨 설명에 대해 변환 적용
            forecastList.forEach { forecast ->
                val originalDescription = forecast.weather[0].description
                val convertedDescription = convertToCommonWeatherDescription(originalDescription)

                // 변환된 설명 출력
                Log.d("WeatherCheck", "API에서 받은 날씨 설명: $originalDescription -> 변환된 날씨 설명: $convertedDescription")
            }

            // 나머지 로직 처리
            val currentTime = System.currentTimeMillis()
            val isWithin6amRange = isWithinTimeRange(currentTime, 6)
            val isWithin9pmRange = isWithinTimeRange(currentTime, 21)

            for (savedItem in savedWeatherItems) {
                // "현재 날씨" 조건에서만 중복 방지 로직 적용
                if (savedItem.time == "현재 날씨") {
                    val forecastDescription = convertToCommonWeatherDescription(forecastList[0].weather[0].description.lowercase(Locale.getDefault()))
                    if (savedItem.weather != forecastDescription) {
                        updateNotificationStatus(savedItem.wNo, false) // 상태 초기화
                    }
                }

                when (savedItem.time) {
                    "당일 오전 6시" -> if (isWithin6amRange) handleTodayNotification(savedItem, forecastList)
                    "전날 오후 9시" -> if (isWithin9pmRange) handleTomorrowNotification(savedItem, forecastList)
                    "현재 날씨" -> handleImmediateNotification(savedItem, forecastList, currentTime)
                }
            }
        } else {
            Log.d("WeatherCheck", "API call failed with code: ${response.code()}")
        }
    }


    private fun handleTodayNotification(savedItem: WeatherListItem, forecastList: List<Forecast>) {
        val today = Calendar.getInstance()
        for (forecast in forecastList) {
            val forecastTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(forecast.dt_txt)?.time ?: continue
            if (isSameDay(today.timeInMillis, forecastTime) &&
                savedItem.weather == convertToCommonWeatherDescription(forecast.weather[0].description.lowercase(Locale.ROOT))) {
                // 조건이 맞으면 바로 알림 전송
                sendNotification(savedItem.contents, forecast.weather[0].description)
                break
            }
        }
    }

    private fun handleTomorrowNotification(savedItem: WeatherListItem, forecastList: List<Forecast>) {
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        for (forecast in forecastList) {
            val forecastTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(forecast.dt_txt)?.time ?: continue
            if (isSameDay(tomorrow.timeInMillis, forecastTime) &&
                savedItem.weather == convertToCommonWeatherDescription(forecast.weather[0].description.lowercase(Locale.ROOT))) {
                // 조건이 맞으면 바로 알림 전송
                sendNotification(savedItem.contents, forecast.weather[0].description)
                break
            }
        }
    }

    private suspend fun handleImmediateNotification(savedItem: WeatherListItem, forecastList: List<Forecast>, currentTime: Long) {
        val currentMinute = Calendar.getInstance().get(Calendar.MINUTE)
        if (currentMinute == 0) {  // 1시간 정각마다 확인
            for (forecast in forecastList) {
                val forecastTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(forecast.dt_txt)?.time ?: continue
                val forecastDescription = convertToCommonWeatherDescription(forecast.weather[0].description.lowercase(Locale.ROOT))
                if (abs(forecastTime - currentTime) <= 1 * 60 * 60 * 1000 &&
                    savedItem.weather == forecastDescription) {
                    if (!savedItem.isNotified) {
                        sendNotification(savedItem.contents, forecast.weather[0].description)
                        updateNotificationStatus(savedItem.wNo, true)
                        break
                    }
                }
            }
        }
    }

    private fun isWithinTimeRange(currentTime: Long, targetHour: Int): Boolean {
        val targetCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val lowerBound = targetCalendar.timeInMillis - 5 * 60 * 1000
        val upperBound = targetCalendar.timeInMillis + 5 * 60 * 1000
        return currentTime in lowerBound..upperBound
    }

    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

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

    private fun convertToCommonWeatherDescription(description: String): String {
        // 소문자 변환 후 양쪽 공백 제거
        val lowerCaseDescription = description.trim().lowercase(Locale.getDefault())

        val commonDescription = when (lowerCaseDescription) {
            "clear sky" -> "clear sky"
            "few clouds", "scattered clouds" -> "partly cloudy"
            "broken clouds", "overcast clouds", "mist" -> "few clouds"
            "drizzle", "light rain", "moderate rain", "heavy intensity rain", "very heavy rain",
            "extreme rain", "freezing rain", "light intensity shower rain", "shower rain",
            "heavy intensity shower rain", "ragged shower rain" -> "light rain"
            "light thunderstorm", "thunderstorm", "heavy thunderstorm" -> "thunderstorm"
            "light snow", "snow", "heavy snow", "sleet", "light shower sleet", "shower sleet",
            "light rain and snow", "rain and snow", "light shower snow", "shower snow", "heavy shower snow" -> "snow"
            else -> "clear sky"  // default fallback description
        }

        // 변환된 날씨 설명 로그로 출력
        Log.d("WeatherCheck", "날씨 설명 변환: $description -> $commonDescription")
        Log.d("WeatherCheck", "원래 날씨 설명: $description -> 소문자 변환: $lowerCaseDescription")

        return commonDescription
    }



    private suspend fun updateNotificationStatus(wNo: Long, isNotified: Boolean) {
        withContext(Dispatchers.IO) {
            database.getWeatherTextDao().updateNotificationStatus(wNo, isNotified)
        }
    }
}
