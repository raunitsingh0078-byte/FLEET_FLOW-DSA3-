package com.fleetflow.ui.page;

import com.fleetflow.ui.TabbedAlgorithmPage;
import com.fleetflow.ui.panel.dp.BitmaskTspPanel;
import com.fleetflow.ui.panel.dp.EditDistancePanel;
import com.fleetflow.ui.panel.dp.MatrixChainPanel;
import com.fleetflow.ui.panel.dp.OptimalBstPanel;

/** M3 – Optimization Lab: advanced dynamic programming. */
public final class OptimizationLabPage extends TabbedAlgorithmPage {

    public OptimizationLabPage() {
        addPanel("Levenshtein / Damerau", new EditDistancePanel(), "Levenshtein", "Damerau-Levenshtein");
        addPanel("Bitmask DP", new BitmaskTspPanel(), "Bitmask DP");
        addPanel("Matrix Chain", new MatrixChainPanel(), "Matrix Chain Multiplication");
        addPanel("Optimal BST", new OptimalBstPanel(), "Optimal BST");
    }

    @Override
    public String title() {
        return "Optimization Lab";
    }

    @Override
    public String subtitle() {
        return "M3 · Advanced dynamic programming for address correction, routing and operation planning";
    }
}
