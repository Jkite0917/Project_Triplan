package com.example.myapplication

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(entities = [DateText::class, WeatherText::class, CheckText::class], version = 1)
abstract class LocalDatabase : RoomDatabase() {
    abstract fun getDateTextDao(): DateTextDao
    abstract fun getWeatherTextDao(): WeatherTextDao
    abstract fun getCheckTextDao(): CheckTextDao

    /*
        싱글턴 인스턴스 생성
        앱에서 단 하나의 데이터베이스만 만들게 해줌 -> 여러 데이터베이스 추가해서 리소스 낭비 및
        싱글턴 인스턴스 생명주기
        앱 설치 (DB 생성) -> 앱 사용(RMID 수행) -> 앱 삭제(DB 삭제)
        우리에겐 필요한 기능 같아 긁어옴
    */

    companion object {
        @Volatile
        private var INSTANCE: LocalDatabase? = null

        fun getDatabase(context: Context): LocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LocalDatabase::class.java,
                    "calendar_database" // 데이터베이스 이름 설정
                ).build().also { INSTANCE = it }
            }
        }
    }
}