package com.fleetflow.algorithm.suffix;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuffixStructuresTest {

    @Test
    void suffixArrayOfBanana() {
        assertArrayEquals(new int[]{5, 3, 1, 0, 4, 2}, SuffixArray.build("banana"));
    }

    @Test
    void saIsMatchesSortBasedConstructionOnRandomStrings() {
        Random rnd = new Random(7);
        for (int trial = 0; trial < 300; trial++) {
            int n = rnd.nextInt(40);
            int alphabet = 1 + rnd.nextInt(4);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++) {
                sb.append((char) ('a' + rnd.nextInt(alphabet)));
            }
            String s = sb.toString();
            assertArrayEquals(SuffixArray.build(s), SaIs.build(s).sa(), "string: " + s);
        }
    }

    @Test
    void saIsHandlesTinyAndUniformInputs() {
        assertEquals(0, SaIs.build("").sa().length);
        assertArrayEquals(new int[]{0}, SaIs.build("x").sa());
        assertArrayEquals(new int[]{3, 2, 1, 0}, SaIs.build("aaaa").sa());
        assertArrayEquals(new int[]{1, 0}, SaIs.build("ba").sa());
    }

    @Test
    void saIsRecordsRecursionLevels() {
        assertTrue(SaIs.build("mississippi").levels().size() >= 1);
        assertEquals("mississippi".length(), SaIs.build("mississippi").levels().get(0).types().length());
    }

    @Test
    void searchFindsRankRange() {
        String s = "banana";
        int[] sa = SuffixArray.build(s);
        SuffixArray.SearchResult r = SuffixArray.search(s, sa, "ana");
        assertEquals(2, r.count());
        assertEquals(List.of(1, 3), List.of(sa[r.first()], sa[r.last()]).stream().sorted().toList());
        assertEquals(0, SuffixArray.search(s, sa, "xyz").count());
        assertEquals(3, SuffixArray.search(s, sa, "a").count());
        assertEquals(1, SuffixArray.search(s, sa, "banana").count());
        assertEquals(0, SuffixArray.search(s, sa, "bananas").count());
    }

    @Test
    void kasaiLcpOfBanana() {
        int[] sa = SuffixArray.build("banana");
        assertArrayEquals(new int[]{0, 1, 3, 0, 0, 2}, Lcp.kasai("banana", sa).lcp());
    }

    @Test
    void repeatedSubstrings() {
        String s = "abcXabcYabc";
        int[] sa = SuffixArray.build(s);
        List<Lcp.Repeat> repeats = Lcp.repeats(s, sa, Lcp.kasai(s, sa).lcp(), 3);
        assertEquals("abc", repeats.get(0).text());
        assertEquals(List.of(0, 4, 8), repeats.get(0).positions());
    }
}
