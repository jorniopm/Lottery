package model;

import javafx.scene.SnapshotParameters;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.*;
        import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.*;
        import java.util.*;
import java.nio.charset.StandardCharsets;

public class UserLoader {

    public static List<User> loadUsers(String filePath) {
        List<User> users = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    // strip UTF-8 BOM if present
                    if (!line.isEmpty() && line.charAt(0) == '\uFEFF') {
                        line = line.substring(1);
                    }
                    firstLine = false;
                }
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    String photoPath = parts[2].trim();

                    System.out.println("UserLoader: Loading user: " + id + ", Name: " + name + ", Photo Path: " + photoPath);

                    // 创建用户并生成圆角头像
                    Image roundedImage = createRoundedImage(photoPath, 113, 150);
                    User user = new User(id, name, photoPath);
                    user.setRoundedImage(roundedImage);  // 添加缓存字段
                    users.add(user);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return users;
    }

    private static Image createRoundedImage(String path, double width, double height) {
        try {
            // 读取图片
            Image img = new Image(new File(path).toURI().toString(), width, height, false, true);

            // 使用 clip 裁剪为圆角矩形
            Rectangle clip = new Rectangle(width, height);
            clip.setArcWidth(15);
            clip.setArcHeight(15);

            ImageView iv = new ImageView(img);
            iv.setFitWidth(width);
            iv.setFitHeight(height);
            iv.setPreserveRatio(false);
            iv.setClip(clip);

            // 截图固定圆角形状
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);

            return iv.snapshot(params, null);
        } catch (Exception e) {
            System.err.println("无法加载图片：" + path);
            return null;
        }
    }
}
