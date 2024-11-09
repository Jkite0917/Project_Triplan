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

        checklistRecyclerView = findViewById(R.id.recyclerview_checklist_list)

        // 어댑터 설정
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
                    weekDay = checklist.weekDay,
                    monthDay = checklist.monthDay
                )
            })
            // 전체 리스트 초기화 시 notifyItemRangeInserted 사용
            checklistAdapter.notifyItemRangeInserted(0, checklistItems.size)
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
            // 데이터베이스에서 해당 항목 삭제
            database.getChecklistDao().deleteChecklistItemByCNo(cNo)
            withContext(Dispatchers.Main) {
                // 삭제된 항목의 위치를 찾아 리스트와 RecyclerView 갱신
                val position = checklistItems.indexOfFirst { it.cNo == cNo }
                if (position != -1) {
                    checklistItems.removeAt(position)
                    checklistAdapter.notifyItemRemoved(position) // 삭제된 위치만 업데이트
                }
            }
        }
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
            // 데이터베이스에 항목 추가 후 ID 반환
            val insertedId = withContext(Dispatchers.IO) {
                database.getChecklistDao().insertChecklistItem(checklist)
            }
            // 삽입된 ID를 기반으로 새로운 ChecklistItem 생성 및 추가
            val updatedItem = newItem.copy(cNo = insertedId)
            checklistItems.add(updatedItem)
            checklistAdapter.notifyItemInserted(checklistItems.size - 1) // 추가된 위치만 업데이트
        }
    }
}
