package org.example.media4;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;


public class MediaPlayerController implements Initializable {



    private Scene mainScene;

    @FXML
    private Button btnPlay;

    @FXML
    private Label lblDuration;

    @FXML
    private MediaView mediaView;

    @FXML
    private Slider slider;

    @FXML
    private Slider volumeSlider; // Volume slider from Scene Builder

    @FXML
    private Button btnForward;

    @FXML
    private Button btnBackward;

    @FXML
    private ChoiceBox<String> themeChoiceBox;

    private String[] theme = {"Dark", "Green", "Blue", "Red"};
    private int fileSelected = 0;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        themeChoiceBox.getItems().addAll(theme);
        themeChoiceBox.setValue("Blue");
        themeChoiceBox.setOnAction(this::applyTheme);
    }

    @FXML
    void applyTheme(ActionEvent event) {
        String selectedTheme = themeChoiceBox.getValue();
        switch (selectedTheme) {
            case "Green":
                changeThemeToGreen();
                break;
            case "Dark":
                changeThemeToDark();
                break;
            case "Blue":
                changeThemeToBlue();
                break;
            case "Red":
                changeThemeToRed();
                break;
            // Add more themes as needed
        }
    }


    public void setMainScene(Scene scene) {
        this.mainScene = scene;
    }

    @FXML
    void changeThemeToGreen() {
        mainScene.getStylesheets().clear();
        mainScene.getStylesheets().add(getClass().getResource("green-theme.css").toExternalForm());
    }

    @FXML
    void changeThemeToDark() {
        mainScene.getStylesheets().clear();
        mainScene.getStylesheets().add(getClass().getResource("dark-theme.css").toExternalForm());
    }

    @FXML
    void changeThemeToBlue() {
        mainScene.getStylesheets().clear();
        mainScene.getStylesheets().add(getClass().getResource("blue-theme.css").toExternalForm());
    }

    @FXML
    void changeThemeToRed() {
        mainScene.getStylesheets().clear();
        mainScene.getStylesheets().add(getClass().getResource("red-theme.css").toExternalForm());
    }
    private Media media;
    private MediaPlayer mediaPlayer;

    private boolean isPlayed = false;

    @FXML
    void btnPlay(MouseEvent event) {
        if(fileSelected != 1) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("File not Found!");
            alert.setHeaderText("Please Choose a Valid File First!");
            Optional<ButtonType> result = alert.showAndWait();
            if (((Optional<?>) result).isPresent() && result.get().equals(ButtonType.OK)) {
                //do nothing
            }
        }

        if (!isPlayed) {
//            btnPlay.setText("Pause");
            mediaPlayer.play();
            isPlayed = true;
        } else {
//            btnPlay.setText("Play");
            mediaPlayer.pause();
            isPlayed = false;
        }
    }

    @FXML
    void btnStop(MouseEvent event) {
//        btnPlay.setText("Play");
        mediaPlayer.stop();
        isPlayed = false;
    }

    @FXML
    void btnForwardClicked(ActionEvent event) {
        seekMedia(5);
    }

    @FXML
    void btnBackwardClicked(ActionEvent event) {
        seekMedia(-5);
    }

    private void seekMedia(double seconds) {
        if(mediaPlayer != null) {
            double currentTime = mediaPlayer.getCurrentTime().toSeconds();
            double newTime = currentTime + seconds;

            if(newTime < 0) {
                newTime = 0; // making sure we don't seek before the start
            } else if (newTime > mediaPlayer.getTotalDuration().toSeconds()) {
                newTime = mediaPlayer.getTotalDuration().toSeconds();
            }

            mediaPlayer.seek(Duration.seconds(newTime));
        }
    }

    private String getFileExtension(File file) {
        String fileName = file.getName();
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
    @FXML
    void selectMedia(ActionEvent event) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open");
        File selectedFile = fileChooser.showOpenDialog(null);
        String fileExtension = getFileExtension(selectedFile);



        if (selectedFile != null) {
            fileSelected = 1;
            String url = selectedFile.toURI().toString();

            media = new Media(url);

            mediaPlayer = new MediaPlayer(media);
            if(fileExtension.equals("mp3") || fileExtension.equals("wav")){
               System.out.println((String) media.getMetadata().get("year"));
            }
            mediaView.setMediaPlayer(mediaPlayer);

            // Update media duration and slider max value when media is ready
            mediaPlayer.setOnReady(() -> {
                Duration totalDuration = media.getDuration();
                slider.setMax(totalDuration.toSeconds());
                int hoursAtStart, secondsAtStart, minutesAtStart, totalSecsAtStart;
                String timeStringAtStart;
                totalSecsAtStart = (int) (int) totalDuration.toSeconds();
                hoursAtStart = totalSecsAtStart / 3600;
                minutesAtStart = (totalSecsAtStart % 3600) / 60;
                secondsAtStart = totalSecsAtStart % 60;
                timeStringAtStart = String.format("%02d:%02d:%02d", hoursAtStart, minutesAtStart, secondsAtStart);
                lblDuration.setText("Duration: 00 / " + timeStringAtStart);
            });

            // Bind volumeSlider to MediaPlayer volume property
            mediaPlayer.volumeProperty().bind(volumeSlider.valueProperty().divide(100.0));

            // Set the initial volume of the media player
            volumeSlider.setValue(75);

            // Initialize volumeSlider with initial volume value of MediaPlayer
            if (mediaPlayer != null) {
                double initialVolume = mediaPlayer.getVolume() * 100.0; // Convert volume range (0.0 - 1.0) to (0 - 100)
                volumeSlider.setValue(initialVolume);
            }

            // Add listener to update slider and lblDuration based on currentTime
            mediaPlayer.currentTimeProperty().addListener((observableValue, oldValue, newValue) -> {
                slider.setValue(newValue.toSeconds());
                int hours, seconds, minutes, totalSecs;
                String timeString;
                totalSecs = (int) media.getDuration().toSeconds();
                hours = totalSecs / 3600;
                minutes = (totalSecs % 3600) / 60;
                seconds = totalSecs % 60;
                timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);

                int hoursNow, secondsNow, minutesNow, totalSecsNow;
                String timeStringNow;
                totalSecsNow = (int) slider.getValue();
                hoursNow = totalSecsNow / 3600;
                minutesNow = (totalSecsNow % 3600) / 60;
                secondsNow = totalSecsNow % 60;
                timeStringNow = String.format("%02d:%02d:%02d", hoursNow, minutesNow, secondsNow);


                lblDuration.setText("Duration: " + timeStringNow + " / " + timeString);
            });

            Scene scene = mediaView.getScene();
            mediaView.fitWidthProperty().bind(scene.widthProperty());
            mediaView.fitHeightProperty().bind(scene.heightProperty());
        }
    }
    @FXML
    private void sliderPressed(MouseEvent event) {
        mediaPlayer.seek(Duration.seconds(slider.getValue()));
    }
}