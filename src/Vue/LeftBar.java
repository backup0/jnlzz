package Vue;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class LeftBar extends VBox {

    private final ToggleButton tbtDaily = new ToggleButton("D");
    private final ToggleButton tbtLoose = new ToggleButton("L");
    private final ToggleButton tbtHash = new ToggleButton("#");
    private final ToggleButton tbtSettings = new ToggleButton("*");

    public LeftBar(final GlueNo1 g1) {

        final ToggleGroup toggleGroup = new ToggleGroup();

        tbtDaily.setUserData(0);
        tbtDaily.setTooltip(new Tooltip("Daily"));

        tbtLoose.setUserData(1);
        tbtLoose.setTooltip(new Tooltip("Loose"));

        tbtHash.setUserData(2);
        tbtHash.setTooltip(new Tooltip("Hash list"));

        tbtSettings.setUserData(3);
        tbtSettings.setTooltip(new Tooltip("Settings"));

        tbtDaily.setToggleGroup(toggleGroup);
        tbtLoose.setToggleGroup(toggleGroup);
        tbtHash.setToggleGroup(toggleGroup);
        tbtSettings.setToggleGroup(toggleGroup);

        tbtDaily.setMinWidth(28);
        tbtLoose.setMinWidth(28);
        tbtHash.setMinWidth(28);
        tbtSettings.setMinWidth(28);

        final Button btnAbout = new Button("?");
        btnAbout.setMinWidth(28);
        btnAbout.setTooltip(new Tooltip("About"));
        btnAbout.setOnAction(actionEvent -> {
            // tbad
        });

        final Button btnClose = new Button("x");
        btnClose.setMinWidth(28);
        btnClose.setTooltip(new Tooltip("Close notebook"));
        btnClose.setOnAction(actionEvent -> {
            // close the shit first ..
            Xc.toBeClosed(); // <-- that, then we can show the entry page.
            Xc.showEntryPage();
        });

        setAlignment(Pos.TOP_CENTER);
        setSpacing(5);
        setPadding(new Insets(5));
        getChildren().addAll(tbtDaily, tbtLoose, tbtHash, tbtSettings, new Label(""), btnAbout, btnClose);

        toggleGroup.selectedToggleProperty().addListener((observableValue, toggle, t1) -> {

            if (toggleGroup.getUserData() == null) {
                // first time -- ?
                if (t1 == null) {
                    return;
                }
                toggleGroup.setUserData(-1);
            }

            final int usrData = (int) toggleGroup.getUserData();

            if (t1 == null) {
                // no change -- same cb is clicked

                switch (usrData) {
                    case 0:
                        tbtDaily.setSelected(true);
                        break;
                    case 1:
                        tbtLoose.setSelected(true);
                        break;
                    case 2:
                        tbtHash.setSelected(true);
                        break;
                    case 3:
                        tbtSettings.setSelected(true);
                        break;
                    default:
                        throw new RuntimeException("errN0datar");
                }
                return;
            }

            final int t1UserData = (int) t1.getUserData();
            if (t1UserData == usrData) {
                return;
            }

            toggleGroup.setUserData(t1UserData);
            g1.switchX(t1UserData);
        });
    }

    public void select(int i) {

        switch (i) {
            case 0:
                tbtDaily.setSelected(true);
                break;
            case 1:
                tbtLoose.setSelected(true);
                break;
            case 2:
                tbtHash.setSelected(true);
                break;
            case 3:
                tbtSettings.setSelected(true);
                break;
            default:
                throw new RuntimeException("err45eShock");
        }
    }
}
