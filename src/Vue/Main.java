package Vue;

import Misc.GlobalStartCheck;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.HashMap;

public class Main extends Application {

    Stage pStage = null;
    Scene xScene = null;
    private final String settingWinPos = "win_pos";

    @Override
    public void start(Stage primaryStage) {

        pStage = primaryStage;
        final ImgGetter ig = new ImgGetter();
        primaryStage.getIcons().addAll(ig.getImg("/imgs/iconb32x.png", false));

        //Font.loadFont(getClass().getResourceAsStream("imgs/NunitoSans-Regular.ttf"), 16);
        Font.loadFont(getClass().getResourceAsStream("imgs/Inter_18pt-Regular.ttf"), 16);

        final StackPane spe = new StackPane();
        spe.setPadding(new Insets(10, 0, 0, 0));
        final Scene scene = new Scene(spe, 650, 480);
        try //>> navigate to other, then back -- keep history ...
        {
            Application.setUserAgentStylesheet(getClass().getResource("/skin/style.css").toExternalForm());
            //Application.setUserAgentStylesheet(getClass().getResource("/skin/cupertino-dark.css").toExternalForm());
            //Application.setUserAgentStylesheet(getClass().getResource("/skin/dracula.css").toExternalForm());
            //Application.setUserAgentStylesheet(getClass().getResource("/skin/nord-dark.css").toExternalForm());
            //Application.setUserAgentStylesheet(getClass().getResource("/skin/primer-dark.css").toExternalForm());
        }
        catch (Exception ignore) {}
        xScene = scene;

        primaryStage.setTitle("- j - n - l - z -");
        primaryStage.setScene(scene);

        KVPairFile kvp = new KVPairFile(GlobalX.settingsFile);
        try {
            kvp.readContent();
            initSetting(kvp);
        }
        catch (Exception e) {
            kvp = null;
        }

        primaryStage.show();
        
        final String s = GlobalStartCheck.doStartCheck();
        if (s.isEmpty()) {
            GlobalX.scene = scene;
            Xc.setStackPane(spe);
            Xc.setEntryPage(new EntryPage(kvp));
            Xc.showEntryPage();
        }
        else {
            showError(s, spe);
        }
    }

    private void showError(String msg, StackPane spn) {

        final String s = "Error during start check!\n\n" + msg;
        final Text txt = new Text();
        txt.setText(s);

        spn.setPadding(new Insets(10));
        spn.setAlignment(Pos.TOP_LEFT);
        spn.getChildren().add(txt);
    }

    private void initSetting(KVPairFile kvp) {

        final HashMap<String, String> map = kvp.getMap();
        final String s = map.get(settingWinPos);
        if (s == null) {
            return;
        }
        // [0(maximized)|>0(not)]:w:h
        // resize here.
        final String[] sps = s.split(":");
        if (sps.length != 3) {
            return;
        }

        if ("0".equalsIgnoreCase(sps[0])) {
            // maximize
            pStage.setMaximized(true);
            return;
        }

        try {
            final double height = Double.parseDouble(sps[1]);
            final double width = Double.parseDouble(sps[2]);

            if (height > Screen.getPrimary().getBounds().getHeight())
            {
                pStage.setHeight(480);
            }
            else
            {
                pStage.setHeight(height);
            }

            if (width > Screen.getPrimary().getBounds().getWidth())
            {
                pStage.setWidth(650);
            }
            else
            {
                pStage.setHeight(width);
            }
        }
        catch (Exception ignored) {}
    }

    private void commitSettingsAtClose() {

        final KVPairFile kvp = new KVPairFile(GlobalX.settingsFile);
        try {
            kvp.readContent();

            final String s;
            if (pStage.isMaximized()) {
                s = "0:0:0";
            }
            else {
                s = "1:" + pStage.getWidth() + ":" + pStage.getHeight();
            }
            kvp.getMap().put(settingWinPos, s);
            kvp.writeContent();
        }
        catch (Exception ignored) {}
    }

    public void stop() throws Exception {
        Xc.toBeClosed();
        commitSettingsAtClose();
        super.stop();
    }

    public Main() {
        System.setProperty("prism.allowhidpi", "true");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
