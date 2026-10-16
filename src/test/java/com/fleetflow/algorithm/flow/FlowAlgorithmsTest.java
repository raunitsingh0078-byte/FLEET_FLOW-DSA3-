package com.fleetflow.algorithm.flow;

import com.fleetflow.datastructure.graph.FlowNetwork;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowAlgorithmsTest {

    /** The classic CLRS network: s=0, v1=1, v2=2, v3=3, v4=4, t=5, max flow 23. */
    private static FlowNetwork clrs() {
        FlowNetwork n = new FlowNetwork(6);
        n.addEdge(0, 1, 16);
        n.addEdge(0, 2, 13);
        n.addEdge(1, 3, 12);
        n.addEdge(2, 1, 4);
        n.addEdge(2, 4, 14);
        n.addEdge(3, 2, 9);
        n.addEdge(3, 5, 20);
        n.addEdge(4, 3, 7);
        n.addEdge(4, 5, 4);
        return n;
    }

    @Test
    void allThreeAlgorithmsSolveClrsExample() {
        assertEquals(23, FordFulkerson.maxFlow(clrs(), 0, 5).maxFlow());
        assertEquals(23, EdmondsKarp.maxFlow(clrs(), 0, 5).maxFlow());
        assertEquals(23, Dinic.maxFlow(clrs(), 0, 5).maxFlow());
    }

    @Test
    void algorithmsAgreeOnRandomNetworks() {
        Random rnd = new Random(5);
        for (int trial = 0; trial < 200; trial++) {
            int n = 2 + rnd.nextInt(8);
            FlowNetwork net = new FlowNetwork(n);
            int edges = rnd.nextInt(n * 3);
            for (int i = 0; i < edges; i++) {
                int a = rnd.nextInt(n);
                int b = rnd.nextInt(n);
                if (a != b) net.addEdge(a, b, rnd.nextInt(20));
            }
            int ff = FordFulkerson.maxFlow(net, 0, n - 1).maxFlow();
            assertEquals(ff, EdmondsKarp.maxFlow(net, 0, n - 1).maxFlow());
            assertEquals(ff, Dinic.maxFlow(net, 0, n - 1).maxFlow());
        }
    }

    @Test
    void flowConservationAndCapacityHold() {
        FlowResult r = Dinic.maxFlow(clrs(), 0, 5);
        FlowNetwork n = r.network();
        for (int v = 1; v <= 4; v++) {
            int in = 0;
            int out = 0;
            for (int k = 0; k < n.edgeCount(); k++) {
                FlowNetwork.Edge e = n.forward(k);
                assertTrue(e.flow() >= 0 && e.flow() <= e.capacity());
                if (e.to() == v) in += e.flow();
                if (e.from() == v) out += e.flow();
            }
            assertEquals(in, out, "conservation at node " + v);
        }
    }

    @Test
    void minCutCapacityEqualsMaxFlow() {
        Random rnd = new Random(9);
        for (int trial = 0; trial < 100; trial++) {
            int n = 3 + rnd.nextInt(6);
            FlowNetwork net = new FlowNetwork(n);
            for (int i = 0; i < n * 2; i++) {
                int a = rnd.nextInt(n);
                int b = rnd.nextInt(n);
                if (a != b) net.addEdge(a, b, 1 + rnd.nextInt(15));
            }
            FlowResult r = EdmondsKarp.maxFlow(net, 0, n - 1);
            MinCut.Result cut = MinCut.compute(r.network(), 0);
            assertEquals(r.maxFlow(), cut.capacity());
            assertTrue(cut.sourceSide()[0]);
            assertTrue(!cut.sourceSide()[n - 1]);
        }
    }

    @Test
    void clrsMinCutEdges() {
        FlowResult r = Dinic.maxFlow(clrs(), 0, 5);
        MinCut.Result cut = MinCut.compute(r.network(), 0);
        assertEquals(23, cut.capacity());
        assertEquals(List.of(2, 7, 8), cut.cutEdges()); // v1->v3 (12) + v4->v3 (7) + v4->t (4) = 23
    }

    @Test
    void disconnectedAndZeroCapacityNetworksHaveZeroFlow() {
        FlowNetwork n = new FlowNetwork(3);
        n.addEdge(0, 1, 5);
        assertEquals(0, Dinic.maxFlow(n, 0, 2).maxFlow());
        FlowNetwork z = new FlowNetwork(2);
        z.addEdge(0, 1, 0);
        assertEquals(0, EdmondsKarp.maxFlow(z, 0, 1).maxFlow());
        assertEquals(0, FordFulkerson.maxFlow(new FlowNetwork(2), 0, 1).maxFlow());
    }

    @Test
    void parallelEdgesAreSummed() {
        FlowNetwork n = new FlowNetwork(2);
        n.addEdge(0, 1, 3);
        n.addEdge(0, 1, 4);
        assertEquals(7, Dinic.maxFlow(n, 0, 1).maxFlow());
    }

    @Test
    void inputNetworkIsNotModified() {
        FlowNetwork n = clrs();
        Dinic.maxFlow(n, 0, 5);
        for (int k = 0; k < n.edgeCount(); k++) assertEquals(0, n.forward(k).flow());
    }

    @Test
    void invalidArgumentsAreRejected() {
        FlowNetwork n = clrs();
        assertThrows(IllegalArgumentException.class, () -> Dinic.maxFlow(n, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> Dinic.maxFlow(n, -1, 5));
        assertThrows(IllegalArgumentException.class, () -> Dinic.maxFlow(n, 0, 99));
        assertThrows(IllegalArgumentException.class, () -> n.addEdge(0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> n.addEdge(0, 1, -1));
        assertThrows(IllegalArgumentException.class, () -> n.addEdge(0, 7, 1));
    }

    @Test
    void dinicUsesFewPhasesAndRecordsLevelGraphs() {
        FlowResult r = Dinic.maxFlow(clrs(), 0, 5);
        assertTrue(r.phases() >= 1 && r.phases() <= 5);
        assertEquals(r.phases(), r.steps().stream().filter(s -> s.kind() == FlowStep.Kind.PHASE).count());
        assertEquals(r.augmentations(), r.steps().stream().filter(s -> s.kind() == FlowStep.Kind.AUGMENT).count());
        assertEquals(23, r.steps().get(r.steps().size() - 1).totalFlow());
    }

    @Test
    void logisticsRouteNetworkHasFlow32() {
        // WH-02 Delhi (0) -> WH-05 Chennai (4) used by the Warehouses page and Demo Mode
        FlowNetwork n = new FlowNetwork(5); // 0=WH-01 1=WH-02 2=WH-03 3=WH-04 4=WH-05
        n.addEdge(1, 0, 20);
        n.addEdge(1, 3, 15);
        n.addEdge(0, 2, 12);
        n.addEdge(0, 3, 8);
        n.addEdge(3, 2, 10);
        n.addEdge(3, 4, 14);
        n.addEdge(2, 4, 18);
        assertEquals(32, Dinic.maxFlow(n, 1, 4).maxFlow());
    }

    // ---------------------------------------------------------------- bipartite matching

    @Test
    void bipartiteMatchingNeedsReassignment() {
        List<List<Integer>> adj = List.of(List.of(0, 1), List.of(0), List.of(1, 2));
        BipartiteMatching.Result r = BipartiteMatching.maximumMatching(3, 3, adj);
        assertEquals(3, r.size());
        assertEquals(1, r.matchOfLeft()[0]);
        assertEquals(0, r.matchOfLeft()[1]);
        assertEquals(2, r.matchOfLeft()[2]);
    }

    @Test
    void bipartiteMatchingEqualsMaxFlowOfUnitNetwork() {
        Random rnd = new Random(21);
        for (int trial = 0; trial < 150; trial++) {
            int l = 1 + rnd.nextInt(6);
            int rr = 1 + rnd.nextInt(6);
            List<List<Integer>> adj = new ArrayList<>();
            FlowNetwork net = new FlowNetwork(l + rr + 2);
            int s = l + rr;
            int t = s + 1;
            for (int u = 0; u < l; u++) {
                List<Integer> list = new ArrayList<>();
                for (int v = 0; v < rr; v++) {
                    if (rnd.nextInt(3) == 0) {
                        list.add(v);
                        net.addEdge(u, l + v, 1);
                    }
                }
                adj.add(list);
                net.addEdge(s, u, 1);
            }
            for (int v = 0; v < rr; v++) net.addEdge(l + v, t, 1);
            assertEquals(Dinic.maxFlow(net, s, t).maxFlow(), BipartiteMatching.maximumMatching(l, rr, adj).size());
        }
    }

    @Test
    void bipartiteMatchingEdgeCases() {
        assertEquals(0, BipartiteMatching.maximumMatching(0, 0, List.of()).size());
        assertEquals(0, BipartiteMatching.maximumMatching(2, 2, List.of(List.of(), List.of())).size());
        assertThrows(IllegalArgumentException.class, () -> BipartiteMatching.maximumMatching(1, 1, List.of(List.of(3))));
        assertThrows(IllegalArgumentException.class, () -> BipartiteMatching.maximumMatching(2, 1, List.of(List.of(0))));
    }
}
