# FleetFlow — Intelligent Logistics Management Platform

**DSA-3 Problem-Based Learning Project**

FleetFlow is a professional, GUI-based Java desktop application that models a logistics-management
platform (vehicles, drivers, warehouses, shipments, documents and events) while demonstrating **29
data-structures-and-algorithms techniques across six modules (M1–M6)** with interactive, step-by-step
visualisations — not just buttons with algorithm names attached.

---

## 1. Features

- **Live dashboard** with KPI cards, five charts (pie, bar, line) and a recent-events feed, all computed
  from the in-memory data set.
- **Shipment management**: add / edit / delete / search / filter / sort / view details. Search is powered
  by a real choice of **KMP or Rabin-Karp** run over every record, with measured comparison counts.
- **Vehicle & driver management**, with a live **bipartite-matching** driver-assignment demo.
- **Warehouse management** with an interactive logistics-network map and **Dinic max-flow / min-cut**
  bottleneck analysis on the real inter-warehouse lanes.
- **Six algorithm laboratories** (String Intelligence, Document Intelligence, Optimization Lab, Network
  Flow Simulator, Complexity & Approximation Lab, Parallel & Randomized Lab) — one tab per algorithm, each
  with input fields, a "Generate Demo Data" button, run/step/back/pause/reset/speed controls, a canvas
  visualisation, a plain-English step narration, and a result panel with real complexity/statistics.
- **Algorithm Explorer**: all 29 algorithms listed with problem/input/output/time/space/explanation and a
  one-click Run / Visualize button that jumps straight to the animated demo.
- **Presentation Demo Mode**: a ten-step classroom walkthrough in a fixed, sensible order.
- **Analytics** page combining shipment/fleet/warehouse KPIs with measured algorithm-execution statistics
  (never fabricated).
- **Light / dark theme toggle** (top-right), persisted between runs.
- CSV persistence for shipment edits (no external database required); a "Reset demo data" button restores
  the prepared data set.
- Background execution (JavaFX `Task`) so no algorithm run freezes the UI; friendly alerts for bad input.

## 2. Technologies

Java 17 · JavaFX 17 (`javafx-controls`) · Maven · JUnit 5. No Spring Boot, no web framework — a native,
modular desktop application.

## 3. Architecture

```
src/main/java/com/fleetflow/
    Main.java                     Application entry point
    controller/                   NavigationController (sidebar routing)
    model/                        Immutable records: Shipment, Vehicle, Driver, Warehouse, ...
    data/                         FleetRepository, DemoDataGenerator, CSV persistence, RouteNetwork
    algorithm/
        string/                   KMP, Z-Function, Rabin-Karp
        suffix/                   Suffix Array, SA-IS, LCP/Kasai
        dp/                       Levenshtein/Damerau, Bitmask TSP, Matrix Chain, Optimal BST
        flow/                     Ford-Fulkerson, Edmonds-Karp, Dinic, Bipartite Matching, Min-Cut
        np/                       SAT, 3-SAT→CLIQUE reduction, CliqueSearch
        approximation/            Vertex-Cover 2-approximation
        randomized/                Randomized QuickSort, Reservoir Sampling, Miller-Rabin
        parallel/                 Blelloch Scan, Parallel Reduce, Brent's Theorem
    datastructure/
        trie/                     Aho-Corasick automaton
        suffix/                   Suffix Tree
        graph/                    FlowNetwork, SimpleGraph
    visualization/                Reusable canvas widgets: Array/Matrix/Tree/Graph/StringMatching visualizers,
                                   StepPlayer (play/pause/step/reset/speed), Palette (theme-aware colours)
    ui/                           AlgorithmPanel (common algorithm-page layout), AppShell, page framework
        page/                     One class per sidebar page
        panel/<module>/           One class per algorithm tab
    util/                         Dialogs, ThemeManager, Async, Geo, AlgorithmStats, ValidationException

src/main/resources/
    css/fleetflow.css             Dark theme (design tokens)
    css/fleetflow-light.css       Light theme (overrides the tokens)

src/test/java/com/fleetflow/     JUnit 5 tests — one class per module/algorithm group, 101 tests total
```

