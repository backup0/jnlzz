package Vue;

import javafx.application.Platform;
import javafx.scene.layout.HBox;

public class SuperDaily extends HBox {

    private final DailyRightBox rightBoxForDaily;

    public SuperDaily() {

        final DailyText tai = new DailyText();
        rightBoxForDaily = new DailyRightBox(tai);

        tai.setPrefWidth(30000);
        rightBoxForDaily.setPrefWidth(242);
        setSpacing(10);
        getChildren().addAll(tai, rightBoxForDaily);

        final Thread th = new Thread(() -> {

            while (true) {
                try {
                    Thread.sleep(25000);
                }
                catch (InterruptedException ignored) {}
                Platform.runLater(() -> rightBoxForDaily.save(false));
            }
        });
        th.setDaemon(true);
        th.start();
    }

    public void totoSave() {
        rightBoxForDaily.save(true);
    }
}
