module com.leviste {
    requires javafx.controls;
    requires javafx.media;
    requires javafx.base;
    requires javafx.fxml;
    requires firebase.admin; // Ensure this is correct
    requires com.google.auth.oauth2;
    requires com.google.auth;
    
    opens com.leviste to javafx.fxml;
    exports com.leviste;
}
