package com.doodoobops.mahms.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeoUtilsTest {

    @Test fun format_withoutAddress_showsGpsOnly() {
        assertEquals("GPS -20.150000, 28.580000", GeoUtils.formatLocation(-20.15, 28.58, null))
    }

    @Test fun format_withAddress_putsAddressFirst() {
        assertEquals(
            "Fife Street, Bulawayo (-20.150000, 28.580000)",
            GeoUtils.formatLocation(-20.15, 28.58, "Fife Street, Bulawayo"),
        )
    }

    @Test fun parse_readsCoordinatesBackFromStoredText() {
        val stored = GeoUtils.formatLocation(-20.1523, 28.5811, "Main Street")
        val (lat, lon) = GeoUtils.parseCoordinates(stored)!!
        assertEquals(-20.1523, lat, 0.000001)
        assertEquals(28.5811, lon, 0.000001)
    }

    @Test fun parse_textWithoutCoordinates_isNull() {
        assertNull(GeoUtils.parseCoordinates("Location unavailable (permission off or no GPS signal)"))
    }

    @Test fun parse_outOfRangeCoordinates_isNull() {
        assertNull(GeoUtils.parseCoordinates("GPS 123.000000, 28.580000"))
    }

    @Test fun mapsUri_buildsGeoLink() {
        assertEquals("geo:-20.150000,28.580000?q=-20.150000,28.580000(SOS)", GeoUtils.mapsUri(-20.15, 28.58))
    }

    @Test fun displayName_isReadFromNominatimReply() {
        val json = """{"place_id":1,"display_name":"Fife Street, Bulawayo, Zimbabwe","address":{"city":"Bulawayo"}}"""
        assertEquals("Fife Street, Bulawayo, Zimbabwe", GeoUtils.parseDisplayName(json))
    }

    @Test fun displayName_handlesEscapedCharacters() {
        val json = """{"display_name":"St \"Anne\" Clinic é"}"""
        assertEquals("St \"Anne\" Clinic é", GeoUtils.parseDisplayName(json))
    }

    @Test fun displayName_missingOrEmpty_isNull() {
        assertNull(GeoUtils.parseDisplayName("""{"error":"Unable to geocode"}"""))
        assertNull(GeoUtils.parseDisplayName("""{"display_name":""}"""))
    }
}
