package model;

import javafx.scene.SnapshotParameters;
import javafx.scene.image.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class UserLoader {

    public static List<User> loadUsers(String filePath) {
        List<User> users = new ArrayList<>();
        File csvFile = new File(filePath);
        File csvDir = csvFile.getParentFile();
        File imagesDir = new File("images");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), StandardCharsets.UTF_8))) {
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
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    String photoPathRaw = parts.length >= 3 ? parts[2].trim() : "";

                    File resolved = resolvePhotoFile(photoPathRaw, id, name, csvDir, imagesDir);
                    String finalPhotoPath = resolved != null ? resolved.getPath() : photoPathRaw;

                    System.out.println("UserLoader: Loading user: " + id + ", Name: " + name + ", Photo Path: " + finalPhotoPath);

                    Image roundedImage = createRoundedImage(finalPhotoPath, 113, 150);
                    User user = new User(id, name, finalPhotoPath);
                    user.setRoundedImage(roundedImage);
                    users.add(user);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return users;
    }

    private static File resolvePhotoFile(String raw, String id, String name, File csvDir, File imagesDir) {
        // Try raw as-is
        if (raw != null && !raw.isEmpty()) {
            File f = new File(raw);
            if (f.isAbsolute() && f.exists()) return f;
            // relative to CSV directory
            if (csvDir != null) {
                File relCsv = new File(csvDir, raw);
                if (relCsv.exists()) return relCsv;
            }
            // relative to working dir
            File relWd = new File(System.getProperty("user.dir"), raw);
            if (relWd.exists()) return relWd;
            // relative to images dir
            if (imagesDir != null) {
                File relImg = new File(imagesDir, raw);
                if (relImg.exists()) return relImg;
            }
            // try by filename only, in images dir
            String fileName = new File(raw).getName();
            if (imagesDir != null) {
                File byName = new File(imagesDir, fileName);
                if (byName.exists()) return byName;
            }
        }
        // Try infer by id/name with common extensions in images dir
        String[] exts = {".jpg", ".jpeg", ".png", ".bmp", ".gif", ".webp"};
        if (imagesDir != null) {
            for (String ext : exts) {
                if (id != null && !id.isEmpty()) {
                    File f = new File(imagesDir, id + ext);
                    if (f.exists()) return f;
                }
                if (name != null && !name.isEmpty()) {
                    File f = new File(imagesDir, name + ext);
                    if (f.exists()) return f;
                }
            }
        }
        // Try infer relative to CSV dir too
        if (csvDir != null) {
            for (String ext : new String[]{".jpg", ".jpeg", ".png", ".bmp", ".gif", ".webp"}) {
                if (id != null && !id.isEmpty()) {
                    File f = new File(csvDir, id + ext);
                    if (f.exists()) return f;
                }
                if (name != null && !name.isEmpty()) {
                    File f = new File(csvDir, name + ext);
                    if (f.exists()) return f;
                }
            }
        }
        return null;
    }

    private static Image createRoundedImage(String path, double width, double height) {
        try {
            if (path == null || path.isEmpty()) return null;
            File f = new File(path);
            if (!f.isAbsolute()) {
                f = new File(System.getProperty("user.dir"), path);
            }
            if (!f.exists()) {
                return null;
            }
            Image img = new Image(f.toURI().toString(), width, height, false, true);

            Rectangle clip = new Rectangle(width, height);
            clip.setArcWidth(15);
            clip.setArcHeight(15);

            ImageView iv = new ImageView(img);
            iv.setFitWidth(width);
            iv.setFitHeight(height);
            iv.setPreserveRatio(false);
            iv.setClip(clip);

            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);

            return iv.snapshot(params, null);
        } catch (Exception e) {
            System.err.println("无法加载图片：" + path);
            return null;
        }
    }
}
