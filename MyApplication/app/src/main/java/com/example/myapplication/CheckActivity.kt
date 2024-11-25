package com.example.myapplication

import android.content.Intent
import android.os.Bundle
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

    // 데이터베이스에서 체크리스트 항목을 불러와 RecyclerView 업데이트
    private fun loadChecklistItems() {
        lifecycleScope.launch {
            val savedItems = withContext(Dispatchers.IO) {
                database.getChecklistDao().getAllChecklistItems()
            }

            // Checklist -> ChecklistItem으로 변환
            val checklistItemList = savedItems.map { checklist ->
                ChecklistItem(
                    cNo = checklist.cNo,
                    cTitle = checklist.cTitle,
                    isChecked = checklist.isChecked,
                    period = checklist.period,
                    weekDay = checklist.weekDay, // 이미 문자열로 저장됨
                    monthDay = checklist.monthDay
                )
            }

            // 기존 항목과 새로 불러온 항목 비교
            val diffCallback = ChecklistDiffCallback(checklistItems, checklistItemList)
            val diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(diffCallback)

            checklistItems.clear()
            checklistItems.addAll(checklistItemList)

            // RecyclerView 업데이트 (변경된 부분만 적용)
            diffResult.dispatchUpdatesTo(checklistAdapter)
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
                val position = checklistItems.indexOfFirst { it.cNo == item.cNo }
                if (position != -1) {
                    checklistItems[position] = item
                    checklistAdapter.notifyItemChanged(position)
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

    // 문자열로 저장된 요일을 숫자로 변환하는 함수
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

    // 특정 체크리스트 항목의 주기 확인 함수
    private fun shouldResetItem(item: Checklist): Boolean {
        val lastCheckedDate = Calendar.getInstance().apply {
            timeInMillis = item.lastCheckedDate
        }
        val currentDate = Calendar.getInstance()

        return when (item.period) {
            "매일" -> isDateDifferent(lastCheckedDate, currentDate)
            "매주" -> {
                // 문자열로 저장된 요일 데이터를 숫자로 변환하여 비교
                val savedWeekDayNumber = mapWeekDayStringToNumber(item.weekDay)
                val currentWeekDay = currentDate.get(Calendar.DAY_OF_WEEK)

                // 조건: 현재 요일이 저장된 요일과 같으면 체크 해제
                val isTargetWeekDay = savedWeekDayNumber == currentWeekDay

                // 조건: 주가 다르면 체크 해제
                val isDifferentWeek = lastCheckedDate.get(Calendar.WEEK_OF_YEAR) != currentDate.get(Calendar.WEEK_OF_YEAR)

                // 주가 다르거나, 같은 주더라도 해당 요일에 도달하면 체크 해제
                isTargetWeekDay || isDifferentWeek
            }
            "매월" -> {
                val currentMonthDay = currentDate.get(Calendar.DAY_OF_MONTH).toString()
                currentMonthDay == item.monthDay
            }
            else -> false
        }
    }

    // 두 날짜가 특정 단위(일, 주, 월)에서 다른지 확인하는 함수
    private fun isDateDifferent(lastCheckedDate: Calendar, currentDate: Calendar): Boolean {
        return lastCheckedDate.get(Calendar.DAY_OF_YEAR) != currentDate.get(Calendar.DAY_OF_YEAR)
    }

    // 버튼 초기화 및 클릭 리스너 설정
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

// DiffUtil Callback 클래스
class ChecklistDiffCallback(
    private val oldList: List<ChecklistItem>,
    private val newList: List<ChecklistItem>
) : androidx.recyclerview.widget.DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // 각 항목의 고유 ID를 비교하여 동일한 항목인지 확인
        return oldList[oldItemPosition].cNo == newList[newItemPosition].cNo
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // 항목의 내용이 동일한지 비교
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
