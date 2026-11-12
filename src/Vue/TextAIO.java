package Vue;

import javafx.geometry.Insets;
import javafx.scene.layout.HBox;

public class TextAIO extends HBox {

    private final SuperDaily daily = new SuperDaily();
    private final SuperLoose loose = new SuperLoose();

    public TextAIO() {

        final LeftBar lfb;
        lfb = new LeftBar(this::doSwitch);
        lfb.setPrefWidth(28);

        //setStyle("-fx-background-color: white"); 18.2.25 -- change to dark modey;
        getChildren().add(lfb);

        setSpacing(10);
        setPadding(new Insets(0,0, 10, 0));

        lfb.select(0);
    }

    private void doSwitch(int i) {

        if (getChildren().size() == 2) {
            getChildren().remove(1);
        }

        switch (i) {
            case 0:
                getChildren().add(daily);
                break;
            case 1:
                getChildren().add(loose);
                break;
            case 2:
                getChildren().add(new HashStuff());
                break;
            case 3:
                getChildren().add(new SettingsPage());
                break;
            default:
                throw new RuntimeException("err45fNm");
        }
    }

    public void toBeClosed() {
        daily.totoSave();
        loose.totoSave();
    }
}
