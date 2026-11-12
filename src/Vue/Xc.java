package Vue;

import Misc.Backup;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;

public class Xc {

    private static StackPane sp = null;
    private static EntryPage ep = null;
    private static TextAIO tai = null;
    private static Ploc ploc = null;

    public static void setStackPane(StackPane p) {
        //p.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null))); // 18.2.25 -- change to dark modey;
        sp = p;
    }

    public static void setEntryPage(EntryPage n) {
        ep = n;
    }

    public static void setTAIO(TextAIO taio) {
        tai = taio;
    }

    public static void showEntryPage() {
        sp.setAlignment(Pos.CENTER);
        sp.getChildren().clear();
        sp.getChildren().add(ep);
    }

    public static void showNoteBook() {
        sp.getChildren().clear();
        sp.getChildren().add(tai);
    }

    public static void showIndexingStuff() {
        sp.getChildren().clear();
        final HBox hbx = new HBox();
        hbx.setPadding(new Insets(10));

        final Label lbl = new Label("Indexing entries ...");
        hbx.getChildren().add(lbl);

        sp.getChildren().add(hbx);
    }

    public static void setPloc(Path dbPath, String pwd) throws SQLException {
        // todo test if cb is actually jnlz db.
        ploc = new Ploc(dbPath, pwd);
    }

    public static void toBeClosed() {
        if (tai != null) {
            tai.toBeClosed();
        }

        if (ploc != null) {
            ploc.close();
            Backup.backup();
        }
        ploc = null;
    }

    public static void showImportingStuff() {
        sp.getChildren().clear();
        sp.getChildren().add(new ImportingUI());
    }

    public static Ploc ploc() {
        return ploc;
    }

    public static void actionOnOpening() {

        showIndexingStuff();

        final Thread th = new Thread(() -> {

            final ArrayList<Integer> unindexedDaily = ploc.getUnIndexedDaily();
            for (int d : unindexedDaily) {

                try {
                    final String x = ploc.getDailyText(d);
                    ploc.updateFT("d_" + d, x);
                    ploc.dailySetIndexed(d);

                } catch (Exception ignored) {}
            }

            final ArrayList<Integer> unindexedLoose = ploc.getUnIndexedLoose();
            for (int d : unindexedLoose) {
                try {
                    final String x = ploc.getLooseText(d);
                    ploc.updateFT("l_" + d, x);
                    ploc.looseSetIndexed(d);

                } catch (Exception ignored) {}
            }
            // because creating index first will cause the db to grow to a ridiculous size when there's a lot
            // of entries inserted quickly. Create after mass insertion.
            ploc.addFTSMapIndex();

            Platform.runLater(() -> {
                setTAIO(new TextAIO());
                showNoteBook();
            });
        });
        th.setDaemon(true);
        th.start();
    }
}
