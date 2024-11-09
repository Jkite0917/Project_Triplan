package com.example.myapplication

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(entities = [DailySchedule::class, WeatherList::class, Checklist::class], version = 2)
abstract class LocalDatabase : RoomDatabase() {
    abstract fun getDailyScheduleDao(): DailyScheduleDao
    abstract fun getWeatherTextDao(): WeatherListDao
    abstract fun getChecklistDao(): ChecklistDao

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
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
                // 주의 fallback....() 사용 시 버전 업 경우 데이터 전부 삭제
            }
        }
    }
}
