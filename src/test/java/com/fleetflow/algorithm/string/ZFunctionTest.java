package com.fleetflow.algorithm.string;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZFunctionTest {

    @Test
    void zArrayOfKnownString() {
        assertArrayEquals(new int[]{7, 0, 1, 0, 3, 0, 1}, ZFunction.compute("abacaba"));
        assertArrayEquals(new int[]{5, 4, 3, 2, 1}, ZFunction.compute("aaaaa"));
    }

    @Test
    void emptyAndSingleCharacter() {
        assertEquals(0, ZFunction.compute("").length);
        assertArrayEquals(new int[]{1}, ZFunction.compute("x"));
    }

    @Test
    void searchFindsOverlappingMatches() {
        assertEquals(List.of(0, 2), ZFunction.search("ababa", "aba"));
        assertTrue(ZFunction.search("abc", "").isEmpty());
        assertTrue(ZFunction.search("ab", "abc").isEmpty());
    }

    @Test
    void detectsExactRepetition() {
        ZFunction.Repetition r = ZFunction.repetition(ZFunction.compute("SHP01SHP01SHP01"));
        assertEquals(5, r.period());
        assertTrue(r.exactRepeat());
        assertEquals(3, r.repeats());
    }

    @Test
    void detectsPartialAndNoRepetition() {
        ZFunction.Repetition partial = ZFunction.repetition(ZFunction.compute("abcabca"));
        assertEquals(3, partial.period());
        assertFalse(partial.exactRepeat());
        ZFunction.Repetition none = ZFunction.repetition(ZFunction.compute("abcd"));
        assertEquals(4, none.period());
    }

    @Test
    void stepTraceCoversEveryIndex() {
        ZFunction.Result r = ZFunction.analyze("abacaba");
        assertEquals(6, r.steps().size());
    }
}
