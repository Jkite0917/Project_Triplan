package com.example.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "DayText")
data class DateText (
    @PrimaryKey val Date: String, // 메모 날짜, pk
    @ColumnInfo(name = "DayText") val DText: String // 내용
)

@Entity(tableName = "WeatherText")
data class WeatherText (
    @PrimaryKey(autoGenerate = true) val WNo: Int = 0, // 날씨 pk, 자동 생성
    @ColumnInfo val Weather: Int, // 날씨 아이콘 리소스
    @ColumnInfo val WTime: String, // 알림 시간
    @ColumnInfo val WText: String // 내용
)

@Entity(tableName = "CheckText")
data class CheckText (
    @PrimaryKey(autoGenerate = true) val CNo: Int = 0, // 체크리스트 pk, 자동 생성
    @ColumnInfo val CTitle: String, // 제목
    @ColumnInfo val CycleValue: String // 주기
)
