package com.doodoobops.mahms.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHashTest {

    @Test fun knownVector_matchesSha256() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            hashPassword("abc"),
        )
    }

    @Test fun hash_isDeterministicAndHex64() {
        val h = hashPassword("Password1")
        assertEquals(h, hashPassword("Password1"))
        assertEquals(64, h.length)
        assertTrue(h.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test fun hash_neverContainsThePlainPassword() {
        assertTrue(!hashPassword("Password1").contains("Password1"))
    }

    @Test fun differentPasswords_giveDifferentHashes() {
        assertNotEquals(hashPassword("Password1"), hashPassword("Password2"))
    }
}
