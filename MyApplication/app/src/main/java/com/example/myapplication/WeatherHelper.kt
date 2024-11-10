package com.example.myapplication

import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class WeatherHelper(
    private val timeTextViews: Array<TextView>,        // 8개의 시간 TextView 배열
    private val temperatureTextViews: Array<TextView>,  // 8개의 기온 TextView 배열
    private val rainInfoLayouts: Array<LinearLayout>,   // 8개의 LinearLayout 배열
    private val rainTextViews: Array<TextView>,         // 8개의 강수 확률 TextView 배열
    private val apiKey: String
) {

    // 날씨 정보를 가져오는 메소드
    fun getWeatherForecast(city: String) {
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

                        // 3시간 간격으로 8개의 데이터만 가져오기
                        for (i in 0 until 8) {
                            if (i < forecastList.size) {
                                val forecast = forecastList[i]
                                val time = forecast.dt_txt
                                val temperature = forecast.main.temp
                                val precipitationChance = forecast.pop ?: 0.0 // 강수 확률

                                // 시간 포맷을 HH:mm 형식으로 변경
                                val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(time)
                                val formattedTime = dateFormat.format(date)

                                // UI 업데이트
                                timeTextViews[i].text = formattedTime // 시간 표시
                                temperatureTextViews[i].text = "${"%.1f".format(temperature)}°C" // 기온 표시
                                rainTextViews[i].text = "${"%.0f".format(precipitationChance * 100)}%" // 강수 확률 표시

                                // 강수 확률에 따른 레이아웃 표시
                                if (precipitationChance > 0) {
                                    rainInfoLayouts[i].visibility = LinearLayout.VISIBLE
                                } else {
                                    rainInfoLayouts[i].visibility = LinearLayout.GONE
                                }
                            }
                        }
                    }
                } else {
                    Log.e("API_ERROR", "Error code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<WeatherForecastResponse>, t: Throwable) {
                Log.e("API_FAILURE", "Error: ${t.message}")
            }
        })
    }
}

/*
package com.example.calendartest

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var timeTextView: TextView
    private lateinit var temperatureTextView: TextView
    private lateinit var rainInfoLayout: LinearLayout
    private lateinit var rainTextView: TextView
    private val apiKey = "74c26aef7529a784cee3247a261edd92"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        timeTextView = findViewById(R.id.textview_mainScrollItem_time)
        temperatureTextView = findViewById(R.id.textview_mainScrollItem_temperature)
        rainInfoLayout = findViewById(R.id.linearLayout_mainScrollItem_rainInfoLine)
        rainTextView = findViewById(R.id.textview_mainScrollItem_rainText)

        getWeatherForecast("Kuala Lumpur") // Fetch weather for Kuala Lumpur
    }

    private fun getWeatherForecast(city: String) {
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
                        val firstForecast = forecastList.first() // Use the first forecast entry for now
                        val time = firstForecast.dt_txt
                        val temperature = firstForecast.main.temp
                        val precipitationChance = firstForecast.pop ?: 0.0 // Precipitation chance

                        // Format the time to show only hour and minute
                        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(time)
                        val formattedTime = dateFormat.format(date)

                        // Set data to the UI components
                        timeTextView.text = formattedTime // Set time (formatted to "HH:mm")
                        temperatureTextView.text = "${"%.1f".format(temperature)}°C" // Set temperature
                        rainTextView.text = "${"%.0f".format(precipitationChance * 100)}%" // Set rain probability

                        // Handle rain info visibility
                        if (precipitationChance > 0) {
                            rainInfoLayout.visibility = LinearLayout.VISIBLE
                        } else {
                            rainInfoLayout.visibility = LinearLayout.GONE
                        }
                    }
                } else {
                    Log.e("API_ERROR", "Error code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<WeatherForecastResponse>, t: Throwable) {
                Log.e("API_FAILURE", "Error: ${t.message}")
            }
        })
    }
}

 */