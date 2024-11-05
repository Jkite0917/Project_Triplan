package com.example.myapplication

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.CalendarView
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var buttonLeft1: ImageButton
    private lateinit var buttonLeft2: ImageButton
    private lateinit var buttonRight1: ImageButton
    private lateinit var buttonRight2: ImageButton
    private lateinit var buttonCenter: ImageButton

    private lateinit var tvCurrentMonth: TextView
    private lateinit var gridCalendar: GridLayout
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        setupButtonListeners()

        tvCurrentMonth = findViewById(R.id.CalenderYearMonth)
        gridCalendar = findViewById(R.id.gridCalendar)

        findViewById<ImageButton>(R.id.btnPrevMonthLeft).setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        findViewById<ImageButton>(R.id.btnPrevMonthRight).setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }

        updateCalendar()
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

    private fun updateCalendar() {
        // 월/연도 업데이트
        val dateFormat = SimpleDateFormat("yyyy년 MM월", Locale.getDefault())
        tvCurrentMonth.text = dateFormat.format(calendar.time)

        // 달력 초기화
        gridCalendar.removeAllViews()

        // 첫 날 설정
        val firstDayOfMonth = calendar.apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }.get(Calendar.DAY_OF_WEEK)

        val maxDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // 이전 달 마지막 일수 계산
        val prevMonthDays = firstDayOfMonth - 1

        // 현재 날짜
        val today = Calendar.getInstance()

        // 날짜 채우기
        for (i in 1..prevMonthDays + maxDaysInMonth) {
            val dayTextView = TextView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
                gravity = Gravity.CENTER
                textSize = 23f
                text = if (i > prevMonthDays) (i - prevMonthDays).toString() else ""

                // 현재 날짜 하이라이트를 원형 배경으로 설정
                if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    text == today.get(Calendar.DAY_OF_MONTH).toString()) {
                    setBackgroundResource(R.drawable.rounded_button)
                }

                // 요일 색상 설정
                when ((i - 1) % 7) {
                    0 -> setTextColor(Color.parseColor("#BE2E22")) // 일요일
                    6 -> setTextColor(Color.parseColor("#009688")) // 토요일
                    else -> setTextColor(Color.BLACK)
                }

                // 13일에 밑줄 추가
                if (text == "13") {
                    setCompoundDrawablesWithIntrinsicBounds(
                        null, // 왼쪽
                        null, // 위쪽
                        null, // 오른쪽
                        ContextCompat.getDrawable(context, R.drawable.underline) // 아래쪽
                    )
                    compoundDrawablePadding = 1 // 텍스트와 밑줄 사이 간격
                }
            }
            gridCalendar.addView(dayTextView)
        }
    }
}






