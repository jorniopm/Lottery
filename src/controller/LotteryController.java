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

    // Store last draw result
    private List<User> lastWinners = new ArrayList<>();

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

            // Build a temporary pool for THIS single draw so we don't pick the same person twice
            // within the same stop() invocation. This respects global `usedIds` only when
            // repeatAllowed == false (i.e. people already drawn in previous rounds are excluded).
            List<User> pool = new ArrayList<>(users);
            if (!repeatAllowed) {
                pool.removeIf(u -> usedIds.contains(u.getId()));
            }

            // Cap the number of winners to the pool size to avoid infinite loops
            int picks = Math.min(count, pool.size());
            for (int i = 0; i < picks; i++) {
                if (pool.isEmpty()) break;
                User selected = pool.remove(random.nextInt(pool.size()));
                winners.add(selected);
                // If repeats are not allowed across rounds, mark selected as used globally
                if (!repeatAllowed) {
                    usedIds.add(selected.getId());
                }
            }

            // Save last winners
            lastWinners = new ArrayList<>(winners);

            view.showMessage("中奖名单：", winners);
        }
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

        // Select 'count' random users for display during scrolling.
        // Remove selected items from the temporary list to avoid duplicates within the
        // same frame (so the same person doesn't appear more than once in that display).
        for (int i = 0; i < count; i++) {
            if (availableUsersForRandom.isEmpty()) {
                break; // Not enough users to display 'count' unique users
            }
            User user = availableUsersForRandom.remove(random.nextInt(availableUsersForRandom.size()));
            usersToDisplay.add(user);
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
            System.err.println("导出 CSV 时发生错误: " + e.getMessage());
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
    }

    // Snapshot of current users for preview
    public List<User> getUsersSnapshot() {
        return new ArrayList<>(users);
    }
}
