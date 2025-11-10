package app;

import javafx.application.Application;
import javafx.stage.Stage;
import model.User;
import model.UserLoader;
import view.LotteryView;

import java.util.List;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        List<User> users = UserLoader.loadUsers("data/users.txt");
        new LotteryView(stage, users);

    }

    public static void main(String[] args) {
        launch();
    }
}
