package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.AdapterView
import android.widget.ImageButton

class CheckActivity : AppCompatActivity() {
    private var selectedButton: ImageButton? = null

    private lateinit var checklistRecyclerView: RecyclerView
    private lateinit var adapter: CheckListAdapter
    private val items = mutableListOf<ChecklistItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check) // 메인 레이아웃 설정

        val buttonLeft1 = findViewById<ImageButton>(R.id.button_left1)
        val buttonLeft2 = findViewById<ImageButton>(R.id.button_left2)
        val buttonRight1 = findViewById<ImageButton>(R.id.button_right1)
        val buttonRight2 = findViewById<ImageButton>(R.id.button_right2)

        checklistRecyclerView = findViewById(R.id.checklist_recycler_view)
        items.add(ChecklistItem("예시 제목 1", "매주", false))
        items.add(ChecklistItem("예시 제목 2", "매월", true))

        adapter = CheckListAdapter(items) { position ->
            // 삭제 버튼 클릭 시 아이템 삭제
            items.removeAt(position)
            adapter.notifyItemRemoved(position)
        }

        checklistRecyclerView.adapter = adapter
        checklistRecyclerView.layoutManager = LinearLayoutManager(this)

        buttonLeft1.setOnClickListener {
            switchButton(buttonLeft1)

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

            // 현재 Activity가 MainActivity인지 확인
            if (this is CheckActivity) {
                // 현재 Activity가 MainActivity이면 아무것도 하지 않음
                return@setOnClickListener
            }

            val intent = Intent(this, CheckActivity::class.java)
            startActivity(intent)
        }

        // 두 번째 오른쪽 버튼 클릭 시 설정 화면으로 전환
        buttonRight2.setOnClickListener {
            switchButton(buttonRight2)
            Log.d("Button Click", "Right 2 button clicked - SettingActivity intent")
            // val intent = Intent(this, SettingActivity::class.java)
            // startActivity(intent)
        }


        val buttonCenter = findViewById<ImageButton>(R.id.button_center)
        buttonCenter.setOnClickListener {

            val bottomSheet = CheckAddActivity()
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        }
    }

    private fun switchButton(button: ImageButton) {
        // 이전 선택된 버튼의 선택 해제
        selectedButton?.isSelected = false
        // 현재 버튼을 선택 상태로 변경
        selectedButton = button
        selectedButton?.isSelected = true
    }
}
