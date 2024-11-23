package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class CheckActivity : AppCompatActivity() {
    // 버튼 변수 초기화
    private lateinit var buttonLeft1: ImageButton
    private lateinit var buttonLeft2: ImageButton
    private lateinit var buttonRight1: ImageButton
    private lateinit var buttonRight2: ImageButton
    private lateinit var buttonCenter: ImageButton

    // 어댑터 및 체크리스트 아이템 리스트 초기화
    private lateinit var checklistAdapter: ChecklistAdapter
    private val checklistItems = mutableListOf<ChecklistItem>()
    private lateinit var checklistRecyclerView: RecyclerView
    private lateinit var database: LocalDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check)

        setupButtonListeners()

        // 데이터베이스 초기화
        database = LocalDatabase.getDatabase(this)

        // 주기적으로 체크리스트 항목 초기화
        resetChecklistItemsIfNeeded()

        // 어댑터 설정
        checklistRecyclerView = findViewById(R.id.recyclerview_checklist_list)
        checklistAdapter = ChecklistAdapter(checklistItems, onDeleteClick = { cNo ->
            deleteChecklistItem(cNo) // 항목 삭제 처리
        }, onCheckedChange = { updatedItem ->
            updateChecklistItem(updatedItem) // 체크박스 상태 변경 처리
        })

        // RecyclerView에 어댑터와 레이아웃 매니저 연결
        checklistRecyclerView.adapter = checklistAdapter
        checklistRecyclerView.layoutManager = LinearLayoutManager(this)

        loadChecklistItems() // 데이터베이스에서 체크리스트 항목 불러오기
    }

    // 데이터베이스에서 체크리스트 항목을 불러와 리스트에 추가
    @SuppressLint("NotifyDataSetChanged")
    private fun loadChecklistItems() {
        lifecycleScope.launch {
            val savedItems = withContext(Dispatchers.IO) {
                database.getChecklistDao().getAllChecklistItems()
            }
            checklistItems.clear()
            checklistItems.addAll(savedItems.map { checklist ->
                ChecklistItem(
                    cNo = checklist.cNo,
                    cTitle = checklist.cTitle,
                    isChecked = checklist.isChecked,
                    period = checklist.period,
                    weekDay = checklist.weekDay, // DB에서 불러온 weekDay 값
                    monthDay = checklist.monthDay
                )
            })
            checklistAdapter.notifyDataSetChanged()

            // weekDay 디버깅 로그 추가
            checklistItems.forEach {
                Log.d("DEBUG", "Loaded ChecklistItem - Title: ${it.cTitle}, WeekDay: ${it.weekDay}")
            }
        }
    }

    // 체크리스트 항목의 상태가 변경되었을 때 해당 항목만 업데이트
    private fun updateChecklistItem(item: ChecklistItem) {
        lifecycleScope.launch(Dispatchers.IO) {
            val checklist = Checklist(
                cNo = item.cNo,
                cTitle = item.cTitle,
                isChecked = item.isChecked,
                period = item.period,
                weekDay = item.weekDay,
                monthDay = item.monthDay
            )
            database.getChecklistDao().updateChecklistItem(checklist)
            withContext(Dispatchers.Main) {
                // 항목 위치를 찾아 해당 위치만 업데이트
                val position = checklistItems.indexOfFirst { it.cNo == item.cNo }
                if (position != -1) {
                    checklistItems[position] = item
                    checklistAdapter.notifyItemChanged(position) // 변경된 위치만 업데이트
                }
            }
        }
    }

    // 체크리스트 항목 삭제 처리
    private fun deleteChecklistItem(cNo: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            database.getChecklistDao().deleteChecklistItemByCNo(cNo)
            withContext(Dispatchers.Main) {
                val position = checklistItems.indexOfFirst { it.cNo == cNo }
                if (position != -1) {
                    checklistItems.removeAt(position)
                    checklistAdapter.notifyItemRemoved(position)
                    checklistAdapter.notifyItemRangeChanged(position, checklistItems.size)
                }
            }
        }
    }

    // 새로운 체크리스트 항목을 데이터베이스에 추가하고 RecyclerView 갱신
    private fun addItemToChecklist(newItem: ChecklistItem) {
        lifecycleScope.launch {
            val checklist = Checklist(
                cTitle = newItem.cTitle,
                isChecked = newItem.isChecked,
                period = newItem.period,
                weekDay = newItem.weekDay,
                monthDay = newItem.monthDay
            )
            val insertedId = withContext(Dispatchers.IO) {
                database.getChecklistDao().insertChecklistItem(checklist)
            }
            val updatedItem = newItem.copy(cNo = insertedId)
            withContext(Dispatchers.Main) {
                checklistItems.add(updatedItem)
                checklistAdapter.notifyItemInserted(checklistItems.size - 1)
            }
        }
    }

    // 필요 시 체크리스트 항목 초기화
    private fun resetChecklistItemsIfNeeded() {
        lifecycleScope.launch {
            val resetItems = withContext(Dispatchers.IO) {
                val currentItems = database.getChecklistDao().getAllChecklistItems()
                currentItems.filter { shouldResetItem(it) }.onEach { item ->
                    // 체크 상태를 해제하고, 마지막 체크 날짜를 현재로 업데이트
                    database.getChecklistDao().updateChecklistItemById(
                        item.cNo,
                        isChecked = false,
                        lastCheckedDate = System.currentTimeMillis()
                    )
                }
            }
            if (resetItems.isNotEmpty()) {
                loadChecklistItems()
            }
        }
    }

    // 요일 문자열을 숫자로 매핑하는 함수
    private fun mapWeekDayStringToNumber(weekDay: String): Int {
        return when (weekDay) {
            "일" -> 1
            "월" -> 2
            "화" -> 3
            "수" -> 4
            "목" -> 5
            "금" -> 6
            "토" -> 7
            else -> -1 // 잘못된 입력 처리
        }
    }

    // 특정 체크리스트 항목의 주기 확인 함수
    private fun shouldResetItem(item: Checklist): Boolean {
        val lastCheckedDate = Calendar.getInstance().apply {
            timeInMillis = item.lastCheckedDate
        }
        val currentDate = Calendar.getInstance()

        // Logcat에 디버깅 메시지 출력
        Log.d("DEBUG", "Last Checked Date: ${lastCheckedDate.time}")
        Log.d("DEBUG", "Current Date: ${currentDate.time}")
        Log.d("DEBUG", "Last Week of Year: ${lastCheckedDate.get(Calendar.WEEK_OF_YEAR)}")
        Log.d("DEBUG", "Current Week of Year: ${currentDate.get(Calendar.WEEK_OF_YEAR)}")
        Log.d("DEBUG", "Saved WeekDay: ${item.weekDay}")
        Log.d("DEBUG", "Current WeekDay: ${currentDate.get(Calendar.DAY_OF_WEEK)}")

        return when (item.period) {
            "매일" -> isDateDifferent(lastCheckedDate, currentDate, Calendar.DAY_OF_YEAR) // 하루 단위 초기화
            "매주" -> {
                val savedWeekDayNumber = item.weekDay?.let { mapWeekDayStringToNumber(it) } ?: -1
                val currentWeekDay = currentDate.get(Calendar.DAY_OF_WEEK)
                val isTargetWeekDay = savedWeekDayNumber == currentWeekDay // 요일 비교
                val isDifferentWeek = lastCheckedDate.get(Calendar.WEEK_OF_YEAR) != currentDate.get(Calendar.WEEK_OF_YEAR) ||
                        lastCheckedDate.get(Calendar.YEAR) != currentDate.get(Calendar.YEAR) // 차주 확인

                Log.d("DEBUG", "Saved WeekDay (Number): $savedWeekDayNumber")
                Log.d("DEBUG", "Is Target WeekDay: $isTargetWeekDay")
                Log.d("DEBUG", "Is Different Week: $isDifferentWeek")

                // 요일이 맞으면 초기화
                isTargetWeekDay && (isDifferentWeek || lastCheckedDate.get(Calendar.WEEK_OF_YEAR) == currentDate.get(Calendar.WEEK_OF_YEAR))
            }
            "매월" -> {
                val currentMonthDay = currentDate.get(Calendar.DAY_OF_MONTH).toString() // 현재 날짜
                currentMonthDay == item.monthDay // 설정된 날짜와 현재 날짜 비교
            }
            else -> false
        }
    }


    // 두 날짜가 특정 단위(일, 주, 월)에서 다른지 확인하는 함수
    @Suppress("SameParameterValue")
    private fun isDateDifferent(lastCheckedDate: Calendar, currentDate: Calendar, unit: Int): Boolean {
        return lastCheckedDate.get(unit) != currentDate.get(unit)
    }

    // 버튼 초기화 및 클릭 리스너 설정
    private fun setupButtonListeners() {
        buttonLeft1 = findViewById(R.id.button_all_cardview_left1)
        buttonLeft2 = findViewById(R.id.button_all_cardview_left2)
        buttonRight1 = findViewById(R.id.button_all_cardview_right1)
        buttonRight2 = findViewById(R.id.button_all_cardview_right2)
        buttonCenter = findViewById(R.id.button_all_cardview_center)

        // 각 버튼 클릭 시 해당 액티비티로 이동
        buttonLeft1.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        buttonLeft2.setOnClickListener {
            startActivity(Intent(this, WeatherActivity::class.java))
        }
        buttonRight1.setOnClickListener {
            // 현재 액티비티가 CheckActivity일 때 아무 동작도 하지 않음
        }
        buttonRight2.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }

        // 중앙 버튼 클릭 시 새로운 체크리스트 항목 추가
        buttonCenter.setOnClickListener {
            val bottomSheet = CheckAddActivity { newItem ->
                addItemToChecklist(newItem)
            }
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        }
    }
}
