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

    /**
     * 알림 채널 생성 (Android 8.0 이상 필요)
     */
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

    /**
     * 날씨 조건 확인 및 알림 처리
     */
    suspend fun checkWeatherConditions(apiService: WeatherApiService, apiKey: String, region: String) {
        val apiCallTime = System.currentTimeMillis()

        // API 호출
        val response = withContext(Dispatchers.IO) {
            apiService.getWeatherForecast(region, apiKey).execute()
        }

        if (response.isSuccessful) {
            val forecastList = response.body()?.list ?: return
            Log.d("WeatherCheck", "API 응답에서 가져온 날씨 항목: ${forecastList.size}")

            val savedWeatherItems = database.getWeatherTextDao().getAllWeatherList().map { weather ->
                WeatherListItem(
                    wNo = weather.wNo,
                    contents = weather.wText,
                    weather = weather.weather,
                    time = weather.wTime,
                    isNotified = weather.isNotified // 저장된 상태 유지
                )
            }

            // 저장된 알림 데이터를 기준으로 처리
            savedWeatherItems.forEach { savedItem ->
                when (savedItem.time) {
                    "현재 날씨" -> handleImmediateNotification(savedItem, forecastList)
                    "당일 오전 6시", "전날 오후 9시" -> handleScheduledNotification(savedItem, forecastList, apiCallTime)
                }
            }
        } else {
            Log.d("WeatherCheck", "API 호출 실패: 코드=${response.code()}")
        }
    }

    /**
     * "현재 날씨" 알림 처리
     */
    private fun handleImmediateNotification(
        savedItem: WeatherListItem,
        forecastList: List<Forecast>
    ) {
        val forecastDescription = convertToCommonWeatherDescription(
            forecastList[0].weather[0].description.lowercase(Locale.KOREA)
        )

        if (savedItem.weather == forecastDescription && !savedItem.isNotified) {
            sendNotification(savedItem.contents, forecastList[0].weather[0].description)
            coroutineScope.launch {
                updateNotificationStatus(savedItem.wNo, true) // 상태 업데이트
            }
            Log.d("WeatherCheck", "즉시 알림 조건 충족: wNo=${savedItem.wNo}")
        }
    }

    /**
     * "당일 오전 6시" 및 "전날 오후 9시" 알림 처리
     */
    private fun handleScheduledNotification(
        savedItem: WeatherListItem,
        forecastList: List<Forecast>,
        apiCallTime: Long
    ) {
        val targetHour = when (savedItem.time) {
            "당일 오전 6시" -> 6
            "전날 오후 9시" -> 21
            else -> return
        }

        val isWithinScheduledTime = when (targetHour) {
            6 -> isWithinTime(apiCallTime, 5, 58, 6, 0)
            21 -> isWithinTime(apiCallTime, 20, 58, 21, 0)
            else -> false
        }

        if (isWithinScheduledTime) {
            val targetForecasts = forecastList.filter { forecast ->
                val forecastTimeMillis = parseForecastTime(forecast.dt_txt)
                val forecastHour = Calendar.getInstance().apply { timeInMillis = forecastTimeMillis }
                    .get(Calendar.HOUR_OF_DAY)

                forecastHour == targetHour
            }

            if (targetForecasts.isNotEmpty()) {
                val combinedDescriptions = targetForecasts.joinToString(", ") { forecast ->
                    convertToCommonWeatherDescription(forecast.weather[0].description)
                }

                if (combinedDescriptions.contains(savedItem.weather) && !savedItem.isNotified) {
                    sendNotification(savedItem.contents, savedItem.weather)
                    coroutineScope.launch {
                        updateNotificationStatus(savedItem.wNo, true)
                    }
                    Log.d("WeatherCheck", "${targetHour}시 알림 조건 충족: wNo=${savedItem.wNo}")
                }
            }
        }
    }

    /**
     * 특정 시간 범위 확인 함수
     */
    private fun isWithinTime(apiCallTime: Long, startHour: Int, startMinute: Int, endHour: Int, endMinute: Int): Boolean {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))

        // 범위 시작 시간
        val startTime = calendar.apply {
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMinute)
            set(Calendar.SECOND, 0)
        }.timeInMillis

        // 범위 종료 시간
        val endTime = calendar.apply {
            set(Calendar.HOUR_OF_DAY, endHour)
            set(Calendar.MINUTE, endMinute)
            set(Calendar.SECOND, 0)
        }.timeInMillis

        return apiCallTime in startTime..endTime
    }

    /**
     * Forecast 시간 문자열을 밀리초로 변환
     */
    private fun parseForecastTime(timeString: String): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        return sdf.parse(timeString)?.time ?: 0L
    }

    /**
     * 알림 상태 업데이트
     */
    private suspend fun updateNotificationStatus(wNo: Long, isNotified: Boolean) {
        withContext(Dispatchers.IO) {
            database.getWeatherTextDao().updateNotificationStatus(wNo, isNotified)
        }
        Log.d("WeatherNotificationManager", "알림 상태 업데이트 완료: wNo=$wNo, isNotified=$isNotified")
    }

    /**
     * 알림 전송
     */
    private fun sendNotification(content: String, weatherDescription: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(context, WeatherActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val smallIcon = getWeatherIcon(weatherDescription)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIcon)
            .setContentTitle("날씨 알림!")
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    /**
     * 날씨 설명 변환
     */
    private fun convertToCommonWeatherDescription(description: String): String {
        return when (description.trim().lowercase(Locale.KOREA)) {
            "clear sky" -> "clear sky"
            "few clouds", "scattered clouds" -> "partly cloudy"
            "broken clouds", "overcast clouds", "mist" -> "few clouds"
            "drizzle", "light rain", "moderate rain", "heavy intensity rain" -> "light rain"
            "light thunderstorm", "thunderstorm", "heavy thunderstorm" -> "thunderstorm"
            "light snow", "snow", "heavy snow", "sleet" -> "snow"
            else -> "clear sky"
        }
    }

    /**
     * 날씨 아이콘 반환
     */
    private fun getWeatherIcon(description: String): Int {
        return when (convertToCommonWeatherDescription(description)) {
            "clear sky" -> R.drawable.weather_sun_icon
            "partly cloudy" -> R.drawable.weather_suncloud_icon
            "few clouds" -> R.drawable.weather_cloud_icon
            "light rain" -> R.drawable.weather_rain_icon
            "thunderstorm" -> R.drawable.weather_thunder_icon
            "snow" -> R.drawable.weather_snow_icon
            else -> R.drawable.weather_sun_icon
        }
    }
}
