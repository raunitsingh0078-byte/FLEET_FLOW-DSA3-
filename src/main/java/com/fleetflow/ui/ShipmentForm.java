package com.fleetflow.ui;

import com.fleetflow.data.FleetRepository;
import com.fleetflow.model.Driver;
import com.fleetflow.model.Priority;
import com.fleetflow.model.Shipment;
import com.fleetflow.model.ShipmentStatus;
import com.fleetflow.model.Vehicle;
import com.fleetflow.model.Warehouse;
import com.fleetflow.util.Dialogs;
import com.fleetflow.util.Geo;
import com.fleetflow.util.ThemeManager;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/** Modal form for adding or editing a shipment, with full input validation. */
public final class ShipmentForm {

    private ShipmentForm() {
    }

    /**
     * Shows the form.
     *
     * @param existing shipment to edit, or null to add a new one
     * @return the saved shipment, or empty if the user cancelled
     */
    public static Optional<Shipment> show(Shipment existing) {
        FleetRepository repo = FleetRepository.get();
        boolean editing = existing != null;
        Dialog<Shipment> dialog = new Dialog<>();
        dialog.setTitle(editing ? "Edit shipment " + existing.id() : "Add shipment");
        dialog.setHeaderText(editing ? "Change the shipment details" : "Enter the new shipment");
        ThemeManager.style(dialog.getDialogPane());
        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        TextField id = new TextField(editing ? existing.id() : nextId(repo));
        id.setDisable(editing);
        TextField code = new TextField(editing ? existing.packageCode() : "");
        TextField customer = new TextField(editing ? existing.customer() : "");
        List<String> cities = new ArrayList<>(new LinkedHashSet<>(Geo.CITIES.keySet()));
        for (Warehouse w : repo.warehouses()) {
            String city = w.location().split(",")[0];
            if (!cities.contains(city)) cities.add(city);
        }
        ComboBox<String> source = editableCombo(cities, editing ? existing.source() : "");
        ComboBox<String> destination = editableCombo(cities, editing ? existing.destination() : "");
        TextField address = new TextField(editing ? existing.address() : "");
        TextField description = new TextField(editing ? existing.description() : "");
        ComboBox<ShipmentStatus> status = new ComboBox<>();
        status.getItems().addAll(ShipmentStatus.values());
        status.setValue(editing ? existing.status() : ShipmentStatus.PENDING);
        ComboBox<Priority> priority = new ComboBox<>();
        priority.getItems().addAll(Priority.values());
        priority.setValue(editing ? existing.priority() : Priority.NORMAL);
        TextField weight = new TextField(editing ? String.valueOf(existing.weightKg()) : "");
        ComboBox<String> driver = idCombo(repo.drivers().stream().map(Driver::id).toList(), editing ? existing.driverId() : "", repo);
        ComboBox<String> vehicle = idCombo(repo.vehicles().stream().map(Vehicle::id).toList(), editing ? existing.vehicleId() : "", repo);
        DatePicker date = new DatePicker(editing ? existing.date() : LocalDate.now());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        Object[][] rows = {{"Shipment ID", id}, {"Package code", code}, {"Customer", customer}, {"Source", source}, {"Destination", destination},
                {"Delivery address", address}, {"Description", description}, {"Status", status}, {"Priority", priority},
                {"Weight (kg)", weight}, {"Assigned driver", driver}, {"Assigned vehicle", vehicle}, {"Date", date}};
        for (int i = 0; i < rows.length; i++) {
            grid.add(Ui.label((String) rows[i][0], "kv-key"), 0, i);
            Node field = (Node) rows[i][1];
            grid.add(field, 1, i);
            GridPane.setHgrow(field, javafx.scene.layout.Priority.ALWAYS);
        }
        ColumnConstraints c0 = new ColumnConstraints(130);
        ColumnConstraints c1 = new ColumnConstraints(330);
        grid.getColumnConstraints().addAll(c0, c1);
        dialog.getDialogPane().setContent(grid);

        Node saveButton = dialog.getDialogPane().lookupButton(save);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            String problem = validate(editing, id.getText(), code.getText(), customer.getText(), source.getEditor().getText(),
                    destination.getEditor().getText(), address.getText(), weight.getText(), date.getValue(), status.getValue(),
                    driver.getValue(), vehicle.getValue());
            if (problem != null) {
                Dialogs.warning("Please check the shipment", problem);
                event.consume();
            }
        });
        dialog.setResultConverter(button -> button != save ? null : new Shipment(id.getText().trim(), code.getText().trim(), customer.getText().trim(),
                source.getEditor().getText().trim(), destination.getEditor().getText().trim(), address.getText().trim(), description.getText().trim(),
                status.getValue(), priority.getValue(), Double.parseDouble(weight.getText().trim()), nullToEmpty(driver.getValue()),
                nullToEmpty(vehicle.getValue()), date.getValue()));
        return dialog.showAndWait();
    }

    private static String validate(boolean editing, String id, String code, String customer, String source, String destination, String address,
                                   String weight, LocalDate date, ShipmentStatus status, String driver, String vehicle) {
        String trimmedId = id == null ? "" : id.trim();
        if (!trimmedId.matches("[A-Za-z0-9-]{3,20}")) {
            return "The shipment ID must have 3–20 letters, digits or hyphens (e.g. SHP-2100).";
        }
        if (!editing && FleetRepository.get().shipments().stream().anyMatch(s -> s.id().equalsIgnoreCase(trimmedId))) {
            return "A shipment with the ID " + trimmedId + " already exists.";
        }
        if (isBlank(code)) return "Please enter the package code.";
        if (isBlank(customer)) return "Please enter the customer name.";
        if (isBlank(source)) return "Please choose or enter the source city.";
        if (isBlank(destination)) return "Please choose or enter the destination city.";
        if (isBlank(address)) return "Please enter the delivery address.";
        try {
            double w = Double.parseDouble(weight == null ? "" : weight.trim());
            if (!(w > 0 && w <= 100_000)) {
                return "The weight must be greater than 0 and at most 100,000 kg.";
            }
        } catch (NumberFormatException e) {
            return "The weight must be a number, e.g. 42.5.";
        }
        if (date == null) return "Please pick a date.";
        boolean onRoad = status == ShipmentStatus.IN_TRANSIT || status == ShipmentStatus.DELAYED;
        if (onRoad && (isBlank(driver) || isBlank(vehicle))) {
            return "A shipment that is " + status + " needs an assigned driver and vehicle.";
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String nextId(FleetRepository repo) {
        int max = 2000;
        for (Shipment s : repo.shipments()) {
            if (s.id().startsWith("SHP-")) {
                try {
                    max = Math.max(max, Integer.parseInt(s.id().substring(4)));
                } catch (NumberFormatException ignored) {
                    // ids that are not numeric are skipped when proposing the next number
                }
            }
        }
        return "SHP-" + (max + 1);
    }

    private static ComboBox<String> editableCombo(List<String> items, String value) {
        ComboBox<String> box = new ComboBox<>();
        box.getItems().addAll(items);
        box.setEditable(true);
        box.getEditor().setText(value);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private static ComboBox<String> idCombo(List<String> ids, String value, FleetRepository repo) {
        ComboBox<String> box = new ComboBox<>();
        box.getItems().add("");
        box.getItems().addAll(ids);
        box.setValue(value == null ? "" : value);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setConverter(new StringConverter<>() {
            @Override
            public String toString(String id) {
                if (id == null || id.isEmpty()) return "(unassigned)";
                return repo.drivers().stream().filter(d -> d.id().equals(id)).findFirst().map(d -> id + " – " + d.name()).orElse(id);
            }

            @Override
            public String fromString(String s) {
                return s;
            }
        });
        return box;
    }
}
