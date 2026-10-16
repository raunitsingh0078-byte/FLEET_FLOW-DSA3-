package com.fleetflow.ui;

import com.fleetflow.util.AlgorithmStats;
import com.fleetflow.util.Async;
import com.fleetflow.util.Dialogs;
import com.fleetflow.util.ThemeManager;
import com.fleetflow.util.ValidationException;
import com.fleetflow.visualization.StepPlayer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Supplier;

/**
 * Common layout of every algorithm screen:
 * title, description, input panel with demo data, run/step/pause/reset controls,
 * visualization, step explanation and a result panel.
 *
 * <p>Subclasses read and validate their inputs on the UI thread in {@link #prepare()} and return the
 * pure computation as a {@link Supplier}; the computation then runs on a worker thread so the UI never freezes.</p>
 */
public abstract class AlgorithmPanel extends VBox {

    private record Timed(Execution execution, long nanos) {
    }

    private final StepPlayer player = new StepPlayer(this::showFrame);
    private final Label stepCounter = Ui.label("Step 0 / 0", "muted");
    private final Label explanation = Ui.label("Press Run to execute the algorithm.", "explanation");
    private final Label resultValue = valueLabel();
    private final Label complexityValue = valueLabel();
    private final Label comparisonsValue = valueLabel();
    private final Label iterationsValue = valueLabel();
    private final Label infoValue = valueLabel();
    private final Button runButton = Ui.button("▶  Run", "button-primary", "Run the algorithm and play the animation");
    private final Button stepButton = Ui.button("Step ▸", "button-secondary", "Advance by one step");
    private final Button backButton = Ui.button("◂ Back", "button-secondary", "Go back one step");
    private final Button pauseButton = Ui.button("⏸ Pause", "button-secondary", "Pause the animation");
    private final Button resetButton = Ui.button("↺ Reset", "button-secondary", "Go back to the first step");
    private Execution execution;
    private String title = "";

    protected AlgorithmPanel() {
        getStyleClass().add("algo-panel");
        ThemeManager.addListener(this::refreshVisualization);
        setSpacing(12);
        setPadding(new Insets(4, 2, 16, 2));
    }

    /**
     * Assembles the standard layout; called once at the end of the subclass constructor.
     *
     * @param inputs        node holding the input fields
     * @param visualization node that draws the animation
     */
    protected final void assemble(String title, String description, Node inputs, Node visualization) {
        this.title = title;
        Label heading = Ui.label(title, "algo-title");
        Label desc = Ui.label(description, "muted");
        desc.setWrapText(true);
        desc.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        Button demo = Ui.button("✦ Generate Demo Data", "button-accent", "Fill the inputs with prepared logistics data");
        demo.setOnAction(e -> {
            loadDemo();
            resetPlayback();
        });
        VBox inputCard = Ui.card(Ui.label("INPUT", "section-title"), inputs, demo);

        Slider speed = new Slider(0.25, 4, 1);
        speed.setPrefWidth(120);
        speed.valueProperty().addListener((obs, old, value) -> player.setSpeed(value.doubleValue()));
        runButton.setOnAction(e -> run(true));
        stepButton.setOnAction(e -> {
            if (execution == null) {
                run(false);
            } else {
                player.next();
            }
        });
        backButton.setOnAction(e -> player.previous());
        pauseButton.setOnAction(e -> player.pause());
        resetButton.setOnAction(e -> player.reset());
        Label speedLabel = Ui.label("Speed", "muted");
        var controls = Ui.row(8, runButton, backButton, stepButton, pauseButton, resetButton, speedLabel, speed, stepCounter);
        controls.setAlignment(Pos.CENTER_LEFT);

        VBox vizCard = Ui.card(Ui.label("VISUALIZATION", "section-title"), visualization, explanation);
        explanation.setWrapText(true);
        VBox.setVgrow(visualization, Priority.ALWAYS);

        GridPane results = new GridPane();
        results.setHgap(16);
        results.setVgap(6);
        addResultRow(results, 0, "Result", resultValue);
        addResultRow(results, 1, "Complexity", complexityValue);
        addResultRow(results, 2, "Comparisons", comparisonsValue);
        addResultRow(results, 3, "Iterations", iterationsValue);
        addResultRow(results, 4, "Execution info", infoValue);
        VBox resultCard = Ui.card(Ui.label("RESULT", "section-title"), results);

        getChildren().setAll(heading, desc, inputCard, controls, vizCard, resultCard);
    }

