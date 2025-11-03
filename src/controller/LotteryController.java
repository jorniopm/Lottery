package controller;

import javafx.animation.*;
import javafx.util.Duration;
import model.User;
import view.LotteryView;

import java.util.*;

public class LotteryController {
    private final List<User> users;
    private final Set<String> usedIds = new HashSet<>();
    private final Random random = new Random();
    private final LotteryView view;

    private Timeline timeline;
    private User currentUser;

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

}
