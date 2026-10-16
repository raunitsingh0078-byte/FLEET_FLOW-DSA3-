package com.fleetflow.datastructure.trie;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AhoCorasickAutomatonTest {

    private static Set<String> asSet(AhoCorasickAutomaton.Result r) {
        return r.matches().stream().map(m -> m.pattern() + "@" + m.start()).collect(Collectors.toSet());
    }

    @Test
    void classicHersExample() {
        AhoCorasickAutomaton ac = new AhoCorasickAutomaton(List.of("he", "she", "his", "hers"));
        assertEquals(Set.of("she@1", "he@2", "hers@2"), asSet(ac.search("ushers")));
    }

    @Test
    void reportsPatternsNestedInsideEachOther() {
        AhoCorasickAutomaton ac = new AhoCorasickAutomaton(List.of("a", "aa", "aaa"));
        assertEquals(6, ac.search("aaa").matches().size());
    }

    @Test
    void noMatches() {
        AhoCorasickAutomaton ac = new AhoCorasickAutomaton(List.of("delay", "hub"));
        assertTrue(ac.search("delivered on time").matches().isEmpty());
        assertTrue(ac.search("").matches().isEmpty());
    }

    @Test
    void failureLinkOfClassicExample() {
        AhoCorasickAutomaton ac = new AhoCorasickAutomaton(List.of("he", "she", "his", "hers"));
        AhoCorasickAutomaton.Node she = ac.nodeFor("she");
        assertEquals(ac.nodeFor("he"), she.fail());
        assertTrue(she.output().contains("he"));
    }

    @Test
    void rejectsEmptyPattern() {
        assertThrows(IllegalArgumentException.class, () -> new AhoCorasickAutomaton(List.of("ok", "")));
    }

    @Test
    void stepTraceHasOneEntryPerCharacter() {
        AhoCorasickAutomaton ac = new AhoCorasickAutomaton(List.of("ab"));
        assertEquals(5, ac.search("xabab").steps().size());
    }
}
