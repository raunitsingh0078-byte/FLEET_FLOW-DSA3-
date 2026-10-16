package com.fleetflow.datastructure.suffix;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Compressed suffix tree (edges hold substring ranges of the text). Built by inserting the suffixes one by one
 * (O(n²) time – an educational construction; Ukkonen's algorithm achieves O(n)). Queries take O(m).
 * A unique terminator '$' is appended so that every suffix ends in its own leaf.
 */
public final class SuffixTree {

    /** Terminator appended to the text. */
    public static final char TERMINATOR = '$';

    /** A tree node; the edge from its parent spells {@code text[start, end)}. */
    public static final class Node {
        private final int id;
        private int start;
        private int end;
        private final Map<Character, Node> children = new TreeMap<>();
        private int suffixIndex = -1;

        private Node(int id, int start, int end) {
            this.id = id;
            this.start = start;
            this.end = end;
        }

        public int id() {
            return id;
        }

        public int start() {
            return start;
        }

        public int end() {
            return end;
        }

        public Map<Character, Node> children() {
            return children;
        }

        /** @return start position of the suffix ending at this leaf, or -1 for internal nodes. */
        public int suffixIndex() {
            return suffixIndex;
        }

        public boolean isLeaf() {
            return children.isEmpty();
        }
    }

    /**
     * Outcome of a substring query.
     *
     * @param found      whether the pattern occurs
     * @param path       ids of the nodes visited while matching (root first)
     * @param positions  start positions of all occurrences (sorted)
     * @param comparisons character comparisons performed
     */
    public record Query(boolean found, List<Integer> path, List<Integer> positions, int comparisons) {
    }

    private final String text;
    private final Node root;
    private int nextId = 1;

    /** Builds the tree of {@code text + '$'} containing the first {@code suffixCount} suffixes. */
    public SuffixTree(String text, int suffixCount) {
        Objects.requireNonNull(text, "text");
        if (text.indexOf(TERMINATOR) >= 0) {
            throw new IllegalArgumentException("text must not contain the terminator '$'");
        }
        this.text = text + TERMINATOR;
        this.root = new Node(0, 0, 0);
        int limit = Math.min(suffixCount, this.text.length());
        for (int i = 0; i < limit; i++) {
            insert(i);
        }
    }

    /** Builds the complete tree. */
    public SuffixTree(String text) {
        this(text, Integer.MAX_VALUE);
    }

    public Node root() {
        return root;
    }

    /** @return the text including the terminator. */
    public String text() {
        return text;
    }

    public String edgeLabel(Node node) {
        return text.substring(node.start, node.end);
    }

    private void insert(int suffix) {
        Node cur = root;
        int pos = suffix;
        while (true) {
            Node child = cur.children.get(text.charAt(pos));
            if (child == null) {
                Node leaf = new Node(nextId++, pos, text.length());
                leaf.suffixIndex = suffix;
                cur.children.put(text.charAt(pos), leaf);
                return;
            }
            int len = child.end - child.start;
            int k = 0;
            while (k < len && text.charAt(child.start + k) == text.charAt(pos + k)) {
                k++;
            }
            if (k == len) { // whole edge matched: descend
                cur = child;
                pos += len;
                continue;
            }
            Node split = new Node(nextId++, child.start, child.start + k); // split the edge
            cur.children.put(text.charAt(split.start), split);
            child.start += k;
            split.children.put(text.charAt(child.start), child);
            Node leaf = new Node(nextId++, pos + k, text.length());
            leaf.suffixIndex = suffix;
            split.children.put(text.charAt(leaf.start), leaf);
            return;
        }
    }

    /** Searches a substring by walking down from the root. */
    public Query search(String pattern) {
        Objects.requireNonNull(pattern, "pattern");
        List<Integer> path = new ArrayList<>();
        path.add(root.id);
        int comparisons = 0;
        Node cur = root;
        int i = 0;
        while (i < pattern.length()) {
            Node child = cur.children.get(pattern.charAt(i));
            comparisons++;
            if (child == null) {
                return new Query(false, path, List.of(), comparisons);
            }
            path.add(child.id);
            int len = child.end - child.start;
            for (int k = 0; k < len && i < pattern.length(); k++, i++) {
                if (k > 0) {
                    comparisons++;
                }
                if (text.charAt(child.start + k) != pattern.charAt(i)) {
                    return new Query(false, path, List.of(), comparisons);
                }
            }
            cur = child;
        }
        List<Integer> positions = new ArrayList<>();
        collect(cur, positions);
        positions.sort(Integer::compare);
        return new Query(true, path, positions, comparisons);
    }

    private void collect(Node node, List<Integer> out) {
        if (node.isLeaf()) {
            out.add(node.suffixIndex);
        }
        for (Node child : node.children.values()) {
            collect(child, out);
        }
    }

    /** @return number of nodes including the root. */
    public int nodeCount() {
        return nextId;
    }
}
