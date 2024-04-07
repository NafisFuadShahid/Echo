package org.example.media4;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;

public class MediaPlayerController {

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

    private Media media;
    private MediaPlayer mediaPlayer;

    private boolean isPlayed = false;

    @FXML
    void btnPlay(MouseEvent event) {
        if (!isPlayed) {
            btnPlay.setText("Pause");
            mediaPlayer.play();
            isPlayed = true;
        } else {
            btnPlay.setText("Play");
            mediaPlayer.pause();
            isPlayed = false;
        }
    }

    @FXML
    void btnStop(MouseEvent event) {
        btnPlay.setText("Play");
        mediaPlayer.stop();
        isPlayed = false;
    }

    @FXML
    void selectMedia(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Media");
        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            String url = selectedFile.toURI().toString();

            media = new Media(url);
            mediaPlayer = new MediaPlayer(media);

            mediaView.setMediaPlayer(mediaPlayer);

            // Update media duration and slider max value when media is ready
            mediaPlayer.setOnReady(() -> {
                Duration totalDuration = media.getDuration();
                slider.setMax(totalDuration.toSeconds());
                lblDuration.setText("Duration: 00 / " + (int) totalDuration.toSeconds());
            });

            // Bind volumeSlider to MediaPlayer volume property
            mediaPlayer.volumeProperty().bind(volumeSlider.valueProperty().divide(100.0));

            // Initialize volumeSlider with initial volume value of MediaPlayer
            if (mediaPlayer != null) {
                double initialVolume = mediaPlayer.getVolume() * 100.0; // Convert volume range (0.0 - 1.0) to (0 - 100)
                volumeSlider.setValue(initialVolume);
            }

            // Add listener to update slider and lblDuration based on currentTime
            mediaPlayer.currentTimeProperty().addListener((observableValue, oldValue, newValue) -> {
                slider.setValue(newValue.toSeconds());
                lblDuration.setText("Duration: " + (int) slider.getValue() + " / " + (int) media.getDuration().toSeconds());
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