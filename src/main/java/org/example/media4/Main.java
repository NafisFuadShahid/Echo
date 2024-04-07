package org.example.media4;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    @Override
    // New Branch
    public void start(Stage stage) throws IOException {
//        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("media-player.fxml"));
//        Scene scene = new Scene(fxmlLoader.load());
        Parent root = FXMLLoader.load(getClass().getResource("media-player.fxml"));
        Scene scene = new Scene(root);
        String imagePath = "src/main/resources/org/example/media4/PlayButtonLogo.png";
        Image icon = new Image("file:" + imagePath);
        stage.getIcons().add(icon);

        stage.setTitle("Echo");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}