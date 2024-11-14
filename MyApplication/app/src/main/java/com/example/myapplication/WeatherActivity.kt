package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WeatherActivity : AppCompatActivity() {
    private lateinit var buttonLeft1: ImageButton
    private lateinit var buttonLeft2: ImageButton
    private lateinit var buttonRight1: ImageButton
    private lateinit var buttonRight2: ImageButton
    private lateinit var buttonCenter: ImageButton

    private lateinit var adapter: WeatherListAdapter
    private val items = mutableListOf<WeatherListItem>() // WeatherListItem 목록
    private lateinit var weatherlistRecyclerView: RecyclerView
    private lateinit var database: LocalDatabase
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather)
        setupButtonListeners()

        // 데이터베이스 초기화
        database = LocalDatabase.getDatabase(this)
        // SharedPreferences 초기화
        sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

        // RecyclerView 설정
        weatherlistRecyclerView = findViewById(R.id.recyclerview_weather_list)
        adapter = WeatherListAdapter(items) { wNo ->
            val itemToRemoveIndex = items.indexOfFirst { it.wNo == wNo }
            if (itemToRemoveIndex != -1) {
                val itemToRemove = items[itemToRemoveIndex]
                items.removeAt(itemToRemoveIndex)
                adapter.notifyItemRemoved(itemToRemoveIndex)

                // 데이터베이스에서도 해당 항목 삭제
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        database.getWeatherTextDao().deleteWeatherList(
                            WeatherList(
                                wNo = itemToRemove.wNo,
                                weather = itemToRemove.weather,
                                wTime = itemToRemove.time,
                                wText = itemToRemove.contents,
                                isNotified = itemToRemove.isNotified // 삭제 시 현재 알림 상태를 그대로 전달
                            )
                        )
                    }
                }
            }
        }
        weatherlistRecyclerView.adapter = adapter
        weatherlistRecyclerView.layoutManager = LinearLayoutManager(this)

        // 데이터베이스에서 저장된 데이터 불러와서 리스트에 추가
        lifecycleScope.launch {
            val savedItems = withContext(Dispatchers.IO) {
                database.getWeatherTextDao().getAllWeatherList()
            }
            items.addAll(savedItems.map { weatherList ->
                WeatherListItem(
                    wNo = weatherList.wNo,
                    contents = weatherList.wText,
                    weather = weatherList.weather,
                    time = weatherList.wTime,
                    isNotified = weatherList.isNotified // 알림 여부 설정
                )
            })
            adapter.notifyItemRangeInserted(0, savedItems.size)
        }
    }

    // 버튼 리스너 설정 함수
    private fun setupButtonListeners() {
        buttonLeft1 = findViewById(R.id.button_all_cardview_left1)
        buttonLeft2 = findViewById(R.id.button_all_cardview_left2)
        buttonRight1 = findViewById(R.id.button_all_cardview_right1)
        buttonRight2 = findViewById(R.id.button_all_cardview_right2)
        buttonCenter = findViewById(R.id.button_all_cardview_center)

        buttonLeft1.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        buttonLeft2.setOnClickListener {
            // 현재 액티비티가 WeatherActivity일 때는 동작하지 않음
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
        lifecycleScope.launch {
            val weatherList = WeatherList(
                weather = newItem.weather,
                wTime = newItem.time,
                wText = newItem.contents,
                isNotified = newItem.isNotified // 새로운 아이템의 알림 여부 설정
            )
            val insertedId = withContext(Dispatchers.IO) {
                database.getWeatherTextDao().insertWeatherList(weatherList)
            }

            val updatedItem = newItem.copy(wNo = insertedId)
            items.add(updatedItem)
            adapter.notifyItemInserted(items.size - 1)
        }
    }
}
