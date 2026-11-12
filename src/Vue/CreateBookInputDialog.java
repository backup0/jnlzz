package Vue;

import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class CreateBookInputDialog extends Dialog<String[]> {

    private final TextField tfName = new TextField();
    private final TextField tfPass = new TextField();

    public CreateBookInputDialog() {

        setupTextFields();
        setupTheRest();

        Platform.runLater(() -> tfName.requestFocus());
    }

    private void setupTheRest() {

        setTitle("Create notebook");

        getDialogPane().getButtonTypes().add(ButtonType.OK);
        setOnCloseRequest(dialogEvent -> {
            close();
        });

        final GridPane gridPane = new GridPane();
        gridPane.setVgap(15);
        gridPane.setHgap(10);
        gridPane.add(new Label("Name"), 0, 0);
        gridPane.add(tfName, 1, 0);
        gridPane.add(new Label("Password"), 0, 1);
        gridPane.add(tfPass, 1, 1);

        getDialogPane().setContent(gridPane);

        setResultConverter(buttonType -> {

            if (ButtonType.OK == buttonType) {
                return new String[] {tfName.getText(), tfPass.getText()};
            }
            else {
                return new String[0];
            }
        });
    }

    private void setupTextFields() {

        tfName.setPrefColumnCount(25);
        tfPass.setPrefColumnCount(25);
        tfPass.setPromptText("Empty; no password");
    }
}
