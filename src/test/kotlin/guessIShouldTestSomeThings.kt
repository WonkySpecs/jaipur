import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Tests {
    @Test
    internal fun testGoodsCombinations() {
        val expected = listOf(
            listOf(Card.GOLD),
            listOf(Card.SILVER),
            listOf(Card.LEATHER),
            listOf(Card.GOLD, Card.GOLD),
            listOf(Card.GOLD, Card.SILVER),
            listOf(Card.GOLD, Card.LEATHER),
            listOf(Card.SILVER, Card.LEATHER),
            listOf(Card.GOLD, Card.GOLD, Card.SILVER),
            listOf(Card.GOLD, Card.GOLD, Card.LEATHER),
            listOf(Card.GOLD, Card.SILVER, Card.LEATHER),
            listOf(Card.GOLD, Card.GOLD, Card.SILVER, Card.LEATHER),
            emptyList()
        )

        val actual = goodsCombinations(listOf(Card.GOLD, Card.GOLD, Card.SILVER, Card.CAMEL, Card.LEATHER))

        // containsInAnyOrder doesn't seem to be handling the empty list properly?
        assertEquals(expected.size, actual.size)
        for (l in expected) {
            assertTrue(actual.contains(l))
        }
    }

    @Test
    internal fun testValidSwaps_simpleCase() {
        val expected = listOf(
            listOf(Card.RUBY, Card.GOLD) to listOf(Card.SILVER, Card.SPICE),
            listOf(Card.RUBY, Card.GOLD) to listOf(Card.SILVER, Card.SILK),
            listOf(Card.RUBY, Card.GOLD) to listOf(Card.SILVER, Card.LEATHER),
            listOf(Card.RUBY, Card.GOLD) to listOf(Card.SPICE, Card.SILK),
            listOf(Card.RUBY, Card.GOLD) to listOf(Card.SPICE, Card.LEATHER),
            listOf(Card.RUBY, Card.GOLD) to listOf(Card.SILK, Card.LEATHER),
        )

        val hand = listOf(Card.RUBY, Card.GOLD)
        val market = listOf(Card.SILVER, Card.SPICE, Card.SILK, Card.LEATHER, Card.CAMEL)
        val herd = 0

        val actual = validSwaps(hand, herd, market).toList()
        assertEquals(expected.size, actual.size)
        for (p in expected) {
            assertTrue(actual.contains(p))
        }
    }

    @Test
    internal fun testValidSwaps_handFull_cantPlaceCamels() {
        val expected = (2..5).map {
            List(it) { Card.LEATHER} to List(it) { Card.SPICE }
        }

        val hand = List(7) { Card.LEATHER }
        val market = List(5) { Card.SPICE }
        val herd = 420

        val actual = validSwaps(hand, herd, market).toList()
        assertEquals(expected.size, actual.size)
        for (p in expected) {
            assertTrue(actual.contains(p))
        }
    }
}
