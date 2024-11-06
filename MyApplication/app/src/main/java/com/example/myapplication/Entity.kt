package com.example.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "DailySchedule")
data class DailySchedule (
    @PrimaryKey val Date: String, // 메모 날짜, pk
    @ColumnInfo(name = "Info") val Info: String // 내용
)

@Entity(tableName = "WeatherText")
data class WeatherText (
    @PrimaryKey(autoGenerate = true) val WNo: Int = 0, // 날씨 pk, 자동 생성
    @ColumnInfo val Weather: Int, // 날씨 아이콘 리소스
    @ColumnInfo val WTime: String, // 알림 시간
    @ColumnInfo val WText: String // 내용
)

@Entity(tableName = "Checklist")
data class Checklist(
    @PrimaryKey(autoGenerate = true) val cNo: Int = 0, // 체크리스트 pk, 자동 생성
    @ColumnInfo val cTitle: String, // 제목
    @ColumnInfo var isChecked: Boolean = false, // 체크리스트 선택
    @ColumnInfo val period: String, // 주기
    @ColumnInfo val weekDay: String? = null, // 요일 (선택 사항)
    @ColumnInfo val monthDay: String? = null // 날짜 (선택 사항)
)
