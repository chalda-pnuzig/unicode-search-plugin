package com.plugin.unicode

data class UnicodeChar(
    val char: String,
    val codePoint: Int,
    val hex: String,
    val name: String,
    val block: String,
    val category: String
)

object UnicodeDataProvider {
    // A simplified list of common Unicode blocks for demonstration.
    // In a real plugin, we would load this from a exhaustive resource or use a library.
    private val characters: List<UnicodeChar> by lazy {
        val result = mutableListOf<UnicodeChar>()
        // Iterating through BMP (0..FFFF) and SMP (10000..1FFFF)
        val ranges = listOf(32..0xFFFF, 0x10000..0x1FFFF)
        
        for (range in ranges) {
            for (i in range) {
                if (Character.isValidCodePoint(i) && Character.getType(i).toByte() != Character.UNASSIGNED) {
                    val char = String(Character.toChars(i))
                    val name = Character.getName(i) ?: "UNKNOWN"
                    val block = Character.UnicodeBlock.of(i)?.toString() ?: "UNKNOWN"
                    val category = getCategoryName(Character.getType(i))
                    
                    result.add(UnicodeChar(
                        char = char,
                        codePoint = i,
                        hex = "U+%04X".format(i),
                        name = name,
                        block = block,
                        category = category
                    ))
                }
            }
        }
        result
    }

    data class SearchResult(
        val results: List<UnicodeChar>,
        val hasMore: Boolean
    )

    fun search(query: String, limit: Int = 100, offset: Int = 0): SearchResult {
        val matchingSequence = if (query.isBlank()) {
            characters.asSequence()
        } else {
            val words = query.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
            characters.asSequence().filter { char ->
                words.all { word ->
                    char.name.lowercase().contains(word) ||
                    char.hex.lowercase().contains(word) ||
                    char.block.lowercase().contains(word) ||
                    char.category.lowercase().contains(word) ||
                    char.char.contains(word)
                }
            }
        }

        val results = matchingSequence.drop(offset).take(limit + 1).toList()
        val hasMore = results.size > limit
        
        return SearchResult(
            results = if (hasMore) results.dropLast(1) else results,
            hasMore = hasMore
        )
    }

    private fun getCategoryName(type: Int): String {
        return when (type) {
            Character.UPPERCASE_LETTER.toInt() -> "Upper Letter"
            Character.LOWERCASE_LETTER.toInt() -> "Lower Letter"
            Character.TITLECASE_LETTER.toInt() -> "Title Letter"
            Character.MODIFIER_LETTER.toInt() -> "Modifier Letter"
            Character.OTHER_LETTER.toInt() -> "Other Letter"
            Character.NON_SPACING_MARK.toInt() -> "Non-spacing Mark"
            Character.ENCLOSING_MARK.toInt() -> "Enclosing Mark"
            Character.COMBINING_SPACING_MARK.toInt() -> "Combining Mark"
            Character.DECIMAL_DIGIT_NUMBER.toInt() -> "Decimal Digit"
            Character.LETTER_NUMBER.toInt() -> "Letter Number"
            Character.OTHER_NUMBER.toInt() -> "Other Number"
            Character.SPACE_SEPARATOR.toInt() -> "Space Separator"
            Character.LINE_SEPARATOR.toInt() -> "Line Separator"
            Character.PARAGRAPH_SEPARATOR.toInt() -> "Paragraph Separator"
            Character.CONTROL.toInt() -> "Control"
            Character.FORMAT.toInt() -> "Format"
            Character.SURROGATE.toInt() -> "Surrogate"
            Character.PRIVATE_USE.toInt() -> "Private Use"
            Character.UNASSIGNED.toInt() -> "Unassigned"
            Character.START_PUNCTUATION.toInt() -> "Start Punctuation"
            Character.END_PUNCTUATION.toInt() -> "End Punctuation"
            Character.CONNECTOR_PUNCTUATION.toInt() -> "Connector Punctuation"
            Character.DASH_PUNCTUATION.toInt() -> "Dash Punctuation"
            Character.INITIAL_QUOTE_PUNCTUATION.toInt() -> "Initial Quote"
            Character.FINAL_QUOTE_PUNCTUATION.toInt() -> "Final Quote"
            Character.OTHER_PUNCTUATION.toInt() -> "Other Punctuation"
            Character.MATH_SYMBOL.toInt() -> "Math Symbol"
            Character.CURRENCY_SYMBOL.toInt() -> "Currency Symbol"
            Character.MODIFIER_SYMBOL.toInt() -> "Modifier Symbol"
            Character.OTHER_SYMBOL.toInt() -> "Other Symbol"
            else -> "Other"
        }
    }
}
