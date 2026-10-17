package com.derekwinters.water.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.derekwinters.water.core.GoalRecord
import com.derekwinters.water.core.LoggedAmount

/** LOG-001: one drink, in decimal ml (GOAL-008), and the instant it was logged (epoch millis). */
@Entity(tableName = "drink_entries", indices = [Index(value = ["loggedAt"])])
data class DrinkEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMl: Int,
    val loggedAt: Long,
) {
    fun toLoggedAmount(): LoggedAmount = LoggedAmount(loggedAtMillis = loggedAt, amountMl = amountMl)
}

/** GOAL-012: the effective goal recorded against a date (one row per epoch day). */
@Entity(tableName = "goal_records")
data class GoalRecordEntity(
    @PrimaryKey val epochDay: Long,
    val goalMl: Int,
) {
    fun toGoalRecord(): GoalRecord = GoalRecord(epochDay = epochDay, goalMl = goalMl)
}
