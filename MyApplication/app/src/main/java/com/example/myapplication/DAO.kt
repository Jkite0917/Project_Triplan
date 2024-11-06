package com.example.myapplication

import androidx.room.*

@Dao
interface DailyScheduleDao {
    @Query("SELECT * FROM DailySchedule")
    fun getAllDailyScheduleInfo(): List<DailySchedule>

    @Query("SELECT Date FROM DailySchedule")
    fun getAllInfoDate(): List<String>

    @Query("SELECT * FROM DailySchedule WHERE Date = :date")
    suspend fun getDailyScheduleInfo(date: String): DailySchedule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyScheduleInfo(dateText: DailySchedule)

    @Update
    suspend fun updateDailyScheduleInfo(dateText: DailySchedule)

    @Delete
    suspend fun deleteDailyScheduleInfo(dateText: DailySchedule)

    @Query("DELETE FROM DailySchedule")
    suspend fun deleteAllDailyScheduleInfo()
}

@Dao
interface WeatherListDao {
    @Query("SELECT * FROM WeatherList")
    fun getAllWeatherList(): List<WeatherList>

    @Query("SELECT * FROM WeatherList WHERE WNo = :wNo")
    suspend fun getWeatherList(wNo: Long): WeatherList?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherList(weatherList: WeatherList): Long

    @Update
    suspend fun updateWeatherList(weatherList: WeatherList)

    @Delete
    suspend fun deleteWeatherList(weatherList: WeatherList)

    @Query("DELETE FROM WeatherList WHERE WNo = :wNo")
    suspend fun deleteWeatherListById(wNo: Long)

    @Query("DELETE FROM WeatherList")
    suspend fun deleteAllWeatherLists()
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



