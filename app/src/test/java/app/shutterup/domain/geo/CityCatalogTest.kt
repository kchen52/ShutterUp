package app.shutterup.domain.geo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CityCatalogTest {

    @Test
    fun bundledListCoversEveryPopulatedContinent() {
        val names = CityCatalog.cities.map { it.name }.toSet()
        // Africa
        assertTrue(names.containsAll(listOf("Cairo", "Lagos", "Nairobi", "Cape Town", "Casablanca")))
        // Asia
        assertTrue(names.containsAll(listOf("Tokyo", "Singapore", "Mumbai", "Bangkok", "Dubai", "Seoul", "Beijing", "Istanbul")))
        // Europe
        assertTrue(names.containsAll(listOf("London", "Paris", "Berlin", "Madrid", "Rome", "Stockholm", "Tromsø")))
        // North America
        assertTrue(names.containsAll(listOf("New York", "Los Angeles", "Chicago", "Mexico City", "Toronto", "Anchorage")))
        // South America
        assertTrue(names.containsAll(listOf("São Paulo", "Buenos Aires", "Lima", "Santiago", "Quito")))
        // Oceania
        assertTrue(names.containsAll(listOf("Sydney", "Melbourne", "Auckland")))
    }

    @Test
    fun includesEquatorAndHighLatitudeAnchors() {
        assertNotNull(CityCatalog.find("quito"))
        assertTrue(CityCatalog.find("quito")!!.latitude > -1.0)
        assertTrue(CityCatalog.find("quito")!!.latitude < 0.0)
        assertNotNull(CityCatalog.find("tromso"))
        assertTrue(CityCatalog.find("tromso")!!.latitude > 66.0)
        assertNotNull(CityCatalog.find("anchorage"))
        assertTrue(CityCatalog.find("anchorage")!!.latitude > 60.0)
    }

    @Test
    fun bothHemispheresAreRepresented() {
        val north = CityCatalog.cities.count { it.latitude > 0 }
        val south = CityCatalog.cities.count { it.latitude < 0 }
        assertTrue("expected several northern cities, got $north", north >= 10)
        assertTrue("expected several southern cities, got $south", south >= 8)
    }

    @Test
    fun sortedByDisplayNameAndIdsAreStable() {
        val names = CityCatalog.cities.map { it.name }
        assertEquals(names.sortedWith { a, b -> String.CASE_INSENSITIVE_ORDER.compare(a, b) }, names)
        assertEquals("sydney", CityCatalog.find("sydney")?.id)
        assertNull(CityCatalog.find(null))
        assertNull(CityCatalog.find("not-a-city"))
    }

    @Test
    fun parseReadsDiacritics() {
        val parsed = CityCatalog.parse(
            """[{"id":"tromso","name":"Tromsø","latitude":69.6496,"longitude":18.9553}]""",
        )
        assertEquals("Tromsø", parsed.single().name)
        assertEquals(69.6496, parsed.single().latitude, 0.0001)
    }
}
