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
import javafx.stage.DirectoryChooser;
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
        centerContent.setStyle("-fx-background-color: linear-gradient(to bottom, #F5F7FA, #abccf4);");

        ScrollPane scroll = new ScrollPane(centerContent);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Menus
        Menu fileMenu = new Menu("文件");
        MenuItem importItem = new MenuItem("导入名单…");
        MenuItem importImagesItem = new MenuItem("导入图片…");
        MenuItem exportItem = new MenuItem("导出结果…");
        importItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        exportItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        fileMenu.getItems().addAll(importItem, importImagesItem, exportItem, new SeparatorMenuItem());

        Menu viewMenu = new Menu("查看");
        MenuItem previewItem = new MenuItem("预览名单…");
        viewMenu.getItems().addAll(previewItem);

        MenuBar menuBar = new MenuBar(fileMenu, viewMenu);

        BorderPane root = new BorderPane();
        VBox top = new VBox(menuBar);
        root.setTop(top);
        root.setCenter(scroll);
        root.setBottom(buttonBox);
        BorderPane.setAlignment(buttonBox, Pos.CENTER);

        Scene scene = new Scene(root, 600, 500);
        stage.setScene(scene);
        stage.setTitle("抽奖程序");
        applyAppIcon(stage);
        stage.show();

        controller = new LotteryController(users, this);

        startBtn.setOnAction(e -> controller.start());
        stopBtn.setOnAction(e -> controller.stop());
        exportItem.setOnAction(e -> saveLastWinnersToCSV());
        importItem.setOnAction(e -> importUsers());
        importImagesItem.setOnAction(e -> importImagesBatch());
        previewItem.setOnAction(e -> previewRoster());
    }

    // Use images/lottery_icon.png or other candidates
    private void applyAppIcon(Stage stage) {
        try {
            String[] candidates = new String[] {
                    "images/lottery_icon.png",
            };
            for (String p : candidates) {
                File f = new File(p);
                if (f.exists()) {
                    Image icon = new Image(f.toURI().toString());
                    if (!icon.isError()) {
                        stage.getIcons().add(icon);
                        break;
                    }
                }
            }
        } catch (Exception ignore) {
            // ignore icon errors
        }
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
        if (newUsers.isEmpty()) {
            new Alert(Alert.AlertType.ERROR, "导入失败，请检查文件格式：编号,姓名,图片路径", ButtonType.OK).showAndWait();
            return;
        }
        controller.replaceUsers(newUsers);
        winnersDisplayPane.getChildren().clear();
        currentStatusLabel.setText("已导入 " + newUsers.size() + " 人，请点击开始抽奖");
    }

    private void previewRoster() {
        List<User> snapshot = controller.getUsersSnapshot();
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("名单预览");
        dialog.setHeaderText("当前导入人数：" + (snapshot == null ? 0 : snapshot.size()));

        FlowPane contentPane = new FlowPane(20, 20);
        contentPane.setAlignment(Pos.CENTER);
        contentPane.setPadding(new Insets(20));

        if (snapshot != null && !snapshot.isEmpty()) {
            for (User user : snapshot) {
                VBox card = new VBox(10);
                card.setAlignment(Pos.CENTER);
                try {
                    Image rounded = loadRoundedImage(user.getPhotoPath(), 113, 150);
                    ImageView iv = new ImageView(rounded);
                    iv.setFitWidth(113);
                    iv.setFitHeight(150);
                    iv.setPreserveRatio(false);
                    card.getChildren().add(iv);
                } catch (Exception ex) {
                    // ignore image errors and only show text
                }
                Label nameLabel = new Label(user.getId() + " - " + user.getName());
                nameLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #333;");
                card.getChildren().add(nameLabel);

                contentPane.getChildren().add(card);
            }
        } else {
            contentPane.getChildren().add(new Label("暂无名单，请先导入。"));
        }

        ScrollPane scrollPane = new ScrollPane(contentPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        scrollPane.setStyle("-fx-background-color: transparent;");

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        dialog.showAndWait();
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
                Image img = loadRoundedImage(user.getPhotoPath(), 113, 150);
                imageView.setImage(img);
                Label nameIdLabel = (Label) card.getChildren().get(1);
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

    // Load raw image (no rounding). Returns default placeholder when missing.
    private Image loadImage(String path, double reqWidth, double reqHeight) {
        if (path == null || path.trim().isEmpty()) {
            return getDefaultImage(reqWidth, reqHeight);
        }

        String cacheKey = "RAW_" + path + "_" + reqWidth + "_" + reqHeight;
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
                if (image.isError()) {
                    Image def = getDefaultImage(reqWidth, reqHeight);
                    imageCache.put(cacheKey, def);
                    return def;
                } else {
                    imageCache.put(cacheKey, image);
                    return image;
                }
            } else {
                Image def = getDefaultImage(reqWidth, reqHeight);
                imageCache.put(cacheKey, def);
                return def;
            }
        } catch (Exception e) {
            return getDefaultImage(reqWidth, reqHeight);
        }
    }

    // Load and return a rounded (snapshot) image with caching
    private Image loadRoundedImage(String path, double reqWidth, double reqHeight) {
        String key = "ROUND_" + (path == null ? "" : path) + "_" + reqWidth + "_" + reqHeight;
        if (imageCache.containsKey(key)) return imageCache.get(key);

        Image base = loadImage(path, reqWidth, reqHeight);
        ImageView iv = new ImageView(base);
        iv.setFitWidth(reqWidth);
        iv.setFitHeight(reqHeight);
        iv.setPreserveRatio(false);

        Rectangle clip = new Rectangle(reqWidth, reqHeight);
        clip.setArcWidth(15);
        clip.setArcHeight(15);
        iv.setClip(clip);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);

        WritableImage rounded = iv.snapshot(params, null);
        iv.setClip(null);
        imageCache.put(key, rounded);
        return rounded;
    }

    private Image getDefaultImage(double reqWidth, double reqHeight) {
        String[] defaults = new String[] { "images/default.png", "images/default.jpg" };
        for (String p : defaults) {
            String key = "__DEFAULT__" + p + "_" + reqWidth + "_" + reqHeight;
            if (imageCache.containsKey(key)) {
                return imageCache.get(key);
            }
            File f = new File(p);
            if (f.exists()) {
                try {
                    Image img = new Image(f.toURI().toString(), reqWidth, reqHeight, true, true);
                    if (!img.isError()) {
                        imageCache.put(key, img);
                        return img;
                    }
                } catch (Exception ignore) {
                    // ignore
                }
            }
            // try classpath resource
            java.net.URL res = getClass().getResource("/" + p);
            if (res != null) {
                try {
                    Image img = new Image(res.toString(), reqWidth, reqHeight, true, true);
                    if (!img.isError()) {
                        imageCache.put(key, img);
                        return img;
                    }
                } catch (Exception ignore) {
                }
            }
        }
        return new WritableImage((int)Math.max(1, reqWidth), (int)Math.max(1, reqHeight));
    }

    private VBox createUserCard(User user) {
        Image roundedImg = loadRoundedImage(user.getPhotoPath(), 113, 150);

        ImageView imageView = new ImageView(roundedImg);
        imageView.setFitWidth(113);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(false);

        Label nameIdLabel = new Label(user.getId() + " - " + user.getName());
        nameIdLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #333;");

        VBox box = new VBox(5, imageView, nameIdLabel);
        box.setAlignment(Pos.CENTER);
        return box;
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

                try {
                    Image rounded = loadRoundedImage(user.getPhotoPath(), 113, 150);
                    ImageView iv = new ImageView(rounded);
                    iv.setFitWidth(113);
                    iv.setFitHeight(150);
                    iv.setPreserveRatio(false);

                    card.getChildren().add(iv);
                } catch (Exception e) {
                    // ignore and only show text
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

    // Batch import images from a selected directory and match them to users by id or name
    private void importImagesBatch() {
        List<User> current = controller.getUsersSnapshot();
        if (current == null || current.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "当前没有导入任何名单，请先导入名单文件。", ButtonType.OK).showAndWait();
            return;
        }

        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("选择包含图片的文件夹");
        File defaultDir = new File("images");
        if (defaultDir.exists() && defaultDir.isDirectory()) dc.setInitialDirectory(defaultDir);
        File dir = dc.showDialog(stage);
        if (dir == null) return;

        // collect image files recursively
        List<File> imageFiles = new java.util.ArrayList<>();
        collectImageFiles(dir, imageFiles);
        if (imageFiles.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "所选目录中未发现图片文件。", ButtonType.OK).showAndWait();
            return;
        }

        // build a map from filenameWithoutExt -> file
        Map<String, File> nameMap = new HashMap<>();
        for (File f : imageFiles) {
            String name = f.getName();
            int idx = name.lastIndexOf('.');
            String base = idx > 0 ? name.substring(0, idx) : name;
            nameMap.put(base.toLowerCase(), f);
        }

        int matched = 0;
        List<User> updated = new java.util.ArrayList<>();
        List<String> unmatched = new java.util.ArrayList<>();
        for (User u : current) {
            String id = u.getId();
            String name = u.getName();
            File match = null;
            if (id != null && !id.isEmpty()) match = nameMap.get(id.toLowerCase());
            if (match == null && name != null && !name.isEmpty()) match = nameMap.get(name.toLowerCase());
            if (match == null) {
                // try more permissive: filenames that contain id or name
                for (Map.Entry<String, File> e : nameMap.entrySet()) {
                    if (id != null && !id.isEmpty() && e.getKey().contains(id.toLowerCase())) { match = e.getValue(); break; }
                    if (name != null && !name.isEmpty() && e.getKey().contains(name.toLowerCase())) { match = e.getValue(); break; }
                }
            }
            if (match != null) {
                matched++;
                updated.add(new User(u.getId(), u.getName(), match.getAbsolutePath()));
            } else {
                unmatched.add(u.getId() + " - " + u.getName());
                updated.add(new User(u.getId(), u.getName(), u.getPhotoPath()));
            }
        }

        controller.replaceUsers(updated);
        winnersDisplayPane.getChildren().clear();

        String msg = "已匹配图片: " + matched + "，未匹配: " + unmatched.size();
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("批量导入图片完成");
        a.setHeaderText(msg);
        if (!unmatched.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(50, unmatched.size()); i++) {
                sb.append(unmatched.get(i)).append("\n");
            }
            if (unmatched.size() > 50) sb.append("... (共 ").append(unmatched.size()).append(" 项未匹配)");
            TextArea ta = new TextArea(sb.toString());
            ta.setEditable(false);
            ta.setWrapText(true);
            ta.setPrefRowCount(Math.min(20, unmatched.size()));
            a.getDialogPane().setContent(ta);
        }
        a.showAndWait();
    }

    private void collectImageFiles(File dir, List<File> out) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) collectImageFiles(f, out);
            else {
                String n = f.getName().toLowerCase();
                if (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".bmp") || n.endsWith(".gif") || n.endsWith(".webp")) {
                    out.add(f);
                }
            }
        }
    }
}
