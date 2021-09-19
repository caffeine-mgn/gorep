package pw.binom.gorep

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VersionTest {

    @Test
    fun compareToTest() {
        assertTrue(Version("1.1") > Version("1.0"))
        assertTrue(Version("1.0.1.0") > Version("1.0.0.0"))
        assertEquals(0, Version("1.0").compareTo(Version("1.0.0")))
    }
}