Every algorithm lives in `algorithm/` or `datastructure/`, implemented by hand (no library shortcuts);
every visualisation is a small, reusable component in `visualization/`; every algorithm page follows the
same `AlgorithmPanel` template (title → description → input → controls → visualization → result).

## 4. How to install

Requirements: JDK 17+, Maven 3.8+, internet access on first build (Maven downloads JavaFX and JUnit).

```bash
git clone <this-repository>   # or unzip the provided archive
cd fleetflow
```

## 5. How to run

```bash
mvn clean javafx:run
```

The application opens with the demo data already loaded (24 shipments, 10 vehicles, 10 drivers, 5
warehouses, shipment events and documents).

Run the unit tests:

```bash
mvn test
```

## 6. DSA algorithms implemented (29)

| # | Algorithm | Module | Problem solved |
|---|---|---|---|
| 1 | KMP | M1 | Pattern searching in shipment IDs / keywords |
| 2 | Z-Function | M1 | Repeated shipment-pattern detection |
| 3 | Rabin-Karp | M1 | Efficient package-code search |
| 4 | Aho-Corasick | M1 | Multiple delivery-status keywords at once |
| 5 | Suffix Array | M2 | Document indexing |
| 6 | SA-IS | M2 | Linear-time suffix array construction |
| 7 | LCP / Kasai | M2 | Repeated address / description detection |
| 8 | Suffix Tree | M2 | Fast substring queries |
| 9 | Levenshtein | M3 | Address correction |
| 10 | Damerau-Levenshtein | M3 | Address correction with adjacent-swap typos |
| 11 | Bitmask DP (Held-Karp) | M3 | Small-scale route optimisation (TSP) |
| 12 | Matrix Chain Multiplication | M3 | Cheapest order of logistics operations |
| 13 | Optimal BST | M3 | Frequently accessed shipment keys |
| 14 | Ford-Fulkerson | M4 | Warehouse → destination shipment flow |
| 15 | Edmonds-Karp | M4 | Capacity-independent max flow |
| 16 | Dinic | M4 | Large distribution networks |
| 17 | Bipartite Matching | M4 | Driver-to-task assignment |
| 18 | Minimum Cut | M4 | Bottleneck / capacity identification |
| 19 | SAT / 3-SAT | M5 | Delivery-constraint satisfiability |
| 20 | 3-SAT → CLIQUE | M5 | NP-hardness reduction |
| 21 | CLIQUE → INDEPENDENT-SET | M5 | Reduction via graph complement |
| 22 | INDEPENDENT-SET → VERTEX-COVER | M5 | Reduction via set complement |
| 23 | Vertex-Cover 2-Approximation | M5 | Provably-bounded conflict-location selection |
| 24 | Randomized QuickSort | M6 | Shipment ranking |
| 25 | Reservoir Sampling | M6 | Continuous vehicle-event streams |
| 26 | Miller-Rabin | M6 | Large-number primality testing |
| 27 | Blelloch Scan | M6 | Cumulative shipment statistics |
| 28 | Parallel Reduce | M6 | Logistics-data aggregation (measured, real threads) |
| 29 | Brent's Theorem | M6 | Evaluating parallel-processing speed-up |

## 7. Complexity table

