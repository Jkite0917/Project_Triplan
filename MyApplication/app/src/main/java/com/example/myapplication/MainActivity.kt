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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class MainActivity : AppCompatActivity() {
    private lateinit var txtCurrentMonth: TextView
    private lateinit var layoutDates: LinearLayout
    private var currentCalendar: Calendar = Calendar.getInstance()

    private var selectedButton: ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)


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

        updateCalendarView()

        // 이전 달 버튼 클릭 이벤트
        findViewById<View>(R.id.btnPrevMonthLeft).setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            updateCalendarView()
        }

        // 다음 달 버튼 클릭 이벤트
        findViewById<View>(R.id.btnPrevMonthRight).setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            updateCalendarView()
        }
    }


    private fun switchButton(button: ImageButton) {
        // 이전 선택된 버튼의 선택 해제
        selectedButton?.isSelected = false
        // 현재 버튼을 선택 상태로 변경
        selectedButton = button
        selectedButton?.isSelected = true
    }

    private fun updateCalendarView() {
        // 현재 달과 연도 가져오기
        val monthFormat = SimpleDateFormat("yyyy년 MM월", Locale.getDefault())
        txtCurrentMonth.text = monthFormat.format(currentCalendar.time)

        // 날짜 그리드 초기화
        layoutDates.removeAllViews()

        // 해당 월의 첫 날과 마지막 날 가져오기
        val firstDayOfMonth = currentCalendar.clone() as Calendar
        firstDayOfMonth.set(Calendar.DAY_OF_MONTH, 1)

        val lastDayOfMonth = currentCalendar.clone() as Calendar
        lastDayOfMonth.set(Calendar.DAY_OF_MONTH, lastDayOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH))

        // 요일에 맞춰 시작하는 날 계산
        val startDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK)
        val daysInMonth = lastDayOfMonth.get(Calendar.DAY_OF_MONTH)

        // 빈 칸 추가 (1일부터 시작하는 주의 요일까지)
        for (i in 1 until startDayOfWeek) {
            val emptyView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = ""
            }
            layoutDates.addView(emptyView)
        }

        // 날짜 추가
        for (day in 1..daysInMonth) {
            val dateView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = day.toString()
                gravity = Gravity.CENTER
                setPadding(4, 4, 4, 4)
                setTextColor(
                    if (day == Calendar.getInstance().get(Calendar.DAY_OF_MONTH) &&
                        currentCalendar.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH)) {
                        android.graphics.Color.RED // 수정된 부분
                    } else {
                        android.graphics.Color.BLACK // 수정된 부분
                    }
                )
            }
            layoutDates.addView(dateView) // 날짜 뷰를 layoutDates에 추가
        }

        if (startDayOfWeek == 7) {
            layoutDates.addView(LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.VERTICAL
            })
        }
    }
}
