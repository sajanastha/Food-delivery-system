package com.fooddelivery;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.stage.Stage;

public class ChooseRoleController {

    @FXML
    private void goCustomer(ActionEvent event) {
        loadScreen(event,
                   "/com/fooddelivery/views/RegisterCustomer.fxml",
                   "Register — Customer");
    }

    @FXML
    private void goRestaurant(ActionEvent event) {
        loadScreen(event,
                   "/com/fooddelivery/views/RegisterRestaurant.fxml",
                   "Register — Restaurant");
    }

    @FXML
    private void goDriver(ActionEvent event) {
        loadScreen(event,
                   "/com/fooddelivery/views/RegisterDriver.fxml",
                   "Register — Driver");
    }

    @FXML
    private void goBack(ActionEvent event) {
        loadScreen(event,
                   "/com/fooddelivery/views/Login.fxml",
                   "Login");
    }

    private void loadScreen(ActionEvent event,
                             String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(
                getClass().getResource(fxmlPath)
            );
            Stage stage = (Stage)((Node) event.getSource())
                              .getScene().getWindow();
            stage.setTitle("Food Delivery — " + title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