| Algorithm | Time | Space |
|---|---|---|
| KMP | O(n + m) | O(m) |
| Z-Function | O(n) | O(n) |
| Rabin-Karp | O(n + m) expected | O(1) |
| Aho-Corasick | O(n + m + z) | O(m·σ) |
| Suffix Array (sort-based) | O(n log n)–O(n² log n) build, O(m log n) query | O(n) |
| SA-IS | O(n) | O(n) |
| LCP / Kasai | O(n) | O(n) |
| Suffix Tree (here) | O(n²) build (Ukkonen: O(n)), O(m) query | O(n) |
| Levenshtein / Damerau | O(m·n) | O(m·n) |
| Bitmask DP (TSP) | O(2ⁿ·n²) | O(2ⁿ·n) |
| Matrix Chain Multiplication | O(n³) | O(n²) |
| Optimal BST | O(n³) | O(n²) |
| Ford-Fulkerson | O(E·\|f*\|) | O(V + E) |
| Edmonds-Karp | O(V·E²) | O(V + E) |
| Dinic | O(V²·E) | O(V + E) |
| Bipartite Matching | O(V·E) | O(V) |
| Minimum Cut | O(V + E) after max flow | O(V) |
| SAT (brute force) | O(2ⁿ·m) | O(n) |
| 3-SAT → CLIQUE (reduction) | O(m²) | O(m²) |
| Vertex-Cover 2-Approximation | O(V + E) | O(V) |
| Randomized QuickSort | Expected O(n log n) | O(log n) |
| Reservoir Sampling | O(N) | O(k) |
| Miller-Rabin | O(k·log n) modular multiplications | O(1) |
| Blelloch Scan | Work O(n), span O(log n) | O(n) |
| Parallel Reduce | Work O(n), span O(n/P + log P) | O(P) |
| Brent's Theorem | T_P = O(W/P + S) | — |

## 8. Demo instructions

1. Launch with `mvn clean javafx:run`.
2. Open **Demo Mode** in the sidebar.
3. Click **"Run this step"** on each of the ten prepared algorithms in order; each click navigates to the
   right lab, loads its demo data and plays the animation automatically:
   `KMP → Aho-Corasick → Levenshtein → Bitmask DP → Dinic → Bipartite Matching → Minimum Cut →
   Vertex Cover 2-Approximation → Randomized QuickSort → Parallel Reduce`.
4. Alternatively, open **Algorithm Explorer** to jump to *any* of the 29 algorithms directly, with full
   problem/input/output/complexity information on each card.
5. Visit **Warehouses** and press **"Analyse flow"** to show Dinic max-flow / min-cut on the real
   inter-warehouse network live.
6. Visit **Shipments** and search — the status bar shows the exact number of comparisons the chosen
   algorithm (KMP or Rabin-Karp) performed.

## 9. Screenshots

_(Add your own screenshots here before submission — e.g. Dashboard, an algorithm panel mid-animation, the
Network Flow Simulator, and Algorithm Explorer.)_

- `docs/screenshot-dashboard.png`
- `docs/screenshot-string-intelligence.png`
- `docs/screenshot-network-flow.png`
- `docs/screenshot-algorithm-explorer.png`

## 10. Team contribution template

| Member | Modules / areas | Key files |
|---|---|---|
| _Name 1_ | M1 String Algorithms, M2 Suffix Structures | `algorithm/string/`, `algorithm/suffix/`, `datastructure/trie/`, `datastructure/suffix/` |
| _Name 2_ | M3 Dynamic Programming, M5 NP-Completeness | `algorithm/dp/`, `algorithm/np/`, `algorithm/approximation/` |
| _Name 3_ | M4 Network Flow, Warehouses/Shipments UI | `algorithm/flow/`, `datastructure/graph/`, `ui/page/WarehousesPage.java`, `ui/page/ShipmentsPage.java` |
| _Name 4_ | M6 Randomized & Parallel, visualization engine | `algorithm/randomized/`, `algorithm/parallel/`, `visualization/` |
| _Name 5_ | UI framework, theming, dashboard, analytics, testing | `ui/AlgorithmPanel.java`, `ui/AppShell.java`, `util/ThemeManager.java`, `ui/page/DashboardPage.java`, `ui/page/AnalyticsPage.java`, `src/test/` |

_(Fill in real names before submission; adjust the split to match actual work.)_

## 11. Future enhancements

