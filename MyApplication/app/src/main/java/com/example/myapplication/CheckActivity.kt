package com.example.myapplication

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
    private lateinit var buttonLeft1: ImageButton
    private lateinit var buttonLeft2: ImageButton
    private lateinit var buttonRight1: ImageButton
    private lateinit var buttonRight2: ImageButton
    private lateinit var buttonCenter: ImageButton

    private lateinit var checklistAdapter: ChecklistAdapter
    private val checklistItems = mutableListOf<ChecklistItem>() // 체크리스트 데이터
    private lateinit var checklistRecyclerView: RecyclerView
    private lateinit var database: LocalDatabase // 로컬 데이터베이스

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check)

        setupButtonListeners() // 버튼 리스너 초기화

        // 데이터베이스 초기화
        database = LocalDatabase.getDatabase(this)

        // 주기에 따라 체크리스트 상태 초기화
        resetChecklistItemsIfNeeded()

        // RecyclerView 및 어댑터 초기화
        checklistRecyclerView = findViewById(R.id.recyclerview_checklist_list)
        checklistAdapter = ChecklistAdapter(checklistItems, onDeleteClick = { cNo ->
            deleteChecklistItem(cNo) // 항목 삭제 처리
        }, onCheckedChange = { updatedItem ->
            updateChecklistItem(updatedItem) // 체크박스 상태 변경 처리
        })

        checklistRecyclerView.adapter = checklistAdapter // 어댑터 설정
        checklistRecyclerView.layoutManager = LinearLayoutManager(this) // 레이아웃 설정

        // 데이터베이스에서 체크리스트 불러오기
        loadChecklistItems()
    }

    private fun loadChecklistItems() {
        lifecycleScope.launch {
            val savedItems = withContext(Dispatchers.IO) {
                database.getChecklistDao().getAllChecklistItems() // DB에서 항목 조회
            }

            // 기존 데이터와 새 데이터 비교 및 업데이트
            val newChecklistItems = savedItems.map { checklist ->
                ChecklistItem(
                    cNo = checklist.cNo,
                    cTitle = checklist.cTitle,
                    isChecked = checklist.isChecked,
                    period = checklist.period,
                    weekDay = checklist.weekDay,
                    monthDay = checklist.monthDay
                )
            }
            updateChecklistItems(newChecklistItems) // RecyclerView 업데이트
        }
    }

    private fun updateChecklistItems(newItems: List<ChecklistItem>) {
        val oldItems = checklistItems.toList() // 기존 리스트 복사
        checklistItems.clear()
        checklistItems.addAll(newItems) // 새로운 리스트로 업데이트

        // DiffUtil을 사용해 효율적으로 변경 반영
        val diffResult = calculateDiff(oldItems, checklistItems)
        diffResult.dispatchUpdatesTo(checklistAdapter)
    }

    private fun calculateDiff(
        oldItems: List<ChecklistItem>,
        newItems: List<ChecklistItem>
    ): androidx.recyclerview.widget.DiffUtil.DiffResult {
        return androidx.recyclerview.widget.DiffUtil.calculateDiff(object : androidx.recyclerview.widget.DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldItems.size
            override fun getNewListSize(): Int = newItems.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldItems[oldItemPosition].cNo == newItems[newItemPosition].cNo
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldItems[oldItemPosition] == newItems[newItemPosition]
            }
        })
    }

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

    private fun resetChecklistItemsIfNeeded() {
        lifecycleScope.launch {
            val resetItems = withContext(Dispatchers.IO) {
                val currentItems = database.getChecklistDao().getAllChecklistItems()
                currentItems.filter { item ->
                    Log.d("CheckActivity", "Item ID: ${item.cNo}, Period: ${item.period}")
                    val result = shouldResetItem(item)
                    Log.d("CheckActivity", "Item: ${item.cNo}, shouldReset: $result")
                    result
                }.onEach { item ->
                    database.getChecklistDao().updateChecklistItemById(
                        item.cNo,
                        isChecked = false,
                        lastCheckedDate = System.currentTimeMillis()
                    )
                    Log.d("CheckActivity", "Reset item: ${item.cNo}, newDate: ${System.currentTimeMillis()}")
                }
            }
            withContext(Dispatchers.Main) {
                resetItems.forEach { resetItem ->
                    val index = checklistItems.indexOfFirst { it.cNo == resetItem.cNo }
                    if (index != -1) {
                        checklistItems[index] = checklistItems[index].copy(isChecked = false)
                        checklistAdapter.notifyItemChanged(index)
                    }
                }
            }
        }
    }

    private fun shouldResetItem(item: Checklist): Boolean {
        val lastCheckedDate = Calendar.getInstance().apply {
            timeInMillis = item.lastCheckedDate
        }
        val currentDate = Calendar.getInstance()

        Log.d("CheckActivity", "shouldResetItem called for item ID: ${item.cNo}, Period: ${item.period}")

        return when (item.period) {
            "매일" -> {
                val result = isDateDifferent(lastCheckedDate, currentDate, Calendar.DAY_OF_YEAR)
                Log.d("CheckActivity", "매일 reset check result for Item ID: ${item.cNo}: $result")
                result
            }
            "매주" -> {
                val result = isDateDifferent(lastCheckedDate, currentDate, Calendar.WEEK_OF_YEAR)
                Log.d("CheckActivity", "매주 reset check result for Item ID: ${item.cNo}: $result")
                result
            }
            "매월" -> {
                val result = isDateDifferent(lastCheckedDate, currentDate, Calendar.MONTH)
                Log.d("CheckActivity", "매월 reset check result for Item ID: ${item.cNo}: $result")
                result
            }
            else -> {
                Log.d("CheckActivity", "Unknown period for item ID: ${item.cNo}")
                false
            }
        }
    }

    private fun isDateDifferent(lastCheckedDate: Calendar, currentDate: Calendar, unit: Int): Boolean {
        // 주기별로 비교
        when (unit) {
            Calendar.WEEK_OF_YEAR -> {
                // 같은 주에 속하는지 체크
                val lastWeekStart = getStartOfWeek(lastCheckedDate)
                val currentWeekStart = getStartOfWeek(currentDate)
                return lastWeekStart != currentWeekStart
            }
            Calendar.MONTH -> {
                // 같은 달에 속하는지 체크
                val lastMonthStart = getStartOfMonth(lastCheckedDate)
                val currentMonthStart = getStartOfMonth(currentDate)
                return lastMonthStart != currentMonthStart
            }
            else -> {
                // 기본적으로 일 단위 비교
                lastCheckedDate.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                currentDate.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                return lastCheckedDate.get(unit) != currentDate.get(unit)
            }
        }
    }

    // 주의 시작일을 가져오는 함수
    private fun getStartOfWeek(calendar: Calendar): Calendar {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek) // 주의 첫 날(일요일)로 설정
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    // 월의 시작일을 가져오는 함수
    private fun getStartOfMonth(calendar: Calendar): Calendar {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1) // 월의 첫 날로 설정
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    private fun setupButtonListeners() {
        buttonLeft1 = findViewById(R.id.button_all_cardview_left1)
        buttonLeft2 = findViewById(R.id.button_all_cardview_left2)
        buttonRight1 = findViewById(R.id.button_all_cardview_right1)
        buttonRight2 = findViewById(R.id.button_all_cardview_right2)
        buttonCenter = findViewById(R.id.button_all_cardview_center)

        buttonLeft1.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        buttonLeft2.setOnClickListener {
            startActivity(Intent(this, WeatherActivity::class.java))
        }
        buttonRight1.setOnClickListener {}
        buttonRight2.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }

        buttonCenter.setOnClickListener {
            val bottomSheet = CheckAddActivity { newItem ->
                addItemToChecklist(newItem)
            }
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        }
    }
}
