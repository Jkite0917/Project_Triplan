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

class WeatherAddActivity : BottomSheetDialogFragment() {

    private var selectedWeatherIcon: Int = 0 // 선택한 날씨 아이콘의 ID 저장
    private lateinit var selectedTimeButton: Button // 선택된 시간 버튼 참조
    private lateinit var editTExt: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 레이아웃을 인플레이트
        val view = inflater.inflate(R.layout.weather_add, container, false)

        val gridWeather: GridLayout = view.findViewById(R.id.WeatherAddGridLayout)
        selectedTimeButton = view.findViewById(R.id.WeatherAddTimeNowButton) // 기본 시간 버튼
        editTExt = view.findViewById(R.id.WeatherAddEditText)

        // 날씨 아이콘 버튼 선택 처리
        gridWeather.children.forEach { child ->
            if (child is ImageButton) {
                child.setOnClickListener {
                    // 선택된 아이콘의 drawable ID 저장
                    selectedWeatherIcon = child.tag?.toString()?.toIntOrNull() ?: 0

                    // 선택된 날씨 아이콘 버튼 강조 표시
                    highlightSelectedWeatherButton(child, gridWeather)
                }
            }
        }

        // 시간 선택 버튼들
        val timeButtons = listOf(
            view.findViewById<Button>(R.id.WeatherAddBeforeDayButton),
            view.findViewById<Button>(R.id.WeatherAddTimeNowButton),
            view.findViewById<Button>(R.id.WeatherAddAllDayButton)
        )

        timeButtons.forEach { button ->
            button.setOnClickListener { selectTimeButton(button, timeButtons) }
        }

        // 저장 버튼 클릭 시 입력 내용 저장
        view.findViewById<Button>(R.id.WeatherAddSaveButton).setOnClickListener {
            val contents = editTExt.text.toString()
            val selectedTime = selectedTimeButton.text.toString()

            if (contents.isNotEmpty() && selectedTime != "선택된 날짜 없음") {
                // Room DB에 데이터 저장
                // WeatherListItem을 생성하고 필요한 데이터를 전달
                val weatherItem = WeatherListItem(contents, selectedWeatherIcon, selectedTime)
                // 데이터를 어댑터나 데이터베이스로 전달하는 로직 추가 가능
                dismiss() // 다이얼로그 닫기
            } else {
                Toast.makeText(requireContext(), "선택과 내용을 전부 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        return view // 인플레이트한 뷰 반환
    }

    // 시간 버튼을 업데이트하는 함수
    private fun selectTimeButton(button: Button, timeButtons: List<Button>) {
        // 모든 버튼 스타일 초기화
        timeButtons.forEach { btn ->
            btn.background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_button)
            btn.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent)) // 기본 투명색으로 초기화
            btn.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        }

        // 선택된 버튼 강조 표시
        button.background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_button)
        button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.buttonC)) // 버튼 강조 색상 적용
        button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        selectedTimeButton = button // 선택된 버튼을 저장
    }

    // 날씨 버튼을 강조 표시하는 함수
    private fun highlightSelectedWeatherButton(selectedButton: ImageButton, gridWeather: GridLayout) {
        // 모든 날씨 버튼 스타일 초기화
        gridWeather.children.forEach { child ->
            if (child is ImageButton) {
                child.background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_button)
                child.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent)) // 기본 투명색으로 초기화
            }
        }

        // 선택된 날씨 버튼 강조 표시
        selectedButton.background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_button)
        selectedButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.buttonC)) // 버튼 강조 색상 적용
    }
}