package com.example.myapplication

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.CalendarView
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var buttonLeft1: ImageButton
    private lateinit var buttonLeft2: ImageButton
    private lateinit var buttonRight1: ImageButton
    private lateinit var buttonRight2: ImageButton
    private lateinit var buttonCenter: ImageButton

    private lateinit var calendarView: CalendarView
    private lateinit var selectedDate: TextView

    // 다년 공휴일 리스트
    private val holidayMap = mapOf(
        2024 to listOf("2024-01-01", "2024-12-25"),
        2025 to listOf("2025-01-01", "2025-12-25"),
        // 필요한 연도를 추가
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        setupButtonListeners()

        calendarView = findViewById(R.id.MainCalendarView)
        selectedDate = findViewById(R.id.WeatherTextView)

        // 현재 날짜를 설정하고 표시
        setCurrentDate()

        // 날짜 선택 리스너 등록
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }
            updateSelectedDate(selectedCalendar)
            updateDateTextColor(selectedCalendar)
        }
    }

    private fun setupButtonListeners() {
        buttonLeft1 = findViewById<ImageButton>(R.id.button_left1)
        buttonLeft2 = findViewById<ImageButton>(R.id.button_left2)
        buttonRight1 = findViewById<ImageButton>(R.id.button_right1)
        buttonRight2 = findViewById<ImageButton>(R.id.button_right2)
        buttonCenter = findViewById(R.id.button_center)

        buttonLeft1.setOnClickListener {
            // 현재 Activity가 MainActivity인지 확인
            if (this is MainActivity) {
                return@setOnClickListener
            }
            startActivity(Intent(this, MainActivity::class.java))
        }

        buttonLeft2.setOnClickListener {
            startActivity(Intent(this, WeatherActivity::class.java))
        }

        buttonRight1.setOnClickListener {
            startActivity(Intent(this, CheckActivity::class.java))
        }

        buttonRight2.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }

        buttonCenter.setOnClickListener {
            val bottomSheet = MainAddActivity()
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        }
    }

    private fun setCurrentDate() {
        val calendar = Calendar.getInstance()
        calendarView.date = calendar.timeInMillis
        updateSelectedDate(calendar)
        updateDateTextColor(calendar)
    }

    private fun updateDateTextColor(calendar: Calendar) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formattedDate = dateFormat.format(calendar.time)
        val year = calendar.get(Calendar.YEAR)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // 기본 색상 설정
        var color = Color.BLACK

        // 요일별 색상 설정
        when (dayOfWeek) {
            Calendar.SUNDAY -> color = Color.RED
            Calendar.SATURDAY -> color = Color.BLUE
        }

        // 공휴일 색상 적용
        holidayMap[year]?.let { holidays ->
            if (holidays.contains(formattedDate)) {
                color = Color.RED
            }
        }
    }


    private fun updateSelectedDate(calendar: Calendar) {
        val month = calendar.get(Calendar.MONTH) + 1 // 월은 0부터 시작하므로 +1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        selectedDate.text = "  $month/$day  날씨"
    }
}






