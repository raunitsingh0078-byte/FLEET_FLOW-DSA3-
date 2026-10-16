package com.fleetflow.ui.page;

import com.fleetflow.ui.TabbedAlgorithmPage;
import com.fleetflow.ui.panel.suffix.LcpPanel;
import com.fleetflow.ui.panel.suffix.SaIsPanel;
import com.fleetflow.ui.panel.suffix.SuffixArrayPanel;
import com.fleetflow.ui.panel.suffix.SuffixTreePanel;

/** M2 – Document Intelligence: Suffix Array, SA-IS, LCP/Kasai and Suffix Tree. */
public final class DocumentIntelligencePage extends TabbedAlgorithmPage {

    public DocumentIntelligencePage() {
        addPanel("Suffix Array", new SuffixArrayPanel(), "Suffix Array");
        addPanel("SA-IS", new SaIsPanel(), "SA-IS");
        addPanel("LCP / Kasai", new LcpPanel(), "LCP / Kasai");
        addPanel("Suffix Tree", new SuffixTreePanel(), "Suffix Tree");
    }

    @Override
    public String title() {
        return "Document Intelligence";
    }

    @Override
    public String subtitle() {
        return "M2 · Suffix structures for indexing shipment documents and detecting repeated addresses";
    }
}
