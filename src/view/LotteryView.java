package view;

import javafx.scene.Node;
import javafx.scene.Node;
import controller.LotteryController;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import model.User;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.io.File;

public class LotteryView {
    private final Stage stage;
    private final FlowPane winnersDisplayPane;
    private final Label currentStatusLabel;
    private final LotteryController controller;
    private final TextField countField;
    private final CheckBox repeatCheck;
    private final Map<String, Image> imageCache = new HashMap<>();

    public LotteryView(Stage stage, List<User> users) {
        this.stage = stage;
        this.currentStatusLabel = new Label("请点击开始抽奖");
        this.currentStatusLabel.setFont(Font.font("Microsoft YaHei", 24));
        this.winnersDisplayPane = new FlowPane();
        this.winnersDisplayPane.setAlignment(Pos.CENTER);
        this.winnersDisplayPane.setHgap(10);
        this.winnersDisplayPane.setVgap(10);

        Label countLabel = new Label("每次抽取人数：");
        countField = new TextField("1");
        countField.setPrefWidth(60);
        repeatCheck = new CheckBox("允许重复抽取");
        repeatCheck.setSelected(false);

        HBox optionsBox = new HBox(10, countLabel, countField, repeatCheck);
        optionsBox.setAlignment(Pos.CENTER);

        Button startBtn = new Button("开始抽奖");
        Button stopBtn = new Button("结束抽奖");

        HBox buttonBox = new HBox(20, startBtn, stopBtn);
        buttonBox.setAlignment(Pos.CENTER);

        VBox root = new VBox(20, currentStatusLabel, winnersDisplayPane, optionsBox, buttonBox);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #F5F7FA, #b8c4db);");

        Scene scene = new Scene(root, 500, 450);
        stage.setScene(scene);
        stage.setTitle("JavaFX 抽奖程序");
        stage.show();

        controller = new LotteryController(users, this);

        startBtn.setOnAction(e -> controller.start());
        stopBtn.setOnAction(e -> controller.stop());
    }

    public void updateDisplay(List<User> usersToDisplay) {
        List<Node> currentCards = winnersDisplayPane.getChildren();
        int currentCardCount = currentCards.size();
        int usersToDisplayCount = usersToDisplay.size();

        // Reuse or create cards
        for (int i = 0; i < usersToDisplayCount; i++) {
            User user = usersToDisplay.get(i);
            VBox card;
            if (i < currentCardCount) {
                card = (VBox) currentCards.get(i);
                ImageView imageView = (ImageView) card.getChildren().get(0);
                Label nameIdLabel = (Label) card.getChildren().get(1);
                Image img = loadImage(user.getPhotoPath(), 100, 100);
                imageView.setImage(img);
                nameIdLabel.setText(user.getId() + " - " + user.getName());
            } else {
                card = createUserCard(user);
                winnersDisplayPane.getChildren().add(card);
            }
        }

        // Remove excess cards
        if (usersToDisplayCount < currentCardCount) {
            winnersDisplayPane.getChildren().remove(usersToDisplayCount, currentCardCount);
        }
    }

    private Image loadImage(String path, double reqWidth, double reqHeight) {
        if (path == null || path.trim().isEmpty()) return null;

        String cacheKey = path + "_" + reqWidth + "_" + reqHeight;
        if (imageCache.containsKey(cacheKey)) {
            return imageCache.get(cacheKey);
        }

        try {
            String uri = null;
            String p = path.trim();
            if (p.startsWith("http://") || p.startsWith("https://") || p.startsWith("file:")) {
                uri = p;
            } else {
                File f = new File(p);
                if (!f.isAbsolute()) {
                    f = new File(System.getProperty("user.dir"), p);
                }
                if (f.exists()) {
                    uri = f.toURI().toString();
                } else {
                    java.net.URL res = getClass().getResource("/" + p);
                    if (res != null) uri = res.toString();
                }
            }
            if (uri != null) {
                Image image = new Image(uri, reqWidth, reqHeight, true, true);
                imageCache.put(cacheKey, image);
                return image;
            }
        } catch (Exception e) {
            // ignore and return null
        }
        return null;
    }

    private VBox createUserCard(User user) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(100);
        imageView.setFitHeight(100);
        imageView.setPreserveRatio(true);
        Image img = loadImage(user.getPhotoPath(), 100, 100);
        imageView.setImage(img);

        Label nameIdLabel = new Label(user.getId() + " - " + user.getName());
        nameIdLabel.setFont(Font.font("Microsoft YaHei", 14));

        VBox card = new VBox(5, imageView, nameIdLabel);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-border-color: lightgray; -fx-border-radius: 5; -fx-padding: 5;");
        return card;
    }

//    public void showWinner(User user) {
//        if (user == null) return;
//        Alert alert = new Alert(Alert.AlertType.INFORMATION);
//        alert.setTitle("抽奖结果");
//        alert.setHeaderText(null);
//        alert.setContentText("中奖者：" + user.getName() + "（编号 " + user.getId() + "）");
//
//        Image img = loadImage(user.getPhotoPath(), 120, 120);
//        if (img != null) {
//            ImageView iv = new ImageView(img);
//            iv.setFitWidth(120);
//            iv.setFitHeight(120);
//            iv.setPreserveRatio(true);
//            alert.setGraphic(iv);
//        } else {
//            alert.setGraphic(null);
//        }
//
//        alert.showAndWait();
//    }

    public void showMessage(String msg, List<User> winners) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("抽奖结果");
        alert.setHeaderText(null);
        alert.setGraphic(null);

        VBox contentBox = new VBox(10);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.getChildren().add(new Label(msg));

        if (winners != null && !winners.isEmpty()) {
            for (User user : winners) {
                HBox winnerBox = new HBox(10);
                winnerBox.setAlignment(Pos.CENTER_LEFT);
                Image img = loadImage(user.getPhotoPath(), 60, 60);
                if (img != null) {
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(60);
                    iv.setFitHeight(60);
                    iv.setPreserveRatio(true);
                    winnerBox.getChildren().add(iv);
                }
                Label winnerLabel = new Label(user.getId() + " - " + user.getName());
                winnerBox.getChildren().add(winnerLabel);
                contentBox.getChildren().add(winnerBox);
            }
        }
        alert.getDialogPane().setContent(contentBox);
        alert.showAndWait();
    }

    public void showMessage(String msg) {
        showMessage(msg, null);
    }

    // 提供接口给控制器读取
    public int getDrawCount() {
        try {
            return Integer.parseInt(countField.getText());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public boolean isRepeatAllowed() {
        return repeatCheck.isSelected();
    }
}
