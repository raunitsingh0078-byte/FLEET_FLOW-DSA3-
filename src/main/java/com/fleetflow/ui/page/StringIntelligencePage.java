package com.fleetflow.ui.page;

import com.fleetflow.ui.TabbedAlgorithmPage;
import com.fleetflow.ui.panel.string.AhoCorasickPanel;
import com.fleetflow.ui.panel.string.KmpPanel;
import com.fleetflow.ui.panel.string.RabinKarpPanel;
import com.fleetflow.ui.panel.string.ZFunctionPanel;

/** M1 – String Intelligence: KMP, Z-function, Rabin-Karp and Aho-Corasick. */
public final class StringIntelligencePage extends TabbedAlgorithmPage {

    public StringIntelligencePage() {
        addPanel("KMP", new KmpPanel(), "KMP");
        addPanel("Z-Function", new ZFunctionPanel(), "Z-Function");
        addPanel("Rabin-Karp", new RabinKarpPanel(), "Rabin-Karp");
        addPanel("Aho-Corasick", new AhoCorasickPanel(), "Aho-Corasick");
    }

    @Override
    public String title() {
        return "String Intelligence";
    }

    @Override
    public String subtitle() {
        return "M1 · Pattern searching over shipment IDs, package codes and delivery-status logs";
    }
}
