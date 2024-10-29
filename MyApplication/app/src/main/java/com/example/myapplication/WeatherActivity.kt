package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager

class WeatherActivity : AppCompatActivity() {
    private var selectedButton: ImageButton? = null
    private lateinit var adapter: WeatherListAdapter
    private val items = mutableListOf<WeatherListItem>()

    private lateinit var weatherlistRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather) // 메인 레이아웃 설정

        val buttonLeft1 = findViewById<ImageButton>(R.id.button_left1)
        val buttonLeft2 = findViewById<ImageButton>(R.id.button_left2)
        val buttonRight1 = findViewById<ImageButton>(R.id.button_right1)
        val buttonRight2 = findViewById<ImageButton>(R.id.button_right2)

        weatherlistRecyclerView = findViewById(R.id.weatherlist_recycler_view)
        items.add(WeatherListItem("예시 제목 1", R.drawable.weather_sun_icon, "오기전날"))
        items.add(WeatherListItem("예시 제목 1", R.drawable.weather_rain_icon, "하루종일"))

        adapter = WeatherListAdapter(items) { position ->
            // 삭제 버튼 클릭 시 아이템 삭제
            items.removeAt(position)
            adapter.notifyItemRemoved(position)
        }

        weatherlistRecyclerView.adapter = adapter
        weatherlistRecyclerView.layoutManager = LinearLayoutManager(this)

        buttonLeft1.setOnClickListener {
            switchButton(buttonLeft1)

            // 현재 Activity가 MainActivity가 아니면 새 Intent로 시작
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // 두 번째 버튼 클릭 시 날씨 화면으로 전환
        buttonLeft2.setOnClickListener {
            switchButton(buttonLeft2)

            // 현재 Activity가 MainActivity인지 확인
            if (this is WeatherActivity) {
                // 현재 Activity가 MainActivity이면 아무것도 하지 않음
                return@setOnClickListener
            }

            val intent = Intent(this, WeatherActivity::class.java)
            startActivity(intent)
        }

        // 첫 번째 오른쪽 버튼 클릭 시 체크리스트 화면으로 전환
        buttonRight1.setOnClickListener {
            switchButton(buttonRight1)

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

            val bottomSheet = WeatherAddActivity()
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