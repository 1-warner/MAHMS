package com.doodoobops.mahms.domain

import com.doodoobops.mahms.domain.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoleTest {

    @Test fun sixRolesExist() = assertEquals(6, Role.all.size)

    @Test fun fromLabel_resolvesEveryRole() {
        Role.entries.forEach { assertEquals(it, Role.fromLabel(it.label)) }
    }

    @Test fun fromLabel_unknownOrNull_isNull() {
        assertNull(Role.fromLabel("Janitor"))
        assertNull(Role.fromLabel(null))
        assertNull(Role.fromLabel("patient")) // labels are case-sensitive
    }
}
