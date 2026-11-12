package Vue;

import Misc.X;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class EntryPage extends TabPane {

    private final TextField tfBooksLoc = new TextField();
    private final TextField tfExportLoc = new TextField();

    private final ListView<String> lvlBooks = new ListView<>();

    private final MenuItem miAdd = new MenuItem("Add");
    private final MenuItem miRemove = new MenuItem("Remove");
    private final MenuItem miChangePass = new MenuItem("Change password");

    private final Button btnOpen = new Button("Open");
    private final Button btnImport = new Button("Import entries");

    private final Tab tabEntries = new Tab("Notebooks");
    private final Label lblSettingNotf = new Label("Settings not set/invalid.");

    private final TextField tfPass = new TextField();

    public EntryPage(KVPairFile kpv) {

        setPadding(new Insets(25, 10, 10, 10));
        setMaxSize(500, 500);

        tabEntries.setDisable(true);
        tabEntries.setClosable(false);
        tabEntries.setContent(getBooksListBox());

        final Tab tabSettings = new Tab("Settings");
        tabSettings.setClosable(false);

        final ScrollPane sp = new ScrollPane();
        setScrollerPane(sp);
        tabSettings.setContent(sp);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        final Tab tabAbout = new Tab("-?-");
        tabAbout.setContent(getAboutTextArea());
        tabAbout.setClosable(false);

        getTabs().addAll(tabEntries, tabSettings, tabAbout);

        lblSettingNotf.setTextFill(Color.web(X.WARNING_TEXT_COLOR));

        lvlBooks.setOnMouseClicked(mouseEvent -> {
            Platform.runLater(() -> {
                tfPass.requestFocus();
            });
        });

        loadSettings(kpv);
        startCheck();
    }

    private void startCheck() {

        lvlBooks.getItems().clear();
        lblSettingNotf.setVisible(true);

        if (GlobalX.booksLocation == null) {
            return;
        }

        if (!GlobalX.booksLocation.toFile().exists()) {
            return;
        }

        if (!GlobalX.booksLocation.toFile().isDirectory()) {
            return;
        }

        lblSettingNotf.setVisible(false);
        tabEntries.setDisable(false);
        getSelectionModel().select(tabEntries);
        lvlBooks.getItems().addAll(populateNotebookList());
    }

    private void saveSettingsCheck() {

        lvlBooks.getItems().clear();

        if (!GlobalX.booksLocation.toFile().exists()) {
            tabEntries.setDisable(true);
            final Alert t = new Alert(Alert.AlertType.INFORMATION);
            t.setHeaderText("");
            t.setContentText("Notebook folder does not exist.");
            t.showAndWait();
            return;
        }

        if (!GlobalX.booksLocation.toFile().isDirectory()) {
            tabEntries.setDisable(true);
            final Alert t = new Alert(Alert.AlertType.INFORMATION);
            t.setHeaderText("");
            t.setContentText("Notebook location is not a folder -- cannot proceed.");
            t.showAndWait();
            return;
        }

        lblSettingNotf.setVisible(false);
        tabEntries.setDisable(false);
        lvlBooks.getItems().addAll(populateNotebookList());
    }

    private void loadSettings(KVPairFile kvp) {

        if (kvp == null) {
            return;
        }

        final HashMap<String, String> settings = kvp.getMap();
        final String booksLocation = settings.get(GlobalX.settings_notebookLoc);
        if (booksLocation == null) {
            GlobalX.booksLocation = null;
        }
        else {
            try {
                GlobalX.booksLocation = Paths.get(booksLocation);
                tfBooksLoc.setText(GlobalX.booksLocation.toString());
            }
            catch (Exception e) {
                GlobalX.booksLocation = null;
            }
        }

        final String exportLocation = settings.get(GlobalX.settings_exportLoc);
        if (exportLocation == null) {
            GlobalX.exportLocation = null;
        }
        else {
            try {
                GlobalX.exportLocation = Paths.get(exportLocation);
                tfExportLoc.setText(GlobalX.exportLocation.toString());
            }
            catch (Exception e) {
                GlobalX.exportLocation = null;
            }
        }
    }

    private void setScrollerPane (ScrollPane sp) {

        final VBox vbArchLoc = new VBox();
        vbArchLoc.setSpacing(5);
        vbArchLoc.getChildren().addAll(new Label("Books location (Required)"), tfBooksLoc);

        final VBox vbExportLoc = new VBox();
        vbExportLoc.setSpacing(5);
        vbExportLoc.getChildren().addAll(new Label("Export location"), tfExportLoc);

        final Button btnSaveSettings = new Button("Save settings");
        btnSaveSettings.setOnAction(actionEvent -> saveSettings());

        lblSettingNotf.setVisible(false);
        final HBox hbBtnBox = new HBox(btnSaveSettings, lblSettingNotf);
        hbBtnBox.setSpacing(5);
        hbBtnBox.setAlignment(Pos.CENTER_LEFT);
        hbBtnBox.setPadding(new Insets(5, 0, 5, 0));

        final VBox vbs = new VBox();
        vbs.setPadding(new Insets(20, 20, 5, 5));
        vbs.setSpacing(10);
        vbs.getChildren().addAll(hbBtnBox, vbArchLoc, vbExportLoc);

        sp.setContent(vbs);
        sp.fitToHeightProperty().set(true);
        sp.fitToWidthProperty().set(true);
    }
    
    private void saveSettings() {

        tabEntries.setDisable(true);

        final KVPairFile kvp = new KVPairFile(GlobalX.settingsFile);
        final HashMap<String, String> hm = kvp.getMap();

        final String nbLoc = tfBooksLoc.getText();
        final String exLoc = tfExportLoc.getText();
        hm.put(GlobalX.settings_notebookLoc, nbLoc);
        hm.put(GlobalX.settings_exportLoc, exLoc);

        if ("".equalsIgnoreCase(nbLoc)) {
            GlobalX.booksLocation = null;
        }
        else {
            try {
                GlobalX.booksLocation = Paths.get(nbLoc);
            }
            catch (Exception e) {
                GlobalX.booksLocation = null;
            }
        }

        if ("".equalsIgnoreCase(exLoc)) {
            GlobalX.exportLocation = null;
        }
        else {
            try {
                GlobalX.exportLocation = Paths.get(exLoc);
            }
            catch (Exception e) {
                GlobalX.exportLocation = null;
            }
        }

        try {
            kvp.writeContent();
        } catch (Exception ignore) {}
        saveSettingsCheck();
    }

    private VBox getBooksListBox() {

        tfPass.setPromptText("Password");

        tfPass.setOnKeyPressed(keyEvent -> {

            if (keyEvent.getCode() == KeyCode.ENTER) {

                if (setPloc(tfPass.getText())) {

                    if (keyEvent.isControlDown()) {
                        Xc.showImportingStuff();
                    }
                    else {
                        Xc.actionOnOpening();
                    }
                    tfPass.setText("");
                }
            }
        });

        btnOpen.setOnAction(actionEvent -> {

            if (setPloc(tfPass.getText())) {
                Xc.actionOnOpening();
            }
            tfPass.setText("");
        });
        
        btnImport.setOnAction(actionEvent -> {
            if (setPloc(tfPass.getText())) {
                Xc.showImportingStuff();
            }
            tfPass.setText("");
        });

        final HBox hb1 = new HBox(btnOpen, btnImport);
        hb1.setAlignment(Pos.CENTER_LEFT);
        hb1.setSpacing(5);
        
        final MenuButton mb = new MenuButton("||");
        mb.getItems().addAll(miAdd, miRemove, miChangePass);

        miAdd.setOnAction(actionEvent -> createNoteBook());
        miChangePass.setOnAction(actionEvent -> changePass());

        final HBox hbAction = new HBox();
        hbAction.setSpacing(5);
        hbAction.getChildren().addAll(mb);

        final VBox vb = new VBox(hbAction, lvlBooks, tfPass, hb1);
        vb.setPadding(new Insets(10, 0, 0, 0));
        vb.setSpacing(10);

        return vb;
    }

    private void changePass() {

        final String nb = lvlBooks.getSelectionModel().getSelectedItem();
        if (nb == null) {
            return;
        }

        final ChangePasswordDialog dlg = new ChangePasswordDialog();
        dlg.showAndWait();

        final String[] pwNpw = dlg.getResult();
        if (pwNpw.length == 0) {
            return;
        }

        final ArrayList<String> errorLog = new ArrayList<>();
        try {

            final Path bookPath = Paths.get(X.getPathWithoutMvDb(nb));
            //---
            final Ploc ploc = new Ploc(bookPath, pwNpw[0]);
            final String ftDbName = ploc.ftsDBName();
            ploc.close();
            //---


            // change passwords of? -- this one first? The book.
            doChangePassword(bookPath, pwNpw, errorLog);

            // now change the full text search index..
            final Path ftsIndex = GlobalX.fullTextSearchDBFolder.resolve(ftDbName);
            doChangePassword(ftsIndex, pwNpw, errorLog);

            // now the backup books
            for (Path p :  getBackupBooks(ftDbName)) {
                final Path p2 = Paths.get(X.getPathWithoutMvDb(p.toString()));
                doChangePassword(p2, pwNpw, errorLog);
            }
        }
        catch (Exception e) {

            final Alert a = new Alert(Alert.AlertType.ERROR);
            a.setHeaderText("Change pass error");
            a.setContentText(e.getMessage());
            a.showAndWait();
        }
    }

    private void doChangePassword(Path p, String[] pws, ArrayList<String> errorLog) {

        try {
            org.h2.tools.ChangeFileEncryption.execute(p.getParent() + "", p.getFileName() + "",
                    "XTEA", pws[0].toCharArray(), pws[1].toCharArray(), true);
        }
        catch (Exception e) {
            errorLog.add(e.getMessage());
        }
    }

    private ArrayList<Path> getBackupBooks(String dbName) {

        final ArrayList<Path> als = new ArrayList<>();
        try {

            try (DirectoryStream<Path> ds = Files.newDirectoryStream(GlobalX.bookBackupFolder)) {

                for (Path p2 : ds) {
                    if (p2.getFileName().toString().startsWith(dbName + "_")) {
                        als.add(p2);
                    }
                }
            }
        } catch (Exception ignored) {}
        return als;
    }


    private boolean setPloc(String password) {

        try {
            final String path = lvlBooks.getSelectionModel().getSelectedItem();
            if (path == null) {
                throw new Exception("No book selected.");
            }
            Xc.setPloc(Paths.get(path), password);
            return true;
        }
        catch (Exception e) {

            final Alert a = new Alert(Alert.AlertType.ERROR);
            a.setHeaderText("@ setPloc()");
            a.setContentText(e.getMessage());
            a.showAndWait();
        }
        return false;
    }

    private void createNoteBook() {

        final CreateBookInputDialog bb = new CreateBookInputDialog();
        bb.showAndWait();

        final String[] nameNpw = bb.getResult();
        if (nameNpw.length == 0) {
            return;
        }

        if ("".equalsIgnoreCase(nameNpw[0])) {
            return;
        }

        try {

            final Path p = GlobalX.booksLocation.resolve("jnl" + nameNpw[0]);
            Ploc.createNoteBook(p.toString(), nameNpw[1]);
            lvlBooks.getItems().add(p.toString() + ".mv.db");
        }
        catch (Exception ignored) {}
    }

    private TextArea getAboutTextArea() {

        final URL url = getClass().getResource("/Vue/about_txt");

        final TextArea ta = new TextArea();
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefSize(5000,5000);

        try (InputStream is = url.openStream()){
            final byte[] bts = is.readAllBytes();
            ta.setText(new String(bts, StandardCharsets.UTF_8));
        }
        catch (Exception e) {
            ta.setText("jnlz wallop");
        }
        return ta;
    }

    private ArrayList<String> populateNotebookList() {

        final ArrayList<String> entries = new ArrayList<>();

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(GlobalX.booksLocation)) {
            for (Path p : ds) {

                final String fileName = p.getFileName().toString();
                if (fileName.length() < 10) {
                    // .mv.db = 6
                    // jnl = 3
                    // = 9;
                    // min 1 char for name. 10.
                    continue;
                }
                if (!"jnl".equalsIgnoreCase(fileName.substring(0, 3))) {
                    continue;
                }
                if (!isH2DB(fileName)) {
                    continue;
                }
                entries.add(p.toString());
            }
        }
        catch (Exception ignored) {}
        return entries;
    }

    private boolean isH2DB(String fileName) {

        final StringBuilder sbs = new StringBuilder(fileName);
        // .mv.db => 6 chars, ok.
        while (sbs.length() > 6) {
            sbs.deleteCharAt(0);
        }

        return ".mv.db".equalsIgnoreCase(sbs.toString());
    }
}
