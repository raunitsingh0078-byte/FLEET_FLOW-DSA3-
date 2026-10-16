package com.fleetflow.data;

import com.fleetflow.model.Priority;
import com.fleetflow.model.Shipment;
import com.fleetflow.model.ShipmentStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists shipments in a plain CSV file (all fields quoted, quotes doubled). No external database is needed;
 * the file is only created after the user edits data, so a fresh start always shows the demo data.
 */
public final class ShipmentCsvStore {

    private static final String HEADER = "id,packageCode,customer,source,destination,address,description,status,priority,weightKg,driverId,vehicleId,date";
    private static final int FIELDS = 13;

    private ShipmentCsvStore() {
    }

    /** @return the default storage file inside the user's home directory. */
    public static Path defaultPath() {
        return Path.of(System.getProperty("user.home"), ".fleetflow", "shipments.csv");
    }

    public static void save(Path file, List<Shipment> shipments) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (Shipment s : shipments) {
            String[] f = {s.id(), s.packageCode(), s.customer(), s.source(), s.destination(), s.address(), s.description(),
                    s.status().name(), s.priority().name(), String.valueOf(s.weightKg()), s.driverId(), s.vehicleId(), s.date().toString()};
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < f.length; i++) {
                sb.append(i == 0 ? "" : ",").append('"').append(f[i].replace("\"", "\"\"")).append('"');
            }
            lines.add(sb.toString());
        }
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp"); // write-then-rename so a crash cannot corrupt the file
        Files.write(tmp, lines, StandardCharsets.UTF_8);
        Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    /** @throws IOException if the file cannot be read or a line is malformed (the message names the line) */
    public static List<Shipment> load(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<Shipment> result = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) { // line 0 is the header
            if (lines.get(i).isBlank()) {
                continue;
            }
            List<String> f = parseLine(lines.get(i));
            if (f.size() != FIELDS) {
                throw new IOException("line " + (i + 1) + " has " + f.size() + " fields, expected " + FIELDS);
            }
            try {
                result.add(new Shipment(f.get(0), f.get(1), f.get(2), f.get(3), f.get(4), f.get(5), f.get(6),
                        ShipmentStatus.valueOf(f.get(7)), Priority.valueOf(f.get(8)), Double.parseDouble(f.get(9)),
                        f.get(10), f.get(11), LocalDate.parse(f.get(12))));
            } catch (IllegalArgumentException | DateTimeParseException e) {
                throw new IOException("line " + (i + 1) + " is invalid: " + e.getMessage());
            }
        }
        return result;
    }

    static List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                fields.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        fields.add(cur.toString());
        return fields;
    }
}
