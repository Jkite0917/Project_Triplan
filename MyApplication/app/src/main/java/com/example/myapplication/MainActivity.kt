package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.CalendarView
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import java.util.Locale


class MainActivity : AppCompatActivity() {
    private lateinit var calendarView: CalendarView
    private var selectedButton: ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        // CalendarView 초기화
        calendarView = findViewById<CalendarView>(R.id.MainCalendarView)
        setCalendarToToday()

        val buttonLeft1 = findViewById<ImageButton>(R.id.button_left1)
        val buttonLeft2 = findViewById<ImageButton>(R.id.button_left2)
        val buttonRight1 = findViewById<ImageButton>(R.id.button_right1)
        val buttonRight2 = findViewById<ImageButton>(R.id.button_right2)

        buttonLeft1.setOnClickListener {
            switchButton(buttonLeft1)

            // 현재 Activity가 MainActivity인지 확인
            if (this is MainActivity) {
                // 현재 Activity가 MainActivity이면 아무것도 하지 않음
                return@setOnClickListener
            }

            // 현재 Activity가 MainActivity가 아니면 새 Intent로 시작
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }


// 두 번째 버튼 클릭 시 날씨 화면으로 전환
        buttonLeft2.setOnClickListener {
            switchButton(buttonLeft2)

            val intent = Intent(this, WeatherActivity::class.java)
            startActivity(intent)
        }

// 첫 번째 오른쪽 버튼 클릭 시 체크리스트 화면으로 전환
        buttonRight1.setOnClickListener {
            switchButton(buttonRight1)

            val intent = Intent(this, CheckActivity::class.java)
            startActivity(intent)
        }

// 두 번째 오른쪽 버튼 클릭 시 설정 화면으로 전환
        buttonRight2.setOnClickListener {
            switchButton(buttonRight2)

            val intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
        }


        val buttonCenter = findViewById<ImageButton>(R.id.button_center)
        buttonCenter.setOnClickListener {
            // BottomSheetDialogFragment 표시
            Log.d("Button Click", "center add button clicked - SettingActivity intent")
            //val bottomSheet = MainAddPage()
            //bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        }


    }

    private fun setCalendarToToday() {
        // 오늘 날짜 가져오기
        val calendar = Calendar.getInstance(Locale.KOREA)

        // CalendarView에 오늘 날짜 설정
        calendarView.date = calendar.timeInMillis
    }

    private fun switchButton(button: ImageButton) {
        // 이전 선택된 버튼의 선택 해제
        selectedButton?.isSelected = false
        // 현재 버튼을 선택 상태로 변경
        selectedButton = button
        selectedButton?.isSelected = true
    }
}
