package com.doodoobops.mahms.utils

import java.util.Locale

/** Location text helpers for the Emergency SOS feature. Pure Kotlin, so unit-testable. */
object GeoUtils {

    private val coordRegex = Regex("(-?\\d{1,3}\\.\\d+)\\s*,\\s*(-?\\d{1,3}\\.\\d+)")
    private val displayNameRegex = Regex("\"display_name\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
    private val unicodeEscape = Regex("\\\\u([0-9a-fA-F]{4})")

    /** "Street, Suburb, City (-20.150000, 28.580000)" or "GPS -20.150000, 28.580000". */
    fun formatLocation(latitude: Double, longitude: Double, address: String?): String {
        val coords = String.format(Locale.US, "%.6f, %.6f", latitude, longitude)
        return if (address.isNullOrBlank()) "GPS $coords" else "$address ($coords)"
    }

    /** Pulls "lat, lon" back out of a stored location string, or null when there is none. */
    fun parseCoordinates(location: String): Pair<Double, Double>? {
        val m = coordRegex.find(location) ?: return null
        val lat = m.groupValues[1].toDoubleOrNull() ?: return null
        val lon = m.groupValues[2].toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return lat to lon
    }

    /** geo: URI that opens any installed maps app at the point. */
    fun mapsUri(latitude: Double, longitude: Double, label: String = "SOS"): String =
        String.format(Locale.US, "geo:%.6f,%.6f?q=%.6f,%.6f(%s)", latitude, longitude, latitude, longitude, label)

    /** Reads "display_name" out of a Nominatim reverse-geocoding JSON reply. */
    fun parseDisplayName(json: String): String? {
        val raw = displayNameRegex.find(json)?.groupValues?.get(1) ?: return null
        val text = raw
            .replace(unicodeEscape) { it.groupValues[1].toInt(16).toChar().toString() }
            .replace("\\\"", "\"")
            .replace("\\/", "/")
            .replace("\\\\", "\\")
            .trim()
        return text.ifBlank { null }
    }
}
