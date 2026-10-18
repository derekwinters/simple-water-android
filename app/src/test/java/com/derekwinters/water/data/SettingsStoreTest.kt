package com.derekwinters.water.data

import android.app.Application
import android.content.Context
import com.derekwinters.water.core.CupTextMode
import com.derekwinters.water.core.WidgetSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * UI-023 (docs/spec/screens.md): the widget settings default to no text with the ideal line on,
 * and are saved and read back with the other settings (APP-003).
 *
 * One test method: DataStore keeps one instance per file for the whole process, so the defaults
 * are checked before anything is written, in the same test that writes them.
 *
 * SDK 34: Robolectric needs Java 21 for SDK 35, and the build runs on Java 17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SettingsStoreTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    // UI-023: defaults, then each setting saved and restored by a fresh store, without touching the rest.
    @Test
    fun widgetSettingsDefaultThenSaveAndRestore() = runBlocking {
        val before = SettingsStore(context).current()
        assertEquals(WidgetSettings(text = CupTextMode.NONE, idealLine = true), before.widget)

        for (chosen in listOf(
            WidgetSettings(text = CupTextMode.PERCENT, idealLine = true),
            WidgetSettings(text = CupTextMode.AMOUNT, idealLine = false),
            WidgetSettings(text = CupTextMode.NONE, idealLine = false),
        )) {
            SettingsStore(context).update { it.copy(widget = chosen) }
            val restored = SettingsStore(context).current()
            assertEquals(chosen, restored.widget)
            assertEquals(before.copy(widget = chosen), restored)
        }
    }
}
