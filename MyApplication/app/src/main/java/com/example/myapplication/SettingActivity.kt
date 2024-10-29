package com.example.myapplication

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity

class SettingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        // Spinner에 사용할 지역 리스트
        val regionList = listOf("서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종")

        // Spinner와 어댑터 설정
        val spinner: Spinner = findViewById(R.id.PositionSpinner)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, regionList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

    }
}