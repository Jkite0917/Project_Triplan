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
    private lateinit var buttonLeft1: ImageButton
    private lateinit var buttonLeft2: ImageButton
    private lateinit var buttonRight1: ImageButton
    private lateinit var buttonRight2: ImageButton
    private lateinit var buttonCenter: ImageButton

    private lateinit var checklistRecyclerView: RecyclerView
    private lateinit var adapter: CheckListAdapter
    private val items = mutableListOf<ChecklistItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check) // 메인 레이아웃 설정
        setupButtonListeners()

        checklistRecyclerView = findViewById(R.id.checklist_recycler_view)
        items.add(ChecklistItem("예시 제목 1", "매주", false))
        items.add(ChecklistItem("예시 제목 2", "매월", true))
        items.add(ChecklistItem("예시 제목 2", "매월", true))
        items.add(ChecklistItem("예시 제목 2", "매월", true))
        items.add(ChecklistItem("예시 제목 2", "매월", true))
        items.add(ChecklistItem("예시 제목 2", "매월", true))
        items.add(ChecklistItem("예시 제목 2", "매월", true))
        items.add(ChecklistItem("예시 제목 2", "매월", true))
        items.add(ChecklistItem("예시 제목 2", "매월", true))


        adapter = CheckListAdapter(items) { position ->
            // 삭제 버튼 클릭 시 아이템 삭제
            items.removeAt(position)
            adapter.notifyItemRemoved(position)
        }

        checklistRecyclerView.adapter = adapter
        checklistRecyclerView.layoutManager = LinearLayoutManager(this)

    }

    private fun setupButtonListeners() {
        buttonLeft1 = findViewById<ImageButton>(R.id.button_left1)
        buttonLeft2 = findViewById<ImageButton>(R.id.button_left2)
        buttonRight1 = findViewById<ImageButton>(R.id.button_right1)
        buttonRight2 = findViewById<ImageButton>(R.id.button_right2)
        buttonCenter = findViewById(R.id.button_center)

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
