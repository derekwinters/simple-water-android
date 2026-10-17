package com.derekwinters.water

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * The home screen widget's declarations and wiring (docs/spec/screens.md UI-014–019 and the widget
 * invariants; docs/spec/brand.md BRAND-056), read from the app's own files.
 */
class WidgetDeclarationTest {
    private val manifest = AppModuleFiles.parse(AppModuleFiles.file("src/main/AndroidManifest.xml"))
    private val receivers = manifest.getElementsByTagName("receiver").elements()
        .associateBy { it.getAttribute("android:name") }
    private val provider: Element by lazy {
        val file = AppModuleFiles.file("src/main/res/xml/water_widget_info.xml")
        assertTrue("No ${file.path} (UI-014).", file.isFile)
        AppModuleFiles.parse(file).documentElement
    }

    private fun source(relative: String): String =
        AppModuleFiles.file("src/main/java/com/derekwinters/water/$relative").readText()

    private val widgetSources: String by lazy {
        AppModuleFiles.file("src/main/java/com/derekwinters/water/widget")
            .walkTopDown().filter { it.extension == "kt" }.joinToString("\n") { it.readText() }
    }

    /** The body of `fun [name]` in [text]: from its declaration to the next declaration at that depth. */
    private fun body(text: String, name: String): String {
        val start = Regex("""\n(\s*)(override\s+|private\s+|suspend\s+)*fun\s+$name\b""").find(text)
        assertTrue("No `fun $name`.", start != null)
        val indent = start!!.groupValues[1]
        val end = text.indexOf("\n$indent}", start.range.last)
        return text.substring(start.range.first, if (end < 0) text.length else end)
    }

    // UI-014: 2 × 2 minimum, resizable both ways, never below 2 × 2.
    @Test
    fun `the widget places at 2 x 2 and resizes both ways`() {
        assertEquals("appwidget-provider", provider.tagName)
        assertEquals("110dp", provider.getAttribute("android:minWidth"))
        assertEquals("110dp", provider.getAttribute("android:minHeight"))
        assertEquals("2", provider.getAttribute("android:targetCellWidth"))
        assertEquals("2", provider.getAttribute("android:targetCellHeight"))
        val resize = provider.getAttribute("android:resizeMode").split('|').toSet()
        assertEquals(setOf("horizontal", "vertical"), resize)
        for (attribute in listOf("android:minResizeWidth", "android:minResizeHeight")) {
            val value = provider.getAttribute(attribute)
            assertTrue("$attribute \"$value\" lets the widget shrink below 2 × 2.", value.isEmpty() || value == "110dp")
        }
        assertEquals("home_screen", provider.getAttribute("android:widgetCategory"))
        // UI-018: refreshed by the app's own hooks, not a periodic update.
        assertEquals("0", provider.getAttribute("android:updatePeriodMillis"))
    }

    // UI-019, BRAND-056: the picker's preview is the launcher foreground.
    @Test
    fun `the picker preview is the launcher foreground`() {
        assertEquals("@drawable/ic_launcher_foreground", provider.getAttribute("android:previewImage"))
    }

    // UI-018: the launcher's broadcasts reach the widget; the midnight alarm stays inside the app.
    @Test
    fun `the widget receivers are declared`() {
        val widget = receivers.getValue(".widget.WaterWidgetReceiver")
        assertEquals("true", widget.getAttribute("android:exported"))
        val actions = widget.getElementsByTagName("action").elements().map { it.getAttribute("android:name") }
        assertEquals(listOf("android.appwidget.action.APPWIDGET_UPDATE"), actions)
        val metadata = widget.getElementsByTagName("meta-data").elements().single()
        assertEquals("android.appwidget.provider", metadata.getAttribute("android:name"))
        assertEquals("@xml/water_widget_info", metadata.getAttribute("android:resource"))

        assertEquals("false", receivers.getValue(".widget.WidgetRefreshReceiver").getAttribute("android:exported"))
    }

    // Widget invariant (BRAND-056): the cup is the launcher foreground drawable, never a redrawn copy.
    @Test
    fun `the widget draws the launcher foreground and no copy of the cup exists`() {
        assertTrue("The widget does not draw R.drawable.ic_launcher_foreground.", "R.drawable.ic_launcher_foreground" in widgetSources)
        val cup = AppModuleFiles.parse(AppModuleFiles.file("src/main/res/drawable/ic_launcher_foreground.xml"))
            .getElementsByTagName("path").elements().map { it.getAttribute("android:pathData") }
        val allowed = setOf("ic_launcher_foreground.xml", "ic_launcher_monochrome.xml")
        AppModuleFiles.file("src/main/res").walkTopDown()
            .filter { it.isFile && it.extension == "xml" && it.name !in allowed }
            .forEach { file ->
                val text = file.readText()
                cup.forEach { path -> assertFalse("${file.path} copies the cup's path data (BRAND-056).", path in text) }
            }
    }

    // Widget invariant: home and the widget take their rings from `Rings`, never a second calculation.
    @Test
    fun `home and the widget take their rings from Rings alone`() {
        val home = source("ui/home/HomeScreen.kt")
        assertTrue("Home's ring card does not use Rings.of.", "Rings.of(" in home)
        assertTrue("The widget does not use Rings.today.", "Rings.today(" in widgetSources)
        for ((name, text) in listOf("HomeScreen.kt" to home, "the widget" to widgetSources)) {
            for (second in listOf("GoalMath.fraction", ".idealFraction", ".tickFractions")) {
                assertFalse("$name works out a ring with $second instead of Rings.", second in text)
            }
        }
    }

    // UI-017: a tap opens the app on home, as the reminder notification does.
    @Test
    fun `a tap opens the app on home`() {
        assertTrue("MainActivity.ACTION_OPEN_HOME" in widgetSources)
        assertTrue("actionStartActivity" in widgetSources)
    }

    // UI-018: every refresh point redraws the widget.
    @Test
    fun `every refresh point redraws the widget`() {
        val refresh = "WaterWidgetUpdater.refreshAll("
        val repository = source("data/WaterRepository.kt")
        for (name in listOf("addEntry", "deleteEntry", "restoreEntry", "updateSettings")) {
            assertTrue("WaterRepository.$name does not refresh the widget.", refresh in body(repository, name))
        }
        val alarms = source("reminders/AlarmReceiver.kt")
        assertTrue("A handled reminder alarm does not refresh the widget.", refresh in body(alarms, "onReceive"))
        val reschedule = alarms.substringAfter("class RescheduleReceiver")
        assertTrue("A time, zone, boot or update broadcast does not refresh the widget.", refresh in body(reschedule, "onReceive"))
        val midnight = source("widget/WidgetRefreshReceiver.kt")
        assertTrue("The midnight alarm does not refresh the widget.", refresh in body(midnight, "onReceive"))
    }
}
