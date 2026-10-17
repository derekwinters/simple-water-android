package com.derekwinters.water.data

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.derekwinters.water.AppModuleFiles
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * LOG-012: a version 1 database (whole-ml INTEGER amounts) moves to version 2 (decimal-ml REAL
 * amounts) with every entry unchanged. The v1 database is built from the committed v1 schema, so
 * it is the database a v1 install really has. Opening it through Room runs the migration and then
 * validates the result against the v2 entities, failing the test if they disagree.
 *
 * SDK 34: Robolectric needs Java 21 for SDK 35, and the build runs on Java 17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DatabaseMigrationTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val name = "migration-test.db"

    @Before
    fun clean() {
        context.deleteDatabase(name)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(name)
    }

    @Test
    fun version1EntriesKeepTheirIdAmountAndInstant() {
        createVersion1 { db ->
            db.execSQL("INSERT INTO drink_entries (id, amountMl, loggedAt) VALUES (1, 250, 1000)")
            db.execSQL("INSERT INTO drink_entries (id, amountMl, loggedAt) VALUES (2, 237, 2000)")
            db.execSQL("INSERT INTO drink_entries (id, amountMl, loggedAt) VALUES (7, 5000, 3000)")
            db.execSQL("INSERT INTO goal_records (epochDay, goalMl) VALUES (20000, 2450)")
        }

        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*AppDatabase.MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            val db = database.openHelper.writableDatabase
            assertEquals(2, db.version)

            val rows = mutableListOf<String>()
            db.query("SELECT id, amountMl, loggedAt, typeof(amountMl) FROM drink_entries ORDER BY id").use { c ->
                while (c.moveToNext()) {
                    rows += "${c.getLong(0)}|${c.getDouble(1)}|${c.getLong(2)}|${c.getString(3)}"
                }
            }
            assertEquals(listOf("1|250.0|1000|real", "2|237.0|2000|real", "7|5000.0|3000|real"), rows)

            db.query("SELECT epochDay, goalMl FROM goal_records").use { c ->
                c.moveToFirst()
                assertEquals(20000L, c.getLong(0))
                assertEquals(2450, c.getInt(1))
            }

            // Decimal ml are kept exactly after the migration (GOAL-008).
            runBlocking { database.drinkEntryDao().insert(DrinkEntry(amountMl = 236.588, loggedAt = 4000)) }
            db.query("SELECT amountMl FROM drink_entries WHERE loggedAt = 4000").use { c ->
                c.moveToFirst()
                assertEquals(236.588, c.getDouble(0), 0.0)
            }
        } finally {
            database.close()
        }
    }

    /** Builds [name] exactly as the committed version 1 schema describes it, then runs [fill]. */
    private fun createVersion1(fill: (SQLiteDatabase) -> Unit) {
        val schema = JSONObject(AppModuleFiles.file("schemas/$SCHEMA_DIR/1.json").readText()).getJSONObject("database")
        assertEquals(1, schema.getInt("version"))
        val file = context.getDatabasePath(name).apply { parentFile?.mkdirs() }
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        try {
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices")
                for (j in 0 until (indices?.length() ?: 0)) {
                    db.execSQL(indices!!.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            fill(db)
            db.version = 1
        } finally {
            db.close()
        }
    }

    private companion object {
        const val SCHEMA_DIR = "com.derekwinters.water.data.AppDatabase"
    }
}
