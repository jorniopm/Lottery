package view;

import javafx.geometry.Insets;
import javafx.scene.Node;
import controller.LotteryController;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
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
        countField.setPrefWidth(40);
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

        Scene scene = new Scene(root, 500, 600);
        stage.setScene(scene);
        stage.setTitle("抽奖程序");
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
                Image img = loadImage(user.getPhotoPath(), 113, 150);
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
        System.out.println("loadImage: Attempting to load image from path: " + path);
        if (path == null || path.trim().isEmpty()) return null;

        String cacheKey = path + "_" + reqWidth + "_" + reqHeight;
        if (imageCache.containsKey(cacheKey)) {
            System.out.println("loadImage: Image found in cache for key: " + cacheKey);
            return imageCache.get(cacheKey);
        }

        try {
            System.out.println("loadImage: Cache miss for key: " + cacheKey);
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
                System.out.println("loadImage: Resolved URI: " + uri);
                Image image = new Image(uri, reqWidth, reqHeight, true, true);
                if (image.isError()) {
                    System.err.println("loadImage: Error loading image from URI: " + uri + ", Error: " + image.exceptionProperty().get().getMessage());
                } else {
                    System.out.println("loadImage: Image loaded successfully from URI: " + uri);
                }
                imageCache.put(cacheKey, image);
                return image;
            } else {
                System.out.println("loadImage: URI is null, image not loaded.");
            }
        } catch (Exception e) {
            System.err.println("loadImage: Exception while loading image from path: " + path + ", Exception: " + e.getMessage());
            // ignore and return null
        }
        return null;
    }

    private VBox createUserCard(User user) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(113);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(false); // 保证矩形比例正确

        Image img = loadImage(user.getPhotoPath(), 113, 150);
        imageView.setImage(img);

        Rectangle clip = new Rectangle(113, 150);
        clip.setArcWidth(15);  // 控制圆角弧度
        clip.setArcHeight(15);
        imageView.setClip(clip);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT); // 避免裁剪后黑边
        WritableImage roundedImg = imageView.snapshot(params, null);

        Label nameIdLabel = new Label(user.getId() + " - " + user.getName());
        nameIdLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #333;");

        return new VBox(5, imageView, nameIdLabel);
    }




    public void showMessage(String msg, List<User> winners) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("抽奖结果");
        dialog.setHeaderText(msg);

        FlowPane contentPane = new FlowPane(20, 20);
        contentPane.setAlignment(Pos.CENTER);
        contentPane.setPadding(new Insets(20));

        if (winners != null && !winners.isEmpty()) {
            for (User user : winners) {
                VBox card = new VBox(10);
                card.setAlignment(Pos.CENTER);

                // 创建圆角矩形头像
                try {
                    String imgPath = new File(user.getPhotoPath()).toURI().toString();
                    Image img = new Image(imgPath, 113, 150, true, true);
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(113);
                    iv.setFitHeight(150);
                    iv.setPreserveRatio(false);

                    // 生成圆角矩形图像
                    Rectangle clip = new Rectangle(113, 150);
                    clip.setArcWidth(15);
                    clip.setArcHeight(15);
                    iv.setClip(clip);

                    SnapshotParameters params = new SnapshotParameters();
                    params.setFill(Color.TRANSPARENT);
                    WritableImage roundedImage = iv.snapshot(params, null);

                    iv.setClip(null);
                    iv.setImage(roundedImage);

                    card.getChildren().add(iv);
                } catch (Exception e) {
                    System.out.println("图片加载失败：" + user.getPhotoPath());
                }

                Label nameLabel = new Label(user.getId() + " - " + user.getName());
                nameLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #333;");
                card.getChildren().add(nameLabel);

                contentPane.getChildren().add(card);
            }
        } else {
            contentPane.getChildren().add(new Label("暂无中奖者"));
        }

        ScrollPane scrollPane = new ScrollPane(contentPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        scrollPane.setStyle("-fx-background-color: transparent;");

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
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
