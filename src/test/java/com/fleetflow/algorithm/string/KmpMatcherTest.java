package com.fleetflow.algorithm.string;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KmpMatcherTest {

    @Test
    void lpsOfRepeatedPattern() {
        assertArrayEquals(new int[]{0, 1, 2, 3}, KmpMatcher.computeLps("aaaa"));
        assertArrayEquals(new int[]{0, 0, 0}, KmpMatcher.computeLps("abc"));
    }

    @Test
    void lpsOfClassicPatterns() {
        assertArrayEquals(new int[]{0, 0, 0, 1, 2, 3, 4, 5}, KmpMatcher.computeLps("abcabcab"));
        assertArrayEquals(new int[]{0, 1, 0, 1, 2, 2, 3}, KmpMatcher.computeLps("aabaaab"));
    }

    @Test
    void findsAllOccurrences() {
        KmpMatcher.Result r = KmpMatcher.search("SHP-2041 SHP-2042 SHP-2041", "SHP-2041");
        assertEquals(List.of(0, 18), r.matches());
    }

    @Test
    void findsOverlappingOccurrences() {
        assertEquals(List.of(0, 1, 2, 3), KmpMatcher.search("aaaaa", "aa").matches());
        assertEquals(List.of(0, 2), KmpMatcher.search("ababa", "aba").matches());
    }

    @Test
    void noMatch() {
        assertTrue(KmpMatcher.search("abcdef", "xyz").matches().isEmpty());
    }

    @Test
    void emptyPatternAndPatternLongerThanText() {
        assertTrue(KmpMatcher.search("abc", "").matches().isEmpty());
        assertTrue(KmpMatcher.search("ab", "abc").matches().isEmpty());
        assertEquals(0, KmpMatcher.search("", "a").comparisons());
    }

    @Test
    void comparisonsStayLinear() {
        String text = "a".repeat(1000);
        KmpMatcher.Result r = KmpMatcher.search(text, "a".repeat(50) + "b");
        assertTrue(r.comparisons() <= 2 * text.length());
    }

    @Test
    void stepTraceMatchesComparisonCount() {
        KmpMatcher.Result r = KmpMatcher.search("abxabcabcaby", "abcaby");
        assertEquals(r.comparisons(), r.steps().size());
        assertEquals(List.of(6), r.matches());
    }
}
