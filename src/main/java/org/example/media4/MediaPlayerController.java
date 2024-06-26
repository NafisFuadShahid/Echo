package org.example.media4;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
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
    private HBox mediaControlBox;

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
    private Button volumeOnOff;


    @FXML
    private ChoiceBox<String> subSync;

    private int subActive = 0;

    private final String[] sync = {"+.5", "+1", "-.5", "-1"};

    @FXML
    private ChoiceBox<String> themeChoiceBox;
    private final String[] theme = {"Dark", "Green", "Blue", "Red"};


    private int fileSelected = 0;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        //intializing the theme choice box
        themeChoiceBox.getItems().addAll(theme);
        themeChoiceBox.setValue("Dark");
        themeChoiceBox.setOnAction(this::applyTheme);

        subSync.getItems().addAll(sync);
        subSync.setOnAction(this::applySync);


        // functionality of playListView
        // Set custom cell factory to show only the file name
        playlistView.setCellFactory(param -> new ListCell<File>() {
            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);
                if (empty || file == null) {
                    setText(null);
                } else {
                    setText(file.getName());
                }
            }
        });

        playlistView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                playSelectedMedia(newValue);
            }
        });

        // Load the necessary images
        playImage = new Image(getClass().getResourceAsStream("/org/example/media4/Icon/play-button.png"));
        pauseImage = new Image(getClass().getResourceAsStream("/org/example/media4/Icon/pause.png"));

        muteImage = new Image(getClass().getResourceAsStream("/org/example/media4/Icon/mute.png"));
        volubleImage = new Image(getClass().getResourceAsStream("/org/example/media4/Icon/high-volume.png"));

    }

    // Function set the main scene
    public void setMainScene(Scene scene) {
        this.mainScene = scene;
    }

    // Function to apply the selected theme
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

    // Function to apply subtitle sync
    @FXML
    private void applySync(ActionEvent event) {
        String syncChoice = subSync.getValue();
        int syncTime = 0;

        switch (syncChoice) {
            case "+.5":
                syncTime = 500; // 0.5 seconds in milliseconds
                break;
            case "+1":
                syncTime = 1000; // 1 second in milliseconds
                break;
            case "-.5":
                syncTime = -500; // -0.5 seconds in milliseconds
                break;
            case "-1":
                syncTime = -1000; // -1 second in milliseconds
                break;
            // Add more cases as needed
        }

        // Apply sync time to the subtitle
        if (!subtitleTimes.isEmpty() && mediaPlayer != null) {
            // Get current media time
            Duration currentMediaTime = mediaPlayer.getCurrentTime();

            // Find the nearest subtitle time after the current media time
            Duration nearestSubtitleTime = null;
            for (Duration subtitleTime : subtitleTimes.values()) {
                if (subtitleTime.greaterThan(currentMediaTime)) {
                    nearestSubtitleTime = subtitleTime;
                    break;
                }
            }

            if (nearestSubtitleTime != null) {
                // Calculate new subtitle time
                Duration newSubtitleTime = nearestSubtitleTime.add(Duration.millis(syncTime));

                // Display the new subtitle text
                displaySubtitleForTime(newSubtitleTime);
            }
        }
    }

    private void displaySubtitleForTime(Duration time) {
        for (Map.Entry<Integer, Duration> entry : subtitleTimes.entrySet()) {
            if (time.greaterThanOrEqualTo(entry.getValue())) {
                // Find the subtitle corresponding to the current time
                int subtitleNumber = entry.getKey();
                String subtitle = subtitles.get(subtitleNumber);
                subtitleText.setText(subtitle);
            }
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


    private Media media;
    private MediaPlayer mediaPlayer;

    @FXML
    private ImageView playPauseImageView;

    private Image playImage;
    private Image pauseImage;

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
            playPauseImageView.setImage(playImage);
            mediaPlayer.play();
            isPlayed = true;
        } else {
//            btnPlay.setText("Play");
            playPauseImageView.setImage(pauseImage);
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
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
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

            // Clear subtitles before loading new subtitles
            subtitles.clear();
            subtitleTimes.clear();
            subtitleText.setText("");
            // Clear audio tags
            lbartist.setText("");
            lbalbum.setText("");
            // Clear the file selected flag
            fileSelected = 0;

            // Load subtitles if the file is an SRT file
            if (fileExtension.equals("srt")) {
                loadSubtitles(selectedFile);
                subActive = 1;
                return;
            }

            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.currentTimeProperty().removeListener(currentTimeListener);
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

            mediaPlayer.currentTimeProperty().addListener(currentTimeListener);

            Scene scene = mediaView.getScene();
            mediaView.fitWidthProperty().bind(scene.widthProperty());
            mediaView.fitHeightProperty().bind(scene.heightProperty());
            mediaPlayer.play();
        }
    }

    private final ChangeListener<Duration> currentTimeListener = (observableValue, oldValue, newValue) -> {
        slider.setValue(newValue.toSeconds());
        updateDurationLabel(media.getDuration());
        if(subActive == 0) {
            displaySubtitle(newValue);
        }
    };

    private void displaySubtitle(Duration currentTime) {
        // Initialize a variable to keep track of the currently displayed subtitle
        String currentSubtitle = null;

        // Iterate through subtitleTimes to find the appropriate subtitle
        for (Map.Entry<Integer, Duration> entry : subtitleTimes.entrySet()) {
            if (currentTime.greaterThanOrEqualTo(entry.getValue())) {
                // Find the subtitle corresponding to the current time
                int subtitleNumber = entry.getKey();
                currentSubtitle = subtitles.get(subtitleNumber);
            } else {
                // Stop once we've found the first subtitle that hasn't appeared yet
                break;
            }
        }

        // Update the subtitle text only if it has changed
        if (currentSubtitle != null && !currentSubtitle.equals(subtitleText.getText())) {
            subtitleText.setText(currentSubtitle);
        }
    }

    @FXML
    private Button SubOnOff;

    @FXML
    private void subOnOff(MouseEvent event) {
        if(subActive == 1){
//            subtitleText.setText("");
            subActive = 0;
        }
        else{
            subtitleText.setText("");
            subActive = 1;
        }
    }

    private void updateDurationLabel(Duration totalDuration) {
        int hours = (int) totalDuration.toHours();
        int minutes = (int) (totalDuration.toMinutes() % 60);
        int seconds = (int) (totalDuration.toSeconds() % 60);
        String durationString = String.format("%02d:%02d:%02d", hours, minutes, seconds);

        int hoursNow = (int) slider.getValue() / 3600;
        int minutesNow = ((int) slider.getValue() % 3600) / 60;
        int secondsNow = (int) slider.getValue() % 60;
        String currentTimeString = String.format("%02d:%02d:%02d", hoursNow, minutesNow, secondsNow);

        lblDuration.setText("Duration: " + currentTimeString + " / " + durationString);
    };





    @FXML
    void selectMedia(ActionEvent event) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open");
        File selectedFile = fileChooser.showOpenDialog(null);
        String fileExtension = getFileExtension(selectedFile);
        if (!fileExtension.equals("srt") && !playlist.contains(selectedFile)) {
            playlist.add(selectedFile);
        }

        ObservableList<File> observablePlaylist = FXCollections.observableArrayList(playlist);

        // Set the ObservableList as the items of the ListView
        playlistView.setItems(observablePlaylist);

        if (fileExtension.equals("srt")) {
            loadSubtitles(selectedFile);

        }

        if (selectedFile != null && fileExtension != "srt") {
            fileSelected = 1;
            String url = selectedFile.toURI().toString();

            String fileName = selectedFile.getName(); // Get the name of the selected file

            try {
                media = new Media(url);
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
                    if(subActive == 0) {
                        displaySubtitle(newValue);
                    }

                    lblDuration.setText("Duration: " + timeStringNow + " / " + timeString);
                });
                if(subActive == 1){subtitleText.setText(""); subActive = 0;}
                Scene scene = mediaView.getScene();
                mediaView.fitWidthProperty().bind(scene.widthProperty());
                mediaView.fitHeightProperty().bind(scene.heightProperty());
                mediaPlayer.play();
            }catch (MediaException e){
                System.out.println("Error: Unsupported Media File " + e);
            }
        }
    }


    private Image muteImage;
    private Image volubleImage;

    @FXML
    private ImageView muteOnOffView;

    private boolean isMute = false;
    @FXML
    private void volumeChanger(MouseEvent event) {
        if(!isMute) {
            volumeSlider.setValue(0);
            muteOnOffView.setImage(muteImage);
            isMute = true;
        }
        else {
            volumeSlider.setValue(75);
            muteOnOffView.setImage(volubleImage);
            isMute = false;
        }
    }


    @FXML
    private void sliderPressed(MouseEvent event) {
        mediaPlayer.seek(Duration.seconds(slider.getValue()));
    }


}

