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

class WeatherHelper(
    private val context: Context,
    private val city: String,
    private val selectedDate: String,
    private val linearLayoutMain: LinearLayout
) {

    private val apiKey = "74c26aef7529a784cee3247a261edd92"

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

                        // selectedDate에 해당하는 예보 항목 찾기
                        val selectedForecasts = forecastList.filter { forecast ->
                            // forecast.dt_txt 형식은 "yyyy-MM-dd HH:mm:ss"이고, selectedDate는 "yyyy-MM-dd" 형식이라고 가정
                            forecast.dt_txt.startsWith(selectedDate)  // 날짜만 비교
                        }

                        // selectedForecasts에 맞는 데이터를 UI에 업데이트
                        selectedForecasts.forEachIndexed { index, forecast ->
                            updateWeatherUI(forecast, index)
                        }
                    }
                } else {
                    Log.e("API_ERROR", "Error code: ${response.code()}")
                    Log.e("API_ERROR", "city check, ${city}")
                }
            }

            override fun onFailure(call: Call<WeatherForecastResponse>, t: Throwable) {
                Log.e("API_FAILURE", "Error: ${t.message}")
            }
        })
    }

    // 공통적으로 날씨 정보를 업데이트하는 함수
    private fun updateWeatherUI(forecast: Forecast, index: Int) {
        // UI 요소 가져오기
        val timeTextView: TextView = linearLayoutMain.findViewById(getTimeTextViewId(index))
        val weatherImageView: ImageView = linearLayoutMain.findViewById(getImageViewId(index))  // 날씨 아이콘 추가
        val descriptionTextView: TextView = linearLayoutMain.findViewById(getWeatherDescriptionTextViewID(index))  // 날씨 설명 추가
        val temperatureTextView: TextView = linearLayoutMain.findViewById(getTemperatureTextViewId(index))
        val rainTextView: TextView = linearLayoutMain.findViewById(getRainTextViewId(index))

        // 시간 형식 변경 (UTC -> KST)
        val utcDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        utcDateFormat.timeZone = TimeZone.getTimeZone("UTC")  // API에서 제공하는 시간은 UTC 기준
        val date = utcDateFormat.parse(forecast.dt_txt)

        val kstDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        kstDateFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")  // 한국 시간대로 설정
        val formattedTime = kstDateFormat.format(date)

        // 날씨 아이콘 로딩 (Glide 사용)
        val iconCode = forecast.weather.firstOrNull()?.icon ?: "01d" // 기본 아이콘 코드
        val weatherCategory = getWeatherCategory(iconCode) // 아이콘 코드 필터링
        val description = getWeatherDescriptionInKorean(iconCode) // 날씨 설명 한글번역
        val iconUrl = "https://openweathermap.org/img/wn/$weatherCategory@2x.png"

        // UI 업데이트
        timeTextView.text = formattedTime // 시간 설정
        temperatureTextView.text = "${"%.1f".format(forecast.main.temp)}°C" // 온도 설정
        rainTextView.text = "${"%.0f".format(forecast.pop?.times(100) ?: 0)}%" // 강수 확률 설정 (0~100%)
        descriptionTextView.text = description // 날씨 설명 설정

        // Glide를 사용해 아이콘 설정
        Glide.with(context)
            .load(iconUrl)  // 아이콘 URL 로드
            .into(weatherImageView)  // 해당 ImageView에 아이콘 적용

        Log.e("API_CONNECT", "Check var[ time: ${formattedTime} | temperature: ${forecast.main.temp} | rain: ${forecast.pop} | description: ${forecast.weather.firstOrNull()?.description} ] ")
        Log.e("API_CONNECT_ICON", "icon: $iconUrl")
    }

    // 각 TextView ID를 동적으로 가져오는 함수들
    private fun getTimeTextViewId(index: Int): Int {
        return when (index) {
            0 -> R.id.textview_mainScrollItem_time1
            1 -> R.id.textview_mainScrollItem_time2
            2 -> R.id.textview_mainScrollItem_time3
            3 -> R.id.textview_mainScrollItem_time4
            4 -> R.id.textview_mainScrollItem_time5
            5 -> R.id.textview_mainScrollItem_time6
            6 -> R.id.textview_mainScrollItem_time7
            7 -> R.id.textview_mainScrollItem_time8
            else -> throw IllegalArgumentException("Invalid index")
        }
    }

    private fun getTemperatureTextViewId(index: Int): Int {
        return when (index) {
            0 -> R.id.textview_mainScrollItem_temperature1
            1 -> R.id.textview_mainScrollItem_temperature2
            2 -> R.id.textview_mainScrollItem_temperature3
            3 -> R.id.textview_mainScrollItem_temperature4
            4 -> R.id.textview_mainScrollItem_temperature5
            5 -> R.id.textview_mainScrollItem_temperature6
            6 -> R.id.textview_mainScrollItem_temperature7
            7 -> R.id.textview_mainScrollItem_temperature8
            else -> throw IllegalArgumentException("Invalid index")
        }
    }

    private fun getImageViewId(index: Int): Int {
        return when (index) {
            0 -> R.id.imageview_mainScrollItem_weatherIcon1
            1 -> R.id.imageview_mainScrollItem_weatherIcon2
            2 -> R.id.imageview_mainScrollItem_weatherIcon3
            3 -> R.id.imageview_mainScrollItem_weatherIcon4
            4 -> R.id.imageview_mainScrollItem_weatherIcon5
            5 -> R.id.imageview_mainScrollItem_weatherIcon6
            6 -> R.id.imageview_mainScrollItem_weatherIcon7
            7 -> R.id.imageview_mainScrollItem_weatherIcon8
            else -> throw IllegalArgumentException("Invalid index")
        }
    }

    fun getWeatherCategory(iconCode: String): String {
        return when (iconCode) {
            // 맑은 날씨  낮, 밤 -> 낮
            "01d", "01n" -> "01d"
            // 흐림     적은 구름 낮/밤, 흐림 낮/밤, 구름이 많음 낮/밤 -> 흐림
            "02d", "02n", "03d", "03n", "04d", "04n" -> "03d"
            // 비
            "10d" -> "10d"
            // 비 (밤)
            "10n" -> "10n"
            // 소나기 (둘다 같음)
            "09d", "09n" -> "09d"
            // 천둥번개 (둘다 같음)
            "11d", "11n" -> "11d"
            // 눈 (둘다 같음)
            "13d", "13n" -> "13d"
            // 안개 (둘다 같음)
            "50d", "50n" -> "50d"

            else -> "알 수 없는 날씨"
        }
    }

    private fun getWeatherDescriptionTextViewID(index: Int): Int {
        return when (index) {
            0 -> R.id.textview_mainScrollItem_description1
            1 -> R.id.textview_mainScrollItem_description2
            2 -> R.id.textview_mainScrollItem_description3
            3 -> R.id.textview_mainScrollItem_description4
            4 -> R.id.textview_mainScrollItem_description5
            5 -> R.id.textview_mainScrollItem_description6
            6 -> R.id.textview_mainScrollItem_description7
            7 -> R.id.textview_mainScrollItem_description8
            else -> throw IllegalArgumentException("Invalid index")
        }
    }

    fun getWeatherDescriptionInKorean(iconCode: String): String {
        return when (iconCode) {
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

    private fun getRainTextViewId(index: Int): Int {
        return when (index) {
            0 -> R.id.textview_mainScrollItem_rainText1
            1 -> R.id.textview_mainScrollItem_rainText2
            2 -> R.id.textview_mainScrollItem_rainText3
            3 -> R.id.textview_mainScrollItem_rainText4
            4 -> R.id.textview_mainScrollItem_rainText5
            5 -> R.id.textview_mainScrollItem_rainText6
            6 -> R.id.textview_mainScrollItem_rainText7
            7 -> R.id.textview_mainScrollItem_rainText8
            else -> throw IllegalArgumentException("Invalid index")
        }
    }

}
