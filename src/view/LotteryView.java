package view;

import controller.LotteryController;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import model.User;

import java.util.List;

public class LotteryView {
    private final Stage stage;
    private final Label nameLabel;
    private final ImageView photoView;
    private final LotteryController controller;

    public LotteryView(Stage stage, List<User> users) {
        this.stage = stage;
        this.nameLabel = new Label("请点击开始抽奖");
        this.nameLabel.setFont(Font.font("Microsoft YaHei", 24));
        this.photoView = new ImageView();
        this.photoView.setFitWidth(220);
        this.photoView.setFitHeight(220);

        Button startBtn = new Button("开始抽奖");
        Button stopBtn = new Button("结束抽奖");

        HBox buttonBox = new HBox(20, startBtn, stopBtn);
        buttonBox.setAlignment(Pos.CENTER);

        VBox root = new VBox(20, nameLabel, photoView, buttonBox);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #F5F7FA, #B8C6DB);");

        Scene scene = new Scene(root, 500, 450);
        stage.setScene(scene);
        stage.setTitle("JavaFX 抽奖程序");
        stage.show();

        controller = new LotteryController(users, this);

        startBtn.setOnAction(e -> controller.start());
        stopBtn.setOnAction(e -> controller.stop());
    }

    public void updateDisplay(User user) {
        nameLabel.setText(user.getId() + " - " + user.getName());
        try {
            Image img = new Image(user.getPhotoPath());
            photoView.setImage(img);
        } catch (Exception e) {
            photoView.setImage(null);
        }
    }

    public void showMessage(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("抽奖结果");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
