package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity

class SettingActivity : AppCompatActivity() {
    private lateinit var buttonLeft1: ImageButton
    private lateinit var buttonLeft2: ImageButton
    private lateinit var buttonRight1: ImageButton
    private lateinit var buttonRight2: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        setupButtonListeners()

        // Spinner에 사용할 지역 리스트
        val regionList = listOf("서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종")

        // Spinner와 어댑터 설정
        val spinner: Spinner = findViewById(R.id.spinner_setting_selectPosition)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, regionList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

    }

    private fun setupButtonListeners() {
        buttonLeft1 = findViewById<ImageButton>(R.id.button_all_cardview_left1)
        buttonLeft2 = findViewById<ImageButton>(R.id.button_all_cardview_left2)
        buttonRight1 = findViewById<ImageButton>(R.id.button_all_cardview_right1)
        buttonRight2 = findViewById<ImageButton>(R.id.button_all_cardview_right2)

        buttonLeft1.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        buttonLeft2.setOnClickListener {
            startActivity(Intent(this, WeatherActivity::class.java))
        }

        buttonRight1.setOnClickListener {
            startActivity(Intent(this, CheckActivity::class.java))
        }

        buttonRight2.setOnClickListener {
            // 현재 액티비티가 SettingActivity일 때 아무 동작도 하지 않음
        }

    }

}