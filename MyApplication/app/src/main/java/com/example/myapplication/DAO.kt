package com.example.myapplication

import androidx.room.*

@Dao
interface DateTextDao {
    @Query("SELECT * FROM DayText")
    fun getAllDateText(): List<DateText>

    @Query("SELECT * FROM DayText WHERE Date = :date")
    suspend fun getDateText(date: String): DateText?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDateText(dateText: DateText)

    @Update
    suspend fun updateDateText(dateText: DateText)

    @Delete
    suspend fun deleteDateText(dateText: DateText)

    @Query("DELETE FROM DayText")
    suspend fun deleteAllDateTexts()
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



