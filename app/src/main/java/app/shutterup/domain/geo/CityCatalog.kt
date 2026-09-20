package app.shutterup.domain.geo

/**
 * One row from the bundled city list. Coordinates are WGS84, latitude
 * positive north, longitude positive east. Used only as a coarse stand-in
 * for daylight and hemisphere — never sent anywhere.
 */
data class CoarseCity(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

/**
 * Bundled city list (`cities.json` on the classpath). Sorted by display name.
 */
object CityCatalog {
    val cities: List<CoarseCity> by lazy { parse(loadBundled()) }

    fun find(id: String?): CoarseCity? {
        if (id.isNullOrBlank()) return null
        return cities.firstOrNull { it.id == id }
    }

    fun parse(json: String): List<CoarseCity> {
        val objects = CITY_OBJECT.findAll(json).toList()
        require(objects.isNotEmpty()) { "cities.json contained no cities" }
        return objects.map { match ->
            val body = match.value
            CoarseCity(
                id = stringField(body, "id"),
                name = stringField(body, "name"),
                latitude = numberField(body, "latitude"),
                longitude = numberField(body, "longitude"),
            )
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    private fun loadBundled(): String {
        val stream = CityCatalog::class.java.getResourceAsStream("/cities.json")
            ?: error("bundled cities.json is missing")
        return stream.bufferedReader().use { it.readText() }
    }

    private fun stringField(body: String, key: String): String {
        val match = Regex(""""$key"\s*:\s*"((?:\\.|[^"\\])*)"""").find(body)
            ?: error("cities.json object missing string $key")
        return match.groupValues[1].replace("\\\"", "\"")
    }

    private fun numberField(body: String, key: String): Double {
        val match = Regex(""""$key"\s*:\s*(-?\d+(?:\.\d+)?)""").find(body)
            ?: error("cities.json object missing number $key")
        return match.groupValues[1].toDouble()
    }
}

private val CITY_OBJECT = Regex("""\{[^{}]+\}""")
