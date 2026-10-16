package com.fleetflow.algorithm.np;

import com.fleetflow.datastructure.graph.SimpleGraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Polynomial-time reductions between NP-complete problems:
 * 3-SAT ≤ CLIQUE ≤ INDEPENDENT-SET ≤ VERTEX-COVER.
 */
public final class Reductions {

    /**
     * The graph built from a 3-CNF formula: one vertex per literal occurrence.
     *
     * @param clauseOf  clause index of each vertex
     * @param literalOf literal (±variable) of each vertex
     */
    public record ThreeSatGraph(SimpleGraph graph, int[] clauseOf, int[] literalOf, int clauses) {
    }

    private Reductions() {
    }

    /**
     * 3-SAT → CLIQUE. Two vertices (literal occurrences) are joined iff they belong to <em>different</em> clauses and are
     * not contradictory (x and ¬x). The formula is satisfiable iff the graph has a clique of size k = number of clauses:
     * a k-clique picks one true literal from every clause, and consistent picks give a satisfying assignment.
     */
    public static ThreeSatGraph threeSatToClique(Sat.Formula f) {
        if (!f.isThreeSat()) {
            throw new IllegalArgumentException("3-SAT → CLIQUE needs clauses with at most three literals");
        }
        List<Integer> clauseOf = new ArrayList<>();
        List<Integer> literalOf = new ArrayList<>();
        for (int c = 0; c < f.clauses().size(); c++) {
            for (int lit : f.clauses().get(c)) {
                clauseOf.add(c);
                literalOf.add(lit);
            }
        }
        SimpleGraph g = new SimpleGraph(clauseOf.size());
        for (int a = 0; a < g.vertexCount(); a++) {
            for (int b = a + 1; b < g.vertexCount(); b++) {
                boolean differentClauses = !clauseOf.get(a).equals(clauseOf.get(b));
                boolean contradictory = literalOf.get(a) == -literalOf.get(b);
                if (differentClauses && !contradictory) {
                    g.addEdge(a, b);
                }
            }
        }
        return new ThreeSatGraph(g, clauseOf.stream().mapToInt(Integer::intValue).toArray(),
                literalOf.stream().mapToInt(Integer::intValue).toArray(), f.clauses().size());
    }

    /** Reads a satisfying assignment off a k-clique: every chosen literal is made true; unconstrained variables default to false. */
    public static boolean[] assignmentFromClique(Sat.Formula f, ThreeSatGraph tg, Collection<Integer> clique) {
        boolean[] assignment = new boolean[f.variables()];
        for (int vertex : clique) {
            int lit = tg.literalOf()[vertex];
            assignment[Math.abs(lit) - 1] = lit > 0;
        }
        return assignment;
    }

    /** CLIQUE → INDEPENDENT-SET: a set is a clique in G iff it is an independent set in the complement of G. */
    public static SimpleGraph cliqueToIndependentSet(SimpleGraph g) {
        return g.complement();
    }

    /** INDEPENDENT-SET → VERTEX-COVER: S is independent iff V \ S is a vertex cover. */
    public static List<Integer> vertexCoverFromIndependentSet(SimpleGraph g, Collection<Integer> independentSet) {
        List<Integer> cover = new ArrayList<>();
        for (int v = 0; v < g.vertexCount(); v++) {
            if (!independentSet.contains(v)) {
                cover.add(v);
            }
        }
        return cover;
    }

    public static boolean isClique(SimpleGraph g, Collection<Integer> set) {
        for (int a : set) {
            for (int b : set) {
                if (a < b && !g.hasEdge(a, b)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isIndependentSet(SimpleGraph g, Collection<Integer> set) {
        for (int a : set) {
            for (int b : set) {
                if (a < b && g.hasEdge(a, b)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isVertexCover(SimpleGraph g, Collection<Integer> set) {
        for (int[] e : g.edges()) {
            if (!set.contains(e[0]) && !set.contains(e[1])) {
                return false;
            }
        }
        return true;
    }
}
