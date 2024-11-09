package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CheckActivity : AppCompatActivity() {
    private lateinit var buttonLeft1: ImageButton
    private lateinit var buttonLeft2: ImageButton
    private lateinit var buttonRight1: ImageButton
    private lateinit var buttonRight2: ImageButton
    private lateinit var buttonCenter: ImageButton

    private lateinit var recyclerView: RecyclerView
    private lateinit var checklistAdapter: ChecklistAdapter
    private lateinit var checklistItems: MutableList<Checklist>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check) // 메인 레이아웃 설정
        setupButtonListeners()
        loadChecklistItems()
    }

    private fun loadChecklistItems() {
        recyclerView = findViewById(R.id.checklist_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 모든 체크리스트 아이템 가져오기
                checklistItems = LocalDatabase.getDatabase(
                    this@CheckActivity).getChecklistDao().getAllChecklistItems().toMutableList()
                withContext(Dispatchers.Main) {
                    checklistAdapter = ChecklistAdapter(checklistItems) { item ->
                        // 삭제 버튼 클릭 시 실행되는 로직
                        deleteChecklistItem(item)  // deleteChecklistItem 함수 호출
                    }
                    recyclerView.adapter = checklistAdapter // 어댑터 설정
                }
            } catch (e: Exception) {
                // 예외 처리: 사용자에게 오류 메시지를 표시하거나 로그를 남기는 등의 작업
                Log.e("CheckActivity", "Failed to load checklist items: ${e.message}")
            }
        }
    }

    // 삭제 아이템 처리 함수
    private fun deleteChecklistItem(item: Checklist) {
        CoroutineScope(Dispatchers.IO).launch {
            // 데이터베이스에서 삭제
            LocalDatabase.getDatabase(this@CheckActivity).getChecklistDao().deleteChecklistItemByCNo(item.cNo)
            checklistItems.remove(item) // 리스트에서 제거
            withContext(Dispatchers.Main) {
                checklistAdapter.notifyDataSetChanged() // RecyclerView 업데이트
            }
        }
    }

    private fun setupButtonListeners() {
        buttonLeft1 = findViewById<ImageButton>(R.id.button_all_cardview_left1)
        buttonLeft2 = findViewById<ImageButton>(R.id.button_all_cardview_left2)
        buttonRight1 = findViewById<ImageButton>(R.id.button_all_cardview_right1)
        buttonRight2 = findViewById<ImageButton>(R.id.button_all_cardview_right2)
        buttonCenter = findViewById(R.id.button_all_cardview_center)

        buttonLeft1.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        buttonLeft2.setOnClickListener {
            startActivity(Intent(this, WeatherActivity::class.java))
        }

        buttonRight1.setOnClickListener {
            // 현재 Activity가 MainActivity인지 확인
            if (this is CheckActivity) {
                // 현재 Activity가 MainActivity이면 아무것도 하지 않음
                return@setOnClickListener
            }

            val intent = Intent(this, CheckActivity::class.java)
            startActivity(intent)
        }

        buttonRight2.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }

        buttonCenter.setOnClickListener {
            val bottomSheet = CheckAddActivity()
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        }
    }
}
