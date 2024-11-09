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

// 날씨 관련 항목을 표시하고 관리하는 액티비티
class WeatherActivity : AppCompatActivity() {
    // 버튼 변수 초기화
    private lateinit var buttonLeft1: ImageButton
    private lateinit var buttonLeft2: ImageButton
    private lateinit var buttonRight1: ImageButton
    private lateinit var buttonRight2: ImageButton
    private lateinit var buttonCenter: ImageButton

    // 어댑터 및 날씨 항목 리스트 초기화
    private lateinit var adapter: WeatherListAdapter
    private val items = mutableListOf<WeatherListItem>() // WeatherListItem 목록
    private lateinit var weatherlistRecyclerView: RecyclerView
    private lateinit var database: LocalDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather) // 메인 레이아웃 설정
        setupButtonListeners() // 버튼 리스너 설정

        // 데이터베이스 초기화
        database = LocalDatabase.getDatabase(this)

        // RecyclerView 설정
        weatherlistRecyclerView = findViewById(R.id.recyclerview_weather_list)

        // 어댑터 초기화 및 삭제 리스너 설정
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
                            WeatherList(wNo = itemToRemove.wNo, weather = itemToRemove.weather, wTime = itemToRemove.time, wText = itemToRemove.contents)
                        )
                    }
                }
            }
        }

        // RecyclerView에 어댑터와 레이아웃 매니저 설정
        weatherlistRecyclerView.adapter = adapter
        weatherlistRecyclerView.layoutManager = LinearLayoutManager(this)

        // 데이터베이스에서 저장된 데이터 불러와서 리스트에 추가
        lifecycleScope.launch {
            val savedItems = withContext(Dispatchers.IO) {
                database.getWeatherTextDao().getAllWeatherList()
            }
            // WeatherList를 WeatherListItem으로 변환 후 추가
            items.addAll(savedItems.map { weatherList ->
                WeatherListItem(
                    wNo = weatherList.wNo,
                    contents = weatherList.wText,
                    weather = weatherList.weather,
                    time = weatherList.wTime
                )
            })
            // RecyclerView에 저장된 항목 추가 갱신
            adapter.notifyItemRangeInserted(0, savedItems.size)
        }
    }

    // 버튼 리스너 설정 함수
    private fun setupButtonListeners() {
        // 버튼 초기화
        buttonLeft1 = findViewById(R.id.button_all_cardview_left1)
        buttonLeft2 = findViewById(R.id.button_all_cardview_left2)
        buttonRight1 = findViewById(R.id.button_all_cardview_right1)
        buttonRight2 = findViewById(R.id.button_all_cardview_right2)
        buttonCenter = findViewById(R.id.button_all_cardview_center)

        // 버튼 클릭 리스너 설정
        buttonLeft1.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java)) // 메인 화면으로 이동
        }

        buttonLeft2.setOnClickListener {
            // 현재 액티비티가 WeatherActivity일 때는 동작하지 않음
        }

        buttonRight1.setOnClickListener {
            startActivity(Intent(this, CheckActivity::class.java)) // 체크 화면으로 이동
        }

        buttonRight2.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java)) // 설정 화면으로 이동
        }

        // 중앙 버튼 클릭 시 날씨 추가 화면 표시
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
                weather = newItem.weather,
                wTime = newItem.time,
                wText = newItem.contents
            )
            // 데이터베이스에 저장된 항목 ID 반환
            val insertedId = withContext(Dispatchers.IO) {
                database.getWeatherTextDao().insertWeatherList(weatherList)
            }

            // 반환된 ID로 WeatherListItem 업데이트 후 리스트에 추가
            val updatedItem = newItem.copy(wNo = insertedId)
            items.add(updatedItem)
            adapter.notifyItemInserted(items.size - 1) // 추가된 위치만 갱신
        }
    }
}
