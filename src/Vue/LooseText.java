package Vue;

import Misc.ExImBackend;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class LooseText extends VBox {

    private final TextArea ta = new TextArea();
    private final Label lblName = new Label();
    private final ToggleButton toggleEditable = new ToggleButton("Editable");
    private final Button btnEntrySearch = new Button(" > >> ");
    private final CrossSearch crs = new CrossSearch(ta);
    private final String notice = "If you're seeing this text, something is not right. Restart.";
    private int docId = -1;
    private ResetIndexedFlag rif = null;


    public LooseText() {

        setSpacing(5);
        btnEntrySearch.setDisable(true);
        getChildren().addAll(toBox3(), vta(), searchBox());
        setAlignment(Pos.CENTER);
        GlobalX.implementSpecialStrokes(ta);

        ta.setText(notice);
        ta.setEditable(false);

        toggleEditable.selectedProperty().addListener((observableValue, aBoolean, t1) -> {

            ta.setEditable(t1);

            if (docId != -1) {
                Xc.ploc().setEditabilityForLoose(docId, t1);
            }
        });
    }

    private VBox vta() {

        lblName.setFont(Font.font(lblName.getFont().getFamily(), FontWeight.BOLD, 16));
        lblName.setPadding(new Insets(5));
        lblName.setText("?");

        ta.setFont(Font.font(14));
        ta.setWrapText(true);
        ta.setPrefHeight(10000);

        toggleEditable.setSelected(false);

        final VBox abx = new VBox(lblName, ta);
        abx.setPadding(new Insets(0, 5, 0, 5));
        abx.setSpacing(10);
        return abx;
    }

    private BorderPane toBox3() {

        final MenuItem miExport = new MenuItem("Export");
        miExport.setOnAction(actionEvent -> ExImBackend.export(lblName.getText() + ".txt", ta.getText()));

        final MenuItem miResetIndexedFlag = new MenuItem("Re-index (full text search)");
        miResetIndexedFlag.setOnAction(actionEvent -> rif.resetFlag());

        final MenuButton mbtn = new MenuButton("...");
        mbtn.getItems().addAll(miExport, miResetIndexedFlag);

        final Label foo = new Label("//");
        final HBox tlb = new HBox(toggleEditable, mbtn);
        tlb.setSpacing(5);
        //tlb.setPadding(new Insets(5));

        final BorderPane bpn = new BorderPane();
        bpn.setLeft(tlb);
        bpn.setRight(foo);

        bpn.setPadding(new Insets(5));

        return bpn;
    }

    private HBox searchBox() {

        //HBox
        final TextField tf = new TextField();
        tf.setPromptText("Search text");

        final Button btnNext = new Button(">");
        btnNext.setOnAction(actionEvent -> {
            GlobalX.searchy(ta, tf.getText());
        });

        final Label lblSpacer = new Label("... ..... .. ..");
        lblSpacer.setVisible(false);

        btnEntrySearch.setOnAction(actionEvent -> crs.search());

        final HBox hbx = new HBox(tf, btnNext, lblSpacer, btnEntrySearch);
        hbx.setSpacing(5);
        hbx.setPadding(new Insets(5));

        return hbx;
    }

    public void setName(String s) {
        lblName.setText(s);
    }

    public void setEntrySearchTerm(String kw) {

        if (kw == null) {
            btnEntrySearch.setDisable(true);
            return;
        }

        btnEntrySearch.setDisable(false);
        crs.setKeyWords(kw);
    }

    public void setText(int id, String txt, boolean editable, ResetIndexedFlag r) {

        docId = id;
        toggleEditable.setSelected(editable);
        ta.setText(txt);
        rif = r;
    }

    public void reset() {
        ta.setText(notice);
        lblName.setText("?");
    }

    public String getText() {
        return ta.getText();
    }
}
