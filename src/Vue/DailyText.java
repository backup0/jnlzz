package Vue;

import Misc.ExImBackend;
import Misc.SpellChecker;
import Misc.X;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.HashMap;
import java.util.HashSet;

public class DailyText extends VBox {

    private final TextArea ta = new TextArea();
    private final Label foo = new Label();
    private final String[] date = new String[]{"", "", ""};
    private final Button btnEntrySearch = new Button("b");
    private final CrossSearch crs = new CrossSearch(ta);
    private final ToggleButton toggleEditable = new ToggleButton("Editable");
    private final String notice = "If you're seeing this text, something has gone wrong, restart the program.";
    //private int id = -1;
    private ResetIndexedFlag rif = null;
    private final HashSet<String> badWords = new HashSet<>();
    private final FlowPane fp = new FlowPane();
    String currentScString = "";
    private int docId = -1;
    private Thread spellCheckerThread;
    private final Button btnForceSpellCheck = new Button("fsck");
    private final Label lblWordCount = new Label();
    private final HashMap<String, Integer> caretHistory = new HashMap<>();

    public DailyText() {
        setSpacing(5);
        btnEntrySearch.setDisable(true);
        getChildren().addAll(toBox3(), vta(), searchBox());
        GlobalX.implementSpecialStrokes(ta);

        //ta.setStyle("-fx-font: 14px 'Open Sans';");
        //ta.setStyle("{-fx-font: 14px 'Nunito Sans Regular'}");
        //ta.setFont(Font.font("Nunito Sans", FontWeight.NORMAL, 15));
        ta.setFont(Font.font("Inter 18pt", FontWeight.NORMAL, 16));

        ta.setText(notice);
        ta.setEditable(false);
        ta.setPadding(new Insets(0, 0, 0, 100));
        ta.setPrefHeight(450);

        toggleEditable.selectedProperty().addListener((observableValue, aBoolean, t1) -> {
            ta.setEditable(t1);

            if (docId != -1) {
                Xc.ploc().setEditabilityForLoose(docId, t1);
            }

            btnForceSpellCheck.setDisable(!toggleEditable.isSelected());
            if (!toggleEditable.isSelected())
            {
                fp.getChildren().clear();
            }
        });

        btnForceSpellCheck.setOnAction(actionEvent -> {
            if (spellCheckerThread != null)
            {
                spellCheckerThread.interrupt();
            }
        });

        fp.setVgap(2.5);

        spellCheckerStuff();
    }

