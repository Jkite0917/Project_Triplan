package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageButton
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
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

    // 어댑터 및 체크 리스트 아이템 리스트 초기화
    private lateinit var checklistAdapter: ChecklistAdapter
    private val checklistItems = mutableListOf<ChecklistItem>()
    private lateinit var checklistRecyclerView: RecyclerView
    private lateinit var database: LocalDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check)

        setupBottomNavigation()

        // 데이터 베이스 초기화
        database = LocalDatabase.getDatabase(this)

        // 주기 적으로 체크 리스트 항목 초기화
        resetChecklistItemsIfNeeded()

        // 어댑터 설정
        checklistRecyclerView = findViewById(R.id.recyclerview_checklist_list)
        checklistAdapter = ChecklistAdapter(checklistItems, onDeleteClick = { cNo ->
            deleteChecklistItem(cNo) // 항목 삭제 처리
        }, onCheckedChange = { updatedItem ->
            updateChecklistItem(updatedItem) // 체크 박스 상태 변경 처리
        })

        // RecyclerView 에 어댑터 와 레이 아웃 매니저 연결
        checklistRecyclerView.adapter = checklistAdapter
        checklistRecyclerView.layoutManager = LinearLayoutManager(this)

        loadChecklistItems() // 데이터 베이스 에서 체크 리스트 항목 불러 오기
    }

    // 데이터 베이스 에서 체크 리스트 항목을 불러와 RecyclerView 업데 이트
    private fun loadChecklistItems() {
        lifecycleScope.launch {
            val savedItems = withContext(Dispatchers.IO) {
                database.getChecklistDao().getAllChecklistItems()
            }

            // Checklist -> ChecklistItem 으로 변환
            val checklistItemList = savedItems.map { checklist ->
                ChecklistItem(
                    cNo = checklist.cNo,
                    cTitle = checklist.cTitle,
                    isChecked = checklist.isChecked,
                    period = checklist.period,
                    weekDay = checklist.weekDay, // 이미 문자열 로 저장됨
                    monthDay = checklist.monthDay
                )
            }

            // 기존 항목과 새로 불러온 항목 비교
            val diffCallback = ChecklistDiffCallback(checklistItems, checklistItemList)
            val diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(diffCallback)

            checklistItems.clear()
            checklistItems.addAll(checklistItemList)

            // RecyclerView 업데 이트 (변경된 부분만 적용)
            diffResult.dispatchUpdatesTo(checklistAdapter)
        }
    }

    // 체크 리스트 항목의 상태가 변경 되었을 때 해당 항목만 업 데이트
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
                val position = checklistItems.indexOfFirst { it.cNo == item.cNo }
                if (position != -1) {
                    checklistItems[position] = item
                    checklistAdapter.notifyItemChanged(position)
                }
            }
        }
    }

    // 체크 리스트 항목 삭제 처리
    private fun deleteChecklistItem(cNo: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            database.getChecklistDao().deleteChecklistItemByCNo(cNo)
            withContext(Dispatchers.Main) {
                val position = checklistItems.indexOfFirst { it.cNo == cNo }
                if (position != -1) {
                    checklistItems.removeAt(position)
                    checklistAdapter.notifyItemRemoved(position)
                }
            }
        }
    }

    // 새로운 체크 리스트 항목을 데이터 베이스 에 추가 하고 RecyclerView 갱신
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

    // 필요 시 체크 리스트 항목 초기화
    private fun resetChecklistItemsIfNeeded() {
        lifecycleScope.launch {
            val resetItems = withContext(Dispatchers.IO) {
                val currentItems = database.getChecklistDao().getAllChecklistItems()
                val currentDateMillis = System.currentTimeMillis()

                currentItems.filter { shouldResetItem(it) }.onEach { item ->
                    database.getChecklistDao().updateChecklistItemById(
                        item.cNo,
                        isChecked = false,
                        lastCheckedDate = System.currentTimeMillis(),
                        lastResetDate = currentDateMillis // 초기화 날짜 업 데이트
                    )
                }
            }

            if (resetItems.isNotEmpty()) {
                loadChecklistItems()
            }
        }
    }

    // 문자 열로 저장된 요일을 숫자로 변환 하는 함수
    private fun mapWeekDayStringToNumber(weekDay: String?): Int {
        return when (weekDay) {
            "일" -> 1
            "월" -> 2
            "화" -> 3
            "수" -> 4
            "목" -> 5
            "금" -> 6
            "토" -> 7
            else -> -1
        }
    }

    // 특정 체크 리스트 항목의 주기 확인 함수
    private fun shouldResetItem(item: Checklist): Boolean {
        val lastCheckedDate = Calendar.getInstance().apply { timeInMillis = item.lastCheckedDate }
        val lastResetDate = Calendar.getInstance().apply { timeInMillis = item.lastResetDate }
        val currentDate = Calendar.getInstance()

        // 날짜가 다른 경우 에만 초기화
        val isDifferentDate = lastResetDate.get(Calendar.DAY_OF_YEAR) != currentDate.get(Calendar.DAY_OF_YEAR) ||
                lastResetDate.get(Calendar.YEAR) != currentDate.get(Calendar.YEAR)

        return when (item.period) {
            "매일" -> isDifferentDate && isDateDifferent(lastCheckedDate, currentDate)
            "매주" -> {
                val savedWeekDayNumber = mapWeekDayStringToNumber(item.weekDay)
                val currentWeekDay = currentDate.get(Calendar.DAY_OF_WEEK)
                val isTargetWeekDay = savedWeekDayNumber == currentWeekDay
                val isDifferentWeek = lastCheckedDate.get(Calendar.WEEK_OF_YEAR) != currentDate.get(Calendar.WEEK_OF_YEAR)
                isDifferentDate && (isTargetWeekDay || isDifferentWeek)
            }
            "매월" -> {
                val currentMonthDay = currentDate.get(Calendar.DAY_OF_MONTH).toString()
                isDifferentDate && currentMonthDay == item.monthDay
            }
            else -> false
        }
    }

    // 두 날짜가 특정 단위(일, 주, 월)에서 다른지 확인 하는 함수
    private fun isDateDifferent(lastCheckedDate: Calendar, currentDate: Calendar): Boolean {
        return lastCheckedDate.get(Calendar.DAY_OF_YEAR) != currentDate.get(Calendar.DAY_OF_YEAR)
    }

    // 하단 메뉴바 화면 이동 기능
    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_main -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_weather -> {
                    startActivity(Intent(this, WeatherActivity::class.java))
                    true
                }
                R.id.nav_plus -> {
                    val bottomSheet = CheckAddActivity { newItem ->
                        addItemToChecklist(newItem)
                    }
                    bottomSheet.show(supportFragmentManager, bottomSheet.tag)
                    true
                }
                R.id.nav_check -> {
                    //startActivity(Intent(this, CheckActivity::class.java))
                    true
                }
                R.id.nav_setting -> {
                    startActivity(Intent(this, SettingActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}

// DiffUtil Callback 클래스
class ChecklistDiffCallback(
    private val oldList: List<ChecklistItem>,
    private val newList: List<ChecklistItem>
) : androidx.recyclerview.widget.DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // 각 항목의 고유 ID를 비교 하여 동일한 항목 인지 확인
        return oldList[oldItemPosition].cNo == newList[newItemPosition].cNo
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // 항목의 내용이 동일 한지 비교
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
