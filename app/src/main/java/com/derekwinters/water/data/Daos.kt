package com.derekwinters.water.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DrinkEntryDao {
    /** LOG-002, LOG-007: entries in `[start, end)`, newest first. */
    @Query("SELECT * FROM drink_entries WHERE loggedAt >= :start AND loggedAt < :end ORDER BY loggedAt DESC, id DESC")
    fun observeBetween(start: Long, end: Long): Flow<List<DrinkEntry>>

    /** LOG-003: the unrounded total of entries in `[start, end)` (GOAL-008 invariant). */
    @Query("SELECT COALESCE(SUM(amountMl), 0.0) FROM drink_entries WHERE loggedAt >= :start AND loggedAt < :end")
    suspend fun totalBetween(start: Long, end: Long): Double

    /** REPLACE so LOG-008's undo can re-insert a deleted row with its original id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: DrinkEntry): Long

    @Delete
    suspend fun delete(entry: DrinkEntry)
}

@Dao
interface GoalRecordDao {
    @Query("SELECT * FROM goal_records ORDER BY epochDay")
    fun observeAll(): Flow<List<GoalRecordEntity>>

    /** GOAL-012: one record per day; a later change today replaces the earlier one. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: GoalRecordEntity)
}
