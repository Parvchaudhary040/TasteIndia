package com.parv.tasteindia.data.repository

import com.parv.tasteindia.data.remote.dto.MealDetailResponseDto
import com.parv.tasteindia.domain.model.Ingredient
import com.parv.tasteindia.testutil.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test requirement #1: ingredient/measure normalization.
 */
class IngredientNormalizationTest {

    @Test
    fun `drops null empty and whitespace-only ingredient slots`() {
        val pairs = listOf(
            "Chicken" to "800g",
            null to "1 tbsp",        // no name -> noise
            "" to "2",               // no name -> noise
            "   " to "pinch",        // blank name -> noise
            " Onion " to " 2 large ", // trimmed on both sides
        )

        val result = normalizeIngredients(pairs)

        assertEquals(
            listOf(
                Ingredient("Chicken", "800g"),
                Ingredient("Onion", "2 large"),
            ),
            result,
        )
    }

    @Test
    fun `keeps ingredient with a blank or missing measure as empty string`() {
        val result = normalizeIngredients(
            listOf(
                "Garlic" to "   ",
                "Ginger" to null,
            )
        )

        assertEquals(
            listOf(Ingredient("Garlic", ""), Ingredient("Ginger", "")),
            result,
        )
    }

    @Test
    fun `normal meal fixture yields exactly its eight populated pairs`() {
        val dto = Fixtures.decode<MealDetailResponseDto>("lookup_normal.json").meals!!.single()

        val detail = dto.toDomain()

        assertEquals(8, detail.ingredients.size)
        assertEquals(Ingredient("Chicken", "800g"), detail.ingredients.first())
        assertEquals(Ingredient("Coriander Leaves", "3 tbsp, chopped"), detail.ingredients.last())
        assertTrue(detail.ingredients.none { it.name.isBlank() })
    }

    @Test
    fun `missing-fields fixture normalizes messy slots and nulls blank fields`() {
        val dto = Fixtures.decode<MealDetailResponseDto>("lookup_missing_fields.json").meals!!.single()

        val detail = dto.toDomain()

        assertEquals(
            listOf("Chicken", "Onion", "Garlic", "Ginger", "Green Chilli"),
            detail.ingredients.map { it.name },
        )
        // slot 4 "  ", slot 5 null, slot 7 non-breaking space -> all become ""
        assertEquals("", detail.ingredients.first { it.name == "Garlic" }.measure)
        assertEquals("", detail.ingredients.first { it.name == "Ginger" }.measure)
        assertEquals("", detail.ingredients.first { it.name == "Green Chilli" }.measure)

        assertNull(detail.category)
        assertNull(detail.area)
        assertNull(detail.youtubeUrl)
        assertNull(detail.sourceUrl) // "" in the fixture
        assertTrue(detail.tags.isEmpty())
    }

    @Test
    fun `a minimal detail (only id and name) maps without crashing to all-null fields`() {
        val dto = Fixtures.decode<MealDetailResponseDto>("lookup_minimal.json").meals!!.single()

        val detail = dto.toDomain()

        assertEquals("Bare Bones Dal", detail.name)
        assertNull(detail.category)
        assertNull(detail.area)
        assertNull(detail.instructions)
        assertNull(detail.thumbnailUrl)
        assertNull(detail.youtubeUrl) // "" in the fixture -> null
        assertTrue(detail.tags.isEmpty())
        assertTrue(detail.ingredients.isEmpty())
    }

    @Test
    fun `splitTags trims, drops blanks and de-duplicates while keeping order`() {
        assertEquals(listOf("Curry", "Chicken", "Spicy"), splitTags("Curry,Chicken,Spicy"))
        assertEquals(listOf("Curry", "Chicken"), splitTags(" Curry , ,Chicken, Curry "))
        assertEquals(emptyList<String>(), splitTags(null))
        assertEquals(emptyList<String>(), splitTags("  , ,"))
    }
}
