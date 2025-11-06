package view;

import javafx.geometry.Insets;
import javafx.scene.Node;
import controller.LotteryController;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import model.User;
import model.UserLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
        buttonBox.setPadding(new Insets(10, 0, 10, 0));

        // Center content that may overflow: status + winners + options
        VBox centerContent = new VBox(20, currentStatusLabel, winnersDisplayPane, optionsBox);
        centerContent.setAlignment(Pos.TOP_CENTER);
        centerContent.setPadding(new Insets(20));
        centerContent.setStyle("-fx-background-color: linear-gradient(to bottom, #F5F7FA, #b8c4db);");

        ScrollPane scroll = new ScrollPane(centerContent);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // MenuBar replacement for toolbar
        Menu fileMenu = new Menu("文件");
        MenuItem importItem = new MenuItem("导入名单…");
        MenuItem exportItem = new MenuItem("导出结果…");
        importItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        exportItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        fileMenu.getItems().addAll(importItem, exportItem, new SeparatorMenuItem());
        MenuBar menuBar = new MenuBar(fileMenu);

        BorderPane root = new BorderPane();
        VBox top = new VBox(menuBar); // keep simple now; can add other top bars if needed
        root.setTop(top);
        root.setCenter(scroll);
        root.setBottom(buttonBox);
        BorderPane.setAlignment(buttonBox, Pos.CENTER);

        Scene scene = new Scene(root, 600, 700);
        stage.setScene(scene);
        stage.setTitle("抽奖程序");
        stage.show();

        controller = new LotteryController(users, this);

        startBtn.setOnAction(e -> controller.start());
        stopBtn.setOnAction(e -> controller.stop());
        exportItem.setOnAction(e -> saveLastWinnersToCSV());
        importItem.setOnAction(e -> importUsers());
    }

    private void importUsers() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导入名单");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("文本/CSV 文件", "*.txt", "*.csv"),
                new FileChooser.ExtensionFilter("所有文件", "*.*")
        );
        // default to current data folder if exists
        File defaultDir = new File("data");
        if (defaultDir.exists() && defaultDir.isDirectory()) {
            chooser.setInitialDirectory(defaultDir);
        }
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;
        List<User> newUsers = UserLoader.loadUsers(file.getAbsolutePath());
        if (newUsers == null || newUsers.isEmpty()) {
            new Alert(Alert.AlertType.ERROR, "导入失败，请检查文件格式：编号,姓名,图片路径", ButtonType.OK).showAndWait();
            return;
        }
        controller.replaceUsers(newUsers);
        winnersDisplayPane.getChildren().clear();
        currentStatusLabel.setText("已导入 " + newUsers.size() + " 人，请点击开始抽奖");
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
                imageView.setImage(user.getRoundedImage());
                Label nameIdLabel = (Label) card.getChildren().get(1);
                Image img = loadImage(user.getPhotoPath(), 113, 150);
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
        // Add buttons: Close and Save CSV
        ButtonType saveType = new ButtonType("保存为CSV", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CLOSE);

        // Wire save action
        Node saveButton = dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, evt -> {
            evt.consume();
            saveLastWinnersToCSV();
        });

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

    private void saveLastWinnersToCSV() {
        if (!controller.hasLastWinners()) {
            new Alert(Alert.AlertType.INFORMATION, "暂无可保存的抽奖结果，请先完成一次抽奖。", ButtonType.OK).showAndWait();
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("保存抽奖结果");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV 文件 (*.csv)", "*.csv"));
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        chooser.setInitialFileName("中奖结果_" + ts + ".csv");
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        // Ensure .csv extension
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getParentFile(), file.getName() + ".csv");
        }

        boolean ok = controller.exportLastWinnersToCSV(file);
        if (ok) {
            new Alert(Alert.AlertType.INFORMATION, "保存成功：" + file.getAbsolutePath(), ButtonType.OK).showAndWait();
        } else {
            new Alert(Alert.AlertType.ERROR, "保存失败，请重试或更换位置。", ButtonType.OK).showAndWait();
        }
    }
}
