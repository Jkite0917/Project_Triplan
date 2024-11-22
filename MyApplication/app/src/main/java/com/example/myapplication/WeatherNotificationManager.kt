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

// 시간 범위를 나타내는 데이터 클래스
data class TimeRange(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
)

class WeatherNotificationManager(val context: Context, private val database: LocalDatabase) {

    private val channelId = "weather_notification_channel"
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // 시간 범위 상수 정의
    companion object {
        val MORNING_TIME = TimeRange(5, 58, 6, 0)  // 당일 오전 6시
        val EVENING_TIME = TimeRange(20, 58, 21, 0) // 전날 오후 9시
    }

    init {
        createNotificationChannel()
    }

    // 알림 채널 생성 (Android 8.0 이상 필요)
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

    // 날씨 조건 확인 및 알림 처리
    suspend fun checkWeatherConditions(apiService: WeatherApiService, apiKey: String, region: String) {
        val apiCallTime = System.currentTimeMillis()

        // 저장된 날씨 알림 리스트 가져오기
        val savedWeatherItems = database.getWeatherTextDao().getAllWeatherList().map { weather ->
            WeatherListItem(
                wNo = weather.wNo,
                contents = weather.wText,
                weather = weather.weather,
                time = weather.wTime,
                isNotified = weather.isNotified
            )
        }

        // 시간 종류별로 리스트 필터링
        val currentWeatherItems = savedWeatherItems.filter { it.time == "현재 날씨" }
        val morningWeatherItems = savedWeatherItems.filter { it.time == "당일 오전 6시" }
        val eveningWeatherItems = savedWeatherItems.filter { it.time == "전날 오후 9시" }

        // "현재 날씨" 알림 처리
        if (currentWeatherItems.isNotEmpty()) {
            handleCurrentWeather(currentWeatherItems, apiService, apiKey, region)
        }

        // "당일 오전 6시" 알림 처리
        if (morningWeatherItems.isNotEmpty() && isWithinTime(apiCallTime, MORNING_TIME)) {
            handleMorningWeather(morningWeatherItems, apiService, apiKey, region)
        }

        // "전날 오후 9시" 알림 처리
        if (eveningWeatherItems.isNotEmpty() && isWithinTime(apiCallTime, EVENING_TIME)) {
            handleEveningWeather(eveningWeatherItems, apiService, apiKey, region)
        }
    }

    // 현재 날씨" 알림 처리
    private suspend fun handleCurrentWeather(
        weatherItems: List<WeatherListItem>,
        apiService: WeatherApiService,
        apiKey: String,
        region: String
    ) {
        // API 호출하여 현재 날씨 정보 가져오기
        val response = withContext(Dispatchers.IO) {
            apiService.getWeatherForecast(region, apiKey).execute()
        }

        if (response.isSuccessful) {
            val forecastList = response.body()?.list ?: return
            val nearestForecast = forecastList[0] // 가장 가까운 시간대의 예보
            val forecastDescription = convertToCommonWeatherDescription(
                nearestForecast.weather[0].description.lowercase(Locale.KOREA)
            )

            // 각 저장된 아이템과 비교하여 알림 조건 확인
            for (savedItem in weatherItems) {
                if (savedItem.weather == forecastDescription) {
                    if (!savedItem.isNotified) {
                        sendNotification(savedItem.contents, nearestForecast.weather[0].description)
                        coroutineScope.launch {
                            updateNotificationStatus(savedItem.wNo, true) // 알림 상태 업데이트
                        }
                        Log.d("WeatherCheck", "현재 날씨 알림 전송: wNo=${savedItem.wNo}")
                    }
                } else {
                    // 날씨가 다르면 isNotified를 false로 설정
                    if (savedItem.isNotified) {
                        coroutineScope.launch {
                            updateNotificationStatus(savedItem.wNo, false)
                        }
                    }
                }
            }
        } else {
            Log.d("WeatherCheck", "현재 날씨 API 호출 실패: 코드=${response.code()}")
        }
    }

