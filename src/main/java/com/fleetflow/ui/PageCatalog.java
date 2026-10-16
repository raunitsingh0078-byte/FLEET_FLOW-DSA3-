package com.fleetflow.ui;

import com.fleetflow.controller.NavigationController;
import com.fleetflow.controller.NavigationController.Route;
import com.fleetflow.ui.page.AboutPage;
import com.fleetflow.ui.page.AlgorithmExplorerPage;
import com.fleetflow.ui.page.AnalyticsPage;
import com.fleetflow.ui.page.ComplexityLabPage;
import com.fleetflow.ui.page.DashboardPage;
import com.fleetflow.ui.page.DemoModePage;
import com.fleetflow.ui.page.DocumentIntelligencePage;
import com.fleetflow.ui.page.NetworkFlowPage;
import com.fleetflow.ui.page.OptimizationLabPage;
import com.fleetflow.ui.page.ParallelRandomizedPage;
import com.fleetflow.ui.page.ShipmentsPage;
import com.fleetflow.ui.page.StringIntelligencePage;
import com.fleetflow.ui.page.VehiclesDriversPage;
import com.fleetflow.ui.page.WarehousesPage;

import java.util.List;

/** The single place where sidebar routes are registered, in the order the project brief specifies. */
public final class PageCatalog {

    private PageCatalog() {
    }

    public static List<Route> routes(NavigationController navigation) {
        return List.of(
                new Route("🏠", "Dashboard", DashboardPage::new),
                new Route("🚚", "Shipments", ShipmentsPage::new),
                new Route("🧑‍✈️", "Vehicles & Drivers", VehiclesDriversPage::new),
                new Route("🏭", "Warehouses", WarehousesPage::new),
                new Route("🔤", "String Intelligence", StringIntelligencePage::new),
                new Route("📄", "Document Intelligence", DocumentIntelligencePage::new),
                new Route("🧠", "Optimization Lab", OptimizationLabPage::new),
                new Route("🌐", "Network Flow", NetworkFlowPage::new),
                new Route("🧩", "Complexity Lab", ComplexityLabPage::new),
                new Route("⚡", "Parallel & Randomized", ParallelRandomizedPage::new),
                new Route("📊", "Analytics", AnalyticsPage::new),
                new Route("🧭", "Algorithm Explorer", () -> new AlgorithmExplorerPage(navigation)),
                new Route("🎯", "Demo Mode", () -> new DemoModePage(navigation)),
                new Route("ℹ️", "About Project", AboutPage::new));
    }
}
