package com.fleetflow.ui.page;

import com.fleetflow.ui.TabbedAlgorithmPage;
import com.fleetflow.ui.panel.parallel.BlellochScanPanel;
import com.fleetflow.ui.panel.parallel.BrentPanel;
import com.fleetflow.ui.panel.parallel.MillerRabinPanel;
import com.fleetflow.ui.panel.parallel.ParallelReducePanel;
import com.fleetflow.ui.panel.parallel.RandomizedQuickSortPanel;
import com.fleetflow.ui.panel.parallel.ReservoirSamplingPanel;

/** M6 – Parallel &amp; Randomized Lab. */
public final class ParallelRandomizedPage extends TabbedAlgorithmPage {

    public ParallelRandomizedPage() {
        addPanel("Randomized QuickSort", new RandomizedQuickSortPanel(), "Randomized QuickSort");
        addPanel("Reservoir Sampling", new ReservoirSamplingPanel(), "Reservoir Sampling");
        addPanel("Miller-Rabin", new MillerRabinPanel(), "Miller-Rabin");
        addPanel("Blelloch Scan", new BlellochScanPanel(), "Blelloch Scan");
        addPanel("Parallel Reduce", new ParallelReducePanel(), "Parallel Reduce");
        addPanel("Brent's Theorem", new BrentPanel(), "Brent's Theorem");
    }

    @Override
    public String title() {
        return "Parallel & Randomized Lab";
    }

    @Override
    public String subtitle() {
        return "M6 · Randomized algorithms, parallel prefix sums, reductions and Brent's bound";
    }
}