- Ukkonen's linear-time suffix-tree construction (currently O(n²) educational version).
- Persist vehicles/drivers/warehouses (currently only shipments are saved) with the same CSV approach.
- Export the Network Flow Simulator's custom network as JSON so classroom examples can be shared.
- Multi-source / multi-sink flow and a full vehicle-routing (VRP) extension of the Bitmask-DP page.
- Knuth's O(n²) optimisation for the Optimal-BST DP.

## 12. Possible faculty questions and answers

**Q: Why is the suffix tree built in O(n²) instead of Ukkonen's O(n)?**
A: The page states this explicitly. Ukkonen's algorithm is substantially more complex to visualise step by
step (implicit suffixes, active points, suffix links built on the fly); the O(n²) naive-insertion
construction produces the *same final tree* and the *same O(m) query time*, which is what matters for the
indexing use case, while staying easy to animate correctly for a classroom demonstration.

**Q: How do you know your SA-IS implementation is actually correct and not a relabelled sort?**
A: `SaIsTest.saIsMatchesSortBasedConstructionOnRandomStrings` cross-checks SA-IS against the independent
sort-based `SuffixArray.build` on 300 random strings of varying alphabet size, including edge cases (empty,
single character, all-equal characters).

**Q: Why does Ford-Fulkerson sometimes run more augmenting paths than Edmonds-Karp on the same graph?**
A: Ford-Fulkerson accepts *any* augmenting path (found here by DFS), which can pick long, inefficient paths
first; Edmonds-Karp always augments along a *shortest* path (BFS), which provably bounds the number of
augmentations by O(V·E) regardless of edge capacities. The Network Flow Simulator's comparison line shows
the measured augmentation counts side by side on the same input.

**Q: Your 3-SAT → CLIQUE reduction — why does a k-clique correspond exactly to a satisfying assignment?**
A: One vertex per literal occurrence, one column per clause; two vertices are joined iff they are in
different clauses and are not directly contradictory (x vs ¬x). Any clique can use at most one vertex per
clause (column), so a clique of size k = #clauses picks exactly one literal per clause, and because no two
chosen literals contradict each other, setting all chosen literals to true is a consistent, satisfying
assignment. `NpCompletenessTest.satisfiableIffCliqueOfSizeKExists` verifies this equivalence against direct
SAT brute force on 300 random 3-CNF formulas.

**Q: Why is Miller-Rabin only "probably prime", and how confident can we be?**
A: A composite number passes a single random round with probability at most 1/4 (at most 1/4 of all bases
are "liars"), so after k independent rounds the error probability is at most 4⁻ᵏ. With the default 5
rounds that is ≤ 1/1024 ≈ 0.1%; the UI shows this bound explicitly. A prime *never* fails any round, so
"composite" answers (a witness was found) are always certain.

**Q: How do you guarantee the Vertex-Cover approximation never exceeds twice the optimum?**
A: The algorithm only ever picks edges whose *both* endpoints are still uncovered, so the picked edges form
a matching M (no two share a vertex). Any vertex cover must include at least one endpoint of every edge in
M, hence OPT ≥ |M|. The algorithm's cover has exactly 2|M| vertices, so |cover| = 2|M| ≤ 2·OPT. The page
verifies this ratio against an exact exponential solver on every run.

**Q: Are the "measured" parallel-reduce timings real, or simulated for the demo?**
A: They are genuinely measured on the machine running the app (`System.nanoTime()`, median of 5 runs after
2 warm-up runs, using a real `ExecutorService`). The result panel explicitly notes that very small arrays
can show *negative* speed-up because thread hand-off cost dominates — a true and pedagogically important
observation, not something a canned demo would show.

**Q: Why does the Bitmask-DP TSP page cap the number of stops at 7?**
A: Held-Karp's O(2ⁿ·n²) time and O(2ⁿ·n) space genuinely explode: at n = 16 the DP table alone has roughly
one million entries. Capping at 7 keeps the run instantaneous and the DP-table visualisation legible while
still demonstrating the real exponential technique (not a shortcut); the code itself supports up to 16
stops (`BitmaskTsp` throws only above that).
