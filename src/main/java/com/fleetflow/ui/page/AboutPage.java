package com.fleetflow.ui.page;

import com.fleetflow.ui.AlgorithmCatalog;
import com.fleetflow.ui.Page;
import com.fleetflow.ui.Ui;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** About Project: fixed content describing FleetFlow for the DSA-3 PBL submission. */
public final class AboutPage implements Page {

    @Override
    public String title() {
        return "About Project";
    }

    @Override
    public String subtitle() {
        return "FleetFlow — DSA-3 Problem-Based Learning project";
    }

    @Override
    public Node content() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(4, 24, 24, 24));

        root.getChildren().add(Ui.card(
                Ui.label("FLEETFLOW", "page-title"),
                Ui.label("Intelligent Logistics Management Platform", "muted"),
                wrapped("FleetFlow models a logistics-management platform: delivery vehicles and drivers, warehouses and "
                        + "distribution centres, packages and customer orders, delivery records and shipment documents, "
                        + "vehicle and shipment events, and resource and delivery constraints. It supports fast shipment search, "
                        + "document indexing, address correction, resource allocation, delivery planning, shipment-flow "
                        + "optimisation, driver assignment, logistics analytics and algorithm visualisation.")));

        VBox modules = Ui.card(Ui.label("MODULES", "section-title"));
        modules.getChildren().add(wrapped("M1  String Algorithms — KMP, Z-Function, Rabin-Karp, Aho-Corasick (String Intelligence)"));
        modules.getChildren().add(wrapped("M2  Suffix Structures — Suffix Array, SA-IS, LCP/Kasai, Suffix Tree (Document Intelligence)"));
        modules.getChildren().add(wrapped("M3  Advanced Dynamic Programming — Levenshtein/Damerau, Bitmask DP, Matrix Chain, Optimal BST (Optimization Lab)"));
        modules.getChildren().add(wrapped("M4  Network Flow — Ford-Fulkerson, Edmonds-Karp, Dinic, Bipartite Matching, Minimum Cut (Network Flow Simulator)"));
        modules.getChildren().add(wrapped("M5  NP-Completeness & Approximation — SAT/3-SAT, 3-SAT→CLIQUE, CLIQUE→IND.SET, IND.SET→VERTEX-COVER, Vertex-Cover 2-Approximation (Complexity Lab)"));
        modules.getChildren().add(wrapped("M6  Randomized & Parallel Algorithms — Randomized QuickSort, Reservoir Sampling, Miller-Rabin, Blelloch Scan, Parallel Reduce, Brent's Theorem (Parallel & Randomized Lab)"));
        root.getChildren().add(modules);

        root.getChildren().add(Ui.card(Ui.label("HOW DSA SOLVES REAL LOGISTICS PROBLEMS", "section-title"),
                wrapped("Every module in FleetFlow answers a concrete operational question with a specific data structure or "
                        + "algorithm rather than a generic database query:"),
                wrapped("• Finding a shipment ID or keyword among thousands of records needs sub-linear, provably-correct pattern "
                        + "matching (M1) instead of naive string scanning."),
                wrapped("• Indexing free-text delivery documents for instant substring search needs a structure built once and "
                        + "queried in O(m) (M2), not a re-scan on every query."),
                wrapped("• Address typos and route-planning are optimisation problems with overlapping subproblems, solved once "
                        + "by dynamic programming instead of exponential brute force (M3)."),
                wrapped("• How many pallets can move from a warehouse to a destination, and where the bottleneck lane is, is "
                        + "exactly the max-flow / min-cut theorem (M4)."),
                wrapped("• Some constraints (driver/vehicle/order compatibility) are inherently exponential to solve exactly; "
                        + "understanding NP-completeness explains why only approximate or restricted solutions scale, and the "
                        + "2-approximation for vertex cover still gives a usable, provably-bounded answer (M5)."),
                wrapped("• At real-world data volumes, a single CPU core is not enough: randomised algorithms give predictable "
                        + "average-case performance and parallel primitives (scan, reduce) turn a data-centre's many cores into "
                        + "actual throughput, with Brent's theorem explaining exactly how many cores are worth using (M6).")));

        root.getChildren().add(Ui.card(Ui.label("PROJECT INFO", "section-title"),
                wrapped("Technology: Java 17, JavaFX 17, Maven, JUnit 5. No Spring Boot, no web framework — a native desktop application."),
                wrapped("Every core DSA algorithm is implemented by hand in algorithm/ and datastructure/ (never a one-line library call); "
                        + AlgorithmCatalog.entries().size() + " algorithms are catalogued in the Algorithm Explorer, each runnable and visualised."),
                wrapped("Screenshots: see the README's Screenshots section (placeholders — add your own captures before submission)."),
                wrapped("Team contribution: fill in the README's contribution template with each member's module(s).")));

        return root;
    }

    private static Label wrapped(String text) {
        Label l = Ui.label(text, "muted");
        l.setWrapText(true);
        return l;
    }
}
