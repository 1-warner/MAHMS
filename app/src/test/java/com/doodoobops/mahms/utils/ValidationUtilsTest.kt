package com.doodoobops.mahms.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidationUtilsTest {

    @Test fun email_valid_isAccepted() {
        assertNull(ValidationUtils.emailError("patient@mpilo.co.zw"))
        assertNull(ValidationUtils.emailError("  first.last+tag@mail.com  "))
    }

    @Test fun email_blank_isRequired() {
        assertEquals("Email is required", ValidationUtils.emailError(""))
        assertEquals("Email is required", ValidationUtils.emailError("   "))
    }

    @Test fun email_invalid_isRejected() {
        listOf("plainaddress", "missing@tld", "@no-user.com", "a@b.c", "two@@signs.com").forEach {
            assertNotNull("'$it' should be rejected", ValidationUtils.emailError(it))
        }
    }

    @Test fun phone_zimbabweanFormats_areAccepted() {
        assertNull(ValidationUtils.phoneError("+263772345678"))
        assertNull(ValidationUtils.phoneError("0772345678"))
        assertNull(ValidationUtils.phoneError("+263 772 345 678"))
    }

    @Test fun phone_invalid_isRejected() {
        assertEquals("Phone number is required", ValidationUtils.phoneError(""))
        listOf("12345", "+26377234567", "0872345678", "abcdefghij").forEach {
            assertNotNull("'$it' should be rejected", ValidationUtils.phoneError(it))
        }
    }

    @Test fun phone_normalize_stripsSpaces() {
        assertEquals("+263772345678", ValidationUtils.normalizePhone(" +263 772 345 678 "))
    }

    @Test fun password_rules_areEnforced() {
        assertEquals("At least 8 characters", ValidationUtils.passwordError("Ab1"))
        assertEquals("Add an uppercase letter", ValidationUtils.passwordError("password1"))
        assertEquals("Add a number", ValidationUtils.passwordError("Password"))
        assertNull(ValidationUtils.passwordError("Password1"))
    }

    @Test fun confirm_mustMatch() {
        assertNull(ValidationUtils.confirmError("Password1", "Password1"))
        assertEquals("Passwords do not match", ValidationUtils.confirmError("Password1", "Password2"))
    }

    @Test fun required_blankFieldNamesTheField() {
        assertEquals("Address is required", ValidationUtils.requiredError(" ", "Address"))
        assertNull(ValidationUtils.requiredError("14 Fife St", "Address"))
    }
}
