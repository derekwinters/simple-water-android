package com.derekwinters.water

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList

/** Locates `:app`'s own files from a JVM unit test, whatever directory Gradle runs it in. */
internal object AppModuleFiles {
    fun directory(): File {
        var directory: File? = File(System.getProperty("user.dir")!!).absoluteFile
        while (directory != null) {
            if (File(directory, "src/main/AndroidManifest.xml").isFile) return directory
            val fromRoot = File(directory, "app")
            if (File(fromRoot, "src/main/AndroidManifest.xml").isFile) return fromRoot
            directory = directory.parentFile
        }
        throw IllegalStateException("Could not find the :app module from ${System.getProperty("user.dir")}.")
    }

    fun file(relative: String): File = File(directory(), relative)

    /** Not namespace-aware, so attributes are read by their prefixed names (`android:name`). */
    fun parse(file: File): Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
}

internal fun NodeList.elements(): List<Element> = (0 until length).map { item(it) as Element }
