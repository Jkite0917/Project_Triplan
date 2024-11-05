package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity() {
    private lateinit var buttonLeft1: ImageButton
    private lateinit var buttonLeft2: ImageButton
    private lateinit var buttonRight1: ImageButton
    private lateinit var buttonRight2: ImageButton
    private lateinit var buttonCenter: ImageButton

    private lateinit var tvCurrentMonth: TextView
    private lateinit var selectedDateTextView: TextView
    private lateinit var gridCalendar: GridLayout
    private val calendar = Calendar.getInstance()

    // 선택한 날짜를 저장할 변수
    private var lastSelectedDay: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        setupButtonListeners()

        gridCalendar = findViewById(R.id.gridCalendar)
        selectedDateTextView = findViewById(R.id.DateWeatherTextView)
        tvCurrentMonth = findViewById(R.id.CalenderYearMonth)

        setupCalendarControls()
        updateCalendar()

        val today = Calendar.getInstance()
        updateSelectedDateText(today)
    }

    // 버튼과 제스처 설정 통합 함수
    private fun setupCalendarControls() {
        findViewById<ImageButton>(R.id.btnPrevMonthLeft).setOnClickListener { navigateMonth(-1) }
        findViewById<ImageButton>(R.id.btnPrevMonthRight).setOnClickListener { navigateMonth(1) }
    }

    // 달력 업데이트 함수
    private fun updateCalendar() {
        updateMonthDisplay()
        displayDaysInGrid()
    }

    // 달력의 날짜를 화면에 표시하는 함수
    private fun displayDaysInGrid() {
        gridCalendar.removeAllViews()
        val firstDayOfMonth = calendar.apply { set(Calendar.DAY_OF_MONTH, 1) }.get(Calendar.DAY_OF_WEEK)
        val maxDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val prevMonthDays = firstDayOfMonth - 1
        val today = Calendar.getInstance()

        for (i in 1..prevMonthDays + maxDaysInMonth) {
            val dayTextView = createDayTextView(i, prevMonthDays)
            // 텍스트 색상 설정
            if (dayTextView.text.isNotEmpty()) {
                val dayOfWeek = (prevMonthDays + (dayTextView.text.toString().toInt())) % 7 // 0:일요일, 1:월요일, ..., 6:토요일
                val correctedDayOfWeek = if (dayOfWeek == 0) 7 else dayOfWeek // 0일 경우 7로 수정
                dayTextView.setTextColor(
                    when (correctedDayOfWeek) {
                        1 -> ContextCompat.getColor(this, R.color.Sunday) // 일요일
                        7 -> ContextCompat.getColor(this, R.color.teal_700) // 토요일
                        else -> ContextCompat.getColor(this, R.color.black) // 평일
                    }
                )
            }
            if (isToday(calendar, today, dayTextView.text.toString())) {
                dayTextView.setTextColor(ContextCompat.getColor(this, R.color.Today))  // 오늘 날짜 하이라이트
            }


            // 날짜 클릭 시 처리
            dayTextView.setOnClickListener { onDaySelected(dayTextView) }
            gridCalendar.addView(dayTextView)
        }
    }

    // TextView 생성 함수 (날짜 셀)
    private fun createDayTextView(dayIndex: Int, prevMonthDays: Int): TextView {
        return TextView(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            gravity = Gravity.CENTER
            textSize = 23f
            text = if (dayIndex > prevMonthDays) (dayIndex - prevMonthDays).toString() else ""
        }
    }

    // 오늘 날짜인지 확인하는 함수
    private fun isToday(calendar: Calendar, today: Calendar, dayText: String): Boolean {
        return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                dayText == today.get(Calendar.DAY_OF_MONTH).toString()
    }

    // 선택한 날짜 표시 업데이트 함수
    private fun updateSelectedDateText(selectedDate: Calendar) {
        val selectedDateFormat = SimpleDateFormat("  MM월 dd일 날씨 정보입니다", Locale.getDefault())
        selectedDateTextView.text = selectedDateFormat.format(selectedDate.time)
    }

    // 현재 달 표시 업데이트 함수
    private fun updateMonthDisplay() {
        val dateFormat = SimpleDateFormat("yyyy년 MM월", Locale.getDefault())
        tvCurrentMonth.text = dateFormat.format(calendar.time)
    }

    // 월 이동 함수
    private fun navigateMonth(offset: Int) {
        calendar.add(Calendar.MONTH, offset)
        updateCalendar()
    }

    // 날짜 선택 시 호출되는 함수
    private fun onDaySelected(dayTextView: TextView) {
        val selectedDay = dayTextView.text.toString().toIntOrNull() ?: return
        val selectedDate = Calendar.getInstance().apply {
            set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), selectedDay)
        }

        // 같은 날짜를 두 번 클릭했는지 확인
        if (lastSelectedDay == selectedDay) {
            // 같은 날짜를 두 번 클릭한 경우 로그 출력
            Log.d("Double", "($selectedDay)")

            // 바텀 시트를 여기에서 보여주기 위한 메소드 호출
            showBottomSheet(selectedDate)
        } else {
            // 선택한 날짜가 다른 경우, 선택한 날짜 업데이트
            lastSelectedDay = selectedDay
            updateSelectedDateText(selectedDate) // 선택한 날짜 표시 업데이트
        }
    }

    // 바텀 시트를 보여주는 메소드 (로그 대신 실제 바텀 시트를 구현할 수 있음)
    private fun showBottomSheet(selectedDate: Calendar) {
        // 여기에 바텀 시트를 보여주는 코드를 작성
        Log.d("showBottomSheetLog", "111111")
    }

    // 하단 메뉴바 화면 이동 기능
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






