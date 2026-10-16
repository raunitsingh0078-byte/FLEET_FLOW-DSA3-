package com.fleetflow.ui.panel.suffix;

import com.fleetflow.data.FleetRepository;
import com.fleetflow.model.ShipmentDocument;
import com.fleetflow.ui.Ui;
import com.fleetflow.util.ValidationException;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Reusable document editor: a text area for a shipment description / address plus a picker for stored documents. */
final class DocumentInput extends VBox {

    private final TextArea area = new TextArea();
    private final ComboBox<ShipmentDocument> picker = new ComboBox<>();
    private final int maxLength;

    DocumentInput(int maxLength) {
        this.maxLength = maxLength;
        setSpacing(6);
        area.setPrefRowCount(3);
        area.setWrapText(true);
        area.setPromptText("Type or paste a shipment description, address or delivery document (max " + maxLength + " characters)");
        picker.setPromptText("Load a stored shipment document…");
        picker.getItems().setAll(FleetRepository.get().documents());
        picker.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(ShipmentDocument d) {
                return d == null ? "" : d.id() + " – " + d.title();
            }

            @Override
            public ShipmentDocument fromString(String s) {
                return null;
            }
        });
        picker.setOnAction(e -> {
            ShipmentDocument d = picker.getValue();
            if (d != null) {
                area.setText(d.text().length() > maxLength ? d.text().substring(0, maxLength) : d.text());
            }
        });
        HBox top = new HBox(8, Ui.label("Document", "kv-key"), picker);
        HBox.setHgrow(picker, Priority.NEVER);
        getChildren().addAll(top, area);
    }

    void setText(String text) {
        area.setText(text);
    }

    /** @return the validated document text. */
    String read() throws ValidationException {
        String text = area.getText();
        if (text == null || text.isEmpty()) {
            throw new ValidationException("Please enter a document text first.");
        }
        if (text.length() > maxLength) {
            throw new ValidationException("The document has " + text.length() + " characters; this view supports at most "
                    + maxLength + ".");
        }
        if (text.indexOf('$') >= 0 && maxLength <= 60) {
            throw new ValidationException("The suffix tree reserves '$' as terminator; please remove '$' from the text.");
        }
        return text;
    }
}
