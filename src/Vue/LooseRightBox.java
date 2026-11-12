package Vue;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.ArrayList;

public class LooseRightBox extends VBox {

    private final DailyText looseText;
    private final HBox hbToolBox = new HBox();
    private final HBox hbTools;
    private final HBox hbTheSearchThing = new HBox();
    private final HBox hbAdd;
    private final HBox hbRename;
    private final HBox hbRearrange;
    private final TextField tfSearchEntries = new TextField();

    private final ListView<String> lvEntries = new ListView<>();
    private final ListView<String> lvSearchedEntries = new ListView<>();

    private final TextField addTextField = new TextField();
    private final TextField renameTextField = new TextField();

    private final ArrayList<LooseMeta> map = new ArrayList<>();

    private int currentId = -1;
    private int textHash = -1;

    private final int notMovingMagixNumber = -66;
    private int indexOfItemToMove = notMovingMagixNumber;

    private int searchIndex = 0;

    private boolean inSearchMode = false;

    // todo --> this
    // Ok since we use map to store stuff, and list to store another stuff -- when we select stuff ... Ok,
    // selection is done on the list, but entry is pull from the map, we may pull the wrong entry -- sp check first.

    public LooseRightBox(DailyText lt) {

        setMinWidth(250);
        setMaxWidth(250);

        looseText = lt;

        hbTools = getToolbox();
        hbAdd = getAddUIbox();
        hbRename = getRenameUI();
        hbRearrange = getRearrange();

        hbToolBox.setPadding(new Insets(0));
        hbToolBox.getChildren().add(hbTools);

        getChildren().addAll(hbToolBox, getEntryListBox(), getEntrySearchBox());

        setUpSearchListView();

        try {
            loadEntriesIntoTheListView();
        } catch (Exception ignored) {}
    }

    private void setUpSearchListView() {

        hbTheSearchThing.setAlignment(Pos.CENTER_LEFT);
        hbTheSearchThing.setSpacing(10);
        hbTheSearchThing.setPadding(new Insets(5));

        final Button btnClose = new Button("x");
        btnClose.setOnAction(actionEvent -> {

            hbToolBox.getChildren().clear();
            hbToolBox.getChildren().add(hbTools);

            getChildren().remove(1);
            getChildren().add(1, getEntryListBox());

            tfSearchEntries.clear();

            inSearchMode = false;

            looseText.setEntrySearchTerm(null);
        });

        hbTheSearchThing.getChildren().addAll(btnClose, new Label("Search result"));
        lvSearchedEntries.setPrefHeight(5000);

        lvSearchedEntries.getSelectionModel().selectedIndexProperty().addListener((observableValue, number, t1) -> {

            if (t1.intValue() < 0) {
                return;
            }

            if (lmsSearch.size() != lvSearchedEntries.getItems().size()) {
                return;
            }


            final int sel = t1.intValue();
            final int entryId = lmsSearch.get(sel);

            for (int i = 0; i < map.size(); i++) {
                final LooseMeta lm = map.get(i);
                if (lm.id == entryId) {
                    lvEntries.getSelectionModel().select(i);
                    lvEntries.scrollTo(i);
                    break;
                }
            }
        });
    }

    private HBox getEntryListBox() {

        lvEntries.setPrefHeight(5000);
        final HBox hbx = new HBox(lvEntries);
        hbx.setPadding(new Insets(5));
        hbx.setSpacing(5);

        return hbx;
    }

