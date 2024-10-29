package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class WeatherAddActivity(
) : BottomSheetDialogFragment() {

    private var selectedWeather: Int? = null
    private var selectedTime: String? = null
    private lateinit var etContent: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.weather_add, container, false)
    }

        // 날씨 선택 버튼들 참조
        /*
        val sunButton = view.findViewById<ImageButton>(R.id.weather_sun_icon)
        val cloudButton = view.findViewById<ImageButton>(R.id.weather_cloud_icon)
        val rainButton = view.findViewById<ImageButton>(R.id.weather_rain_icon)
        val thunderButton = view.findViewById<ImageButton>(R.id.weather_thunder_icon)
        val snowButton = view.findViewById<ImageButton>(R.id.weather_snow_icon)
        val sunCloudButton = view.findViewById<ImageButton>(R.id.weather_suncloud_icon)

        // 시간 선택 버튼들 참조
        val btnPreviousDay = view.findViewById<Button>(R.id.button_previous_day)
        val btnTodayMoment = view.findViewById<Button>(R.id.button_today_moment)
        val btnAllDay = view.findViewById<Button>(R.id.button_all_day)

        // 내용 입력 참조
        etContent = view.findViewById(R.id.etContent)

        // 날씨 선택 버튼 클릭 이벤트
        val weatherButtons = listOf(sunButton, cloudButton, rainButton, thunderButton, snowButton, sunCloudButton)
        val weatherIcons = listOf(
            R.drawable.weather_sun_icon, R.drawable.weather_cloud_icon,
            R.drawable.weather_rain_icon, R.drawable.weather_thunder_icon,
            R.drawable.weather_snow_icon, R.drawable.weather_suncloud_icon
        )

        weatherButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                selectedWeather = weatherIcons[index]
                weatherButtons.forEach { it.isSelected = false }
                button.isSelected = true
            }
        }

        // 시간 선택 버튼 클릭 이벤트
        btnPreviousDay.setOnClickListener {
            selectedTime = btnPreviousDay.text.toString()
            resetTimeSelection(btnPreviousDay, btnTodayMoment, btnAllDay)
        }
        btnTodayMoment.setOnClickListener {
            selectedTime = btnTodayMoment.text.toString()
            resetTimeSelection(btnTodayMoment, btnPreviousDay, btnAllDay)
        }
        btnAllDay.setOnClickListener {
            selectedTime = btnAllDay.text.toString()
            resetTimeSelection(btnAllDay, btnPreviousDay, btnTodayMoment)
        }

        // 저장 버튼 클릭 이벤트
        view.findViewById<Button>(R.id.btnSave).setOnClickListener {
            val contents = etContent.text.toString()
            if (selectedWeather != null && selectedTime != null && contents.isNotBlank()) {
                val newItem = WeatherListItem(
                    contents = contents,
                    weather = selectedWeather!!,
                    time = selectedTime!!
                )
                onSave(newItem)
                dismiss()
            }
        }

        return view
    }

    // 다른 시간 버튼들의 선택 해제
    private fun resetTimeSelection(selected: Button, vararg others: Button) {
        selected.isSelected = true
        others.forEach { it.isSelected = false }
         */
}
