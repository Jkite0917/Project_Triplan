package com.example.myapplication

import androidx.room.*

@Dao
interface DailyScheduleDao {
    @Query("SELECT * FROM DailySchedule")
    fun getAllDailyScheduleInfo(): List<DailySchedule>

    @Query("SELECT Date FROM DailySchedule")
    fun getAllInfoDate(): List<String>

    @Query("SELECT Info FROM DailySchedule WHERE Date = :date LIMIT 1")  // 하나만 가져오기
    suspend fun getDailyScheduleInfo(date: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyScheduleInfo(date: DailySchedule)

    @Update
    suspend fun updateDailyScheduleInfo(date: DailySchedule)

    @Query("DELETE FROM DailySchedule WHERE Date = :date")
    suspend fun deleteDailyScheduleInfo(date: String)

    @Query("DELETE FROM DailySchedule")
    suspend fun deleteAllDailyScheduleInfo()
}

@Dao
interface WeatherTextDao {
    @Query("SELECT * FROM WeatherText")
    fun getAllWeatherText(): List<WeatherText>

    @Query("SELECT * FROM WeatherText WHERE WNo = :wNo")
    suspend fun getWeatherText(wNo: Int): WeatherText?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherText(weatherText: WeatherText)

    @Update
    suspend fun updateWeatherText(weatherText: WeatherText)

    @Delete
    suspend fun deleteWeatherText(weatherText: WeatherText)

    @Query("DELETE FROM WeatherText")
    suspend fun deleteAllWeatherTexts()
}

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM Checklist")
    fun getAllChecklistItems(): List<Checklist>

    @Query("SELECT * FROM Checklist WHERE cNo = :cNo")
    suspend fun getChecklistItem(cNo: Int): Checklist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItem(checklist: Checklist)

    @Query("DELETE FROM Checklist WHERE cNo = :cNo")
    suspend fun deleteChecklistItemByCNo(cNo: Int)

    @Update
    suspend fun updateChecklistItem(checklist: Checklist)

    @Query("DELETE FROM Checklist")
    suspend fun deleteAllChecklistItems()
}



