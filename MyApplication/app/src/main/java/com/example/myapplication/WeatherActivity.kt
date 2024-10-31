package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager

class WeatherActivity : AppCompatActivity() {
    private lateinit var buttonLeft1: ImageButton
    private lateinit var buttonLeft2: ImageButton
    private lateinit var buttonRight1: ImageButton
    private lateinit var buttonRight2: ImageButton
    private lateinit var buttonCenter: ImageButton


    private lateinit var adapter: WeatherListAdapter
    private val items = mutableListOf<WeatherListItem>()

    private lateinit var weatherlistRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather) // 메인 레이아웃 설정
        setupButtonListeners()


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
            if (this is WeatherActivity) {
                // 현재 Activity가 MainActivity이면 아무것도 하지 않음
                return@setOnClickListener
            }
        }

        buttonRight1.setOnClickListener {
            startActivity(Intent(this, CheckActivity::class.java))
        }

        buttonRight2.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }

        buttonCenter.setOnClickListener {
            val bottomSheet = WeatherAddActivity()
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        }
    }
}