    private HBox getToolbox() {

        final HBox hb = new HBox();
        hb.setPadding(new Insets(5));
        hb.setSpacing(5);

        final MenuItem miAdd = new MenuItem("+ Add");
        miAdd.setOnAction(actionEvent -> {
            hbToolBox.getChildren().clear();
            hbToolBox.getChildren().add(hbAdd);
            addTextField.requestFocus();

        });

        final MenuItem miDelete = new MenuItem("- Delete");
        miDelete.setOnAction(actionEvent -> {
            deleteLooseEntry();
        });

        final MenuItem miRename = new MenuItem("Rename");
        miRename.setOnAction(actionEvent -> {

            final int sel = lvEntries.getSelectionModel().getSelectedIndex();
            if (sel < 0) {
                return;
            }

            lvEntries.setDisable(true);

            final LooseMeta lms = map.get(sel);
            renameTextField.setText(lms.name);

            hbToolBox.getChildren().clear();
            hbToolBox.getChildren().add(hbRename);

            renameTextField.requestFocus();
            renameTextField.selectAll();
        });

        final MenuItem miRearrange = new MenuItem("Up/Down");
        miRearrange.setOnAction(actionEvent -> {

            final int sel = lvEntries.getSelectionModel().getSelectedIndex();
            if (sel < 0) {
                return;
            }
            indexOfItemToMove = sel;

            hbToolBox.getChildren().clear();
            hbToolBox.getChildren().add(hbRearrange);
        });

        final MenuButton mbtn = new MenuButton("...");
        mbtn.getItems().addAll(miAdd, miDelete, miRename, new SeparatorMenuItem(), miRearrange);

        lvEntries.getSelectionModel().selectedIndexProperty().addListener((observableValue, number, t1) -> {
            loadEntry();
        });

        final TextField tef = new TextField();
        tef.setPromptText("Search name");

        final Button btnGo = new Button(">");
        btnGo.setOnAction(new EventHandler<>() {

            private String sKey = "";

            @Override
            public void handle(ActionEvent actionEvent) {
                final String key = tef.getText().toLowerCase();
                titSearch(key, sKey);
                sKey = key;
            }
        });

        final HBox hbSN = new HBox(tef, btnGo);
        hbSN.setSpacing(5);

        hb.getChildren().addAll(mbtn, hbSN);

        return hb;
    }

    private void titSearch(String key, String oldKey) {

        if (!key.equalsIgnoreCase(oldKey)) {
            searchIndex = 0;
        }

        int r = -1;
        for (int i = searchIndex; i < map.size(); i++) {
            final LooseMeta lms = map.get(i);
            if (lms.name.toLowerCase().contains(key)) {
                r = i;
                break;
            }
        }

        if (r == -1) {
            searchIndex = 0;
            return;
        }

        if (r != lvEntries.getSelectionModel().getSelectedIndex()) {
            lvEntries.getSelectionModel().select(r);
        }
        lvEntries.scrollTo(r);
        searchIndex = r + 1;
    }

    private void loadEntry() {

        if (indexOfItemToMove != notMovingMagixNumber) {
            return;
        }

        final int selIndex = lvEntries.getSelectionModel().getSelectedIndex();
        if (selIndex < 0) {
            return;
        }

        save(true);

        currentId = -1;
        looseText.reset();

        final LooseMeta lm = map.get(selIndex);

        final String s;
        final boolean editable;
        try {
            s = Xc.ploc().getLooseText(lm.id);
            editable = Xc.ploc().getEditability(lm.id);
        }
        catch (Exception e) {
            showLoadFailureDialog(e.getMessage());
            return;
        }

        currentId = lm.id;

        final ResetIndexedFlag riff = new ResetIndexedFlag()
        {
            @Override
            public void resetFlag()
            {
                Xc.ploc().looseSetNotIndexed(currentId);
            }
        };

        looseText.setText(currentId, s, editable, riff);
        textHash = looseText.getText().hashCode();
        looseText.setName(lm.name);
    }

    private void showLoadFailureDialog(String s) {

        final Alert al = new Alert(Alert.AlertType.ERROR);
        al.setHeaderText("Can't get entry");
        al.setContentText(s);
        al.showAndWait();
    }

    private HBox getRearrange() {

        final Button btnX = new Button("x");
        btnX.setOnAction(actionEvent -> {
            cancelRearrange();
        });

        final Button btnOk = new Button("Move target");
        btnOk.setOnAction(actionEvent -> {
            reArrange();
            hbToolBox.getChildren().clear();
            hbToolBox.getChildren().add(hbTools);
            indexOfItemToMove = notMovingMagixNumber;
        });

        final HBox hb = new HBox(btnX, btnOk);
        hb.setAlignment(Pos.CENTER_LEFT);
        hb.setPadding(new Insets(5));
        hb.setSpacing(5);

        return hb;
    }

    private HBox getRenameUI() {

        renameTextField.setPromptText("New name");
        renameTextField.setPrefColumnCount(8);

        final Button btnX = new Button("x");
        btnX.setOnAction(actionEvent -> {
            closeRename();
        });

        final Button btnOk = new Button("Ok");
        btnOk.setOnAction(actionEvent -> {

            final int sel = lvEntries.getSelectionModel().getSelectedIndex();
            if (sel >= 0) {
                rename(sel, renameTextField.getText());
            }
            lvEntries.setDisable(false);
            closeRename();
        });

        final HBox hb = new HBox(btnX, renameTextField, btnOk);
        hb.setAlignment(Pos.CENTER_LEFT);
        hb.setPadding(new Insets(5));
        hb.setSpacing(5);

        return hb;
    }

