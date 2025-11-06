package controller;

import javafx.animation.*;
import javafx.util.Duration;
import model.User;
import view.LotteryView;

import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LotteryController {
    private final List<User> users;
    private final Set<String> usedIds = new HashSet<>();
    private final Random random = new Random();
    private final LotteryView view;

    private Timeline timeline;
    private User currentUser;

    // Store last draw result and simple history
    private List<User> lastWinners = new ArrayList<>();
    private final List<List<User>> history = new ArrayList<>();

    public LotteryController(List<User> users, LotteryView view) {
        this.users = new ArrayList<>(users);
        this.view = view;
    }

    public void start() {
        boolean repeatAllowed = view.isRepeatAllowed();
        if (!repeatAllowed && usedIds.size() == users.size()) {
            view.showMessage("所有人都已被抽过！", new ArrayList<>());
            return;
        }
        if (timeline != null && timeline.getStatus() == Animation.Status.RUNNING)
            return;

        timeline = new Timeline(new KeyFrame(Duration.millis(80), e -> showRandomUser()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    public void stop() {
        if (timeline != null && timeline.getStatus() == Animation.Status.RUNNING) {
            timeline.stop();
            int count = view.getDrawCount();
            boolean repeatAllowed = view.isRepeatAllowed();

            List<User> winners = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                User user = drawOne(repeatAllowed);
                if (user != null) winners.add(user);
            }

            // Save last winners and append to history
            lastWinners = new ArrayList<>(winners);
            if (!lastWinners.isEmpty()) {
                history.add(new ArrayList<>(lastWinners));
            }

            view.showMessage("中奖名单：", winners);
        }
    }

    private User drawOne(boolean repeatAllowed) {
        List<User> available = new ArrayList<>(users);
        if (!repeatAllowed) {
            available.removeIf(u -> usedIds.contains(u.getId()));
        }
        if (available.isEmpty()) return null;
        User selected = available.get(random.nextInt(available.size()));
        if (!repeatAllowed) usedIds.add(selected.getId());
        return selected;
    }

    private void showRandomUser() {
        int count = view.getDrawCount();
        boolean repeatAllowed = view.isRepeatAllowed();
        List<User> usersToDisplay = new ArrayList<>();

        List<User> availableUsersForRandom = new ArrayList<>(users);
        if (!repeatAllowed) {
            availableUsersForRandom.removeIf(u -> usedIds.contains(u.getId()));
        }

        if (availableUsersForRandom.isEmpty()) {
            view.showMessage("所有人都已被抽过！", new ArrayList<>());
            if (timeline != null) {
                timeline.stop();
            }
            return;
        }

        // Select 'count' random users for display during scrolling
        for (int i = 0; i < count; i++) {
            if (availableUsersForRandom.isEmpty()) {
                break; // Not enough users to display 'count' unique users
            }
            User user = availableUsersForRandom.get(random.nextInt(availableUsersForRandom.size()));
            usersToDisplay.add(user);
            if (!repeatAllowed) {
                availableUsersForRandom.remove(user);
            }
        }
        view.updateDisplay(usersToDisplay);
    }

    // Export the latest draw result to CSV
    public boolean exportLastWinnersToCSV(File file) {
        if (lastWinners == null || lastWinners.isEmpty() || file == null) {
            return false;
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String drawTime = LocalDateTime.now().format(fmt);
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            // UTF-8 BOM for Excel compatibility on Windows
            bw.write('\uFEFF');
            bw.write("编号,姓名,照片路径,抽取时间");
            bw.newLine();
            for (User u : lastWinners) {
                String id = safe(u.getId());
                String name = safe(u.getName());
                String photo = safe(u.getPhotoPath());
                bw.write(String.join(",", escapeCsv(id), escapeCsv(name), escapeCsv(photo), escapeCsv(drawTime)));
                bw.newLine();
            }
            bw.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static String escapeCsv(String s) {
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = s.replace("\"", "\"\"");
            return "\"" + s + "\"";
        }
        return s;
    }

    public boolean hasLastWinners() {
        return lastWinners != null && !lastWinners.isEmpty();
    }

    // Allow replacing the roster safely
    public void replaceUsers(List<User> newUsers) {
        // Stop timeline if running
        if (timeline != null && timeline.getStatus() == Animation.Status.RUNNING) {
            timeline.stop();
        }
        // Replace list contents without reassigning the final reference
        users.clear();
        if (newUsers != null) {
            users.addAll(newUsers);
        }
        // Reset state
        usedIds.clear();
        lastWinners = new ArrayList<>();
        history.clear();
    }

    // Snapshot of current users for preview
    public List<User> getUsersSnapshot() {
        return new ArrayList<>(users);
    }
}
