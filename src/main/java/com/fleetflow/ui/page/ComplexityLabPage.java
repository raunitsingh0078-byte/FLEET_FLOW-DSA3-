package com.fleetflow.ui.page;

import com.fleetflow.ui.TabbedAlgorithmPage;
import com.fleetflow.ui.Ui;
import com.fleetflow.ui.panel.np.CliqueIndependentSetPanel;
import com.fleetflow.ui.panel.np.IndependentSetVertexCoverPanel;
import com.fleetflow.ui.panel.np.SatPanel;
import com.fleetflow.ui.panel.np.ThreeSatCliquePanel;
import com.fleetflow.ui.panel.np.VertexCoverPanel;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** M5 – Complexity &amp; Approximation Lab. */
public final class ComplexityLabPage extends TabbedAlgorithmPage {

    public ComplexityLabPage() {
        addPanel("SAT / 3-SAT", new SatPanel(), "SAT / 3-SAT");
        addPanel("3-SAT → CLIQUE", new ThreeSatCliquePanel(), "3-SAT to CLIQUE");
        addPanel("CLIQUE → IND. SET", new CliqueIndependentSetPanel(), "CLIQUE to INDEPENDENT-SET");
        addPanel("IND. SET → VERTEX COVER", new IndependentSetVertexCoverPanel(), "INDEPENDENT-SET to VERTEX-COVER");
        addPanel("Vertex Cover 2-Approx.", new VertexCoverPanel(), "Vertex Cover 2-Approximation");
    }

    @Override
    public String title() {
        return "Complexity & Approximation Lab";
    }

    @Override
    public String subtitle() {
        return "M5 · NP-completeness reductions and approximation algorithms";
    }

    @Override
    public Node content() {
        VBox root = (VBox) super.content();
        Label banner = Ui.label("ⓘ  Educational demonstration – exponential problems are restricted to small input sizes.", "chip");
        banner.setWrapText(true);
        root.getChildren().add(0, banner);
        root.setSpacing(10);
        return root;
    }
}
