module org.example.media4 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;


    opens org.example.media4 to javafx.fxml;
    exports org.example.media4;
}