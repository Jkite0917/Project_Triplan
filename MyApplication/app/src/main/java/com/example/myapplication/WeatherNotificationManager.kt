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
    private val timeOffsetMillis = 1 * 60 * 1000 // 시간 오차: 1분 (59분에 API 호출하기 위한 설정)

    init {
        createNotificationChannel()
    }

    /**
     * 알림 채널 생성 (Android 8.0 이상 필요)
     * 앱이 실행될 때 알림을 전송하기 위한 채널을 생성합니다.
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
     * API에서 날씨 정보를 가져오고, 저장된 데이터와 비교하여 알림을 전송합니다.
     */
    suspend fun checkWeatherConditions(apiService: WeatherApiService, apiKey: String, region: String) {
        val apiCallTime = System.currentTimeMillis()
        val nextHour = calculateNextHour(apiCallTime) // 다음 정각 계산
        val apiFetchTime = nextHour - timeOffsetMillis // 정각 1분 전

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
                    "당일 오전 6시" -> handleScheduledNotification(savedItem, forecastList, true)
                    "전날 오후 9시" -> handleScheduledNotification(savedItem, forecastList, false)
                }
            }
        } else {
            Log.d("WeatherCheck", "API 호출 실패: 코드=${response.code()}")
        }
    }

    /**
     * "현재 날씨" 알림 처리
     * 현재 시간과 가장 가까운 예보를 기준으로 알림을 판단합니다.
     */
    private fun handleImmediateNotification(
        savedItem: WeatherListItem,
        forecastList: List<Forecast>
    ) {
        val forecastDescription = convertToCommonWeatherDescription(
            forecastList[0].weather[0].description.lowercase(Locale.KOREA)
        )
        val currentTime = System.currentTimeMillis()
        val forecastTimeMillis = parseForecastTime(forecastList[0].dt_txt)

        if (savedItem.weather == forecastDescription &&
            !savedItem.isNotified &&
            isWithinTimeOffset(currentTime, forecastTimeMillis)
        ) {
            sendNotification(savedItem.contents, forecastList[0].weather[0].description)
            coroutineScope.launch {
                updateNotificationStatus(savedItem.wNo, true) // 상태 업데이트
            }
            Log.d("WeatherCheck", "즉시 알림 조건 충족: wNo=${savedItem.wNo}")
        }
    }

    /**
     * "당일 오전 6시" 및 "전날 오후 9시" 알림 처리
     * 저장된 시간에 따라 오늘 또는 내일 데이터를 비교합니다.
     */
    private fun handleScheduledNotification(
        savedItem: WeatherListItem,
        forecastList: List<Forecast>,
        isMorning: Boolean // true = 오전 6시, false = 전날 오후 9시
    ) {
        val targetForecasts = forecastList.filter { forecast ->
            val forecastTimeMillis = parseForecastTime(forecast.dt_txt)
            val forecastDay = Calendar.getInstance().apply { timeInMillis = forecastTimeMillis }
            val currentDay = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))

            if (isMorning) {
                forecastDay.get(Calendar.DAY_OF_YEAR) == currentDay.get(Calendar.DAY_OF_YEAR)
            } else {
                forecastDay.get(Calendar.DAY_OF_YEAR) == currentDay.get(Calendar.DAY_OF_YEAR) + 1
            }
        }

        targetForecasts.forEach { forecast ->
            val forecastDescription = convertToCommonWeatherDescription(
                forecast.weather[0].description.lowercase(Locale.KOREA)
            )
            if (savedItem.weather == forecastDescription && !savedItem.isNotified) {
                sendNotification(savedItem.contents, savedItem.weather)
                coroutineScope.launch {
                    updateNotificationStatus(savedItem.wNo, true) // 상태 업데이트
                }
                Log.d("WeatherCheck", "${if (isMorning) "오전 6시" else "오후 9시"} 알림 조건 충족: wNo=${savedItem.wNo}")
            }
        }
    }

    /**
     * 다음 정각 계산 함수
     * 현재 시간을 기준으로 다음 정각(한국 시간)을 반환합니다.
     */
    private fun calculateNextHour(currentTime: Long): Long {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).apply {
            timeInMillis = currentTime
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.HOUR_OF_DAY, 1) // 한 시간 추가
        }
        return calendar.timeInMillis // 다음 정각의 밀리초 반환
    }

    /**
     * 시간 오차를 고려한 비교 함수
     */
    private fun isWithinTimeOffset(currentTime: Long, targetTime: Long): Boolean {
        return targetTime in (currentTime - timeOffsetMillis)..(currentTime + timeOffsetMillis)
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
     * 시간 포맷 함수
     * 로그 및 디버그 용도로 사용됩니다.
     */
    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        return sdf.format(Date(timestamp))
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
