package com.fleetflow.algorithm.string;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RabinKarpTest {

    @Test
    void findsPackageCode() {
        String text = "PKG-ABC-1001 PKG-XYZ-2002 PKG-ABC-1001";
        assertEquals(List.of(0, 26), RabinKarp.search(text, "PKG-ABC-1001", 1_000_000_007).matches());
    }

    @Test
    void overlappingMatches() {
        assertEquals(List.of(0, 1, 2), RabinKarp.search("aaaa", "aa", 101).matches());
    }

    @Test
    void smallModulusProducesSpuriousHitsButCorrectMatches() {
        String text = "abcdefghijklmnopqrstuvwxyz".repeat(4);
        RabinKarp.Result tiny = RabinKarp.search(text, "xyz", 2);
        RabinKarp.Result large = RabinKarp.search(text, "xyz", 1_000_000_007);
        assertEquals(large.matches(), tiny.matches());
        assertTrue(tiny.spuriousHits() > 0);
        assertEquals(0, large.spuriousHits());
    }

    @Test
    void noMatchAndEdgeCases() {
        assertTrue(RabinKarp.search("abcdef", "zzz", 101).matches().isEmpty());
        assertTrue(RabinKarp.search("abc", "", 101).matches().isEmpty());
        assertTrue(RabinKarp.search("ab", "abc", 101).matches().isEmpty());
    }

    @Test
    void windowCountIsNMinusMPlusOne() {
        RabinKarp.Result r = RabinKarp.search("abcdefgh", "cde", 101);
        assertEquals(6, r.hashComparisons());
        assertEquals(6, r.steps().size());
    }

    @Test
    void rejectsInvalidModulus() {
        assertThrows(IllegalArgumentException.class, () -> RabinKarp.search("abc", "a", 1));
    }
}
