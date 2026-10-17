package com.derekwinters.water.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Version 1 stored drink amounts as whole ml (INTEGER); version 2 stores decimal ml (REAL,
 * GOAL-008). Schemas are exported to `app/schemas/`.
 */
@Database(
    entities = [DrinkEntry::class, GoalRecordEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun drinkEntryDao(): DrinkEntryDao
    abstract fun goalRecordDao(): GoalRecordDao

    companion object {
        /**
         * LOG-012: `drink_entries.amountMl` becomes REAL. SQLite cannot change a column's type in
         * place, so the table is rebuilt; every row keeps its id, amount and instant.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `drink_entries_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`amountMl` REAL NOT NULL, `loggedAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "INSERT INTO `drink_entries_new` (`id`, `amountMl`, `loggedAt`) " +
                        "SELECT `id`, CAST(`amountMl` AS REAL), `loggedAt` FROM `drink_entries`",
                )
                db.execSQL("DROP TABLE `drink_entries`")
                db.execSQL("ALTER TABLE `drink_entries_new` RENAME TO `drink_entries`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_drink_entries_loggedAt` ON `drink_entries` (`loggedAt`)")
            }
        }

        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "water.db")
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