    private static Label valueLabel() {
        Label label = Ui.label("—", "result-value");
        label.setWrapText(true);
        label.setMaxWidth(900);
        return label;
    }

    private static void addResultRow(GridPane grid, int row, String key, Label value) {
        Label k = Ui.label(key + ":", "kv-key");
        k.setMinWidth(110);
        GridPane.setValignment(k, javafx.geometry.VPos.TOP);
        grid.add(k, 0, row);
        grid.add(value, 1, row);
    }

    /**
     * Reads and validates the inputs (on the UI thread).
     *
     * @return the computation to run on a worker thread
     * @throws ValidationException with a user-friendly message if the input is unusable
     */
    protected abstract Supplier<Execution> prepare() throws ValidationException;

    /** Fills the input controls with prepared demo data. */
    protected abstract void loadDemo();

    /** Loads the prepared demo data and runs the algorithm with animation (used by Demo Mode and Explorer). */
    public final void runDemo() {
        loadDemo();
        resetPlayback();
        run(true);
    }

    /** Lets panels that cover several algorithms (e.g. a combo box) switch to the requested one. */
    public void prefer(String algorithm) {
    }

    public final String title() {
        return title;
    }

    /** Re-draws the current frame (for display options that do not need a new run). */
    protected final void refreshVisualization() {
        player.refresh();
    }

    private void run(boolean autoplay) {
        Supplier<Execution> work;
        try {
            work = prepare();
        } catch (ValidationException ex) {
            Dialogs.warning("Invalid input", ex.getMessage());
            return;
        }
        setBusy(true);
        Async.run(() -> {
            long start = System.nanoTime();
            Execution result = work.get();
            return new Timed(result, System.nanoTime() - start);
        }, timed -> {
            setBusy(false);
            show(timed, autoplay);
        }, error -> {
            setBusy(false);
            Dialogs.error("The algorithm could not be executed", String.valueOf(error.getMessage()));
        });
    }

    private void show(Timed timed, boolean autoplay) {
        execution = timed.execution();
        AlgorithmStats.record(title, timed.nanos(), execution.comparisons());
        resultValue.setText(execution.result());
        complexityValue.setText(execution.complexity());
        comparisonsValue.setText(execution.comparisons() < 0 ? "n/a" : String.valueOf(execution.comparisons()));
        iterationsValue.setText(String.valueOf(execution.iterations()));
        infoValue.setText(execution.info() + String.format("%nMeasured run time: %.3f ms", timed.nanos() / 1_000_000.0));
        player.load(execution.frames());
        if (execution.frames() == 0) {
            explanation.setText("Nothing to animate for this input.");
            stepCounter.setText("Step 0 / 0");
        } else if (autoplay) {
            player.play();
        }
    }

    private void showFrame(int index) {
        if (execution == null) {
            return;
        }
        execution.renderer().accept(index);
        explanation.setText(execution.narrator().apply(index));
        stepCounter.setText("Step " + (index + 1) + " / " + execution.frames());
    }

    private void resetPlayback() {
        execution = null;
        player.load(0);
        stepCounter.setText("Step 0 / 0");
        explanation.setText("Demo data loaded. Press Run to execute the algorithm.");
    }

    private void setBusy(boolean busy) {
        runButton.setDisable(busy);
        stepButton.setDisable(busy);
        runButton.setText(busy ? "Running…" : "▶  Run");
    }
}
