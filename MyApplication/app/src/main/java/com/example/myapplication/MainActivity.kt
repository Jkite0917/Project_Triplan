package com.example.myapplication

import android.content.Context
import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private lateinit var selectedDateTextView: TextView
    private lateinit var gridCalendar: GridLayout
    private val calendar = Calendar.getInstance()

    // 선택한 날짜를 저장할 변수
    private var lastSelectedDay: Int? = null

    // 지역 정보 저장 공유 변수
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var weatherNotificationManager: WeatherNotificationManager // 알림 매니저 인스턴스

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 알림 권한 요청
        requestNotificationPermissionIfNeeded()

        setupButtonListeners()

        gridCalendar = findViewById(R.id.gridLayout_calender_date)
        selectedDateTextView = findViewById(R.id.textview_main_dateWeather)
        tvCurrentMonth = findViewById(R.id.textview_calender_yearMonth)

        // sharedPreferences 초기화
        sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

        // 초기값 설정
        initializeDefaultSharedPreferences()

        setupCalendarControls()
        updateCalendar()

        val today = Calendar.getInstance()
        updateSelectedDateText(today)

        // 알림 매니저 초기화
        weatherNotificationManager = WeatherNotificationManager(this, LocalDatabase.getDatabase(this))

        // 앱 실행 시 날씨 조건 확인 및 알림 설정
        setupWeatherNotifications()
    }

    // 초기값 설정 함수 정의
    private fun initializeDefaultSharedPreferences() {
        if (!sharedPreferences.contains("selectedRegion")) {
            with(sharedPreferences.edit()) {
                putString("selectedRegion", "defaultCity")
                apply() // 비동기적으로 저장
            }
        }
    }

    // 날씨 알림 설정
    private fun setupWeatherNotifications() {
        lifecycleScope.launch {
            val selectedRegion = sharedPreferences.getString("selectedRegion", "Seoul") ?: "Seoul"
            val apiService = ApiClient.weatherApiService
            val apiKey = "74c26aef7529a784cee3247a261edd92" // 실제 OpenWeather API 키로 변경 필요

            // 날씨 조건 확인 및 알림 설정
            withContext(Dispatchers.IO) {
                weatherNotificationManager.checkWeatherConditions(apiService, apiKey, selectedRegion)
            }
        }
    }

    // 알림 권한을 요청하는 함수 (Android 13 이상)
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
    }

    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.d("MainActivity", "알림 권한이 허용되었습니다.")
            } else {
                Log.d("MainActivity", "알림 권한이 거부되었습니다. 알림 기능이 제한됩니다.")
            }
        }
    }

    // 버튼 액션 통합 함수
    private fun setupCalendarControls() {
        findViewById<ImageButton>(R.id.imageButton_calender_monthLeft).setOnClickListener { navigateMonth(-1) }
        findViewById<ImageButton>(R.id.imageButton_calender_monthRight).setOnClickListener { navigateMonth(1) }
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

            // 위아래 간격 조정
            setPadding(0, 8, 0, 8) // top과 bottom에 각각 20dp 간격 추가
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

        // 스크롤뷰에도 업데이트 반영
        updateWeatherScrollView(sharedPreferences, selectedDate)
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

    // 바텀 시트를 보여주는 메소드
    private fun showBottomSheet(selectedDate: Calendar) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formattedDate: String = dateFormat.format(selectedDate.time)

        val bottomSheet = MainDateInfoActivity(formattedDate)
        bottomSheet.show(supportFragmentManager, "DateInfoBottomSheet")
    }

    // api 파일에 값 넣어서 실행하도록 보내기
    private fun updateWeatherScrollView(sharedPreferences: SharedPreferences, selectedDate: Calendar) {
        // 레이아웃 참조
        val weatherScrollLayout: LinearLayout = findViewById(R.id.linearLayout_main_in_scrollview)

        // 예를 들어, "city"라는 키로 저장된 값을 가져와서 변수에 할당
        val city = sharedPreferences.getString("selectedRegion", "defaultCity") ?: "defaultCity"
        Log.e("API_LOG_Checking_Region", "selected Region is : ${city}")

        // selectedDate가 Calender 타입이라 String으로
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formattedDate: String = dateFormat.format(selectedDate.time)

        // WeatherHelper 인스턴스 생성 및 날씨 정보 요청
        val weatherHelper = WeatherHelper(this, city, formattedDate, weatherScrollLayout)
        weatherHelper.getWeatherForecast()
    }

    // 하단 메뉴바 화면 이동 기능
    private fun setupButtonListeners() {
        buttonLeft1 = findViewById<ImageButton>(R.id.button_all_cardview_left1)
        buttonLeft2 = findViewById<ImageButton>(R.id.button_all_cardview_left2)
        buttonRight1 = findViewById<ImageButton>(R.id.button_all_cardview_right1)
        buttonRight2 = findViewById<ImageButton>(R.id.button_all_cardview_right2)
        buttonCenter = findViewById(R.id.button_all_cardview_center)

        buttonLeft1.setOnClickListener {
            // 현재 액티비티가 MainActivity일 때 아무 동작도 하지 않음
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
