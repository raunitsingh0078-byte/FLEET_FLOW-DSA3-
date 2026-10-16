package com.fleetflow.algorithm.np;

import com.fleetflow.algorithm.approximation.VertexCoverApprox;
import com.fleetflow.datastructure.graph.SimpleGraph;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpCompletenessTest {

    // ---------------------------------------------------------------- SAT

    @Test
    void parsesDifferentLiteralStyles() {
        Sat.Formula f = Sat.parse("(x1 | !x2 | x3)\n-1 ~3 x2");
        assertEquals(3, f.variables());
        assertEquals(2, f.clauses().size());
        assertEquals(List.of(1, -2, 3), java.util.Arrays.stream(f.clauses().get(0)).boxed().toList());
        assertEquals(List.of(-1, -3, 2), java.util.Arrays.stream(f.clauses().get(1)).boxed().toList());
    }

    @Test
    void rejectsInvalidFormulas() {
        assertThrows(IllegalArgumentException.class, () -> Sat.parse(""));
        assertThrows(IllegalArgumentException.class, () -> Sat.parse("x1 y2"));
        assertThrows(IllegalArgumentException.class, () -> Sat.parse("x0"));
        assertThrows(IllegalArgumentException.class, () -> Sat.parse("   \n  "));
    }

    @Test
    void solvesSatisfiableAndUnsatisfiableFormulas() {
        Sat.Result sat = Sat.solve(Sat.parse("x1 x2\n!x1 x2\nx1 !x2"));
        assertTrue(sat.satisfiable());
        assertTrue(Sat.satisfies(Sat.parse("x1 x2\n!x1 x2\nx1 !x2"), sat.model()));
        assertEquals(1, sat.solutionCount());
        Sat.Result unsat = Sat.solve(Sat.parse("x1\n!x1"));
        assertFalse(unsat.satisfiable());
        assertNull(unsat.model());
        assertEquals(2, unsat.attempts().size());
    }

    @Test
    void assignmentCheckingReportsEachClause() {
        Sat.Formula f = Sat.parse("x1 x2\n!x1 x3");
        assertEquals(List.of(true, false), toList(Sat.clauseValues(f, new boolean[]{true, false, false})));
    }

    private static List<Boolean> toList(boolean[] a) {
        List<Boolean> l = new ArrayList<>();
        for (boolean b : a) l.add(b);
        return l;
    }

    // ---------------------------------------------------------------- reductions

    private static Sat.Formula randomThreeSat(Random rnd, int vars, int clauses) {
        List<int[]> list = new ArrayList<>();
        for (int c = 0; c < clauses; c++) {
            int len = 1 + rnd.nextInt(3);
            int[] clause = new int[len];
            for (int i = 0; i < len; i++) clause[i] = (1 + rnd.nextInt(vars)) * (rnd.nextBoolean() ? 1 : -1);
            list.add(clause);
        }
        return new Sat.Formula(vars, list);
    }

    @Test
    void satisfiableIffCliqueOfSizeKExists() {
        Random rnd = new Random(17);
        int satisfiable = 0;
        for (int trial = 0; trial < 300; trial++) {
            Sat.Formula f = randomThreeSat(rnd, 2 + rnd.nextInt(3), 2 + rnd.nextInt(4));
            Reductions.ThreeSatGraph tg = Reductions.threeSatToClique(f);
            CliqueSearch.Result r = CliqueSearch.findClique(tg.graph(), tg.clauses());
            boolean sat = Sat.solve(f).satisfiable();
            assertEquals(sat, r.clique() != null, "formula trial " + trial);
            if (r.clique() != null) {
                satisfiable++;
                assertTrue(Reductions.isClique(tg.graph(), r.clique()));
                assertTrue(Sat.satisfies(f, Reductions.assignmentFromClique(f, tg, r.clique())), "assignment from clique must satisfy formula");
            }
        }
        assertTrue(satisfiable > 20, "test should cover satisfiable instances");
    }

    @Test
    void contradictoryLiteralsAreNeverAdjacent() {
        Reductions.ThreeSatGraph tg = Reductions.threeSatToClique(Sat.parse("x1\n!x1"));
        assertEquals(0, tg.graph().edgeCount());
    }

    @Test
    void reductionRejectsLongClauses() {
        assertThrows(IllegalArgumentException.class, () -> Reductions.threeSatToClique(Sat.parse("x1 x2 x3 x4")));
    }

    @Test
    void maxCliqueOfGraphEqualsMaxIndependentSetOfComplement() {
        Random rnd = new Random(23);
        for (int trial = 0; trial < 60; trial++) {
            int n = 2 + rnd.nextInt(7);
            SimpleGraph g = new SimpleGraph(n);
            for (int a = 0; a < n; a++) for (int b = a + 1; b < n; b++) if (rnd.nextBoolean()) g.addEdge(a, b);
            SimpleGraph c = Reductions.cliqueToIndependentSet(g);
            List<Integer> clique = CliqueSearch.maximumClique(g);
            assertTrue(Reductions.isClique(g, clique));
            assertTrue(Reductions.isIndependentSet(c, clique));
            assertEquals(clique.size(), bruteMaxIndependentSet(c));
        }
    }

    private static int bruteMaxIndependentSet(SimpleGraph g) {
        int best = 0;
        for (int mask = 0; mask < (1 << g.vertexCount()); mask++) {
            List<Integer> set = new ArrayList<>();
            for (int v = 0; v < g.vertexCount(); v++) if ((mask & (1 << v)) != 0) set.add(v);
            if (Reductions.isIndependentSet(g, set)) best = Math.max(best, set.size());
        }
        return best;
    }

    @Test
    void independentSetComplementIsVertexCover() {
        SimpleGraph g = SimpleGraph.parse(5, "0-1, 1-2, 2-3, 3-4");
        List<Integer> independent = List.of(0, 2, 4);
        List<Integer> cover = Reductions.vertexCoverFromIndependentSet(g, independent);
        assertEquals(List.of(1, 3), cover);
        assertTrue(Reductions.isVertexCover(g, cover));
        assertEquals(g.vertexCount(), independent.size() + cover.size());
    }

    // ---------------------------------------------------------------- graph parsing

    @Test
    void graphParsingAndValidation() {
        SimpleGraph g = SimpleGraph.parse(4, "0-1, 1-2 ; 2-3\n0-1");
        assertEquals(3, g.edgeCount());
        assertThrows(IllegalArgumentException.class, () -> SimpleGraph.parse(3, "0-5"));
        assertThrows(IllegalArgumentException.class, () -> SimpleGraph.parse(3, "1-1"));
        assertThrows(IllegalArgumentException.class, () -> SimpleGraph.parse(3, "a-b"));
        assertThrows(IllegalArgumentException.class, () -> SimpleGraph.parse(3, "0-1-2"));
        assertEquals(0, SimpleGraph.parse(3, "").edgeCount());
    }

    // ---------------------------------------------------------------- vertex cover 2-approximation

    @Test
    void approximationIsValidCoverWithinFactorTwo() {
        Random rnd = new Random(31);
        for (int trial = 0; trial < 200; trial++) {
            int n = 1 + rnd.nextInt(10);
            SimpleGraph g = new SimpleGraph(n);
            for (int a = 0; a < n; a++) for (int b = a + 1; b < n; b++) if (rnd.nextInt(3) == 0) g.addEdge(a, b);
            VertexCoverApprox.Result r = VertexCoverApprox.approximate(g);
            List<Integer> opt = VertexCoverApprox.optimal(g);
            assertTrue(Reductions.isVertexCover(g, r.cover()));
            assertTrue(Reductions.isVertexCover(g, opt));
            assertTrue(r.cover().size() <= 2 * opt.size(), "ratio violated: " + r.cover().size() + " vs " + opt.size());
            assertEquals(2 * r.matching().size(), r.cover().size());
        }
    }

    @Test
    void approximationEdgeCases() {
        assertTrue(VertexCoverApprox.approximate(new SimpleGraph(0)).cover().isEmpty());
        assertTrue(VertexCoverApprox.approximate(new SimpleGraph(4)).cover().isEmpty());
        SimpleGraph star = SimpleGraph.parse(5, "0-1, 0-2, 0-3, 0-4");
        assertEquals(2, VertexCoverApprox.approximate(star).cover().size());
        assertEquals(List.of(0), VertexCoverApprox.optimal(star));
        assertNotNull(VertexCoverApprox.optimal(new SimpleGraph(3)));
    }
}
