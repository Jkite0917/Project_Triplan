package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.CalendarView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class MainActivity : AppCompatActivity() {
    private lateinit var database: LocalDatabase
    private lateinit var dateTextDao: DateTextDao

    private lateinit var buttonLeft1: ImageButton
    private lateinit var buttonLeft2: ImageButton
    private lateinit var buttonRight1: ImageButton
    private lateinit var buttonRight2: ImageButton
    private lateinit var buttonCenter: ImageButton

    private lateinit var calendarView: CalendarView
    private lateinit var selectedDate: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        setupButtonListeners()

        database = LocalDatabase.getDatabase(this) // DB 생성
        dateTextDao = database.getDateTextDao() // DAO 생성
        Log.d("DatabaseCheck", "Database instance created: $database") // DB 연동 확인 로그

        // 데이터베이스에 항목 추가
        val dateText = DateText("2024-11-04", "Sample memo")
        CoroutineScope(Dispatchers.IO).launch {
            dateTextDao.insertDateText(dateText)
            val allDateTexts = dateTextDao.getAllDateText()
            Log.d("DatabaseCheck", "All DateTexts: $allDateTexts") // 테스트용 값 입력
        }


        calendarView = findViewById(R.id.MainCalendarView)
        selectedDate = findViewById(R.id.WeatherTextView)

        // 현재 날짜 가져오기
        val calendar = Calendar.getInstance()
        calendarView.date = calendar.timeInMillis // 캘린더뷰를 현재 날짜로 설정
        updateSelectedDate(calendar)

        // 날짜 변경 리스너
        calendarView.setOnDateChangeListener { view, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(year, month, dayOfMonth)
            updateSelectedDate(selectedCalendar)
        }
    }

    private fun updateSelectedDate(calendar: Calendar) {
        val month = calendar.get(Calendar.MONTH) + 1 // 월은 0부터 시작하므로 +1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        selectedDate.text = "  보고 있는 날짜: $month/$day"
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

}
