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
                    users.add(new User(parts[0].trim(), parts[1].trim(), parts[2].trim()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return users;
    }
}
