package com.example.myapplication

import android.content.Context
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
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
                        val firstForecast = forecastList.first() // 첫 번째 날씨 정보 사용
                        val time = firstForecast.dt_txt
                        val temperature = firstForecast.main.temp
                        val precipitationChance = firstForecast.pop ?: 0.0 // 강수 확률

                        // UI 업데이트
                        val timeTextView: TextView = linearLayoutMain.findViewById(R.id.textview_mainScrollItem_time1)
                        val temperatureTextView: TextView = linearLayoutMain.findViewById(R.id.textview_mainScrollItem_temperature1)
                        val rainTextView: TextView = linearLayoutMain.findViewById(R.id.textview_mainScrollItem_rainText1)

                        // 시간 형식 변경
                        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(time)
                        val formattedTime = dateFormat.format(date)

                        timeTextView.text = formattedTime // 시간 설정
                        temperatureTextView.text = "${"%.1f".format(temperature)}°C" // 온도 설정
                        rainTextView.text = "${"%.0f".format(precipitationChance * 100)}%" // 강수 확률 설정
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
}
