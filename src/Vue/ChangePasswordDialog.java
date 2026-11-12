package Vue;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class ChangePasswordDialog extends Dialog<String[]> {

    private final TextField tfOldPass = new TextField();
    private final TextField tfNewPass = new TextField();

    public ChangePasswordDialog() {

        setupTextFields();
        setupTheRest();

        Platform.runLater(() -> tfOldPass.requestFocus());
    }

    private void setupTheRest() {

        setTitle("Change book password");

        getDialogPane().getButtonTypes().add(ButtonType.OK);
        setOnCloseRequest(dialogEvent -> {
            close();
        });

        final GridPane gridPane = new GridPane();
        gridPane.setVgap(15);
        gridPane.setHgap(10);
        gridPane.add(new Label("Old password"), 0, 0);
        gridPane.add(tfOldPass, 1, 0);
        gridPane.add(new Label("New password"), 0, 1);
        gridPane.add(tfNewPass, 1, 1);

        final Text lbl = new Text("** Password for notebook backups will also be changed." +
                "\nDon't forget password, not possible to recover.");

        final VBox vBox = new VBox(lbl, gridPane);
        vBox.setPadding(new Insets(5));
        vBox.setSpacing(15);

        getDialogPane().setContent(vBox);

        setResultConverter(buttonType -> {

            if (ButtonType.OK == buttonType) {
                return new String[] {tfOldPass.getText(), tfNewPass.getText()};
            }
            else {
                return new String[0];
            }
        });
    }

    private void setupTextFields() {

        tfOldPass.setPrefColumnCount(25);
        tfNewPass.setPrefColumnCount(25);
        tfNewPass.setPromptText("Empty; no password");
    }
}
