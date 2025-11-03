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
        if (usedIds.size() == users.size()) {
            view.showMessage("所有人都已被抽过！");
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
            if (currentUser != null) {
                usedIds.add(currentUser.getId());
                view.showMessage("中奖者：" + currentUser.getName() + "（编号 " + currentUser.getId() + "）");
            }
        }
    }

    private void showRandomUser() {
        User user;
        do {
            user = users.get(random.nextInt(users.size()));
        } while (usedIds.contains(user.getId()));
        currentUser = user;
        view.updateDisplay(user);
    }

}
