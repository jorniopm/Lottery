package model;

import java.io.*;
import java.util.*;

public class UserLoader {
    public static List<User> loadUsers(String filePath) {
        List<User> users = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    String photoPath = parts[2].trim();
                    System.out.println("UserLoader: Loading user: " + id + ", Name: " + name + ", Photo Path: " + photoPath);
                    users.add(new User(id, name, photoPath));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return users;
    }
}
