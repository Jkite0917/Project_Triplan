package com.example.myapplication

import android.content.Context
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class MainScrollView(
    private val context: Context,
    private val city: String,
    private val selectedDate: String,
    private val linearLayoutMain: LinearLayout
) {

    private val apiKey = "74c26aef7529a784cee3247a261edd92"
    private val maxUIItems = 8 // UI에 표시할 최대 시간대

    fun getWeatherForecast() {
        val call = ApiClient.weatherApiService.getWeatherForecast(city, apiKey)

        call.enqueue(object : Callback<WeatherForecastResponse> {
            override fun onResponse(
                call: Call<WeatherForecastResponse>,
                response: Response<WeatherForecastResponse>
            ) {
                if (response.isSuccessful) {
                    val forecastResponse = response.body()
                    forecastResponse?.let {
                        val forecastList = it.list

                        // 현재 날짜 데이터 필터링
                        val currentDateForecasts = forecastList.filter { forecast ->
                            forecast.dt_txt.startsWith(selectedDate)
                        }

                        // 다음날 오전 06시 데이터 포함
                        val nextDateForecasts = forecastList.filter { forecast ->
                            forecast.dt_txt.startsWith(getNextDateString(selectedDate))
                        }

                        // 현재 날짜와 다음날 데이터 결합
                        val combinedForecasts = currentDateForecasts + nextDateForecasts

                        // 현재 시간 이후 데이터만 필터링 (UTC -> KST 변환)
                        val currentTime = System.currentTimeMillis()
                        val futureForecasts = combinedForecasts.filter { forecast ->
                            val forecastTime = parseUtcToKstTime(forecast.dt_txt)?.time ?: 0L
                            forecastTime >= currentTime
                        }

                        // UI 업데이트
                        futureForecasts.forEachIndexed { index, forecast ->
                            if (index < maxUIItems) { // UI 슬롯 제한 확인
                                updateWeatherUI(forecast, index)
                            }
                        }
                    }
                } else {
                    Log.e("API_ERROR", "Error code: ${response.code()}")
                    Log.e("API_ERROR", "city check, $city")
                }
            }

            override fun onFailure(call: Call<WeatherForecastResponse>, t: Throwable) {
                Log.e("API_FAILURE", "Error: ${t.message}")
            }
        })
    }

    // UTC 시간을 KST 시간으로 변환하는 함수
    private fun parseUtcToKstTime(utcDateTime: String): Date? {
        val utcDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        utcDateFormat.timeZone = TimeZone.getTimeZone("UTC") // UTC 시간대

        val kstDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        kstDateFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul") // 한국 시간대 (UTC+9)

        return utcDateFormat.parse(utcDateTime)?.let { utcDate ->
            kstDateFormat.parse(kstDateFormat.format(utcDate)) // KST로 변환된 시간 반환
        }
    }

    // UI 업데이트 함수
    private fun updateWeatherUI(forecast: Forecast, index: Int) {
        val timeTextView: TextView = linearLayoutMain.findViewById(getTimeTextViewId(index))
        val weatherImageView: ImageView = linearLayoutMain.findViewById(getImageViewId(index))
        val descriptionTextView: TextView = linearLayoutMain.findViewById(getWeatherDescriptionTextViewID(index))
        val temperatureTextView: TextView = linearLayoutMain.findViewById(getTemperatureTextViewId(index))
        val rainTextView: TextView = linearLayoutMain.findViewById(getRainTextViewId(index))

        val utcDateTime = forecast.dt_txt
        val kstDate = parseUtcToKstTime(utcDateTime)

        val kstTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val formattedTime = kstDate?.let { kstTimeFormat.format(it) } ?: "알 수 없음"

        val iconCode = forecast.weather.firstOrNull()?.icon ?: "01d"
        val weatherCategory = getWeatherCategory(iconCode)
        val description = getWeatherDescriptionInKorean(iconCode)
        val iconUrl = "https://openweathermap.org/img/wn/$weatherCategory@2x.png"

        timeTextView.text = formattedTime
        temperatureTextView.text = "${"%.1f".format(forecast.main.temp)}°C"
        rainTextView.text = "${"%.0f".format(forecast.pop?.times(100) ?: 0)}%"
        descriptionTextView.text = description

        Glide.with(context)
            .load(iconUrl)
            .into(weatherImageView)

        Log.e(
            "API_CONNECT",
            "Check var[ time: $formattedTime | temperature: ${forecast.main.temp} | rain: ${forecast.pop} | description: ${forecast.weather.firstOrNull()?.description} ]"
        )
        Log.e("API_CONNECT_ICON", "icon: $iconUrl")
    }

    // 다음날 날짜 계산 함수
    private fun getNextDateString(selectedDate: String): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = dateFormat.parse(selectedDate) ?: return selectedDate
        val calendar = Calendar.getInstance().apply { time = date }
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        return dateFormat.format(calendar.time)
    }

    // UI 요소 ID 가져오는 함수들
    private fun getTimeTextViewId(index: Int): Int = listOf(
        R.id.textview_mainScrollItem_time1,
        R.id.textview_mainScrollItem_time2,
        R.id.textview_mainScrollItem_time3,
        R.id.textview_mainScrollItem_time4,
        R.id.textview_mainScrollItem_time5,
        R.id.textview_mainScrollItem_time6,
        R.id.textview_mainScrollItem_time7,
        R.id.textview_mainScrollItem_time8
    )[index]

    private fun getTemperatureTextViewId(index: Int): Int = listOf(
        R.id.textview_mainScrollItem_temperature1,
        R.id.textview_mainScrollItem_temperature2,
        R.id.textview_mainScrollItem_temperature3,
        R.id.textview_mainScrollItem_temperature4,
        R.id.textview_mainScrollItem_temperature5,
        R.id.textview_mainScrollItem_temperature6,
        R.id.textview_mainScrollItem_temperature7,
        R.id.textview_mainScrollItem_temperature8
    )[index]

    private fun getImageViewId(index: Int): Int = listOf(
        R.id.imageview_mainScrollItem_weatherIcon1,
        R.id.imageview_mainScrollItem_weatherIcon2,
        R.id.imageview_mainScrollItem_weatherIcon3,
        R.id.imageview_mainScrollItem_weatherIcon4,
        R.id.imageview_mainScrollItem_weatherIcon5,
        R.id.imageview_mainScrollItem_weatherIcon6,
        R.id.imageview_mainScrollItem_weatherIcon7,
        R.id.imageview_mainScrollItem_weatherIcon8
    )[index]

    private fun getWeatherDescriptionTextViewID(index: Int): Int = listOf(
        R.id.textview_mainScrollItem_description1,
        R.id.textview_mainScrollItem_description2,
        R.id.textview_mainScrollItem_description3,
        R.id.textview_mainScrollItem_description4,
        R.id.textview_mainScrollItem_description5,
        R.id.textview_mainScrollItem_description6,
        R.id.textview_mainScrollItem_description7,
        R.id.textview_mainScrollItem_description8
    )[index]

    private fun getRainTextViewId(index: Int): Int = listOf(
        R.id.textview_mainScrollItem_rainText1,
        R.id.textview_mainScrollItem_rainText2,
        R.id.textview_mainScrollItem_rainText3,
        R.id.textview_mainScrollItem_rainText4,
        R.id.textview_mainScrollItem_rainText5,
        R.id.textview_mainScrollItem_rainText6,
        R.id.textview_mainScrollItem_rainText7,
        R.id.textview_mainScrollItem_rainText8
    )[index]

    private fun getWeatherCategory(iconCode: String): String = when (iconCode) {
        "01d", "01n" -> "01d"
        "02d", "02n", "03d", "03n", "04d", "04n" -> "03d"
        "10d", "10n" -> "10d"
        "09d", "09n" -> "09d"
        "11d", "11n" -> "11d"
        "13d", "13n" -> "13d"
        "50d", "50n" -> "50d"
        else -> "01d"
    }

    private fun getWeatherDescriptionInKorean(iconCode: String): String = when (iconCode) {
        "01d" -> "맑은 날씨 (낮)"
        "01n" -> "맑은 날씨 (밤)"
        "02d" -> "적은 구름 (낮)"
        "02n" -> "적은 구름 (밤)"
        "03d" -> "흐림 (낮)"
        "03n" -> "흐림 (밤)"
        "04d" -> "많은 구름 (낮)"
        "04n" -> "많은 구름 (밤)"
        "09d" -> "소나기 (낮)"
        "09n" -> "소나기 (밤)"
        "10d" -> "비 (낮)"
        "10n" -> "비 (밤)"
        "11d" -> "천둥 번개 (낮)"
        "11n" -> "천둥 번개 (밤)"
        "13d" -> "눈 (낮)"
        "13n" -> "눈 (밤)"
        "50d" -> "안개 (낮)"
        "50n" -> "안개 (밤)"
        else -> "알 수 없는 날씨"
    }
}
