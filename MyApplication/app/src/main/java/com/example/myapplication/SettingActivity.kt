package com.example.myapplication

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity

class SettingActivity : AppCompatActivity() {
    private lateinit var buttonLeft1: ImageButton
    private lateinit var buttonLeft2: ImageButton
    private lateinit var buttonRight1: ImageButton
    private lateinit var buttonRight2: ImageButton


    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        setupButtonListeners()

        sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)

        // Spinner에 사용할 지역 리스트
        val regionList = listOf(
            "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종",
            "경기도", "강원도", "충청북도", "충청남도", "전라북도", "전라남도", "경상북도", "경상남도",
            "제주도", "수도권", "호남", "영남", "강원"
        )
            // Spinner와 어댑터 설정
        val spinner: Spinner = findViewById(R.id.spinner_setting_selectPosition)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, regionList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

            // 이전에 저장된 값이 있으면 그 값을 Spinner에 반영
        val savedRegion = sharedPreferences.getString("selectedRegion", "서울")  // 기본값은 "서울"
        val position = regionList.indexOf(savedRegion)
        if (position >= 0) {
            spinner.setSelection(position)
        }

            // Spinner에서 선택된 값 SharedPreferences에 저장
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedRegion = regionList[position]
                sharedPreferences.edit().putString("selectedRegion", selectedRegion).apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            // 선택되지 않은 경우 처리 (필요시)
            }
        }
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