    private void closeRename() {
        renameTextField.clear();
        hbToolBox.getChildren().clear();
        hbToolBox.getChildren().add(hbTools);
        lvEntries.setDisable(false);
    }

    private HBox getAddUIbox() {

        addTextField.setPromptText("Name");
        addTextField.setPrefColumnCount(8);

        final Button btnX = new Button("x");
        btnX.setOnAction(actionEvent -> {
            addTextField.clear();
            hbToolBox.getChildren().clear();
            hbToolBox.getChildren().add(hbTools);
        });

        final Button btnOk = new Button("Add");
        btnOk.setOnAction(actionEvent -> {
            addItem(addTextField.getText());
        });

        final HBox hb = new HBox(btnX, addTextField, btnOk);
        hb.setAlignment(Pos.CENTER_LEFT);
        hb.setPadding(new Insets(5));
        hb.setSpacing(5);

        return hb;
    }

    private HBox getEntrySearchBox() {

        final ToggleButton tbt = new ToggleButton("#");
        tfSearchEntries.setPromptText("Search entries");

        tfSearchEntries.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                try {
                    // search fails, do nothing - no change.
                    searchEntries(tbt.isSelected(), tfSearchEntries.getText());
                }
                catch (Exception ignored) {}
            }
        });

        final Button btn = new Button(">");
        btn.setOnAction(actionEvent -> {
            try {
                // search fails, do nothing - no change.
                searchEntries(tbt.isSelected(), tfSearchEntries.getText());
            }
            catch (Exception ignored) {}
        });

        final HBox hbx = new HBox(tbt, tfSearchEntries, btn);
        hbx.setSpacing(5);

        hbx.setPadding(new Insets(5));

        return hbx;
    }

    private void loadEntriesIntoTheListView() throws SQLException {

        final ArrayList<LooseMeta> lms = Xc.ploc().getLooseEntries();
        for (LooseMeta lm : lms) {
            map.add(lm);
            lvEntries.getItems().add(lm.name);
        }

        if (Xc.ploc().getLooseEntries().size() == 0) {
           addAutoGeneratedItem();
        }

        if (!map.isEmpty()) {
            lvEntries.getSelectionModel().select(0);
            // todo -- may-be store last selected -- then open the last selected? (v.100.)
        }
    }

    private void addAutoGeneratedItem() {
        addItem("Auto-gen-" + System.currentTimeMillis());
    }

    private void addItem(String name) {

        if ("".equalsIgnoreCase(name)) {
            name = "Untitled-" + System.currentTimeMillis();
        }

        searchIndex = 0;

        final int sel = lvEntries.getSelectionModel().getSelectedIndex();
        if (sel == -1) {
            final LooseMeta lms;
            try {
                lms = Xc.ploc().createLoose(name);
            }
            catch (Exception e) {
                // can't create -- should not add right?
                return;
            }
            map.add(lms);
            lvEntries.getItems().add(name);
            selectAfterAdding(lms);
            // add @ last
            return;
        }

        final int nl = sel + 1;

        if (nl >= map.size()) {
            final LooseMeta lms;
            try {
                lms = Xc.ploc().createLoose(name);
            }
            catch (Exception e) {
                return;
            }
            map.add(lms);
            lvEntries.getItems().add(name);
            selectAfterAdding(lms);
            // add @ last
            return;
        }

        // add somewhere else.
        final LooseMeta lms2;
        try {
            lms2 = Xc.ploc().createLoose(name);
        }
        catch (Exception e) {
            return;
        }
        map.add(nl, lms2);
        lvEntries.getItems().add(nl, name);
        selectAfterAdding(lms2);

        try {
            Xc.ploc().reOrder(map);
        }
        catch (Exception ignored) {
            // nothing.
        }
    }

    private void selectAfterAdding(LooseMeta lms) {

        int i = 0;
        for (;i < map.size(); i++) {

            final LooseMeta lamo = map.get(i);
            if (lms.id == lamo.id) {
              break;
            }
        }

        if (i >= map.size()) {
            lvEntries.getSelectionModel().selectFirst();
        }
        else {
            lvEntries.getSelectionModel().select(i);
        }
    }

    private void rename(int sel, String name) {

        final LooseMeta lms = map.get(sel);
        // rename here - ok?

        try {
            // name is just name --- so no need to change anything on the UI if renaming fails.
            Xc.ploc().rename(lms.id, name);
        }
        catch (Exception ignored) {}

        final LooseMeta lmsUpdated = new LooseMeta(lms.id, name);
        map.set(sel, lmsUpdated);
        lvEntries.getItems().set(sel, name);
        looseText.setName(name);
    }

    private void cancelRearrange() {
        lvEntries.getSelectionModel().select(indexOfItemToMove);
        hbToolBox.getChildren().clear();
        hbToolBox.getChildren().add(hbTools);
        indexOfItemToMove = notMovingMagixNumber;
    }

    private void reArrange() {

        searchIndex = 0;

        final int seLoc = lvEntries.getSelectionModel().getSelectedIndex();
        if (seLoc < 0) {
            // todo -- don't execute?
            cancelRearrange();
            return;
        }

        final LooseMeta lmsSelected = map.get(seLoc);
        final LooseMeta lmsToMove = map.remove(indexOfItemToMove);
        final int indexOfSelected = map.indexOf(lmsSelected);

        if (indexOfSelected == -1) {
            map.add(indexOfItemToMove, lmsToMove);
            cancelRearrange();
            return;
        }

        map.add(indexOfSelected, lmsToMove);

        lvEntries.getItems().clear();

        for (LooseMeta lem : map) {
            lvEntries.getItems().add(lem.name);
        }

        try {
            Xc.ploc().reOrder(map);
        }
        catch (Exception ignored) {
            //not a big deal if it fails.
        }

        final int indexOfToMove = map.indexOf(lmsToMove);
        if (indexOfToMove == -1) {
            // just select the 1st item
            lvEntries.getSelectionModel().select(0);
            return;
        }

        if (currentId != lmsToMove.id) {
            lvEntries.getSelectionModel().select(0);
            return;
        }
        lvEntries.getSelectionModel().select(indexOfToMove);
    }

    private void deleteLooseEntry() {

        searchIndex = 0;

        final int indexOfItemToDelete = lvEntries.getSelectionModel().getSelectedIndex();
        if (indexOfItemToDelete < 0) {
            return;
        }

        try {
            Xc.ploc().deleteLooseEntry(map.get(indexOfItemToDelete).id);
        }
        catch (Exception ignored) {}

        map.remove(indexOfItemToDelete);
        lvEntries.getItems().remove(indexOfItemToDelete);

        if (lvEntries.getItems().size() == 0) {
            // if all is deleted, add a new one automatically
            currentId = -1;
            looseText.reset();
            addAutoGeneratedItem();
            lvEntries.getSelectionModel().select(0);
            return;
        }

        // if we delete the first item -- new item won't be selected automatically
        // so we need to manually select
        if (indexOfItemToDelete == 0) {
            loadEntry();
        }
    }

    final ArrayList<Integer> lmsSearch = new ArrayList<>();

    private void searchEntries(boolean tagSearch, String kWord) throws SQLException {

        lvSearchedEntries.getItems().clear();
        looseText.setEntrySearchTerm(kWord);
        lmsSearch.clear();

        final ArrayList<String> al = Xc.ploc().searchEntries(kWord);

        for (String s : al) {
            // l for loose -- out fts searches everything.
            if (s.charAt(0) != 'l') {
                continue;
            }

            final int id = Integer.parseInt(s.substring(2));
            if (Xc.ploc().isDeleted_loose(id)) {
                continue;
            }

            final String o = Xc.ploc().getLooseName(id);
            lvSearchedEntries.getItems().add(o);
            lmsSearch.add(id);
        }

        if (inSearchMode) {
            return;
        }

        inSearchMode = true;

        hbToolBox.getChildren().clear();
        hbToolBox.getChildren().add(hbTheSearchThing);

        final StackPane sp = new StackPane(lvSearchedEntries);
        sp.setPadding(new Insets(5));

        getChildren().remove(1);
        getChildren().add(1, sp);
    }

    public synchronized void save(boolean resetId) {

        if (currentId == -1) {
            return;
        }

        final String text = looseText.getText();
        if (text.hashCode() != textHash) {
            try {
                Xc.ploc().storeLooseText(currentId, text);
                textHash = text.hashCode();
            } catch (Exception ignored) {
                // will remain the same - the text hash, can try again later.
            }
        }

        if (resetId) {
            currentId = -1;
        }
    }
}
