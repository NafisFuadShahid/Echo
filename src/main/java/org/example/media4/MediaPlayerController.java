package org.example.media4;

import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class MediaPlayerController implements Initializable {



    private Scene mainScene;
    @FXML
    private Label lbartist;

    @FXML
    private Label lbalbum;
    @FXML
    private Text subtitleText;

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

    private final String[] theme = {"Dark", "Green", "Blue", "Red"};
    private int fileSelected = 0;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        themeChoiceBox.getItems().addAll(theme);
        themeChoiceBox.setValue("Blue");
        themeChoiceBox.setOnAction(this::applyTheme);

//        playlistView.setVisible(false);

//        // Show the ListView on mouse entered
//        playlistView.setOnMouseEntered((MouseEvent event) -> {
//            playlistView.setVisible(true);
//        });
//
//        // Hide the ListView on mouse exited
//        playlistView.setOnMouseExited((MouseEvent event) -> {
//            playlistView.setVisible(false);
//        });
        playlistView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                playSelectedMedia(newValue);
            }
        });
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
    private Map<Integer, String> subtitles = new HashMap<>(); // Map to store subtitle texts with their sequence numbers
    private Map<Integer, Duration> subtitleTimes = new HashMap<>(); // Map to store subtitle timings
    private void loadSubtitles(File srtFile) {
        subtitles.clear();
        subtitleTimes.clear();

        try (BufferedReader reader = new BufferedReader(new FileReader(srtFile))) {
            String line;
            int sequenceNumber = 0;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    // Parse subtitle sequence number
                    if (line.matches("^[0-9]+$")) {
                        sequenceNumber = Integer.parseInt(line);
                    } else if (line.matches("\\d{2}:\\d{2}:\\d{2},\\d{3} --> \\d{2}:\\d{2}:\\d{2},\\d{3}")) {
                        // Parse subtitle timings
                        String[] timings = line.split(" --> ");
                        Duration startTime = parseTime(timings[0]);
                        Duration endTime = parseTime(timings[1]);
                        subtitleTimes.put(sequenceNumber, startTime);
                    } else {
                        // Parse subtitle text
                        StringBuilder subtitleText = new StringBuilder(line);
                        while ((line = reader.readLine()) != null && !line.isEmpty()) {
                            subtitleText.append(" ").append(line);
                        }
                        subtitles.put(sequenceNumber, subtitleText.toString().trim());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private Duration parseTime(String timeString) {
        String[] parts = timeString.split("[:,]");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);
        int milliseconds = Integer.parseInt(parts[3]);
        long totalMilliseconds = (hours * 3600 + minutes * 60 + seconds) * 1000 + milliseconds;
        return Duration.millis(totalMilliseconds);
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

    ArrayList<File> playlist = new ArrayList<>();

    @FXML
    private ListView<File> playlistView;

    @FXML
    void showplaylist(ActionEvent event) {
        playlistView.setVisible(true);
    }

    @FXML
    void donotshowplaylist(ActionEvent event) {
        playlistView.setVisible(false);
    }

    private void playSelectedMedia(File selectedFile) {
        if (selectedFile != null) {
            String fileExtension = getFileExtension(selectedFile);

            if (mediaPlayer != null) {
                mediaPlayer.stop();
            }

            String url = selectedFile.toURI().toString();
            media = new Media(url);
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);

            // Update window title with the name of the selected file
            Stage stage = (Stage) mediaView.getScene().getWindow();
            stage.setTitle(selectedFile.getName());

            // Update media duration and slider max value when media is ready
            mediaPlayer.setOnReady(() -> {
                Duration totalDuration = media.getDuration();
                slider.setMax(totalDuration.toSeconds());
                updateDurationLabel(totalDuration);
            });

            // Bind volume slider to media player volume property
            mediaPlayer.volumeProperty().bind(volumeSlider.valueProperty().divide(100.0));
            volumeSlider.setValue(75);

            mediaPlayer.currentTimeProperty().addListener((observableValue, oldValue, newValue) -> {
                slider.setValue(newValue.toSeconds());
                updateDurationLabel(media.getDuration());
                displaySubtitle(newValue);
            });

            Scene scene = mediaView.getScene();
            mediaView.fitWidthProperty().bind(scene.widthProperty());
            mediaView.fitHeightProperty().bind(scene.heightProperty());
            mediaPlayer.play();
        }
    }

    private void updateDurationLabel(Duration totalDuration) {
        int hours = (int) totalDuration.toHours();
        int minutes = (int) (totalDuration.toMinutes() % 60);
        int seconds = (int) (totalDuration.toSeconds() % 60);
        String durationString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        lblDuration.setText("Duration: 00:00:00 / " + durationString);
    }


    @FXML
    void selectMedia(ActionEvent event) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open");
        File selectedFile = fileChooser.showOpenDialog(null);
        String fileExtension = getFileExtension(selectedFile);
        if (!fileExtension.equals("srt")) {
            playlist.add(selectedFile);
        }
//        System.out.println(playlist);
//        System.out.println(selectedFile);
        ObservableList<File> observablePlaylist = FXCollections.observableArrayList(playlist);

        // Set the ObservableList as the items of the ListView
        playlistView.setItems(observablePlaylist);

//        // Hide the ListView initially
//        playlistView.setVisible(false);

//        // Show the ListView on mouse entered
//        playlistView.setOnMouseEntered((MouseEvent even) -> {
//            playlistView.setVisible(true);
//        });
//
//        // Hide the ListView on mouse exited
//        playlistView.setOnMouseExited((MouseEvent even) -> {
//            playlistView.setVisible(false);
//        });

        if (fileExtension.equals("srt")) {
            loadSubtitles(selectedFile);

        }

        if (selectedFile != null && fileExtension != "srt") {
            fileSelected = 1;
            String url = selectedFile.toURI().toString();


            media = new Media(url);
            String fileName = selectedFile.getName(); // Get the name of the selected file

            mediaPlayer = new MediaPlayer(media);
            if(fileExtension.equals("mp3") || fileExtension.equals("wav")){
                media.getMetadata().addListener((MapChangeListener.Change<? extends String, ? extends Object> c) -> {
                    if (c.wasAdded()) {
                        if ("artist".equals(c.getKey())) {
                            String  artist = c.getValueAdded().toString();
                            lbartist.setText(artist);
                        } else if ("title".equals(c.getKey())) {
                            String title = c.getValueAdded().toString();
                        } else if ("year".equals(c.getKey())) {
                            String album = c.getValueAdded().toString();
                            lbalbum.setText(album);
                        }
                    }
                });
            }
            mediaView.setMediaPlayer(mediaPlayer);

            // Set window title to the name of the file being played
            Stage stage = (Stage) mediaView.getScene().getWindow();
            stage.setTitle(fileName);

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
                displaySubtitle(newValue);

                lblDuration.setText("Duration: " + timeStringNow + " / " + timeString);
            });

            Scene scene = mediaView.getScene();
            mediaView.fitWidthProperty().bind(scene.widthProperty());
            mediaView.fitHeightProperty().bind(scene.heightProperty());
            mediaPlayer.play();
        }
    }

    //        // Show the ListView on mouse entered
//        playlistView.setOnMouseEntered((MouseEvent even) -> {
//            playlistView.setVisible(true);
//        });
//
//        // Hide the ListView on mouse exited
//        playlistView.setOnMouseExited((MouseEvent even) -> {
//            playlistView.setVisible(false);
//        });


    @FXML
    private void sliderPressed(MouseEvent event) {
        mediaPlayer.seek(Duration.seconds(slider.getValue()));
    }
    private void displaySubtitle(Duration currentTime) {
        for (Map.Entry<Integer, Duration> entry : subtitleTimes.entrySet()) {
            if (currentTime.greaterThanOrEqualTo(entry.getValue())) {
                // Find the subtitle corresponding to the current time
                int subtitleNumber = entry.getKey();
                String subtitle = subtitles.get(subtitleNumber);
                subtitleText.setText(subtitle);
            }
        }
    }

}

