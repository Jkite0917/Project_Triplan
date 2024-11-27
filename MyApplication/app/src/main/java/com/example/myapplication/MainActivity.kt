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
import android.view.View
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

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
    private lateinit var weatherNotificationManager: WeatherNotificationManager // 알림 매니저 인스 턴스

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Android 13 이상 에서 알림 권한 요청
        requestNotificationPermissionIfNeeded()

        // 버튼 리스너 설정
        setupButtonListeners()

        gridCalendar = findViewById(R.id.gridLayout_calender_date)
        selectedDateTextView = findViewById(R.id.textview_main_dateWeather)
        tvCurrentMonth = findViewById(R.id.textview_calender_yearMonth)

        // SharedPreferences 초기화
        sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

        // 초기값 설정
        initializeDefaultSharedPreferences()

        // 캘린더 컨트롤 설정 및 업 데이트
        setupCalendarControls()
        updateCalendar()

        val today = Calendar.getInstance()
        updateSelectedDateText(today)

        // 알림 매니저 초기화
        weatherNotificationManager = WeatherNotificationManager(this, LocalDatabase.getDatabase(this))

        // 앱 첫 실행 시에만 날씨 알림 예약 설정
        if (!sharedPreferences.getBoolean("isNotificationScheduled", false)) {
            scheduleWeatherNotifications()
            sharedPreferences.edit().putBoolean("isNotificationScheduled", true).apply()
        }
    }

    // SharedPreferences 에 기본 지역 설정 저장
    private fun initializeDefaultSharedPreferences() {
        if (!sharedPreferences.contains("selectedRegion")) {
            with(sharedPreferences.edit()) {
                putString("selectedRegion", "Seoul")
                apply() // 비동기 적으로 저장
            }
        }
    }

    // 알림 권한을 요청 하는 함수 (Android 13 이상)
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
                Log.d("MainActivity", "알림 권한이 허용 ")
            } else {
                Log.d("MainActivity", "알림 권한이 거부 , 알림 기능이 제한 됩니다.")
            }
        }
    }

    // 매 정각에 알림을 예약 하는 함수
    private fun scheduleWeatherNotifications() {
        // 다음 1시간 정각 까지 남은 시간 계산
        val initialDelay = calculateInitialDelay()
        val notificationRequest = OneTimeWorkRequestBuilder<WeatherWorker>()
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS) // 정각에 맞춰 첫 알림 실행
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "HourlyWeatherNotification",
            ExistingWorkPolicy.REPLACE,
            notificationRequest
        )
    }

    // 다음 1시간 정각 까지 남은 초기 지연 시간 계산
    private fun calculateInitialDelay(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR_OF_DAY, 1)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar.timeInMillis - System.currentTimeMillis()
    }

    // 캘린더 컨트롤 설정
    private fun setupCalendarControls() {
        findViewById<ImageButton>(R.id.imageButton_calender_monthLeft).setOnClickListener { navigateMonth(-1) }
        findViewById<ImageButton>(R.id.imageButton_calender_monthRight).setOnClickListener { navigateMonth(1) }
    }

    // 달력 현재 날로 초기화 onResume, moveTOCurrentMonth
    override fun onResume() {
        super.onResume()
        moveToCurrentMonth()
    }

    private fun moveToCurrentMonth() {
        calendar.time = Calendar.getInstance().time
        updateCalendar()
    }

    // 달력 업 데이트 함수
    private fun updateCalendar() {
        updateMonthDisplay()
        displayDaysInGrid()
    }

    // 달력의 날짜를 화면에 표시 하는 함수
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
                dayTextView.setTextColor(ContextCompat.getColor(this, R.color.Today))  // 오늘 날짜 하이 라이트
            }

            // DAO 를 가져 오기
            val dailyScheduleDao = LocalDatabase.getDatabase(this@MainActivity).getDailyScheduleDao()
            // 날짜 포맷 설정 (yyyy-MM-dd)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            // 데이터 베이스 에서 해당 날짜에 일정이 있는지 확인
            if (dayTextView.text.isNotEmpty()) {
                val day = dayTextView.text.toString().toInt()
                val currentCalendar = Calendar.getInstance().apply {
                    set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), day)
                }
                val currentDate = dateFormat.format(currentCalendar.time) // yyyy-MM-dd 형식의 String 값 생성

                lifecycleScope.launch {
                    val dailySchedule = dailyScheduleDao.getDailyScheduleInfo(currentDate)
                    if (dailySchedule != null) {
                        dayTextView.setBackgroundResource(R.drawable.calender_underline)
                    }
                }
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
            setPadding(0, 8, 0, 8) // top 과 bottom 에 각각 20dp 간격 추가
        }
    }

    // 오늘 날짜 인지 확인 하는 함수
    private fun isToday(calendar: Calendar, today: Calendar, dayText: String): Boolean {
        return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                dayText == today.get(Calendar.DAY_OF_MONTH).toString()
    }

    // 선택한 날짜 표시 업 데이트 함수
    private fun updateSelectedDateText(selectedDate: Calendar) {
        // SharedPreferences 에서 저장된 영어 값을 한국어 로 변환
        val regionInKorean = getRegionInKorean(sharedPreferences)
        val regionText = "$regionInKorean 날씨 정보 입니다."
        val selectedDateFormat = SimpleDateFormat("  MM월 dd일 $regionText", Locale.getDefault())
        selectedDateTextView.text = selectedDateFormat.format(selectedDate.time)

        // 스크롤 뷰에도 업 데이트 반영
        updateWeatherScrollView(sharedPreferences, selectedDate)
    }

    // SharedPreferences 를 통해 저장된 지역 값을 한국어 로 변환 하는 함수
    private fun getRegionInKorean(sharedPreferences: SharedPreferences): String {
        // 영어와 한국어 를 매핑한 리스트
        val regionMap = mapOf(
            "서울" to "Seoul",
            "부산" to "Busan",
            "대구" to "Daegu",
            "인천" to "Incheon",
            "광주" to "Gwangju",
            "대전" to "Daejeon",
            "울산" to "Ulsan",
            "세종" to "Sejong",
            "경기도" to "Gyeonggi-do",
            "강원도" to "Gangwon-do",
            "충청북도" to "Chungcheongbuk-do",
            "충청남도" to "Chungcheongnam-do",
            "전라북도" to "Jeollabuk-do",
            "전라남도" to "Jeollanam-do",
            "경상북도" to "Gyeongsangbuk-do",
            "경상남도" to "Gyeongsangnam-do",
            "제주도" to "Jeju-do"
        )

        // 영어 값을 SharedPreferences 에서 가져옴
        val savedRegionInEnglish = sharedPreferences.getString("selectedRegion", "Seoul") ?: "Seoul"

        // 영어 값을 한국어 로 변환
        return regionMap.keys.find { regionMap[it] == savedRegionInEnglish } ?: "서울"
    }

    // 현재 달 표시 업 데이트 함수
    private fun updateMonthDisplay() {
        val dateFormat = SimpleDateFormat("yyyy 년 MM월", Locale.getDefault())
        tvCurrentMonth.text = dateFormat.format(calendar.time)
    }

    // 월 이동 함수
    private fun navigateMonth(offset: Int) {
        calendar.add(Calendar.MONTH, offset)
        updateCalendar()
    }

    // 날짜 선택 시 호출 되는 함수
    private fun onDaySelected(dayTextView: TextView) {
        val selectedDay = dayTextView.text.toString().toIntOrNull() ?: return
        val selectedDate = Calendar.getInstance().apply {
            set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), selectedDay)
        }

        // 같은 날짜를 두 번 클릭 했는지 확인
        if (lastSelectedDay == selectedDay) {
            // 같은 날짜를 두 번 클릭한 경우 로그 출력
            Log.d("Double", "($selectedDay)")

            // 바텀 시트를 여기 에서 보여 주기 위한 메소드 호출
            showBottomSheet(selectedDate)
        } else {
            // 선택한 날짜가 다른 경우, 선택한 날짜 업 데이트
            lastSelectedDay = selectedDay
            updateSelectedDateText(selectedDate) // 선택한 날짜 표시 업 데이트
        }
    }

    // 바텀 시트를 보여 주는 메소드
    private fun showBottomSheet(selectedDate: Calendar) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formattedDate: String = dateFormat.format(selectedDate.time)

        val bottomSheet = MainDateInfoActivity(formattedDate)
        bottomSheet.show(supportFragmentManager, "DateInfoBottomSheet")
    }

    // api 파일에 값 넣어서 실행 하도록 보내기
    private fun updateWeatherScrollView(sharedPreferences: SharedPreferences, selectedDate: Calendar) {
        // 레이 아웃 참조
        val weatherScrollLayout: LinearLayout = findViewById(R.id.linearLayout_main_in_scrollview)

        // "selectedRegion"이라는 키로 저장된 값을 가져 와서 변수에 할당
        val city = sharedPreferences.getString("selectedRegion", "defaultCity") ?: "defaultCity"
        Log.e("API_LOG_Checking_Region", "selected Region is : $city")

        // selectedDate 를 문자열 로 변환 (yyyy-MM-dd 형식)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formattedDate: String = dateFormat.format(selectedDate.time)

        // 현재 날짜와 5일 후 날짜 에서 시간 제거
        val currentDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val fiveDaysLater = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 5)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val parentScrollView: HorizontalScrollView = findViewById(R.id.scrollview_main_in_cardview)

        // selectedDate 가 currentDate 와 같거나 이후 이면서, fiveDaysLater 이전인 경우 에만 날씨 정보 요청
        if (selectedDate.before(currentDate) || selectedDate.after(fiveDaysLater)) {
            // 날짜가 유효 하지 않으면 레이 아웃 숨기기
            parentScrollView.visibility = View.GONE
        } else {
            // 유효한 날짜 라면 레이 아웃 보이기 및 날씨 정보 요청
            parentScrollView.visibility = View.VISIBLE
            val mainScrollView = MainScrollView(this, city, formattedDate, weatherScrollLayout)
            mainScrollView.getWeatherForecast()
        }
    }

    // 하단 메뉴바 화면 이동 기능
    private fun setupButtonListeners() {
        buttonLeft1 = findViewById(R.id.button_all_cardview_left1)
        buttonLeft2 = findViewById(R.id.button_all_cardview_left2)
        buttonRight1 = findViewById(R.id.button_all_cardview_right1)
        buttonRight2 = findViewById(R.id.button_all_cardview_right2)
        buttonCenter = findViewById(R.id.button_all_cardview_center)

        buttonLeft1.setOnClickListener {
            // 현재 액티 비티 가 MainActivity 일 때 아무 동작도 하지 않음
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
