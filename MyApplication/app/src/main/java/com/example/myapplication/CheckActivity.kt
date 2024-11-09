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
    private lateinit var buttonLeft1: ImageButton
    private lateinit var buttonLeft2: ImageButton
    private lateinit var buttonRight1: ImageButton
    private lateinit var buttonRight2: ImageButton
    private lateinit var buttonCenter: ImageButton

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
            deleteChecklistItem(cNo)
        }, onCheckedChange = { updatedItem ->
            updateChecklistItem(updatedItem)
        })

        checklistRecyclerView.adapter = checklistAdapter
        checklistRecyclerView.layoutManager = LinearLayoutManager(this)

        loadChecklistItems()
    }

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

            checklistAdapter.notifyItemRangeInserted(0, checklistItems.size)
        }
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

        buttonCenter.setOnClickListener {
            val bottomSheet = CheckAddActivity { newItem ->
                addItemToChecklist(newItem)
            }
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
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
            checklistItems.add(updatedItem)
            checklistAdapter.notifyItemInserted(checklistItems.size - 1)
        }
    }
}
