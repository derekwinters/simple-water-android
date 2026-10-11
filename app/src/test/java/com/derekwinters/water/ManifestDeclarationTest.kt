package com.derekwinters.water

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/** Reads AndroidManifest.xml and the theme it names (DS-022, UI-012, REM-016, BRAND-052). */
class ManifestDeclarationTest {
    private val manifest = AppModuleFiles.parse(AppModuleFiles.file("src/main/AndroidManifest.xml"))
    private val application = manifest.getElementsByTagName("application").elements().single()

    // DS-022, UI-012: the theme's own body suppresses the action bar.
    @Test
    fun `the application theme suppresses the platform action bar in its own body`() {
        val themeAttribute = application.getAttribute("android:theme")
        assertTrue("<application> declares no @style theme (DS-022).", themeAttribute.startsWith("@style/"))
        val name = themeAttribute.removePrefix("@style/")
        val values = AppModuleFiles.file("src/main/res/values")
        val theme = values.listFiles { f -> f.isFile && f.extension == "xml" }!!.sorted()
            .flatMap { AppModuleFiles.parse(it).getElementsByTagName("style").elements() }
            .firstOrNull { it.getAttribute("name") == name }
        assertNotNull("No <style name=\"$name\"> under res/values (DS-022).", theme)
        assertEquals("false", item(theme!!, "android:windowActionBar"))
        assertEquals("true", item(theme, "android:windowNoTitle"))
    }

    // REM-016: permissions.
    @Test
    fun `the reminder permissions are declared`() {
        val permissions = manifest.getElementsByTagName("uses-permission").elements()
            .associateBy { it.getAttribute("android:name") }
        for (name in listOf(
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.RECEIVE_BOOT_COMPLETED",
            "android.permission.SCHEDULE_EXACT_ALARM",
            "android.permission.USE_EXACT_ALARM",
        )) {
            assertTrue("$name is not declared (REM-016).", name in permissions)
        }
        assertEquals("32", permissions.getValue("android.permission.SCHEDULE_EXACT_ALARM").getAttribute("android:maxSdkVersion"))
    }

    // REM-016, REM-008: the receivers, their export flags and the reschedule broadcasts.
    @Test
    fun `the reminder receivers are declared`() {
        val receivers = application.getElementsByTagName("receiver").elements()
            .associateBy { it.getAttribute("android:name") }
        assertEquals("false", receivers.getValue(".reminders.AlarmReceiver").getAttribute("android:exported"))
        assertEquals("false", receivers.getValue(".reminders.NotificationActionReceiver").getAttribute("android:exported"))
        val reschedule = receivers.getValue(".reminders.RescheduleReceiver")
        assertEquals("true", reschedule.getAttribute("android:exported"))
        val actions = reschedule.getElementsByTagName("action").elements().map { it.getAttribute("android:name") }.toSet()
        assertEquals(
            setOf(
                "android.intent.action.BOOT_COMPLETED",
                "android.intent.action.TIME_SET",
                "android.intent.action.TIMEZONE_CHANGED",
                "android.intent.action.MY_PACKAGE_REPLACED",
                "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
            ),
            actions,
        )
    }

    // REM-016: every class the manifest names exists in the app's sources.
    @Test
    fun `every component the manifest names has a source file`() {
        val names = listOf(application.getAttribute("android:name")) +
            listOf("activity", "receiver").flatMap { tag ->
                application.getElementsByTagName(tag).elements().map { it.getAttribute("android:name") }
            }
        val sources = AppModuleFiles.file("src/main/java/com/derekwinters/water")
        val allSource = sources.walkTopDown().filter { it.extension == "kt" }.joinToString("\n") { it.readText() }
        for (name in names) {
            val simple = name.substringAfterLast('.')
            val pkg = "com.derekwinters.water" + name.substringBeforeLast('.', "").let { if (it.isEmpty()) "" else it }
            assertTrue("$name: no `class $simple` in the app sources.", Regex("""class\s+$simple\b""").containsMatchIn(allSource))
            val file = sources.walkTopDown().first { f -> f.extension == "kt" && Regex("""class\s+$simple\b""").containsMatchIn(f.readText()) }
            assertTrue("$name: ${file.name} is not in package $pkg.", file.readText().contains("package $pkg\n"))
        }
    }

    // BRAND-052: the adaptive icons.
    @Test
    fun `the application icon is the adaptive icon`() {
        assertEquals("@mipmap/ic_launcher", application.getAttribute("android:icon"))
        assertEquals("@mipmap/ic_launcher_round", application.getAttribute("android:roundIcon"))
        for (name in listOf("ic_launcher", "ic_launcher_round")) {
            val icon = AppModuleFiles.parse(AppModuleFiles.file("src/main/res/mipmap-anydpi-v26/$name.xml"))
            val adaptive = icon.documentElement
            assertEquals("adaptive-icon", adaptive.tagName)
            assertEquals(1, adaptive.getElementsByTagName("monochrome").length)
        }
    }

    private fun item(style: Element, name: String): String? =
        style.getElementsByTagName("item").elements().firstOrNull { it.getAttribute("name") == name }?.textContent?.trim()
}
