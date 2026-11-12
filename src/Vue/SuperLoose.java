package Vue;

import javafx.application.Platform;
import javafx.scene.layout.HBox;

public class SuperLoose extends HBox {

    private final LooseRightBox looseRightBox;

    public SuperLoose() {

        final DailyText ltx = new DailyText();
        looseRightBox = new LooseRightBox(ltx);

        ltx.setPrefWidth(30000);
        looseRightBox.setPrefWidth(242);
        setSpacing(10);
        getChildren().addAll(ltx, looseRightBox);

        final Thread th = new Thread(() -> {

            while (true) {
                try {
                    Thread.sleep(25000);
                }
                catch (InterruptedException ignored) {}
                Platform.runLater(() -> looseRightBox.save(false));
            }
        });
        th.setDaemon(true);
        th.start();
    }

    public void totoSave() {
        looseRightBox.save(true);
    }
}
