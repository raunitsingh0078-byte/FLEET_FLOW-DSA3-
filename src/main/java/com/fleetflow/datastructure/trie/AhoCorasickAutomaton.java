package com.fleetflow.datastructure.trie;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Aho-Corasick automaton: a trie of all patterns augmented with failure links so that every
 * pattern can be found in a single pass over the text in O(n + total pattern length + matches).
 */
public final class AhoCorasickAutomaton {

    /** A trie state. */
    public static final class Node {
        private final int id;
        private final int depth;
        private final char character;
        private final Map<Character, Node> children = new TreeMap<>();
        private Node fail;
        private String patternEnd;
        private final Set<String> output = new LinkedHashSet<>();

        private Node(int id, int depth, char character) {
            this.id = id;
            this.depth = depth;
            this.character = character;
        }

        public int id() {
            return id;
        }

        public int depth() {
            return depth;
        }

        /** @return the edge character leading to this node (meaningless for the root). */
        public char character() {
            return character;
        }

        public Map<Character, Node> children() {
            return children;
        }

        /** @return the failure link: longest proper suffix of this node's string that is also a trie prefix. */
        public Node fail() {
            return fail;
        }

        /** @return the pattern that ends exactly at this node, or null. */
        public String patternEnd() {
            return patternEnd;
        }

        /** @return all patterns that end at this state, including those reached via failure links. */
        public Set<String> output() {
            return output;
        }
    }

    /**
     * One text character processed by the automaton.
     *
     * @param failHops number of failure links followed before a transition was found
     * @param matched  patterns that end at this text position
     */
    public record Step(int textIndex, char character, int fromState, int toState, int failHops,
                       List<String> matched) {
    }

    /** A pattern occurrence. */
    public record Match(int start, int end, String pattern) {
    }

    /** Result of scanning a text. */
    public record Result(List<Match> matches, List<Step> steps, int transitions) {
    }

    private final Node root = new Node(0, 0, '\0');
    private final List<Node> nodes = new ArrayList<>();
    private final List<String> patterns;

    /**
     * Builds the trie and failure links.
     *
     * @param patterns non-empty, non-blank patterns
     */
    public AhoCorasickAutomaton(List<String> patterns) {
        Objects.requireNonNull(patterns, "patterns");
        this.patterns = List.copyOf(patterns);
        nodes.add(root);
        for (String p : this.patterns) {
            if (p.isEmpty()) {
                throw new IllegalArgumentException("patterns must not be empty");
            }
            insert(p);
        }
        buildFailureLinks();
    }

    private void insert(String pattern) {
        Node cur = root;
        for (char c : pattern.toCharArray()) {
            Node next = cur.children.get(c);
            if (next == null) {
                next = new Node(nodes.size(), cur.depth + 1, c);
                nodes.add(next);
                cur.children.put(c, next);
            }
            cur = next;
        }
        cur.patternEnd = pattern;
        cur.output.add(pattern);
    }

    /** Breadth-first construction: a node's failure link always points to a shallower, already-built node. */
    private void buildFailureLinks() {
        root.fail = root;
        List<Node> queue = new ArrayList<>();
        for (Node child : root.children.values()) {
            child.fail = root;
            queue.add(child);
        }
        for (int head = 0; head < queue.size(); head++) {
            Node u = queue.get(head);
            for (Map.Entry<Character, Node> e : u.children.entrySet()) {
                char c = e.getKey();
                Node v = e.getValue();
                Node f = u.fail;
                while (f != root && !f.children.containsKey(c)) {
                    f = f.fail;
                }
                Node candidate = f.children.get(c);
                v.fail = candidate != null && candidate != v ? candidate : root;
                v.output.addAll(v.fail.output); // dictionary-suffix patterns are reported too
                queue.add(v);
            }
        }
    }

    public Node root() {
        return root;
    }

    /** @return all trie nodes indexed by id. */
    public List<Node> nodes() {
        return nodes;
    }

    public List<String> patterns() {
        return patterns;
    }

    /** @return the node reached by spelling {@code pattern}, or null if it is not in the trie. */
    public Node nodeFor(String pattern) {
        Node cur = root;
        for (char c : pattern.toCharArray()) {
            cur = cur.children.get(c);
            if (cur == null) {
                return null;
            }
        }
        return cur;
    }

    /** Scans the text once and reports every occurrence of every pattern. */
    public Result search(String text) {
        Objects.requireNonNull(text, "text");
        List<Match> matches = new ArrayList<>();
        List<Step> steps = new ArrayList<>();
        int transitions = 0;
        Node state = root;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int from = state.id;
            int hops = 0;
            while (state != root && !state.children.containsKey(c)) {
                state = state.fail;
                hops++;
            }
            Node next = state.children.get(c);
            if (next != null) {
                state = next;
            }
            transitions += hops + 1;
            List<String> found = new ArrayList<>(state.output);
            for (String p : found) {
                matches.add(new Match(i - p.length() + 1, i, p));
            }
            steps.add(new Step(i, c, from, state.id, hops, found));
        }
        return new Result(matches, steps, transitions);
    }
}
