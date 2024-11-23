package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class WeatherAddActivity(private val onSave: (WeatherListItem) -> Unit) : BottomSheetDialogFragment() {

    private lateinit var selectedWeatherDescription: String // 선택한 날씨 설명 저장
    private var isWeatherIconSelected: Boolean = false // 날씨 버튼 선택 여부 저장
    private var selectedTimeButton: Button? = null // 선택된 시간 버튼 참조
    private lateinit var editText: EditText

    // 날씨 버튼 ID와 설명 매핑
    private val weatherIconMap = mapOf(
        R.id.imageButton_weatherAdd_Icon_Sun to "clear sky",
        R.id.imageButton_weatherAdd_Icon_Cloud to "clouds",
        R.id.imageButton_weatherAdd_Icon_Rain to "rain",
        R.id.imageButton_weatherAdd_Icon_Thunder to "thunderstorm",
        R.id.imageButton_weatherAdd_Icon_Show to "snow",
        R.id.imageButton_weatherAdd_Icon_SunCloud to "partly cloudy"
    )

    // 에러 메시지 매핑
    private val errorMessageMap = mapOf(
        "weather" to "날씨 버튼을 선택해주세요.",
        "time" to "시간 버튼을 선택해주세요.",
        "content" to "내용을 입력해주세요."
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 레이아웃을 인플레이트
        val view = inflater.inflate(R.layout.bottomsheet_weather_add, container, false)

        val gridWeather: GridLayout = view.findViewById(R.id.gridLayout_weatherAdd_selectWeather)
        editText = view.findViewById(R.id.edittext_weatherAdd_inputText)

        // 날씨 아이콘 버튼 선택 처리
        gridWeather.children.forEach { child ->
            if (child is ImageButton) {
                child.setOnClickListener {
                    selectedWeatherDescription = weatherIconMap[child.id] ?: ""
                    isWeatherIconSelected = true // 날씨 버튼 선택됨 표시
                    highlightSelectedWeatherButton(child, gridWeather) // 버튼 강조
                }
            }
        }

        // 시간 선택 버튼들
        val timeButtons = listOf(
            view.findViewById<Button>(R.id.button_weatherAdd_dayBefore),
            view.findViewById(R.id.button_weatherAdd_timeNow),
            view.findViewById(R.id.button_weatherAdd_allDay)
        )

        timeButtons.forEach { button ->
            button.setOnClickListener { selectTimeButton(button, timeButtons) }
        }

        // 저장 버튼 클릭 시 입력 내용 저장
        view.findViewById<Button>(R.id.button_weatherAdd_saveData).setOnClickListener {
            val contents = editText.text.toString()
            val selectedTime = selectedTimeButton?.text?.toString() ?: ""

            when {
                !isWeatherIconSelected -> {
                    Toast.makeText(requireContext(), errorMessageMap["weather"], Toast.LENGTH_SHORT).show()
                }
                selectedTimeButton == null -> {
                    Toast.makeText(requireContext(), errorMessageMap["time"], Toast.LENGTH_SHORT).show()
                }
                contents.isEmpty() -> {
                    Toast.makeText(requireContext(), errorMessageMap["content"], Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // 새로운 아이템 생성 및 저장 콜백 호출
                    val newItem = WeatherListItem(
                        wNo = 0L,                        // 기본값
                        contents = contents,             // 입력한 내용
                        weather = selectedWeatherDescription,  // 선택된 날씨 설명
                        time = selectedTime,             // 선택된 시간
                        isNotified = false               // 알림 여부 초기값
                    )
                    onSave(newItem)
                    dismiss() // 다이얼로그 닫기
                }
            }
        }

        return view // 인플레이트한 뷰 반환
    }

    // 시간 버튼을 업데이트하는 함수
    private fun selectTimeButton(button: Button, timeButtons: List<Button>) {
        timeButtons.forEach { resetButtonStyle(it) } // 모든 버튼 초기화
        applyHighlightStyle(button) // 선택된 버튼 강조
        selectedTimeButton = button // 선택된 버튼 저장
    }

    // 날씨 버튼 강조 표시 함수
    private fun highlightSelectedWeatherButton(selectedButton: ImageButton, gridWeather: GridLayout) {
        gridWeather.children.forEach { child ->
            if (child is ImageButton) resetButtonStyle(child) // 모든 버튼 초기화
        }
        applyHighlightStyle(selectedButton) // 선택된 버튼 강조
    }

    // 버튼 스타일 초기화 함수
    private fun resetButtonStyle(button: View) {
        button.background = ContextCompat.getDrawable(requireContext(), R.drawable.bottomsheet_button_background)
        button.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
    }

    // 버튼 강조 스타일 함수
    private fun applyHighlightStyle(button: View) {
        button.background = ContextCompat.getDrawable(requireContext(), R.drawable.bottomsheet_button_background)
        button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.buttonC))
    }
}
