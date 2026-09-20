package app.shutterup.domain.ai

/**
 * Defensive JSON → [GeneratedPrompt] / [LibraryPrompt] (SPEC §7.1, §7.7).
 * Extra object fields are ignored. Length limits: title ≤ 40, oneLiner ≤ 100, theme ≤ 24.
 */
object PromptParser {

    fun parse(json: String, source: PromptSource): Result<GeneratedPrompt> =
        runAsIllegalArgument {
            val root = JsonReader(json).readDocument()
            val obj = root as? Js.Obj
                ?: throw IllegalArgumentException("top-level value must be an object")
            parseGenerated(obj, source, index = null)
        }

    fun parseLibrary(json: String): Result<List<LibraryPrompt>> =
        runAsIllegalArgument {
            val root = JsonReader(json).readDocument()
            val arr = root as? Js.Arr
                ?: throw IllegalArgumentException("top-level value must be an array")
            arr.elements.mapIndexed { index, element ->
                val obj = element as? Js.Obj
                    ?: throw IllegalArgumentException("index $index: item must be an object")
                parseLibraryItem(obj, index)
            }
        }

    fun parseSeries(json: String, source: PromptSource): Result<GeneratedSeries> =
        runAsIllegalArgument {
            val root = JsonReader(json).readDocument()
            val obj = root as? Js.Obj
                ?: throw IllegalArgumentException("top-level value must be an object")
            val title = requiredString(obj, "title", index = null)
            if (title.length > 40) fail("title exceeds 40 characters", index = null)
            val promptsValue = obj.map["prompts"] ?: fail("missing prompts", index = null)
            val arr = promptsValue as? Js.Arr ?: fail("prompts must be an array", index = null)
            if (arr.elements.size != 7) fail("prompts must contain 7 objects", index = null)
            val prompts = arr.elements.mapIndexed { index, element ->
                val item = element as? Js.Obj
                    ?: fail("item must be an object", index)
                parseGenerated(item, source, index)
            }
            val theme = when (val value = obj.map["theme"]) {
                is Js.Str -> value.value
                else -> prompts.first().theme
            }
            GeneratedSeries(title = title, theme = theme, prompts = prompts)
        }

    fun parseMonthly(json: String): Result<GeneratedMonthlyIssue> =
        runAsIllegalArgument {
            val root = JsonReader(json).readDocument()
            val obj = root as? Js.Obj
                ?: throw IllegalArgumentException("top-level value must be an object")
            val headline = requiredString(obj, "headline", index = null)
            val body = requiredString(obj, "body", index = null)
            if (headline.length > 30) fail("headline exceeds 30 characters", index = null)
            GeneratedMonthlyIssue(headline = headline, body = body)
        }

    private fun parseGenerated(obj: Js.Obj, source: PromptSource, index: Int?): GeneratedPrompt {
        val fields = parseSharedFields(obj, index)
        return GeneratedPrompt(
            title = fields.title,
            oneLiner = fields.oneLiner,
            details = fields.details,
            tips = fields.tips,
            constraint = fields.constraint,
            theme = fields.theme,
            source = source,
        )
    }

    private fun parseLibraryItem(obj: Js.Obj, index: Int): LibraryPrompt {
        val fields = parseSharedFields(obj, index)
        val id = requiredString(obj, "id", index)
        if (id.isBlank()) fail("id is blank", index)
        val tags = stringArray(obj, "tags", index, required = true)
        return LibraryPrompt(
            id = id,
            title = fields.title,
            oneLiner = fields.oneLiner,
            details = fields.details,
            tips = fields.tips,
            constraint = fields.constraint,
            theme = fields.theme,
            tags = tags,
        )
    }

    private data class SharedFields(
        val title: String,
        val oneLiner: String,
        val details: String,
        val tips: List<String>,
        val constraint: String?,
        val theme: String,
    )

    private fun parseSharedFields(obj: Js.Obj, index: Int?): SharedFields {
        val title = requiredString(obj, "title", index)
        val oneLiner = requiredString(obj, "oneLiner", index)
        val details = requiredString(obj, "details", index)
        val theme = requiredString(obj, "theme", index)
        if (title.length > 40) fail("title exceeds 40 characters", index)
        if (oneLiner.length > 100) fail("oneLiner exceeds 100 characters", index)
        if (theme.length > 24) fail("theme exceeds 24 characters", index)
        val tips = stringArray(obj, "tips", index, required = true)
        if (tips.size !in 1..3) fail("tips must contain 1 to 3 strings", index)
        tips.forEachIndexed { tipIndex, tip ->
            if (tip.isBlank()) fail("tips[$tipIndex] is blank", index)
        }
        val constraint = optionalConstraint(obj, index)
        return SharedFields(title, oneLiner, details, tips, constraint, theme)
    }

    private fun requiredString(obj: Js.Obj, key: String, index: Int?): String {
        val value = obj.map[key] ?: fail("missing $key", index)
        return (value as? Js.Str)?.value ?: fail("$key must be a string", index)
    }

    private fun optionalConstraint(obj: Js.Obj, index: Int?): String? {
        return when (val value = obj.map["constraint"]) {
            null, Js.Null -> null
            is Js.Str -> value.value
            else -> fail("constraint must be a string or null", index)
        }
    }

    private fun stringArray(obj: Js.Obj, key: String, index: Int?, required: Boolean): List<String> {
        val value = obj.map[key]
        if (value == null) {
            if (required) fail("missing $key", index)
            return emptyList()
        }
        val arr = value as? Js.Arr ?: fail("$key must be an array", index)
        return arr.elements.mapIndexed { i, element ->
            (element as? Js.Str)?.value ?: fail("$key[$i] must be a string", index)
        }
    }

