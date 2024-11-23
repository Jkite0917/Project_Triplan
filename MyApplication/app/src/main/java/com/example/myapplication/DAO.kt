package com.example.myapplication

import androidx.room.*

@Dao
interface DailyScheduleDao {

    @Query("SELECT Info FROM DailySchedule WHERE date = :date LIMIT 1")  // 하나만 가져오기
    suspend fun getDailyScheduleInfo(date: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyScheduleInfo(date: DailySchedule)

    @Query("DELETE FROM DailySchedule WHERE date = :date")
    suspend fun deleteDailyScheduleInfo(date: String)

}

@Dao
interface WeatherListDao {
    // WeatherList 데이터를 가져오는 쿼리
    @Query("SELECT * FROM WeatherList")
    fun getAllWeatherList(): List<WeatherList>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherList(weatherList: WeatherList): Long

    @Delete
    suspend fun deleteWeatherList(weatherList: WeatherList)

    // isNotified 필드 업데이트를 위한 쿼리
    @Query("UPDATE WeatherList SET isNotified = :isNotified WHERE wNo = :wNo")
    suspend fun updateNotificationStatus(wNo: Long, isNotified: Boolean)

}


@Dao
interface ChecklistDao {

    @Query("SELECT * FROM Checklist")
    fun getAllChecklistItems(): List<Checklist>

    @Query("SELECT * FROM Checklist WHERE cNo = :cNo")
    suspend fun getChecklistItem(cNo: Long): Checklist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItem(checklist: Checklist): Long

    @Query("DELETE FROM Checklist WHERE cNo = :cNo")
    suspend fun deleteChecklistItemByCNo(cNo: Long)

    @Update
    suspend fun updateChecklistItem(checklist: Checklist)

    // lastCheckedDate와 isChecked만 업데이트하는 쿼리
    @Query("""
        UPDATE Checklist
        SET isChecked = :isChecked, lastCheckedDate = :lastCheckedDate
        WHERE cNo = :cNo
    """)
    suspend fun updateChecklistItemById(cNo: Long, isChecked: Boolean, lastCheckedDate: Long)

    @Query("DELETE FROM Checklist")
    suspend fun deleteAllChecklistItems()
}


