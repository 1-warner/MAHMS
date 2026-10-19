package com.doodoobops.mahms.utils

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DateUtilsTest {

    @Before fun setUp() = Locale.setDefault(Locale.US)

    @Test fun prettyDate_formatsStoredDate() {
        assertEquals("04 Sep 2026", DateUtils.prettyDate("2026-09-04"))
    }

    @Test fun prettyDate_invalidInput_fallsBackToRawValue() {
        assertEquals("not-a-date", DateUtils.prettyDate("not-a-date"))
    }

    @Test fun parseStore_validAndInvalid() {
        assertNotNull(DateUtils.parseStore("2026-09-04"))
        assertNull(DateUtils.parseStore("04/09/2026"))
    }

    @Test fun pastAndFutureChecks() {
        assertTrue(DateUtils.isPastDate("2000-01-01"))
        assertFalse(DateUtils.isFutureDate("2000-01-01"))
        assertTrue(DateUtils.isFutureDate("2999-01-01"))
        assertFalse(DateUtils.isPastDate("2999-01-01"))
        assertFalse(DateUtils.isPastDate("garbage"))
    }

    @Test fun today_isNeitherPastNorFuture_boundary() {
        val today = DateUtils.todayStore()
        assertFalse(DateUtils.isPastDate(today))
        assertTrue(DateUtils.isFutureDate(today))
    }

    @Test fun lastNDates_returnsOldestFirstEndingToday() {
        val dates = DateUtils.lastNDates(7)
        assertEquals(7, dates.size)
        assertEquals(DateUtils.todayStore(), dates.last())
        assertEquals(dates.sorted(), dates)
    }

    @Test fun currency_formatsWithThousandsSeparator() {
        assertEquals("USD 1,234.50", DateUtils.currency(1234.5))
    }
}
