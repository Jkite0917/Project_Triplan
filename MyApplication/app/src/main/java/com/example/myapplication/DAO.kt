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
interface WeatherListDao {
    @Query("SELECT * FROM WeatherList")
    fun getAllWeatherList(): List<WeatherList>

    @Query("SELECT * FROM WeatherList WHERE wNo = :wNo")
    suspend fun getWeatherList(wNo: Long): WeatherList?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherList(weatherList: WeatherList): Long

    @Update
    suspend fun updateWeatherList(weatherList: WeatherList)

    @Delete
    suspend fun deleteWeatherList(weatherList: WeatherList)

    @Query("DELETE FROM WeatherList WHERE wNo = :wNo")
    suspend fun deleteWeatherListById(wNo: Long)

    @Query("DELETE FROM WeatherList")
    suspend fun deleteAllWeatherLists()
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


