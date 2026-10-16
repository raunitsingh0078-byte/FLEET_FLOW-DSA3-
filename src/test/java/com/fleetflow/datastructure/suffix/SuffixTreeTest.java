package com.fleetflow.datastructure.suffix;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuffixTreeTest {

    @Test
    void findsAllOccurrences() {
        SuffixTree tree = new SuffixTree("banana");
        assertEquals(List.of(1, 3), tree.search("ana").positions());
        assertEquals(List.of(1, 3, 5), tree.search("a").positions());
        assertEquals(List.of(0), tree.search("banana").positions());
    }

    @Test
    void rejectsAbsentPatterns() {
        SuffixTree tree = new SuffixTree("banana");
        assertFalse(tree.search("nab").found());
        assertFalse(tree.search("bananas").found());
        assertFalse(tree.search("z").found());
    }

    @Test
    void hasOneLeafPerSuffix() {
        SuffixTree tree = new SuffixTree("abab");
        assertEquals(5, tree.search("").positions().size()); // 4 suffixes + the lone terminator
        assertTrue(tree.nodeCount() >= 6);
    }

    @Test
    void partialTreeContainsOnlyInsertedSuffixes() {
        SuffixTree tree = new SuffixTree("banana", 1);
        assertTrue(tree.search("banana").found());
        assertFalse(tree.search("nana").found());
    }

    @Test
    void terminatorInTextIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SuffixTree("a$b"));
    }
}
