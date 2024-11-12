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
    private lateinit var editTExt: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 레이아웃을 인플레이트
        val view = inflater.inflate(R.layout.bottomsheet_weather_add, container, false)

        val gridWeather: GridLayout = view.findViewById(R.id.gridLayout_weatherAdd_selectWeather)
        editTExt = view.findViewById(R.id.edittext_weatherAdd_inputText)

        // 날씨 아이콘 버튼 선택 처리
        gridWeather.children.forEach { child ->
            if (child is ImageButton) {
                child.setOnClickListener {
                    // 선택된 날씨 설명 문자열 저장
                    selectedWeatherDescription = when (child.id) {
                        R.id.imageButton_weatherAdd_Icon_Sun -> "clear sky"
                        R.id.imageButton_weatherAdd_Icon_Cloud -> "few clouds"
                        R.id.imageButton_weatherAdd_Icon_Rain -> "light rain"
                        R.id.imageButton_weatherAdd_Icon_Thunder -> "thunderstorm"
                        R.id.imageButton_weatherAdd_Icon_Show -> "snow"
                        R.id.imageButton_weatherAdd_Icon_SunCloud -> "partly cloudy"
                        else -> ""
                    }

                    // 날씨 아이콘 선택됨을 표시
                    isWeatherIconSelected = true

                    // 선택된 날씨 아이콘 버튼 강조 표시
                    highlightSelectedWeatherButton(child, gridWeather)
                }
            }
        }

        // 시간 선택 버튼들
        val timeButtons = listOf(
            view.findViewById<Button>(R.id.button_weatherAdd_dayBefore),
            view.findViewById<Button>(R.id.button_weatherAdd_timeNow),
            view.findViewById<Button>(R.id.button_weatherAdd_allDay)
        )

        timeButtons.forEach { button ->
            button.setOnClickListener { selectTimeButton(button, timeButtons) }
        }

        // 저장 버튼 클릭 시 입력 내용 저장
        view.findViewById<Button>(R.id.button_weatherAdd_saveData).setOnClickListener {
            val contents = editTExt.text.toString()
            val selectedTime = selectedTimeButton?.text?.toString() ?: ""

            when {
                !isWeatherIconSelected -> {
                    Toast.makeText(requireContext(), "날씨 버튼을 선택해주세요.", Toast.LENGTH_SHORT).show()
                }
                selectedTimeButton == null -> {
                    Toast.makeText(requireContext(), "시간 버튼을 선택해주세요.", Toast.LENGTH_SHORT).show()
                }
                contents.isEmpty() -> {
                    Toast.makeText(requireContext(), "내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // 새로운 아이템 생성 및 저장 콜백 호출
                    val newItem = WeatherListItem(
                        wNo = 0L,                        // 처음에는 기본값으로 설정
                        contents = contents,             // 입력한 내용
                        weather = selectedWeatherDescription,  // 선택된 날씨 설명 문자열
                        time = selectedTime              // 선택된 시간
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
        // 모든 버튼 스타일 초기화
        timeButtons.forEach { btn ->
            btn.background = ContextCompat.getDrawable(requireContext(), R.drawable.bottomsheet_button_background)
            btn.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent)) // 기본 투명색으로 초기화
            btn.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        }

        // 선택된 버튼 강조 표시
        button.background = ContextCompat.getDrawable(requireContext(), R.drawable.bottomsheet_button_background)
        button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.buttonC)) // 버튼 강조 색상 적용
        button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        selectedTimeButton = button // 선택된 버튼을 저장
    }

    // 날씨 버튼을 강조 표시하는 함수
    private fun highlightSelectedWeatherButton(selectedButton: ImageButton, gridWeather: GridLayout) {
        // 모든 날씨 버튼 스타일 초기화
        gridWeather.children.forEach { child ->
            if (child is ImageButton) {
                child.background = ContextCompat.getDrawable(requireContext(), R.drawable.bottomsheet_button_background)
                child.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent)) // 기본 투명색으로 초기화
            }
        }

        // 선택된 날씨 버튼 강조 표시
        selectedButton.background = ContextCompat.getDrawable(requireContext(), R.drawable.bottomsheet_button_background)
        selectedButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.buttonC)) // 버튼 강조 색상 적용
    }
}