    private void spellCheckerStuff()
    {
        final Thread th = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    while (true)
                    {
                        try
                        {
                            //Thread.sleep(10_000); // 60000 // 300000
                            //Thread.sleep(60_000); //1 min
                            Thread.sleep(300_000); //5 mins
                        }
                        catch (Exception ignore) {}

                        Platform.runLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                Platform.runLater(() -> checkSpelling(ta.getText()));
                            }
                        });
                    }
                }
                catch (Exception ignore)
                {
                }
            }
        });
        th.setDaemon(true);
        th.start();
        spellCheckerThread = th;
    }

    private void checkSpelling(final String s)
    {
        if (!toggleEditable.isSelected())
        {
            return;
        }

        final Thread th = new Thread(() ->
        {
            badWords.clear();
            final var wrds = SpellChecker.checkSpelling(s, badWords);

            Platform.runLater(() ->
            {
                fp.getChildren().clear();
                for (String s1 : badWords)
                {
                    final var b1 = new Button(s1);
                    b1.setOnMouseClicked(mouseEvent -> {

                        if (mouseEvent.isControlDown())
                        {
                            SpellChecker.addWord(b1.getText());
                            b1.setDisable(true);
                            return;
                        }
                        findMisspelledWord(b1);

                    });
                    fp.getChildren().add(b1);
                }
                lblWordCount.setText(wrds + "");
            });
        });
        th.setDaemon(true);
        th.start();
    }

    private void findMisspelledWord(Button b1)
    {
        ta.positionCaret(0);
        var i0 = ta.getText().indexOf(b1.getText(), ta.getCaretPosition());
        if (i0 == -1)
        {
            b1.setDisable(true);
            return;
        }

        if (b1.getUserData() != null)
        {
            ta.positionCaret((int) b1.getUserData());
        }

        if (!currentScString.equals(b1.getText()))
        {
            ta.positionCaret(0);
            currentScString = b1.getText();
        }

        var i = ta.getText().indexOf(b1.getText(), ta.getCaretPosition());
        if (i == -1)
        {
            ta.positionCaret(0);
            i = ta.getText().indexOf(b1.getText(), ta.getCaretPosition());
        }

        final var cp = i + (b1.getText().length());
        ta.selectRange(i, cp);
        ta.requestFocus();

        b1.setUserData(cp);
    }

    private VBox vta() {

        foo.setFont(Font.font(foo.getFont().getFamily(), FontWeight.BOLD, 14));
        ta.setFont(Font.font(14));
        ta.setWrapText(true);
        ta.setPrefHeight(10000);

        VBox abx = new VBox(ta);
        abx.setPadding(new Insets(0, 5, 0, 5));
        abx.setSpacing(10);
        return abx;
    }

    private HBox toBox3() {

        final Button btnExport = new Button("Export");
        btnExport.setOnAction(actionEvent -> {
            ExImBackend.export(getExportFileName(), ta.getText());
        });

        final MenuItem miResetIndexedFlag = new MenuItem("Re-index (full text search)");
        miResetIndexedFlag.setOnAction(actionEvent -> rif.resetFlag());

        final MenuButton mbtn = new MenuButton("...");
        mbtn.getItems().addAll(miResetIndexedFlag);

        final Label lblSpacer = new Label(" .... .. ..... .. .......");
        lblSpacer.setVisible(false);

        final Label lblSpacer2 = new Label(" .... .. ..... .. .......");
        lblSpacer2.setVisible(false);

        fp.setAlignment(Pos.CENTER_LEFT);
        fp.setHgap(5);

        final HBox tlb = new HBox(toggleEditable, new Label(" ".repeat(5)), btnExport, mbtn,
                lblSpacer, foo, lblSpacer2, lblWordCount);
        tlb.setAlignment(Pos.CENTER_LEFT);
        tlb.setSpacing(5);
        tlb.setPadding(new Insets(5));
        return tlb;
    }

    private String getExportFileName() {

        final StringBuilder sbs = new StringBuilder();

        sbs.append(date[0]);
        sbs.append("-");

        if (date[1].length() == 1) {
            sbs.append("0");
        }
        sbs.append(date[1]);
        sbs.append("-");

        if (date[2].length() == 1) {
            sbs.append("0");
        }
        sbs.append(date[2]);

        return sbs + ".txt";
    }

    private HBox searchBox() {

        //HBox
        final TextField tf = new TextField();
        tf.setPromptText("Search text");

        final Button btnSearch = new Button("a");
        btnSearch.setOnAction(actionEvent -> GlobalX.searchy(ta, tf.getText()));

        final Label lblSpacer = new Label("... ..");
        lblSpacer.setVisible(false);

        btnEntrySearch.setOnAction(actionEvent -> crs.search());

        final HBox hbx = new HBox(tf, btnSearch, btnEntrySearch,  lblSpacer, btnForceSpellCheck, fp);
        hbx.setSpacing(5);
        hbx.setPadding(new Insets(5));
        return hbx;
    }

    public void setEntrySearchTerm(String kw) {

        if (kw == null) {
            btnEntrySearch.setDisable(true);
            return;
        }

        btnEntrySearch.setDisable(false);
        crs.setKeyWords(kw);
    }

    public void setText(String text, String yr, String mth, String day, boolean editable, ResetIndexedFlag r) {

        ta.setText(text);

        ta.setEditable(editable);
        toggleEditable.setSelected(editable);

        foo.setText(day + "/" + mth + "/" + yr);
        // use the below for exporting -- yo!
        date[0] = yr;
        date[1] = mth;
        date[2] = day;

        rif = r;
        fp.getChildren().clear();
        setCaret();
    }

    public void setText(int id, String txt, boolean editable, ResetIndexedFlag r) {

        docId = id;
        toggleEditable.setSelected(editable);
        ta.setText(txt);
        rif = r;
        fp.getChildren().clear();
        setCaret();
    }

    public void reset() {

        storeCaretLocation();

        if (docId != -1)
        {
            ta.setText(notice);
            foo.setText("?");
        }
        else
        {
            ta.setText(notice);
            ta.setEditable(false);
            foo.setText("00/00/00");
        }
        lblWordCount.setText("");
    }

    public String getText() {
        return ta.getText();
    }

    public void setName(String s) {
        foo.setText(s);
    }

    public void gotoEnd()
    {
        ta.end();
        ta.requestFocus();
    }

    private void storeCaretLocation()
    {
        if (!toggleEditable.isSelected())
        {
            return;
        }

        if (foo.getText().isEmpty())
        {
            return;
        }

        //if (caretHistory.keySet().size() > 1 million??)

        if (docId == -1)
        {
            caretHistory.put(foo.getText(), ta.getCaretPosition());
        }
        else
        {
            caretHistory.put(docId + "", ta.getCaretPosition());
        }
    }

    private void setCaret()
    {
        final Integer i;
        if (docId == -1)
        {
           i = caretHistory.get(foo.getText());
        }
        else
        {
            i = caretHistory.get(docId + "");
        }

        if (i == null)
        {
            return;
        }

        Platform.runLater(() -> {
            ta.requestFocus();
            ta.positionCaret(i);
        });
    }
}