    private fun fail(message: String, index: Int?): Nothing {
        if (index != null) throw IllegalArgumentException("index $index: $message")
        throw IllegalArgumentException(message)
    }

    private inline fun <T> runAsIllegalArgument(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException(e.message ?: "invalid JSON", e))
        }
}

private sealed interface Js {
    data class Obj(val map: Map<String, Js>) : Js
    data class Arr(val elements: List<Js>) : Js
    data class Str(val value: String) : Js
    data class Num(val raw: String) : Js
    data class Bool(val value: Boolean) : Js
    data object Null : Js
}

/** Recursive-descent JSON (objects, arrays, strings with escapes including \\u, numbers, true/false/null). */
private class JsonReader(private val s: String) {
    private var i = 0

    fun readDocument(): Js {
        skipWs()
        if (i >= s.length) throw IllegalArgumentException("empty JSON")
        val value = readValue()
        skipWs()
        if (i < s.length) throw IllegalArgumentException("trailing JSON content")
        return value
    }

    private fun readValue(): Js {
        skipWs()
        if (i >= s.length) throw IllegalArgumentException("unexpected end of JSON")
        return when (val c = s[i]) {
            '{' -> readObject()
            '[' -> readArray()
            '"' -> Js.Str(readString())
            't' -> readLiteral("true", Js.Bool(true))
            'f' -> readLiteral("false", Js.Bool(false))
            'n' -> readLiteral("null", Js.Null)
            '-' -> readNumber()
            in '0'..'9' -> readNumber()
            else -> throw IllegalArgumentException("unexpected character '$c'")
        }
    }

    private fun readObject(): Js.Obj {
        expect('{')
        skipWs()
        if (peek('}')) {
            i++
            return Js.Obj(emptyMap())
        }
        val map = linkedMapOf<String, Js>()
        while (true) {
            skipWs()
            if (i >= s.length || s[i] != '"') throw IllegalArgumentException("object key must be a string")
            val key = readString()
            skipWs()
            expect(':')
            map[key] = readValue()
            skipWs()
            when {
                peek(',') -> {
                    i++
                    skipWs()
                    if (peek('}')) throw IllegalArgumentException("trailing comma in object")
                }
                peek('}') -> {
                    i++
                    break
                }
                else -> throw IllegalArgumentException("expected comma or end of object")
            }
        }
        return Js.Obj(map)
    }

    private fun readArray(): Js.Arr {
        expect('[')
        skipWs()
        if (peek(']')) {
            i++
            return Js.Arr(emptyList())
        }
        val elements = mutableListOf<Js>()
        while (true) {
            elements.add(readValue())
            skipWs()
            when {
                peek(',') -> {
                    i++
                    skipWs()
                    if (peek(']')) throw IllegalArgumentException("trailing comma in array")
                }
                peek(']') -> {
                    i++
                    break
                }
                else -> throw IllegalArgumentException("expected comma or end of array")
            }
        }
        return Js.Arr(elements)
    }

    private fun readString(): String {
        expect('"')
        val out = StringBuilder()
        while (i < s.length) {
            val c = s[i++]
            when (c) {
                '"' -> return out.toString()
                '\\' -> out.append(readEscape())
                else -> {
                    if (c.code < 0x20) throw IllegalArgumentException("unescaped control character in string")
                    out.append(c)
                }
            }
        }
        throw IllegalArgumentException("unterminated string")
    }

    private fun readEscape(): Char {
        if (i >= s.length) throw IllegalArgumentException("unterminated string escape")
        return when (val c = s[i++]) {
            '"', '\\', '/' -> c
            'b' -> '\b'
            'f' -> '\u000C'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'u' -> readHexChar()
            else -> throw IllegalArgumentException("invalid string escape")
        }
    }

    private fun readHexChar(): Char {
        if (i + 4 > s.length) throw IllegalArgumentException("unterminated unicode escape")
        val hex = s.substring(i, i + 4)
        i += 4
        val code = hex.toIntOrNull(16) ?: throw IllegalArgumentException("invalid unicode escape")
        return code.toChar()
    }

    private fun readNumber(): Js.Num {
        val start = i
        if (peek('-')) i++
        if (i >= s.length || s[i] !in '0'..'9') throw IllegalArgumentException("invalid number")
        if (s[i] == '0') {
            i++
            if (i < s.length && s[i] in '0'..'9') throw IllegalArgumentException("invalid number")
        } else {
            while (i < s.length && s[i] in '0'..'9') i++
        }
        if (peek('.')) {
            i++
            if (i >= s.length || s[i] !in '0'..'9') throw IllegalArgumentException("invalid number")
            while (i < s.length && s[i] in '0'..'9') i++
        }
        if (i < s.length && (s[i] == 'e' || s[i] == 'E')) {
            i++
            if (i < s.length && (s[i] == '+' || s[i] == '-')) i++
            if (i >= s.length || s[i] !in '0'..'9') throw IllegalArgumentException("invalid number")
            while (i < s.length && s[i] in '0'..'9') i++
        }
        return Js.Num(s.substring(start, i))
    }

    private fun readLiteral(literal: String, value: Js): Js {
        if (i + literal.length > s.length || s.substring(i, i + literal.length) != literal) {
            throw IllegalArgumentException("invalid literal")
        }
        i += literal.length
        return value
    }

    private fun skipWs() {
        while (i < s.length && s[i].isWhitespace()) i++
    }

    private fun peek(c: Char): Boolean = i < s.length && s[i] == c

    private fun expect(c: Char) {
        if (!peek(c)) throw IllegalArgumentException("expected '$c'")
        i++
    }
}
