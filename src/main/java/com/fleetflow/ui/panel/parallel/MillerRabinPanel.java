package com.fleetflow.ui.panel.parallel;

import com.fleetflow.algorithm.randomized.MillerRabin;
import com.fleetflow.ui.AlgorithmPanel;
import com.fleetflow.ui.Execution;
import com.fleetflow.ui.Ui;
import com.fleetflow.util.ValidationException;
import com.fleetflow.visualization.CellState;
import com.fleetflow.visualization.RowTableVisualizer;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

/** Miller-Rabin page: probabilistic primality test for large numbers (e.g. checksum / key-generation style computations). */
public final class MillerRabinPanel extends AlgorithmPanel {

    private final TextField numberField = new TextField();
    private final TextField roundsField = new TextField("5");
    private final TextField seedField = new TextField("11");
    private final RowTableVisualizer table = new RowTableVisualizer("Round", "Base a", "Sequence a^d, a^2d, a^4d … (mod n)", "Verdict");

    public MillerRabinPanel() {
        numberField.setPromptText("Integer to test, up to " + MillerRabin.MAX_N);
        HBox.setHgrow(numberField, Priority.ALWAYS);
        roundsField.setPrefColumnCount(4);
        seedField.setPrefColumnCount(6);
        table.setPrefHeight(230);
        assemble("Miller-Rabin – Probabilistic Primality Test",
                "Real problem: large-number computations (hash sizes, keys, identifiers) need big primes, and trial division is hopeless for 19-digit numbers. "
                        + "Write n − 1 = 2^s·d. For a random base a, a prime always passes; a composite passes only with probability ≤ 1/4. "
                        + "After k independent rounds a composite survives with probability ≤ 4^−k – so the answer is 'composite' (certain) or 'probably prime' (error ≤ 4^−k).",
                new HBox(12, Ui.label("Number n", "kv-key"), numberField, Ui.label("Rounds k", "kv-key"), roundsField, Ui.label("Seed", "kv-key"), seedField),
                table);
        loadDemo();
    }

    @Override
    protected void loadDemo() {
        numberField.setText("2305843009213693951");
        roundsField.setText("5");
        seedField.setText("11");
    }

    @Override
    protected Supplier<Execution> prepare() throws ValidationException {
        long n;
        int rounds;
        long seed;
        try {
            n = Long.parseLong(numberField.getText().trim());
        } catch (NumberFormatException | NullPointerException e) {
            throw new ValidationException("Please enter a whole number (digits only, at most 18–19 digits).");
        }
        if (n < 0 || n > MillerRabin.MAX_N) {
            throw new ValidationException("The number must be between 0 and " + MillerRabin.MAX_N + ".");
        }
        try {
            rounds = Integer.parseInt(roundsField.getText().trim());
            seed = Long.parseLong(seedField.getText().trim());
        } catch (NumberFormatException | NullPointerException e) {
            throw new ValidationException("Rounds and seed must be whole numbers.");
        }
        if (rounds < 1 || rounds > 40) {
            throw new ValidationException("The number of rounds must be between 1 and 40.");
        }
        long number = n;
        return () -> execute(number, rounds, seed);
    }

    private Execution execute(long n, int rounds, long seed) {
        MillerRabin.Result r = MillerRabin.test(n, rounds, new Random(seed));
        boolean truth = MillerRabin.isPrime(n);
        String verdict;
        if (r.reason() != null) {
            verdict = (r.probablyPrime() ? "PRIME" : "COMPOSITE") + " – " + r.reason() + ".";
        } else if (r.probablyPrime()) {
            BigDecimal bound = BigDecimal.ONE.divide(new BigDecimal(4).pow(rounds), new MathContext(3));
            verdict = "PROBABLY PRIME after " + rounds + " passing rounds. If n were composite the chance of this is at most 4^-" + rounds + " = " + bound.toEngineeringString() + ".";
        } else {
            verdict = "COMPOSITE with certainty: base " + r.rounds().get(r.rounds().size() - 1).base() + " is a witness (found in round " + r.rounds().size() + ").";
        }
        String result = verdict + "\nCross-check with the deterministic 64-bit variant (12 fixed bases): n is " + (truth ? "prime" : "composite")
                + (truth == r.probablyPrime() ? " ✓ agrees." : " ✗ differs (a probabilistic false positive).");
        String complexity = "O(k · log n) modular multiplications (here O(k · log² n) bit steps)   |   Space O(1)";
        String info = r.reason() != null ? "No rounds were needed." : "n − 1 = 2^" + r.s() + " · " + r.d() + "; " + r.rounds().size() + " round(s) executed.";
        int frames = 1 + r.rounds().size();
        return new Execution(frames, f -> draw(r, f), f -> narrate(r, f), result, complexity, -1, r.rounds().size(), info);
    }

    private void draw(MillerRabin.Result r, int frame) {
        List<String[]> rows = new ArrayList<>();
        Map<Integer, CellState> states = new HashMap<>();
        for (int i = 0; i < frame; i++) {
            MillerRabin.Round round = r.rounds().get(i);
            StringBuilder seq = new StringBuilder();
            for (long x : round.sequence()) seq.append(seq.length() == 0 ? "" : " → ").append(x);
            rows.add(new String[]{String.valueOf(i + 1), String.valueOf(round.base()), seq.toString(),
                    round.witness() ? "WITNESS – composite" : "passes (probably prime)"});
            states.put(i, round.witness() ? CellState.MISMATCH : CellState.DONE);
        }
        if (!rows.isEmpty()) states.put(rows.size() - 1, r.rounds().get(rows.size() - 1).witness() ? CellState.MISMATCH : CellState.ACTIVE);
        table.show(rows, states);
    }

    private static String narrate(MillerRabin.Result r, int frame) {
        if (frame == 0) {
            return r.reason() != null ? "Trivial case: " + r.reason() + ". No random bases are needed."
                    : "Decompose n − 1 = " + (r.n() - 1) + " = 2^" + r.s() + " · " + r.d() + " (d odd). Each round picks a random base a and examines a^d, then squares repeatedly.";
        }
        MillerRabin.Round round = r.rounds().get(frame - 1);
        String seq = java.util.Arrays.toString(round.sequence());
        return "Round " + frame + ", base a = " + round.base() + ": x = a^d mod n, then squared up to s−1 times: " + seq + ". "
                + (round.witness() ? "Neither x = 1 at the start nor x = n−1 at any point occurred, so a is a WITNESS: n is composite."
                : "The sequence started at 1 or reached n−1, so n passes this round (a prime always does).");
    }
}
