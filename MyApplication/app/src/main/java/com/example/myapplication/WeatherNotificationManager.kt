package com.example.myapplication

import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.util.*

class WeatherNotificationManager(val context: Context, private val database: LocalDatabase) {

    private val channelId = "weather_notification_channel"
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

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
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

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

        // "현재 날씨" 알림 처리
        val currentWeatherItems = savedWeatherItems.filter { it.time == "현재 날씨" }
        if (currentWeatherItems.isNotEmpty()) {
            handleWeather(currentWeatherItems, apiService, apiKey, region, "현재 날씨")
        }

        // "당일 오전 6시" 알림 처리
        if (currentHour == 6 && currentMinute < 5) {
            val morningItems = savedWeatherItems.filter { it.time == "당일 오전 6시" }
            if (morningItems.isNotEmpty()) {
                handleWeather(morningItems, apiService, apiKey, region, "당일 오전 6시")
            }
        }

        // "전날 오후 9시" 알림 처리
        if (currentHour == 21 && currentMinute < 5) {
            val eveningItems = savedWeatherItems.filter { it.time == "전날 오후 9시" }
            if (eveningItems.isNotEmpty()) {
                handleWeather(eveningItems, apiService, apiKey, region, "전날 오후 9시")
            }
        }
    }

    // 특정 시간에 따른 알림 처리
    private suspend fun handleWeather(
        weatherItems: List<WeatherListItem>,
        apiService: WeatherApiService,
        apiKey: String,
        region: String,
        timeDescription: String
    ) {
        val response = withContext(Dispatchers.IO) {
            apiService.getWeatherForecast(region, apiKey).execute()
        }

        if (response.isSuccessful) {
            val forecastList = response.body()?.list ?: return
            val forecastDescription = convertToCommonWeatherDescription(
                forecastList[0].weather[0].description.lowercase(Locale.KOREA)
            )

            // API 데이터와 변환된 값 로그
            Log.d("APIWeatherCheck", "$timeDescription - API 원본: ${forecastList[0].weather[0].description}, 변환된 값: $forecastDescription")

            for (savedItem in weatherItems) {
                Log.d(
                    "DBWeatherCheck",
                    "$timeDescription - DB에서 읽은 값 - ID: ${savedItem.wNo}, 날씨: ${savedItem.weather}, 내용: ${savedItem.contents}"
                )

                val savedDescription = savedItem.weather

                if (savedDescription == forecastDescription) {
                    Log.d("WeatherMatch", "$timeDescription - DB와 API 값 매칭 - 알림 준비: ${savedItem.weather}")
                    sendNotification(savedItem.contents, savedItem.weather)
                    coroutineScope.launch { updateNotificationStatus(savedItem.wNo, true) }
                } else {
                    Log.d("WeatherMatch", "$timeDescription - DB와 API 값 불일치 - DB=${savedDescription}, API=$forecastDescription")
                }
            }
        } else {
            Log.e("WeatherAPIError", "$timeDescription - API 호출 실패: 코드=${response.code()}, 메시지=${response.message()}")
        }
    }


    // 날씨 설명을 공통된 날씨 분류로 변환
    private fun convertToCommonWeatherDescription(description: String): String {
        return when (description.trim().lowercase(Locale.KOREA)) {
            "clear sky" -> "clear sky"
            "few clouds", "scattered clouds" -> "partly cloudy"
            "broken clouds", "overcast clouds", "mist" -> "clouds"
            "drizzle", "light rain", "moderate rain", "heavy intensity rain" -> "rain"
            "light thunderstorm", "thunderstorm", "heavy thunderstorm" -> "thunderstorm"
            "light snow", "snow", "heavy snow", "sleet" -> "snow"
            else -> "clear sky"
        }
    }

    // 알림 전송
    private fun sendNotification(content: String, savedWeather: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // DB에서 가져온 값과 매핑된 아이콘 확인
        val smallIcon = getWeatherIcon(savedWeather)
        Log.d("NotificationDebug", "알림 생성 - 내용: $content, 날씨: $savedWeather, 매핑된 아이콘 ID: $smallIcon")

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIcon)
            .setContentTitle("날씨 알림")
            .setContentText(content)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(savedWeather.hashCode(), notification)
    }



    // 알림 상태 업데이트
    private suspend fun updateNotificationStatus(wNo: Long, isNotified: Boolean) {
        withContext(Dispatchers.IO) {
            database.getWeatherTextDao().updateNotificationStatus(wNo, isNotified)
        }
        Log.d("WeatherNotificationManager", "알림 상태 업데이트: wNo=$wNo, isNotified=$isNotified")
    }

    // 날씨 설명에 따른 아이콘 반환
    private fun getWeatherIcon(weatherDescription: String): Int {
        val iconId = when (weatherDescription) {
            "clear sky" -> R.drawable.weather_sun_icon
            "clouds" -> R.drawable.weather_cloud_icon
            "rain" -> R.drawable.weather_rain_icon
            "thunderstorm" -> R.drawable.weather_thunder_icon
            "snow" -> R.drawable.weather_snow_icon
            "partly cloudy" -> R.drawable.weather_suncloud_icon
            else -> R.drawable.weather_sun_icon
        }
        Log.d("IconMapping", "날씨 설명: $weatherDescription → 매핑된 아이콘 ID: $iconId")
        return iconId
    }
}
