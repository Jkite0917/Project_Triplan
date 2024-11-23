package com.example.myapplication

import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
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
            handleCurrentWeather(currentWeatherItems, apiService, apiKey, region, "현재 날씨")
        }

        // "당일 오전 6시" 알림 처리
        if (currentHour == 6 && currentMinute < 25) {
            handleDailyWeather(apiService, apiKey, region, savedWeatherItems, false, "당일 오전 6시")
        }

        // "전날 오후 9시" 알림 처리
        if (currentHour == 21 && currentMinute < 25) {
            handleDailyWeather(apiService, apiKey, region, savedWeatherItems, true, "전날 오후 9시")
        }
    }

    // 현재 날씨 처리 로직
    private suspend fun handleCurrentWeather(
        weatherItems: List<WeatherListItem>,
        apiService: WeatherApiService,
        apiKey: String,
        region: String,
        timeDescription: String
    ) {
        // OpenWeather API 호출
        val response = withContext(Dispatchers.IO) {
            apiService.getWeatherForecast(region, apiKey).execute()
        }

        if (response.isSuccessful) {
            val forecastList = response.body()?.list ?: return

            // API의 가장 최근 데이터로 현재 날씨 확인
            val forecastDescription = convertToCommonWeatherDescription(
                forecastList[0].weather[0].description.lowercase(Locale.KOREA)
            )

            Log.d("APIWeatherCheck", "$timeDescription - API 원본: ${forecastList[0].weather[0].description}, 변환된 값: $forecastDescription")

            // DB 항목과 API 값을 비교
            for (savedItem in weatherItems) {
                val savedDescription = savedItem.weather

                if (savedDescription == forecastDescription) {
                    if (!savedItem.isNotified) {
                        Log.d("WeatherNotification", "$timeDescription - 알림 전송: ${savedItem.weather}")
                        sendNotification(savedItem.contents, savedItem.weather) // 알림 전송
                        coroutineScope.launch { updateNotificationStatus(savedItem.wNo, true) } // 상태 업데이트
                    } else {
                        Log.d("WeatherNotification", "$timeDescription - 동일 날씨 조건: 알림 전송 안 함 (이미 전송됨)")
                    }
                } else {
                    Log.d("WeatherNotification", "$timeDescription - 날씨 변경으로 상태 초기화: ${savedItem.weather}")
                    coroutineScope.launch { updateNotificationStatus(savedItem.wNo, false) } // 상태 초기화
                }
            }
        } else {
            Log.e("WeatherAPIError", "$timeDescription - API 호출 실패: 코드=${response.code()}, 메시지=${response.message()}")
        }
    }

    // 하루치 데이터 비교 및 알림 처리
    private suspend fun handleDailyWeather(
        apiService: WeatherApiService,
        apiKey: String,
        region: String,
        savedWeatherItems: List<WeatherListItem>,
        isForTomorrow: Boolean, // true면 내일 데이터, false면 오늘 데이터
        timeDescription: String
    ) {
        // OpenWeather API 호출
        val response = withContext(Dispatchers.IO) {
            apiService.getWeatherForecast(region, apiKey).execute()
        }

        if (response.isSuccessful) {
            val forecastList = response.body()?.list ?: return

            // 오늘 또는 내일의 날짜 계산
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
            if (isForTomorrow) calendar.add(Calendar.DAY_OF_YEAR, 1) // 내일로 이동
            val targetDate = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(calendar.time)

            // 하루치 데이터 필터링
            val dailyForecasts = forecastList.filter { forecast ->
                forecast.dt_txt.startsWith(targetDate)
            }

            if (dailyForecasts.isNotEmpty()) {
                Log.d("DailyWeatherCheck", "$timeDescription - 대상 날짜: $targetDate, 데이터 개수: ${dailyForecasts.size}")

                // 중복 알림 방지를 위한 Set
                val alreadyNotifiedConditions = mutableSetOf<String>()

                // 하루치 데이터 순회하며 DB 조건과 비교
                for (forecast in dailyForecasts) {
                    val forecastDescription = convertToCommonWeatherDescription(
                        forecast.weather[0].description.lowercase(Locale.KOREA)
                    )

                    Log.d("APIWeatherCheck", "$timeDescription - 시간: ${forecast.dt_txt}, 날씨: $forecastDescription")

                    for (savedItem in savedWeatherItems) {
                        val savedDescription = savedItem.weather

                        // 1. 이미 알림이 발생한 조건은 스킵
                        if (alreadyNotifiedConditions.contains(savedDescription)) {
                            Log.d("WeatherNotification", "$timeDescription - 이미 알림이 발생한 조건: $savedDescription (시간: ${forecast.dt_txt})")
                            continue
                        }

                        // 2. 조건이 일치하고 아직 알림이 전송되지 않은 경우
                        if (savedDescription == forecastDescription && !savedItem.isNotified) {
                            Log.d("WeatherNotification", "$timeDescription - 알림 전송: ${savedItem.weather}")
                            sendNotification(savedItem.contents, savedItem.weather) // 알림 전송
                            coroutineScope.launch { updateNotificationStatus(savedItem.wNo, true) }
                            alreadyNotifiedConditions.add(savedDescription) // 알림 발생 조건 추가
                        } else if (savedDescription != forecastDescription) {
                            coroutineScope.launch { updateNotificationStatus(savedItem.wNo, false) } // 상태 초기화
                        }
                    }
                }
            } else {
                Log.d("DailyWeatherCheck", "$timeDescription - 대상 날짜에 해당하는 데이터가 없습니다.")
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
