package Vue;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;

import java.nio.file.Path;
import java.util.Arrays;

public class SettingsPage extends TabPane {

    private final TextArea ta = new TextArea();

    public SettingsPage() {

        setPrefSize(5000, 5000);
        setPadding(new Insets(5));
        ex();
    }

    private void ex() {

        ta.setEditable(false);
        loadStats();
        final Tab taTab = new Tab("Stats", ta);
        taTab.setClosable(false);

        final Tab ndTab = new Tab("2nd", hit());
        ndTab.setClosable(false);

        getTabs().addAll(taTab, ndTab);
    }

    private void loadStats() {

        ta.appendText("Book location: " + Xc.ploc().getDbPath());

        ta.appendText("\n\nApp data: " + GlobalX.jnlzAppDataFolder);

        ta.appendText("\n\nSearch index/backup name: " + Xc.ploc().ftsDBName());
        
        final Path p = GlobalX.fullTextSearchDBFolder.resolve(Xc.ploc().ftsDBName() + ".mv.db");
        ta.appendText("\n\n*FTS index: " + p);
        ta.appendText("\n*Backup folder: " + GlobalX.bookBackupFolder.toString());
        ta.appendText("\n\nEntries [dly, dly deleted, lse, lse deleted]: " + Arrays.toString(Xc.ploc().countEntries()));

        // pin set/not set
        // how mnay entries not indexed yet.
        // etc. add as much detals as you please.
        // book location
    }

    private VBox hit()
    {
        final VBox vBox = new VBox();
        vBox.setPadding(new Insets(10));
        vBox.setSpacing(5);

        final String s = "Rebuilt full text search database's indice.";
        final Button btn1 = new Button(s);

        btn1.setOnAction(actionEvent -> {
            btn1.setText("(IN PROGRESS) " + s);
            Xc.ploc().dropFTSIndice();
            Xc.ploc().addFTSMapIndex();
            btn1.setText(s);
        });

        final Path p = GlobalX.fullTextSearchDBFolder.resolve(Xc.ploc().ftsDBName() + ".mv.db");
        final Label x1 = new Label("To rebuilt the full text search database, delete the database file at ");
        final Label x2 = new Label(" then rebuild the indice (recommended), may help reduce the database size.");
        final TextField tf1 = new TextField(p.toString());
        tf1.setEditable(false);

        final TextFlow tf = new TextFlow();
        tf.getChildren().add(x1);
        tf.getChildren().add(tf1);
        tf.getChildren().add(x2);

        vBox.getChildren().addAll(btn1, tf);
        return vBox;
    }
}
