package Vue;

import Misc.X;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DailyRightBox extends VBox {

    private final DailyText dailyText;
    private final Button btnNewDay = new Button("**");
    private final HBox hbToolBox = new HBox();
    private final HBox hbTools;
    private final HBox hbGoto;
    private final HBox hbAdd;
    private final HBox hbTheSearchThing = new HBox();
    private final HBox hbEntryTools;
    private final ListView<String> lvEntries = new ListView<>();
    private final ListView<String> lvSearchedEntries = new ListView<>();
    private final ComboBox<String> comboYears = new ComboBox<>();
    private final TextField tfSearchEntries = new TextField();

    private final Button[] btnsMonth = new Button[] { new Button("1"), new Button("2"), new Button("3"),
            new Button("4"), new Button("5"), new Button("6"), new Button("7"), new Button("8"),
            new Button("9"), new Button("10"), new Button("11"), new Button("12")};

    private HashMap<String, Integer> dayMonth2IndexMap = new HashMap<>();
    private HashMap<Integer, Integer> entriesAnchorMap = new HashMap<>();

    private int currentId = -1;
    private int textHash = -1;

    private int todayDate = DateAndTime.getTodayDateInInt();

    private boolean skipLoadingEntries = false;
    private int indexOfSelection = -1;

    //selection out of range goes to 0, why? Say no entry for July, hit 7 it goes to 0.
    public DailyRightBox(DailyText dte) {

        setMinWidth(250);
        setMaxWidth(250);

        dailyText = dte;
        hbToolBox.setPadding(new Insets(0));
        hbTools = getToolbox();
        hbGoto = getGoToUIbox();
        hbAdd = getAddUIbox();

        setUpSearchListView();

        hbToolBox.getChildren().add(hbTools);

        hbEntryTools = getEntryBox();
        getChildren().addAll(hbToolBox, hbEntryTools, getSearchBox());

        try {
            add(todayDate);
        }
        catch (Exception ignored) {}

        comboYears.getSelectionModel().selectedItemProperty().addListener((observableValue, s, t1) -> {
            if (skipLoadingEntries) {
                skipLoadingEntries = false;
                return;
            }

            loadEntryListView(Integer.parseInt(t1));
            lvEntries.getSelectionModel().selectFirst();
        });

        lvSearchedEntries.getItems().addListener(new ListChangeListener<String>()
        {
            @Override
            public void onChanged(Change<? extends String> change)
            {
                //lvSearchedEntries.scrollTo(lvSearchedEntries.getItems().size() - 1);
            }
        });

        resetTextHash();
        loadYearsComboBox();

        goToByFullId(todayDate);
        startNewDayDetector();
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
            getChildren().add(1, hbEntryTools);

            clearFullTextSearch();

            btnsMonth[0].setUserData(null);
            lvEntries.scrollTo(indexOfSelection);

            dailyText.setEntrySearchTerm(null);
        });

        hbTheSearchThing.getChildren().addAll(btnClose, new Label("Search result"));
        lvSearchedEntries.setPrefHeight(5000);

        lvSearchedEntries.getSelectionModel().selectedIndexProperty().addListener((observableValue, number, t1) -> {
            try {
                goToBySel();
            }
            catch (Exception ignored) {
                // todo: safe to ignore?
            }
        });
    }

    private void goToBySel() throws SQLException {

        final String s = lvSearchedEntries.getSelectionModel().getSelectedItem();
        if (s == null) {
            return;
        }

        final String[] sParts = s.split("/");
        if (sParts.length != 3) {
            return;
        }

        final String yr = sParts[2].substring(0, 4);
        final StringBuilder sbs = new StringBuilder();
        sbs.append(yr);

        if (sParts[1].length() == 1) {
            sbs.append("0");
        }
        sbs.append(sParts[1]);

        if (sParts[0].length() == 1) {
            sbs.append("0");
        }
        sbs.append(sParts[0]);

        if (!Xc.ploc().dayEntryExists(Integer.parseInt(sbs.toString()))) {
            return;
        }

        doTheGoTo(yr, sParts[1], sParts[0]);
    }

    private void startNewDayDetector() {

        final Thread th = new Thread(() -> {

            while (true) {
                try {
                    Thread.sleep(5000); // 600000
                }
                catch (InterruptedException e) {
                    break;
                }

                if (DateAndTime.getTodayDateInInt() > todayDate) {
                    Platform.runLater(() -> btnNewDay.setVisible(true));
                }
            }
        });
        th.setDaemon(true);
        th.start();

    }

    private void resetTextHash() {
        textHash = -1;
    }

    private HBox getEntryBox() {

        lvEntries.setPrefHeight(5000);
        final HBox hb = new HBox(lvEntries, getMonthBtnBox());
        hb.setPadding(new Insets(5));
        hb.setSpacing(5);

        return hb;
    }

    private HBox getAddUIbox() {

        final TextField tf = new TextField();
        tf.setPromptText("dd/mm/yyyy");
        tf.setPrefColumnCount(8);
        tf.setAlignment(Pos.CENTER);

        final Button btnX = new Button("x");
        btnX.setOnAction(actionEvent -> {
            tf.clear();
            hbToolBox.getChildren().clear();
            hbToolBox.getChildren().add(hbTools);
        });

        final Button btnOk = new Button("Add");
        btnOk.setOnAction(actionEvent -> addFromAddBox(tf.getText()));

        final HBox hb = new HBox(btnX, tf, btnOk);
        hb.setAlignment(Pos.CENTER_LEFT);
        hb.setPadding(new Insets(5));
        hb.setSpacing(5);
        return hb;
    }

    private HBox getGoToUIbox() {

        final Label lblNa = new Label(" NA!");
        lblNa.setTextFill(Color.web(X.WARNING_TEXT_COLOR));
        lblNa.setVisible(false);

        final TextField tf = new TextField();
        tf.setPromptText("dd/mm/yyyy");
        tf.setPrefColumnCount(8);
        tf.setAlignment(Pos.CENTER);
        tf.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() != KeyCode.ENTER) {
                return;
            }
            goToFromGoToBox(tf.getText(), lblNa);
        });

        final Button btnX = new Button("x");
        btnX.setOnAction(actionEvent -> {
            tf.clear();
            hbToolBox.getChildren().clear();
            hbToolBox.getChildren().add(hbTools);
            lblNa.setVisible(false);
        });

        final Button btnOk = new Button("Go");
        btnOk.setOnAction(actionEvent -> {
            goToFromGoToBox(tf.getText(), lblNa);
        });

        final HBox hb = new HBox(btnX, tf, btnOk, lblNa);
        hb.setAlignment(Pos.CENTER_LEFT);
        hb.setPadding(new Insets(5));
        hb.setSpacing(5);
        return hb;
    }

    private HBox getToolbox() {

        final HBox hb = new HBox();
        hb.setPadding(new Insets(5));
        hb.setSpacing(5);

        final MenuItem miGoto = new MenuItem("GoTo");
        miGoto.setOnAction(actionEvent -> {
            hbToolBox.getChildren().clear();
            hbToolBox.getChildren().add(hbGoto);
        });

        final MenuItem miAdd = new MenuItem("+ Add");
        miAdd.setOnAction(actionEvent -> {
            hbToolBox.getChildren().clear();
            hbToolBox.getChildren().add(hbAdd);
        });

        final SeparatorMenuItem miSep = new SeparatorMenuItem();
        final MenuItem miDelete = new MenuItem("- Delete");
        miDelete.setOnAction(actionEvent -> deleteEntry());

        final MenuButton mbtn = new MenuButton("...");
        mbtn.getItems().addAll(miGoto, miAdd, miSep, miDelete);

        lvEntries.getSelectionModel().selectedIndexProperty().addListener((observableValue, number, t1) -> {

            save(true);
            currentId = -1;
            dailyText.reset();

            try {
                selectEntryFromLV(t1.intValue());
            }
            catch (Exception ignored) {}
        });

        btnNewDay.setOnAction(actionEvent -> {

            todayDate = DateAndTime.getTodayDateInInt();
            try {
                //
                add(todayDate);
                goToByFullId(todayDate);
                btnNewDay.setVisible(false);
            }
            catch (Exception ignored){}
        });
        btnNewDay.setTooltip(new Tooltip("It's a brand new day, create today's entry?"));
        btnNewDay.setVisible(false);
        hb.getChildren().addAll(mbtn, comboYears, btnNewDay);

        return hb;
    }

    private void selectEntryFromLV(int selIndex) throws Exception {

        if (selIndex < 0) {
            return;
        }

        final String selYear = comboYears.getSelectionModel().getSelectedItem();
        final String[] extractedMthAndDay =  extractMonthAndDayFromEntryLabel(lvEntries.getItems().get(selIndex));
        final String selMthAndDay = extractedMthAndDay[0];
        final int selDate = Integer.parseInt(selYear + selMthAndDay);
        // todo maybe should make it match Loose, @ some point v20021.
        final String s = Xc.ploc().getDailyText(selDate);
        currentId = selDate;

        final ResetIndexedFlag riff = new ResetIndexedFlag()
        {
            @Override
            public void resetFlag()
            {
                Xc.ploc().dailySetNotIndexed(currentId);
            }
        };

        dailyText.setText(s, selYear, extractedMthAndDay[1], extractedMthAndDay[2],
                currentId == todayDate, riff);
        textHash = dailyText.getText().hashCode();
        indexOfSelection = selIndex;
    }

    private void deleteEntry() {

        try {

            if (todayDate == currentId) {
                Xc.ploc().deleteTodayEntry(currentId, dailyText.getText());
            }
            else {
                Xc.ploc().deleteDailyEntry(currentId, dailyText.getText());
            }
        }
        catch (Exception ignored) {}

        try {

            int selIndex = lvEntries.getSelectionModel().getSelectedIndex();

            final int year = Integer.parseInt(comboYears.getSelectionModel().getSelectedItem());
            loadEntryListView(year);

            final int size = lvEntries.getItems().size();

            if (size == 0) {
                skipLoadingEntries = true;
                loadYearsComboBox();
                skipLoadingEntries = false;

                goToByFullId(todayDate);
                return;
            }

            if (selIndex == 0) {
                lvEntries.getSelectionModel().selectFirst();
                return;
            }

            if (selIndex >= lvEntries.getItems().size()) {
                lvEntries.getSelectionModel().select(selIndex - 1);
                return;
            }

            lvEntries.getSelectionModel().select(selIndex);
        }
        catch (Exception ignored) {}
    }

    private void addFromAddBox(String s) {

        final String[] dateSeg = s.split("/");
        if (dateSeg.length != 3) {
            return;
        }

        final StringBuilder sbs = new StringBuilder();
        if (dateSeg[2].length() != 4) {
            return;
        }
        sbs.append(dateSeg[2]);

        if (dateSeg[1].length() == 1) {
            sbs.append("0");
        }
        sbs.append(dateSeg[1]);

        if (dateSeg[0].length() == 1) {
            sbs.append("0");
        }
        sbs.append(dateSeg[0]);

        if (sbs.length() != 8) {
            return;
        }

        final int id = Integer.parseInt(sbs.toString());
        try {
            add(id);
            goToByFullId(id);
        }
        catch (Exception ignored) {
            // stay where we are -- OK?
        }
    }

    private void add(int id) throws Exception {
        Xc.ploc().createDayEntry(id);
        skipLoadingEntries = true;
        loadYearsComboBox();
        skipLoadingEntries = false;
    }

    private void goToFromGoToBox(String s, Label lblNa) {

        try {
            // dd/mm/yyyy
            final String[] dateSeg = s.split("/");

            // todo: check if it's available here -- pull data from DB.
            if (!Xc.ploc().dayEntryExists(getIntId(dateSeg))) {
                lblNa.setVisible(true);
                return;
            }

            lblNa.setVisible(false);
            doTheGoTo(dateSeg[2], dateSeg[1], dateSeg[0]);
            lvEntries.scrollTo(indexOfSelection);
        }
        catch (Exception ignored) {
            // exception -- we stay where we are?
        }
    }

    private void goToByFullId(int id) {

        final int d = getD(id);
        if (d == -1) {
            return;
        }

        final String ds = "" + d;
        if (ds.length() != 8) {
            return;
        }

        String month = ds.substring(4, 6);
        if (month.charAt(0) == '0') {
            month = month.substring(1);
        }
        String day =  ds.substring(6);
        if (day.charAt(0) == '0') {
            day = day.substring(1);
        }
        doTheGoTo(ds.substring(0, 4), month, day);
    }

    private int getD(int id) {

        try {

            if (Xc.ploc().dayEntryExists(id)) {
                return id;
            }

            if (Xc.ploc().dayEntryExists(currentId)) {
                return currentId;
            }

            if (Xc.ploc().dayEntryExists(todayDate)) {
                return todayDate;
            }

            final int someEntryId = Xc.ploc().getSomeDayEntry();
            if (someEntryId != -1) {
                return someEntryId;
            }
        }
        catch (Exception ignored) {}
        return -1;
    }

    private void doTheGoTo(String yr, String mth, String day) {

        if (comboYears.getItems().size() == 0) {
            return;
        }

        final String selYr = comboYears.getSelectionModel().getSelectedItem();

        if (!yr.equalsIgnoreCase(selYr)) {
            comboYears.getSelectionModel().select(yr);
        }

        final Integer entryIndex = dayMonth2IndexMap.get(day + "/" + mth);
        if (entryIndex == null) {
            return;
        }
        lvEntries.getSelectionModel().select(entryIndex);
    }

    private void loadYearsComboBox() {

        comboYears.getItems().clear();

        final int[] minMaxDates = Xc.ploc().getMinMaxDates();
        if (minMaxDates.length == 0) {
            return;
        }

        if ((minMaxDates[0] + "").length() != 8) {
            return;
        }

        if ((minMaxDates[1] + "").length() != 8) {
            return;
        }

        final int minYear = DateAndTime.getYearPart(minMaxDates[0]);
        final int maxYear = DateAndTime.getYearPart(minMaxDates[1]);

        final ArrayList<Integer> validYears = new ArrayList<>();

        for (int x = maxYear; x >= minYear ; x--) {

            if (!hasEntryForYear(x)) {
                continue;
            }
            validYears.add(x);
        }

        /*
        // not sure why this is necessary -- so remove 12-11-2023
        final int currentYear = DateAndTime.getTodayDate()[0];
        if (!validYears.contains(currentYear)) {
            validYears.add(currentYear);
        }
        */

        final Integer[] ints = validYears.toArray(new Integer[0]);
        Arrays.sort(ints);
        for (int x : ints) {
            comboYears.getItems().add(0, "" + x);
        }
    }

    private boolean hasEntryForYear(int x) {

        try {
            return Xc.ploc().hasEntryForYear(x);
        }
        catch (Exception e) {
            return false;
        }
    }

    private void loadEntryListView(int year) {

        entriesAnchorMap.clear();
        dayMonth2IndexMap.clear();
        lvEntries.getItems().clear();
        disableMthBtns();

        try {

            final ArrayList<Integer> alsT = Xc.ploc().getEntriesForYear(year);
            final Integer[] entriesInTheYear = alsT.toArray(new Integer[0]);
            Arrays.sort(entriesInTheYear);

            // we need this so we can scroll to different months (by clicking the month button)
            // hash map stored the display text, and the month
            // eg. mm/DD named-day, month_in_int
            // later we can get the index from the list.
            final HashMap<String, Integer> tempAnchorMap = new HashMap<>();
            int currentMth = -1;

            for (Integer i : entriesInTheYear) {
                final int[] dateInfo = DateAndTime.getDateDetails(i);
                // dd/mm named_day
                final String s = dateInfo[2] + "/" + dateInfo[1] + " " + DateAndTime.getNamedDay(dateInfo[3])[0];
                lvEntries.getItems().add(0, s);
                // dateInfo[1] is the month.
                if (dateInfo[1] != currentMth) {
                    currentMth = dateInfo[1];
                    tempAnchorMap.put(s, currentMth);
                }
            }

            final int entryCount = lvEntries.getItems().size();
            for (int entryIndex = 0; entryIndex < entryCount; entryIndex++) {
                final String entryTextLabel = lvEntries.getItems().get(entryIndex);
                final String dayAndMonthString = entryTextLabel.substring(0, entryTextLabel.indexOf(" "));
                dayMonth2IndexMap.put(dayAndMonthString, entryIndex);
            }

            for (Map.Entry<String, Integer> entry : tempAnchorMap.entrySet()) {
                final int entryIndex = lvEntries.getItems().indexOf(entry.getKey());
                final int month = entry.getValue();
                entriesAnchorMap.put(month, entryIndex);
                enableMthBtn(month);
            }
        }
        catch (Exception e) {
            // todo -- handle properly?
        }
    }

    private String[] extractMonthAndDayFromEntryLabel(String s) {

        final StringBuilder dayPart = new StringBuilder();
        final StringBuilder monthPart = new StringBuilder();

        boolean slashDone = false;

        for (int i = 0; i < s.length(); i++) {

            final String c = s.substring(i, i+1);
            if (slashDone) {
                if (" ".equalsIgnoreCase(c)) {
                    break;
                }
                monthPart.append(c);
            }
            else {
                if ("/".equalsIgnoreCase(c)) {
                    slashDone = true;
                    continue;
                }
                dayPart.append(c);
            }
        }

        final String[] ss = new String[3];

        ss[2] = dayPart.toString();
        if (dayPart.length() < 2) {
            dayPart.insert(0, "0");
        }

        ss[1] = monthPart.toString();
        if (monthPart.length() < 2) {
            monthPart.insert(0, "0");
        }

        ss[0] = monthPart.toString() + dayPart.toString();

        return ss;
    }

    private int getIntId(String[] s) {

        final StringBuilder sbs = new StringBuilder();
        sbs.append(s[2]);
        if(s[1].length() < 2) {
            sbs.append("0");
        }
        sbs.append(s[1]);
        if (s[0].length() < 2) {
            sbs.append("0");
        }
        sbs.append(s[0]);

        return Integer.parseInt(sbs.toString());
    }

    private void enableMthBtn(int i) {

        try {
            btnsMonth[i-1].setDisable(false);
        }
        catch (Exception ignored) {}
    }

    private void disableMthBtns() {

        for (Button b : btnsMonth) {
            b.setDisable(true);
        }
    }

    private VBox getMonthBtnBox() {

        final VBox vbMonthList = new VBox();
        vbMonthList.setSpacing(5);

        for (Button tbn : btnsMonth) {

            final int x = Integer.parseInt(tbn.getText());
            tbn.setMinWidth(32);
            vbMonthList.getChildren().add(tbn);
            tbn.setOnAction(actionEvent -> {
                jumpTo(x);
            });
        }

        final Button btnToday = new Button("-");
        btnToday.setMinWidth(32);
        vbMonthList.getChildren().add(btnToday);
        btnToday.setOnAction(actionEvent ->
        {
            goToByFullId(todayDate);
            lvEntries.scrollTo(indexOfSelection);
            dailyText.gotoEnd();
        });

        return vbMonthList;
    }

    private void jumpTo(int mth) {

        final Integer index = entriesAnchorMap.get(mth);
        if (index == null) {
            return;
        }
        lvEntries.getSelectionModel().select(index);
        lvEntries.scrollTo(indexOfSelection);
    }

    private void searchEntries(String kWord) {

        if (tfSearchEntries.isEditable())
        {
            lvSearchedEntries.getItems().clear();
            tfSearchEntries.setEditable(false);
        }
        dailyText.setEntrySearchTerm(kWord);

        final ArrayList<String> xx = Xc.ploc().searchEntries(kWord);
        for (String s : xx) {
            if (s.charAt(0) != 'd') {
                continue;
            }

            final int id = Integer.parseInt(s.substring(2));
            if (Xc.ploc().isDeleted_daily(id)) {
                continue;
            }

            final int[] dateInfo = DateAndTime.getDateDetails(id);
            final String y = dateInfo[2] + "/" + dateInfo[1] + "/" + dateInfo[0] + " "
                    + DateAndTime.getNamedDay(dateInfo[3])[0];
            lvSearchedEntries.getItems().add(y);
        }

        lvSearchedEntries.scrollTo(lvSearchedEntries.getItems().size() - 1);

        if (btnsMonth[0].getUserData() != null) {
            return;
        }

        hbToolBox.getChildren().clear();
        hbToolBox.getChildren().add(hbTheSearchThing);

        final StackPane sp = new StackPane(lvSearchedEntries);
        sp.setPadding(new Insets(5));

        getChildren().remove(1);
        getChildren().add(1, sp);

        btnsMonth[0].setUserData("");
    }

    private HBox getSearchBox() {

        final Button btn = new Button(">");
        btn.setOnAction(actionEvent -> {
            searchEntries(tfSearchEntries.getText());
            btn.setDisable(Xc.ploc().ftSearch.isDone());
        });

        tfSearchEntries.setPromptText("Search entries");
        tfSearchEntries.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                searchEntries(tfSearchEntries.getText());
                btn.setDisable(Xc.ploc().ftSearch.isDone());
            }
        });

        final Button tbt = new Button("x");
        tbt.setOnAction(actionEvent -> {
            clearFullTextSearch();
            btn.setDisable(false);

        });

        final HBox hbx = new HBox(tbt, tfSearchEntries, btn);
        hbx.setSpacing(5);

        hbx.setPadding(new Insets(5));

        return hbx;
    }

    public synchronized void save(boolean resetId) {

        if (currentId == -1) {
            return;
        }

        // Thread-1 threw exception - something, something null.
        final String x = dailyText.getText();
        if (x.hashCode() != textHash) {
            try {
                Xc.ploc().storeDailyText(currentId, x);
                textHash = x.hashCode();
            } catch (Exception ignored) {}
        }

        if (resetId) {
            currentId = -1;
        }
    }

    private void clearFullTextSearch()
    {
        Xc.ploc().ftSearch.clearSearch();
        tfSearchEntries.clear();
        tfSearchEntries.setEditable(true);
    }
}
