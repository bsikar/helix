package com.bsikar.helix

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

internal class EpubSpineParser {

    fun parseSpine(inputStream: InputStream?): List<String> {
        if (inputStream == null) return emptyList()
        return parseSpineFromStream(inputStream)
    }

    private fun parseSpineFromStream(inputStream: InputStream): List<String> {
        val spine = mutableListOf<String>()
        val manifest = mutableMapOf<String, String>()

        val parser = Xml.newPullParser()
        parser.setInput(inputStream, null)

        parseManifestAndSpine(parser, manifest, spine)

        return spine
    }

    private fun parseManifestAndSpine(
        parser: XmlPullParser,
        manifest: MutableMap<String, String>,
        spine: MutableList<String>
    ) {
        var eventType = parser.eventType
        var inManifest = false
        var inSpine = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    processStartTag(parser, manifest, spine, inManifest, inSpine)
                    when (parser.name) {
                        "manifest" -> inManifest = true
                        "spine" -> inSpine = true
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "manifest" -> inManifest = false
                        "spine" -> inSpine = false
                    }
                }
            }
            eventType = parser.next()
        }
    }

    private fun processStartTag(
        parser: XmlPullParser,
        manifest: MutableMap<String, String>,
        spine: MutableList<String>,
        inManifest: Boolean,
        inSpine: Boolean
    ) {
        when (parser.name) {
            "item" -> if (inManifest) processManifestItem(parser, manifest)
            "itemref" -> if (inSpine) processSpineItem(parser, manifest, spine)
        }
    }

    private fun processManifestItem(parser: XmlPullParser, manifest: MutableMap<String, String>) {
        val id = parser.getAttributeValue(null, "id")
        val href = parser.getAttributeValue(null, "href")
        if (id != null && href != null) {
            manifest[id] = href
        }
    }

    private fun processSpineItem(
        parser: XmlPullParser,
        manifest: Map<String, String>,
        spine: MutableList<String>
    ) {
        val idref = parser.getAttributeValue(null, "idref")
        if (idref != null) {
            manifest[idref]?.let { href ->
                spine.add(href)
            }
        }
    }
}
