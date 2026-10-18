package com.derekwinters.water

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * The home screen widget's declarations and wiring (docs/spec/screens.md UI-014–023 and the widget
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

    // UI-019, BRAND-056: the picker's preview is the launcher foreground, the cup at three-quarters.
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

    // Widget invariant (BRAND-056): the cup is drawn from CupGeometry's master path data and brand
    // colours, never the fixed three-quarters foreground or a copy of the cup in a resource.
    @Test
    fun `the widget draws the cup from CupGeometry and no copy of the cup exists`() {
        assertTrue("The widget does not draw with CupBitmap.", "CupBitmap.render(" in widgetSources)
        for (geometry in listOf("CupGeometry.cupPath", "CupGeometry.waterPath(", "CupGeometry.BLUE", "CupGeometry.GREY")) {
            assertTrue("The widget does not draw $geometry.", geometry in widgetSources)
        }
        for (gone in listOf("R.drawable.ic_launcher_foreground", "RingBitmap")) {
            assertFalse("The widget still uses $gone.", gone in widgetSources)
        }
        assertFalse("The widget hard-codes a colour (DS-052, BRAND-056).", Regex("""0x[Ff]{2}[0-9A-Fa-f]{6}""").containsMatchIn(widgetSources))
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

    // Widget invariant: home takes its rings from Rings, and the widget its cup from WidgetCup, which
    // reads the same Rings and GoalMath values; neither works them out a second time.
    @Test
    fun `home and the widget take their values from Rings and WidgetCup alone`() {
        val home = source("ui/home/HomeScreen.kt")
        assertTrue("Home's ring card does not use Rings.of.", "Rings.of(" in home)
        assertTrue("The widget does not use WidgetCup.today.", "WidgetCup.today(" in widgetSources)
        for (second in listOf("GoalMath.fraction", ".idealFraction", ".tickFractions")) {
            assertFalse("HomeScreen.kt works out a ring with $second instead of Rings.", second in home)
        }
        for (second in listOf("GoalMath.", "Rings.of(", "Rings.today(", "Pace.today(", ".idealFraction", ".tickFractions", "Units.")) {
            assertFalse("The widget works out its cup with $second instead of WidgetCup.", second in widgetSources)
        }
    }

    // UI-015: the cup is fitted to the widget less spacing.xs (4dp) each side, not spacing.md.
    @Test
    fun `the cup comes within spacing xs of the widget's edge`() {
        val content = body(source("widget/WaterWidget.kt"), "WidgetContent")
        assertTrue("The cup's margin is not spacing.xs.", Regex("""paddingPx\s*=\s*spacing\.xs\b""").containsMatchIn(content))
        assertFalse("The cup's margin is still spacing.md.", Regex("""paddingPx\s*=\s*spacing\.md\b""").containsMatchIn(content))
    }

    // UI-015, background invariant: the card and its corners are drawn only while Background is on,
    // the cup fill only while Cup fill is on, and the fill is CupGeometry's closed cup outline.
    @Test
    fun `the background and the cup fill follow their settings`() {
        val content = body(source("widget/WaterWidget.kt"), "WidgetContent")
        val cardWhen = Regex("""if\s*\(\s*cup\.background\s*\)""").find(content)
        assertTrue("The card is not conditional on cup.background.", cardWhen != null)
        val afterIf = content.substring(cardWhen!!.range.last)
        for (card in listOf(".background(colors.chip)", ".cornerRadius(")) {
            assertTrue("$card is not drawn under the Background setting.", card in afterIf)
            assertEquals("$card is drawn more than once.", 1, content.split(card).size - 1)
        }
        val fill = body(source("widget/CupBitmap.kt"), "drawFill")
        assertTrue("The cup fill is not CupGeometry.fillPath.", "CupGeometry.fillPath" in fill)
        assertTrue("The cup fill is not bg at FILL_OPACITY.", "colors.bg.copy(alpha = CupGeometry.FILL_OPACITY" in fill)
        val render = body(source("widget/CupBitmap.kt"), "render")
        assertTrue("The cup fill is not under the Cup fill setting.", Regex("""if\s*\(\s*state\.cupFill\s*\)\s*drawFill\(""").containsMatchIn(render))
        // UI-015: from the back, fill, water, ideal line, cup, text.
        val order = listOf("drawFill(", "drawWater(", "drawIdealLine(", "drawCup(", "drawCheck(")
        val at = order.map { render.indexOf(it) }
        assertTrue("Drawn out of order: ${order.zip(at)}", at.all { it >= 0 } && at == at.sorted())
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