    // 당일 오전 6시" 알림 처리
    private suspend fun handleMorningWeather(
        weatherItems: List<WeatherListItem>,
        apiService: WeatherApiService,
        apiKey: String,
        region: String
    ) {
        // API 호출하여 오늘의 날씨 예보 가져오기
        val response = withContext(Dispatchers.IO) {
            apiService.getWeatherForecast(region, apiKey).execute()
        }

        if (response.isSuccessful) {
            val forecastList = response.body()?.list ?: return

            // 오늘 날짜의 예보만 필터링
            val todayForecasts = forecastList.filter { forecast ->
                isToday(parseForecastTime(forecast.dt_txt))
            }

            // 오늘의 날씨 예보 중 날씨 설명들을 가져와서 병합
            val combinedDescriptions = todayForecasts.joinToString(", ") { forecast ->
                convertToCommonWeatherDescription(forecast.weather[0].description)
            }

            // 각 저장된 아이템과 비교하여 알림 조건 확인
            for (savedItem in weatherItems) {
                if (combinedDescriptions.contains(savedItem.weather)) {
                    sendNotification(savedItem.contents, savedItem.weather)
                    Log.d("WeatherCheck", "당일 오전 6시 알림 전송: wNo=${savedItem.wNo}")
                }
            }
        } else {
            Log.d("WeatherCheck", "당일 오전 6시 API 호출 실패: 코드=${response.code()}")
        }
    }

    // 전날 오후 9시" 알림 처리
    private suspend fun handleEveningWeather(
        weatherItems: List<WeatherListItem>,
        apiService: WeatherApiService,
        apiKey: String,
        region: String
    ) {
        // API 호출하여 내일의 날씨 예보 가져오기
        val response = withContext(Dispatchers.IO) {
            apiService.getWeatherForecast(region, apiKey).execute()
        }

        if (response.isSuccessful) {
            val forecastList = response.body()?.list ?: return

            // 내일 날짜의 예보만 필터링
            val tomorrowForecasts = forecastList.filter { forecast ->
                isTomorrow(parseForecastTime(forecast.dt_txt))
            }

            // 내일의 날씨 예보 중 날씨 설명들을 가져와서 병합
            val combinedDescriptions = tomorrowForecasts.joinToString(", ") { forecast ->
                convertToCommonWeatherDescription(forecast.weather[0].description)
            }

            // 각 저장된 아이템과 비교하여 알림 조건 확인
            for (savedItem in weatherItems) {
                if (combinedDescriptions.contains(savedItem.weather)) {
                    sendNotification(savedItem.contents, savedItem.weather)
                    Log.d("WeatherCheck", "전날 오후 9시 알림 전송: wNo=${savedItem.wNo}")
                }
            }
        } else {
            Log.d("WeatherCheck", "전날 오후 9시 API 호출 실패: 코드=${response.code()}")
        }
    }

    // 특정 시간 범위 확인 함수
    private fun isWithinTime(apiCallTime: Long, timeRange: TimeRange): Boolean {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))

        // 범위 시작 시간 설정
        val startTime = calendar.clone() as Calendar
        startTime.set(Calendar.HOUR_OF_DAY, timeRange.startHour)
        startTime.set(Calendar.MINUTE, timeRange.startMinute)
        startTime.set(Calendar.SECOND, 0)
        startTime.set(Calendar.MILLISECOND, 0)

        // 범위 종료 시간 설정
        val endTime = calendar.clone() as Calendar
        endTime.set(Calendar.HOUR_OF_DAY, timeRange.endHour)
        endTime.set(Calendar.MINUTE, timeRange.endMinute)
        endTime.set(Calendar.SECOND, 0)
        endTime.set(Calendar.MILLISECOND, 0)

        return apiCallTime in startTime.timeInMillis..endTime.timeInMillis
    }

    // 시간 문자열에서 밀리초로 변환
    private fun parseForecastTime(timeString: String): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        return sdf.parse(timeString)?.time ?: 0L
    }

    // 주어진 시간이 오늘인지 확인
    private fun isToday(timeMillis: Long): Boolean {
        val today = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
        val targetDay = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            timeZone = TimeZone.getTimeZone("Asia/Seoul")
        }
        return today.get(Calendar.YEAR) == targetDay.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == targetDay.get(Calendar.DAY_OF_YEAR)
    }

    // 주어진 시간이 내일인지 확인
    private fun isTomorrow(timeMillis: Long): Boolean {
        val tomorrow = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val targetDay = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            timeZone = TimeZone.getTimeZone("Asia/Seoul")
        }
        return tomorrow.get(Calendar.YEAR) == targetDay.get(Calendar.YEAR) &&
                tomorrow.get(Calendar.DAY_OF_YEAR) == targetDay.get(Calendar.DAY_OF_YEAR)
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

    // 날씨 설명을 공통된 날씨 분류로 변환
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

    // 날씨 설명에 따른 아이콘 반환
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

    //알림 상태 업데이트
    private suspend fun updateNotificationStatus(wNo: Long, isNotified: Boolean) {
        withContext(Dispatchers.IO) {
            database.getWeatherTextDao().updateNotificationStatus(wNo, isNotified)
        }
        Log.d("WeatherNotificationManager", "알림 상태 업데이트: wNo=$wNo, isNotified=$isNotified")
    }
}
