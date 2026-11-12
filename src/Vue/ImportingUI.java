package Vue;

import Misc.ExImBackend;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.nio.file.Paths;

public class ImportingUI extends VBox {

    private final ProgressIndicator piDaily = new ProgressIndicator(-1);
    private final ProgressIndicator piLoose = new ProgressIndicator(-1);
    private final Button btnClose = new Button("x");

    private final ComboBox<String> cbLoose = getDropBox();
    private final ComboBox<String> cbImport = getDropBox();

    public ImportingUI() {

        piDaily.setVisible(false);
        piLoose.setVisible(false);
        createUI();
    }

    private void createUI() {

        btnClose.setOnAction(actionEvent -> {
            Xc.actionOnOpening();
        });

        final String locPrompt = "Location; folder or file.";

        // The daily part - yo!
        final TextField tfDaily = new TextField();
        tfDaily.setPromptText(locPrompt);

        final Button btnImportDaily = new Button("Import");
        piDaily.setPrefSize(16, 16);
        btnImportDaily.setOnAction(actionEvent -> {
            importDaily(tfDaily.getText());
        });

        final HBox hbBtnContDaily = new HBox(btnImportDaily, cbImport, piDaily);
        hbBtnContDaily.setSpacing(5);
        hbBtnContDaily.setAlignment(Pos.CENTER_LEFT);

        final String dailyNote = "(!) The file name should contain date sequence as yyyy-mm-dd.";

        final VBox vbDaily = new VBox(tfDaily, hbBtnContDaily, new Label(dailyNote));
        vbDaily.setSpacing(5);

        final TitledPane tpDaily = new TitledPane("Daily entries", vbDaily);
        tpDaily.setCollapsible(false);

        // the loose part
        final TextField tfLoose = new TextField();
        tfLoose.setPromptText(locPrompt);

        final Button btnImportLoose = new Button("Import");
        piLoose.setPrefSize(16, 16);
        btnImportLoose.setOnAction(actionEvent -> {
            importLoose(tfLoose.getText());
        });

        final HBox hbBtnContLoose = new HBox(btnImportLoose, cbLoose, piLoose);
        hbBtnContLoose.setSpacing(5);
        hbBtnContLoose.setAlignment(Pos.CENTER_LEFT);

        final VBox vbLoose= new VBox(tfLoose, hbBtnContLoose);
        vbLoose.setSpacing(5);

        final TitledPane tpLoose = new TitledPane("Loose entries", vbLoose);
        tpLoose.setCollapsible(false);

        // finalize --
        setSpacing(15);
        setPadding(new Insets(10));
        setAlignment(Pos.TOP_LEFT);
        getChildren().addAll(btnClose, tpDaily, tpLoose);
    }

    private ComboBox<String> getDropBox() {

        final ComboBox<String> cb = new ComboBox<>();
        for (String k : ExImBackend.getCsMap()) {
            cb.getItems().add(k);
        }
        cb.getSelectionModel().select(0);
        return cb;
    }

    private void importDaily(String loc) {

        btnClose.setDisable(true);
        piDaily.setVisible(true);

        final Thread th = new Thread(() -> {
            ExImBackend.importDaily(Paths.get(loc), cbImport.getSelectionModel().getSelectedItem());
            hidePi(piDaily);
            enableClose();
        });
       th.setDaemon(true);
       th.start();
    }

    private void importLoose(String loc) {

        btnClose.setDisable(true);
        piLoose.setVisible(true);

        final Thread th = new Thread(() -> {
            ExImBackend.importLoose(Paths.get(loc), cbImport.getSelectionModel().getSelectedItem());
            hidePi(piLoose);
            enableClose();
        });
        th.setDaemon(true);
        th.start();
    }

    private void enableClose() {
        Platform.runLater(() -> btnClose.setDisable(false));
    }

    private void hidePi(ProgressIndicator pi) {
        Platform.runLater(() -> pi.setVisible(false));
    }
}
