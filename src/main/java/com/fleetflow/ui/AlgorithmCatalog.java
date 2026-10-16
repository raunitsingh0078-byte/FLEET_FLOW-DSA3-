package com.fleetflow.ui;

import java.util.List;

/** Metadata of every implemented algorithm: drives the Algorithm Explorer and the Presentation Demo Mode. */
public final class AlgorithmCatalog {

    /**
     * One algorithm.
     *
     * @param route sidebar label of the page that hosts the algorithm; {@code name} is also the key used to focus it there
     */
    public record Entry(String module, String name, String problem, String input, String output, String time, String space,
                        String explanation, String visualization, String application, String route) {
    }

    public static final String M1 = "M1 – String Algorithms";
    public static final String M2 = "M2 – Suffix Structures";
    public static final String M3 = "M3 – Dynamic Programming";
    public static final String M4 = "M4 – Network Flow";
    public static final String M5 = "M5 – NP-Completeness";
    public static final String M6 = "M6 – Randomized & Parallel";

    private static final List<Entry> ENTRIES = List.of(
            new Entry(M1, "KMP", "Pattern searching", "Text, pattern", "Match positions, LPS table", "O(n + m)", "O(m)",
                    "Precomputes the longest-proper-prefix-that-is-also-a-suffix table so the pattern pointer falls back inside the pattern and the text pointer never moves backwards.",
                    "Text and pattern rows with the compared cells, LPS table and fallback jumps", "Find a shipment ID or delivery keyword in thousands of records", "String Intelligence"),
            new Entry(M1, "Z-Function", "Repeated-pattern detection", "String (and optional pattern)", "Z array, period, occurrences", "O(n)", "O(n)",
                    "z[i] is the longest substring at i equal to a prefix; the Z-box [l, r) reuses earlier work so each character is matched once.",
                    "Character row, Z-box highlight and the Z values as they are computed", "Detect a manifest that repeats the same route code", "String Intelligence"),
            new Entry(M1, "Rabin-Karp", "Package-code search", "Text, pattern, hash modulus", "Match positions, hash statistics", "O(n + m) expected", "O(1)",
                    "A rolling polynomial hash of the window is updated in O(1); characters are compared only when hashes are equal (spurious hits are caught).",
                    "Sliding window with window hash versus pattern hash and verification result", "Search package codes efficiently", "String Intelligence"),
            new Entry(M1, "Aho-Corasick", "Multi-pattern search", "Several keywords, text", "All occurrences of all keywords", "O(n + m + z)", "O(m·σ)",
                    "Builds a trie of all keywords, adds failure links with BFS, then scans the text once, following links on mismatches.",
                    "Trie with dashed failure links, current automaton state and highlighted matches", "Detect many delivery-status keywords in a status log at once", "String Intelligence"),
            new Entry(M2, "Suffix Array", "Document indexing", "Document text, query", "Sorted suffixes, match range", "Build O(n log n)–O(n² log n); query O(m log n)", "O(n)",
                    "Sorts all suffixes; a substring is located with two binary searches over the sorted suffixes.",
                    "Table of sorted suffixes, then the binary-search window and the matching rank range", "Index shipment descriptions and delivery documents", "Document Intelligence"),
            new Entry(M2, "SA-IS", "Linear-time suffix array", "Document text", "Suffix array, recursion levels", "O(n)", "O(n)",
                    "Classifies suffixes as S/L, sorts LMS suffixes recursively on a reduced string, then induces all others with two linear scans.",
                    "S/L type row, LMS markers per recursion level and the final suffix array", "Large-scale logistics-document indexing", "Document Intelligence"),
            new Entry(M2, "LCP / Kasai", "Repeated address detection", "Document text, minimum length", "LCP array, repeated substrings", "O(n)", "O(n)",
                    "Computes the longest common prefix of neighbouring suffixes in text order, reusing h − 1 from the previous step.",
                    "Suffix table with LCP values filled in text order", "Detect repeated addresses and shipment descriptions", "Document Intelligence"),
            new Entry(M2, "Suffix Tree", "Fast substring queries", "Text (≤ 24 chars), query", "Tree, occurrence positions", "Build O(n²) (Ukkonen O(n)); query O(m)", "O(n)",
                    "Inserts suffixes into a compressed trie with substring edges; a query walks down from the root.",
                    "Tree growing suffix by suffix, then the query path highlighted", "Substring queries on shipment texts", "Document Intelligence"),
            new Entry(M3, "Levenshtein", "Address correction", "Two strings / address", "Edit distance, edit script, suggestions", "O(m·n)", "O(m·n)",
                    "DP over prefixes: a cell is the cheapest of insert, delete, substitute (or copy on equal letters); traceback gives the edit script.",
                    "Edit-distance matrix filled cell by cell, dependencies and the optimal path", "Correct misspelt delivery addresses", "Optimization Lab"),
            new Entry(M3, "Damerau-Levenshtein", "Address correction with swaps", "Two strings / address", "Edit distance incl. transpositions", "O(m·n)", "O(m·n)",
                    "Levenshtein plus one extra transition: swapping two adjacent letters costs 1 (optimal-string-alignment variant).",
                    "Same matrix with the transposition transition highlighted", "Typos such as 'Raod' for 'Road'", "Optimization Lab"),
            new Entry(M3, "Bitmask DP", "Small-scale route optimisation (TSP)", "Stops with coordinates", "Optimal round trip and cost", "O(2ⁿ·n²)", "O(2ⁿ·n)",
                    "dp[mask][last] is the cheapest way to visit the stops in mask ending at last (Held-Karp).",
                    "Map with the best partial route per subset and the DP table", "Plan the cheapest delivery round trip", "Optimization Lab"),
            new Entry(M3, "Matrix Chain Multiplication", "Sequencing of logistics operations", "Dimension list", "Minimum cost and parenthesisation", "O(n³)", "O(n²)",
                    "m[i][j] = min over split k of m[i][k] + m[k+1][j] + p(i−1)·p(k)·p(j).",
                    "Triangular cost matrix with the two sub-chains of the best split highlighted", "Choose the cheapest order of combining operations", "Optimization Lab"),
            new Entry(M3, "Optimal BST", "Frequently accessed shipment keys", "Keys with access frequencies", "Minimum expected cost and tree", "O(n³)", "O(n²)",
                    "cost[i][j] = Σ freq + min over root r of cost[i][r−1] + cost[r+1][j].",
                    "Cost matrix and the optimal subtree of the interval being computed", "Put hot shipment IDs near the root of a lookup tree", "Optimization Lab"),
            new Entry(M4, "Ford-Fulkerson", "Warehouse-to-destination flow", "Capacity network, source, sink", "Maximum flow", "O(E·|f*|)", "O(V + E)",
                    "Find any augmenting path in the residual graph (DFS) and push its bottleneck; repeat until none exists.",
                    "Interactive graph with the augmenting path, flow/capacity labels and residual view", "How many pallets can move from warehouse to destination", "Network Flow"),
            new Entry(M4, "Edmonds-Karp", "Capacity-based logistics networks", "Capacity network, source, sink", "Maximum flow", "O(V·E²)", "O(V + E)",
                    "Ford-Fulkerson with shortest (BFS) augmenting paths, independent of capacities.",
                    "Same graph, augmenting paths animated in shortest-path order", "Bandwidth / capacity planning", "Network Flow"),
            new Entry(M4, "Dinic", "Large distribution networks", "Capacity network, source, sink", "Maximum flow, phases", "O(V²·E)", "O(V + E)",
                    "Builds a BFS level graph, saturates it with a blocking flow, and repeats with longer shortest paths.",
                    "Levels under the nodes, level-graph edges and the blocking-flow paths per phase", "Large distribution networks", "Network Flow"),
            new Entry(M4, "Bipartite Matching", "Driver–task assignment", "Drivers, tasks, allowed pairs", "Maximum assignment", "O(V·E)", "O(V)",
                    "For each driver search an augmenting path; a task that is taken may be reassigned along the path.",
                    "Drivers left, tasks right, matched edges in green and the current search", "Assign drivers to delivery tasks", "Network Flow"),
            new Entry(M4, "Minimum Cut", "Bottleneck detection", "Capacity network, source, sink", "Cut edges and capacity", "O(V + E) after max flow", "O(V)",
                    "After max flow, nodes reachable in the residual graph form the source side; saturated edges leaving it form the minimum cut.",
                    "Cut edges in red, source side in green", "Identify distribution bottlenecks", "Network Flow"),
            new Entry(M5, "SAT / 3-SAT", "Delivery constraint satisfiability", "CNF clauses (and optional assignment)", "Model or UNSAT, clause values", "Verify O(m); search O(2ⁿ·m)", "O(n)",
                    "Checks an assignment clause by clause (the NP certificate) or tries all 2ⁿ assignments.",
                    "Variable values and a clause table coloured true/false", "Complex delivery constraints", "Complexity Lab"),
            new Entry(M5, "3-SAT to CLIQUE", "Demonstrating NP-hardness", "3-CNF formula", "Reduction graph, clique, assignment", "Reduction O(m²)", "O(m²)",
                    "One vertex per literal occurrence; join literals of different clauses that do not contradict; a k-clique means satisfiable.",
                    "Clause columns, edges and the backtracking clique search", "Show that clique is as hard as 3-SAT", "Complexity Lab"),
            new Entry(M5, "CLIQUE to INDEPENDENT-SET", "Conflict modelling", "Graph", "Complement graph, independent set", "O(n²)", "O(n²)",
                    "A clique of G is exactly an independent set of the complement graph.",
                    "Graph and complement side by side with the same vertex set highlighted", "Compatible vs. conflicting depots", "Complexity Lab"),
            new Entry(M5, "INDEPENDENT-SET to VERTEX-COVER", "Conflict modelling", "Graph", "Vertex cover = V \\ independent set", "O(n)", "O(n)",
                    "S is independent iff V \\ S covers every edge, so |S| + |cover| = n.",
                    "Independent set, its complement and edge-by-edge cover verification", "Choosing conflict-free or watching locations", "Complexity Lab"),
            new Entry(M5, "Vertex Cover 2-Approximation", "Important conflict locations", "Graph", "Cover within factor 2 of optimal", "O(V + E)", "O(V)",
                    "Repeatedly take an uncovered edge and add both endpoints; the chosen edges form a matching so the cover is ≤ 2·OPT.",
                    "Picked edges, selected vertices, covered edges and the comparison with the optimum", "Identify important conflict locations quickly", "Complexity Lab"),
            new Entry(M6, "Randomized QuickSort", "Shipment ranking", "Weights", "Sorted ranking, comparison count", "Expected O(n log n)", "O(log n)",
                    "Partition around a uniformly random pivot and recurse on both sides.",
                    "Array with pivot, scan pointer, partition regions and fixed pivots", "Rank shipments by weight", "Parallel & Randomized"),
            new Entry(M6, "Reservoir Sampling", "Continuous vehicle-event streams", "Stream length, sample size", "Uniform random sample", "O(N)", "O(k)",
                    "Keep the first k; replace a random slot with probability k/i for item i.",
                    "Reservoir slots changing as events arrive", "Fair sample of an endless event stream", "Parallel & Randomized"),
            new Entry(M6, "Miller-Rabin", "Large numerical computations", "Integer, rounds", "Prime / composite, witness", "O(k·log n) multiplications", "O(1)",
                    "Tests whether a random base is a witness of compositeness; error ≤ 4^−k for composites, none for primes.",
                    "Round table with the squaring sequence and verdict", "Primality of large numbers", "Parallel & Randomized"),
            new Entry(M6, "Blelloch Scan", "Cumulative shipment statistics", "Numbers", "Exclusive / inclusive prefix sums", "Work O(n), span O(log n)", "O(n)",
                    "Up-sweep builds partial sums in a binary tree, the root is cleared, the down-sweep pushes prefixes back down.",
                    "Array after each parallel round with the combined pairs highlighted", "Running totals over huge data sets", "Parallel & Randomized"),
            new Entry(M6, "Parallel Reduce", "Logistics data aggregation", "Large array, operator", "Aggregate and measured timings", "Work O(n), span O(n/P + log P)", "O(P)",
                    "Chunks are reduced by ExecutorService workers and the partial results combined.",
                    "Bar chart of measured sequential vs parallel times", "Aggregate millions of records", "Parallel & Randomized"),
            new Entry(M6, "Brent's Theorem", "Evaluating parallel processing", "Work W, span S, processors P", "Time bounds, speed-up", "T_P = O(W/P + S)", "—",
                    "A greedy scheduler needs at most (W − S)/P + S; speed-up is capped by W/S.",
                    "Chart of guaranteed vs ideal speed-up and a table of bounds", "How many processors are worth using", "Parallel & Randomized"));

    private AlgorithmCatalog() {
    }

    public static List<Entry> entries() {
        return ENTRIES;
    }

    /** @return the entry with the given name. @throws IllegalArgumentException if unknown */
    public static Entry byName(String name) {
        return ENTRIES.stream().filter(e -> e.name().equals(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown algorithm: " + name));
    }

    /** The ten algorithms of the classroom demo, in presentation order. */
    public static List<String> demoSequence() {
        return List.of("KMP", "Aho-Corasick", "Levenshtein", "Bitmask DP", "Dinic", "Bipartite Matching", "Minimum Cut",
                "Vertex Cover 2-Approximation", "Randomized QuickSort", "Parallel Reduce");
    }
}
