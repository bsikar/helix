package com.bsikar.helix

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream

internal class EpubMetadataParser {

    fun extractMetadata(inputStream: InputStream?): String {
        if (inputStream == null) return ""
        return parseMetadataFromStream(inputStream)
    }

    private fun parseMetadataFromStream(inputStream: InputStream): String {
        val result = StringBuilder()

        try {
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, null)
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    processMetadataTag(parser, result)
                }
                eventType = parser.next()
            }
        } catch (e: IOException) {
            // If metadata parsing fails, continue without it
        } catch (e: XmlPullParserException) {
            // If metadata parsing fails, continue without it
        }

        return result.toString()
    }

    private fun processMetadataTag(parser: XmlPullParser, result: StringBuilder) {
        when (parser.name) {
            "dc:title" -> extractTagContent(parser, result, "Title: ")
            "dc:creator" -> extractTagContent(parser, result, "Author: ")
        }
    }

    private fun extractTagContent(parser: XmlPullParser, result: StringBuilder, prefix: String) {
        try {
            parser.next()
            if (parser.eventType == XmlPullParser.TEXT) {
                result.append(prefix).append(parser.text).append("\n")
            }
        } catch (e: XmlPullParserException) {
            // Skip if can't extract content
        }
    }
}
