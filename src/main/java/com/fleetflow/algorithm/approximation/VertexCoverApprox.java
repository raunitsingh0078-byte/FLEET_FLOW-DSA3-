package com.fleetflow.algorithm.approximation;

import com.fleetflow.datastructure.graph.SimpleGraph;

import java.util.ArrayList;
import java.util.List;

/**
 * Classic 2-approximation for minimum vertex cover: repeatedly pick an edge whose endpoints are both uncovered
 * and put <em>both</em> endpoints in the cover. The picked edges form a matching M; any cover needs at least one
 * endpoint per matching edge, so OPT ≥ |M| while the algorithm returns 2|M| ≤ 2·OPT vertices.
 */
public final class VertexCoverApprox {

    /**
     * @param u,v            the picked edge
     * @param newlyCovered   edges that became covered by adding u and v
     * @param coverSize      size of the cover after this step
     */
    public record Step(int u, int v, List<int[]> newlyCovered, int coverSize) {
    }

    /** @param matching the picked edges (a maximal matching) */
    public record Result(List<Integer> cover, List<int[]> matching, List<Step> steps) {
    }

    private VertexCoverApprox() {
    }

    public static Result approximate(SimpleGraph g) {
        boolean[] inCover = new boolean[g.vertexCount()];
        List<Integer> cover = new ArrayList<>();
        List<int[]> matching = new ArrayList<>();
        List<Step> steps = new ArrayList<>();
        for (int[] e : g.edges()) {
            if (inCover[e[0]] || inCover[e[1]]) {
                continue; // edge already covered
            }
            inCover[e[0]] = true;
            inCover[e[1]] = true;
            cover.add(e[0]);
            cover.add(e[1]);
            matching.add(e);
            List<int[]> covered = new ArrayList<>();
            for (int[] f : g.edges()) {
                if ((f[0] == e[0] || f[0] == e[1] || f[1] == e[0] || f[1] == e[1])) {
                    covered.add(f);
                }
            }
            steps.add(new Step(e[0], e[1], covered, cover.size()));
        }
        return new Result(cover, matching, steps);
    }

    /** Exact minimum vertex cover by trying subsets in increasing size (exponential; n ≤ 20). */
    public static List<Integer> optimal(SimpleGraph g) {
        int n = g.vertexCount();
        if (n > 20) {
            throw new IllegalArgumentException("exact vertex cover is limited to 20 vertices");
        }
        int bestMask = (1 << n) - 1;
        int bestSize = n;
        for (int mask = 0; mask < (1 << n); mask++) {
            int size = Integer.bitCount(mask);
            if (size >= bestSize) {
                continue;
            }
            boolean covers = true;
            for (int[] e : g.edges()) {
                if ((mask & (1 << e[0])) == 0 && (mask & (1 << e[1])) == 0) {
                    covers = false;
                    break;
                }
            }
            if (covers) {
                bestMask = mask;
                bestSize = size;
            }
        }
        List<Integer> result = new ArrayList<>();
        for (int v = 0; v < n; v++) {
            if ((bestMask & (1 << v)) != 0) {
                result.add(v);
            }
        }
        return result;
    }
}
