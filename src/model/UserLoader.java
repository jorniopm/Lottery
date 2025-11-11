package model;

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
                    String finalPhotoPath = resolved != null ? resolved.getPath() : "";

                    User user = new User(id, name, finalPhotoPath);
                    users.add(user);
                }
            }
        } catch (IOException e) {
            // Let caller handle empty or failed load by returning empty list
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
}
