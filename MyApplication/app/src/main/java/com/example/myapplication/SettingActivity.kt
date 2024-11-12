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

        // 지역 이름과 영문 이름을 매핑한 리스트
        val regionMap = mapOf(
            "서울" to "Seoul",
            "부산" to "Busan",
            "대구" to "Daegu",
            "인천" to "Incheon",
            "광주" to "Gwangju",
            "대전" to "Daejeon",
            "울산" to "Ulsan",
            "세종" to "Sejong",
            "경기도" to "Gyeonggi-do",
            "강원도" to "Gangwon-do",
            "충청북도" to "Chungcheongbuk-do",
            "충청남도" to "Chungcheongnam-do",
            "전라북도" to "Jeollabuk-do",
            "전라남도" to "Jeollanam-do",
            "경상북도" to "Gyeongsangbuk-do",
            "경상남도" to "Gyeongsangnam-do",
            "제주도" to "Jeju-do"
        )

        // Spinner에 사용할 지역 리스트 (한국어)
        val regionList = regionMap.keys.toList()

        // Spinner와 어댑터 설정
        val spinner: Spinner = findViewById(R.id.spinner_setting_selectPosition)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, regionList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // 영어로 저장된 값 읽어오기
        val savedRegionInEnglish = sharedPreferences.getString("selectedRegion", "Seoul") ?: "Seoul"

        // 영어 값을 한국어로 변환
        val savedRegion = regionMap.filter { it.value == savedRegionInEnglish }.keys.firstOrNull() ?: "서울"

        // 이전에 저장된 값이 있으면 그 값을 Spinner에 반영
        val position = regionList.indexOf(savedRegion)
        if (position >= 0) {
            spinner.setSelection(position)
        }

        // Spinner에서 선택된 값 SharedPreferences에 영어로 저장
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedRegion = regionList[position]
                val selectedRegionInEnglish = regionMap[selectedRegion] ?: "Seoul" // 영어로 저장
                sharedPreferences.edit().putString("selectedRegion", selectedRegionInEnglish).apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // 선택되지 않은 경우 처리 (필요시)
            }
        }

    }

    private fun setupButtonListeners() {
        buttonLeft1 = findViewById(R.id.button_all_cardview_left1)
        buttonLeft2 = findViewById(R.id.button_all_cardview_left2)
        buttonRight1 = findViewById(R.id.button_all_cardview_right1)
        buttonRight2 = findViewById(R.id.button_all_cardview_right2)

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
