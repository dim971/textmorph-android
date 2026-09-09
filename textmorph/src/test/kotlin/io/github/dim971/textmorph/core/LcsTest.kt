package io.github.dim971.textmorph.core

import org.junit.Assert.assertEquals
import org.junit.Test

class LcsTest {
    @Test
    fun `pairs the elements that survive, in order`() {
        val (a, b) =
            lcsIndices(
                listOf("the", "quick", "brown", "fox"),
                listOf("the", "brown", "fox"),
            )
        assertEquals(listOf(0, 2, 3), a)
        assertEquals(listOf(0, 1, 2), b)
    }

    @Test
    fun `an empty sequence pairs with nothing`() {
        assertEquals(emptyList<Int>() to emptyList<Int>(), lcsIndices(emptyList<String>(), listOf("a")))
        assertEquals(emptyList<Int>() to emptyList<Int>(), lcsIndices(listOf("a"), emptyList<String>()))
        assertEquals(
            emptyList<Int>() to emptyList<Int>(),
            lcsIndices(emptyList<String>(), emptyList<String>()),
        )
    }

    @Test
    fun `a repeated element pairs with the earliest match, not the latest`() {
        // This is the whole reason the table is filled backwards and walked
        // forwards. Paired with the later "a", the segment would travel the
        // width of the value to reach it.
        assertEquals(
            listOf(0) to listOf(0),
            lcsIndices(listOf("a", "x", "a"), listOf("a", "y")),
        )
        assertEquals(
            listOf(0) to listOf(0),
            lcsIndices(listOf("a", "b"), listOf("a", "z", "a")),
        )
    }

    @Test
    fun `nothing in common pairs nothing`() {
        assertEquals(
            emptyList<Int>() to emptyList<Int>(),
            lcsIndices(listOf("a", "b"), listOf("c", "d")),
        )
    }

    @Test
    fun `identical sequences pair completely`() {
        val words = listOf("one", "two", "three")
        assertEquals(listOf(0, 1, 2) to listOf(0, 1, 2), lcsIndices(words, words))
    }

    @Test
    fun `works per character, which is how a word morphs into another`() {
        val old = "balance".toList()
        val new = "banana".toList()
        val (a, b) = lcsIndices(old, new)
        assertEquals(a.map { old[it] }, b.map { new[it] })
        // b, a, a, n: "bana" is not a subsequence of "balance", because
        // nothing follows its only "n".
        assertEquals("baan", a.map { old[it] }.joinToString(""))
        assertEquals(listOf(0, 1, 3, 4), a)
        assertEquals(listOf(0, 1, 3, 4), b)
    }
}
