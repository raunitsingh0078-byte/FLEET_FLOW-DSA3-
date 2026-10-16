package com.fleetflow.ui;

import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;

import java.util.Map;

/** Factory for the standard, consistently configured charts of the dashboard and analytics pages. */
public final class Charts {

    private Charts() {
    }

    /** Single-series bar chart. */
    public static BarChart<String, Number> bar(String title, String yLabel, String series, Map<String, Number> data, boolean wholeNumbers) {
        return grouped(title, yLabel, Map.of(series, data), wholeNumbers, false);
    }

    /**
     * Multi-series bar chart; series appear in the iteration order of the given map.
     *
     * @param legend whether to show the legend (always useful with more than one series)
     */
    public static BarChart<String, Number> grouped(String title, String yLabel, Map<String, Map<String, Number>> series, boolean wholeNumbers, boolean legend) {
        NumberAxis y = new NumberAxis();
        y.setLabel(yLabel);
        y.setMinorTickVisible(false);
        if (wholeNumbers) {
            y.setTickUnit(1);
        }
        BarChart<String, Number> chart = new BarChart<>(new CategoryAxis(), y);
        chart.setTitle(title);
        chart.setLegendVisible(legend || series.size() > 1);
        chart.setAnimated(false);
        series.forEach((name, data) -> {
            XYChart.Series<String, Number> s = new XYChart.Series<>();
            s.setName(name);
            data.forEach((k, v) -> s.getData().add(new XYChart.Data<>(k, v)));
            chart.getData().add(s);
        });
        return chart;
    }

    public static PieChart pie(String title, Map<String, ? extends Number> data) {
        PieChart chart = new PieChart();
        data.forEach((k, v) -> chart.getData().add(new PieChart.Data(k, v.doubleValue())));
        chart.setTitle(title);
        chart.setLabelsVisible(false);
        chart.setAnimated(false);
        return chart;
    }
}
