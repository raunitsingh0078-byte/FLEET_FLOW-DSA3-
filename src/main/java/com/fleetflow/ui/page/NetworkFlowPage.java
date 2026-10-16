package com.fleetflow.ui.page;

import com.fleetflow.ui.TabbedAlgorithmPage;
import com.fleetflow.ui.panel.flow.BipartiteMatchingPanel;
import com.fleetflow.ui.panel.flow.FlowNetworkPanel;

/** M4 – Network Flow Simulator. */
public final class NetworkFlowPage extends TabbedAlgorithmPage {

    public NetworkFlowPage() {
        addPanel("Max Flow & Min Cut", new FlowNetworkPanel(), "Ford-Fulkerson", "Edmonds-Karp", "Dinic", "Minimum Cut");
        addPanel("Driver Assignment (Matching)", new BipartiteMatchingPanel(), "Bipartite Matching");
    }

    @Override
    public String title() {
        return "Network Flow Simulator";
    }

    @Override
    public String subtitle() {
        return "M4 · Warehouse-to-destination flow, bottleneck detection and driver assignment";
    }
}
