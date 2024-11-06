package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherActivity : AppCompatActivity() {
    private lateinit var buttonLeft1: ImageButton
    private lateinit var buttonLeft2: ImageButton
    private lateinit var buttonRight1: ImageButton
    private lateinit var buttonRight2: ImageButton
    private lateinit var buttonCenter: ImageButton

    private lateinit var adapter: WeatherListAdapter
    private val items = mutableListOf<WeatherListItem>()
    private lateinit var weatherlistRecyclerView: RecyclerView
    private lateinit var database: LocalDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather) // 메인 레이아웃 설정
        setupButtonListeners()

        // 데이터베이스 초기화
        database = LocalDatabase.getDatabase(this)

        weatherlistRecyclerView = findViewById(R.id.WeatherlistRecyclerView)

        // 어댑터 설정
        adapter = WeatherListAdapter(items) { wNo ->
            val itemToRemoveIndex = items.indexOfFirst { it.wNo == wNo }
            if (itemToRemoveIndex != -1) {
                val itemToRemove = items[itemToRemoveIndex]
                items.removeAt(itemToRemoveIndex)
                adapter.notifyItemRemoved(itemToRemoveIndex)

                // 데이터베이스에서도 삭제
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        database.getWeatherTextDao().deleteWeatherList(
                            WeatherList(WNo = itemToRemove.wNo, Weather = itemToRemove.weather, WTime = itemToRemove.time, WText = itemToRemove.contents)
                        )
                    }
                }
            }
        }

        weatherlistRecyclerView.adapter = adapter
        weatherlistRecyclerView.layoutManager = LinearLayoutManager(this)

        // 데이터베이스에서 저장된 데이터 불러오기
        lifecycleScope.launch {
            val savedItems = withContext(Dispatchers.IO) {
                database.getWeatherTextDao().getAllWeatherList()
            }
            items.addAll(savedItems.map { weatherList ->
                WeatherListItem(
                    wNo = weatherList.WNo,
                    contents = weatherList.WText,
                    weather = weatherList.Weather,
                    time = weatherList.WTime
                )
            })
            adapter.notifyItemRangeInserted(0, savedItems.size)
        }
    }

    private fun setupButtonListeners() {
        buttonLeft1 = findViewById(R.id.button_left1)
        buttonLeft2 = findViewById(R.id.button_left2)
        buttonRight1 = findViewById(R.id.button_right1)
        buttonRight2 = findViewById(R.id.button_right2)
        buttonCenter = findViewById(R.id.button_center)

        buttonLeft1.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        buttonRight1.setOnClickListener {
            startActivity(Intent(this, CheckActivity::class.java))
        }

        buttonRight2.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }

        buttonCenter.setOnClickListener {
            val bottomSheet = WeatherAddActivity { newItem ->
                addItemToWeatherList(newItem)
            }
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        }
    }

    // 새로운 아이템을 리스트에 추가하고 데이터베이스에 저장하는 함수
    private fun addItemToWeatherList(newItem: WeatherListItem) {
        // 데이터베이스에 새로운 항목 저장
        lifecycleScope.launch {
            val weatherList = WeatherList(
                Weather = newItem.weather,
                WTime = newItem.time,
                WText = newItem.contents
            )
            val insertedId = withContext(Dispatchers.IO) {
                database.getWeatherTextDao().insertWeatherList(weatherList)
            }

            // 새로 추가된 아이템에 WNo를 업데이트하고 리스트에 추가
            val updatedItem = newItem.copy(wNo = insertedId)
            items.add(updatedItem)
            adapter.notifyItemInserted(items.size - 1)
        }
    }
}
