package java;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        // Boot the database — creates all tables on first run
        DatabaseManager.getInstance();

        // Load the login screen
        Parent root = FXMLLoader.load(
            getClass().getResource("/com/fooddelivery/views/Login.fxml")
        );

        stage.setTitle("Food Delivery System");
        stage.setScene(new Scene(root, 480, 520));
        stage.setResizable(false);
        stage.show();
    }

    @Override
    public void stop() {
        // Close MySQL connection cleanly when app closes
        DatabaseManager.getInstance().closeConnection();
    }

    public static void main(String[] args) {
        launch(args);
    }
}