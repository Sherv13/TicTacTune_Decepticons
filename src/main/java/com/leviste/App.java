package com.leviste;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.File;

import java.util.*;
import javafx.util.Duration;
import javafx.animation.PauseTransition;

public class App extends Application {
    private FirebaseService firebaseService;
    private String roomId;
    private String playerId;
    private boolean isPlayerX;
    private Button[][] boardButtons;
    private boolean isMyTurn;
    private Stage primaryStage;
    private MediaPlayer mediaPlayer;
    private Slider volumeSlider;
    private boolean hasWon = false;
    private String currentUsername;
    private MediaPlayer backgroundVideoPlayer;

    private ListView<String> songListView;
    private Map<String, FirebaseService.Song> songsMap;

    private Queue<Map.Entry<String, Map<String, Object>>> songQueue = new LinkedList<>();
    private Map<String, Map<String, Object>> queueMap = new HashMap<>();


    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        firebaseService = new FirebaseService();
        
        showLoginPage();
    }

    private void showLoginPage() {
        VBox loginLayout = new VBox(10);
        loginLayout.setAlignment(Pos.CENTER);
        loginLayout.setPadding(new Insets(20));
        loginLayout.getStyleClass().add("login-page");

        Label titleLabel = new Label("TicTacTune");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(200);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(200);

        Button loginButton = new Button("Login");
        loginButton.setMaxWidth(200);
        loginButton.getStyleClass().add("button");

        Button signupButton = new Button("Sign Up");
        signupButton.setMaxWidth(200);
        signupButton.getStyleClass().addAll("button", "signup-button");

        Label messageLabel = new Label("");
        messageLabel.setStyle("-fx-text-fill: red;");

        loginButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();
            
            if (username.isEmpty() || password.isEmpty()) {
                messageLabel.setText("Please fill in all fields");
                return;
            }

            firebaseService.verifyLogin(username, password, isValid -> {
                Platform.runLater(() -> {
                    if (isValid) {
                        currentUsername = username;
                        showMainMenu();
                    } else {
                        messageLabel.setText("Invalid username or password");
                    }
                });
            });
        });

        signupButton.setOnAction(e -> showSignupPage());

        loginLayout.getChildren().addAll(
            titleLabel,
            new Label("Login"),
            usernameField,
            passwordField,
            messageLabel,
            loginButton,
            new Label("Don't have an account?"),
            signupButton
        );

        

        Scene loginScene = new Scene(loginLayout, 300, 400);
        primaryStage.setTitle("TicTacTune - Login");
        primaryStage.setScene(loginScene);

        Image image = new Image("Tic-Tac-Tune.png");
        primaryStage.getIcons().add(image);

        primaryStage.getScene().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.show();
    }

    private void showSignupPage() {
        VBox signupLayout = new VBox(10);
        signupLayout.setAlignment(Pos.CENTER);
        signupLayout.setPadding(new Insets(20));
        signupLayout.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("TicTacTune");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(200);
        usernameField.getStyleClass().add("text-field");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(200);
        passwordField.getStyleClass().add("text-field");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm Password");
        confirmPasswordField.setMaxWidth(200);
        confirmPasswordField.getStyleClass().add("text-field");

        Button signupButton = new Button("Create Account");
        signupButton.setMaxWidth(200);
        signupButton.getStyleClass().addAll("button", "signup-button");

        Button backButton = new Button("Back to Login");
        backButton.setMaxWidth(200);
        backButton.getStyleClass().add("button");

        Label messageLabel = new Label("");
        messageLabel.setStyle("-fx-text-fill: red;");

        signupButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();
            String confirmPassword = confirmPasswordField.getText().trim();

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                messageLabel.setText("Please fill in all fields");
                return;
            }

            if (!password.equals(confirmPassword)) {
                messageLabel.setText("Passwords do not match");
                return;
            }

            firebaseService.createUser(username, password, success -> {
                Platform.runLater(() -> {
                    if (success) {
                        showLoginPage();
                    } else {
                        messageLabel.setText("Username already exists");
                    }
                });
            });
        });

        backButton.setOnAction(e -> showLoginPage());

        signupLayout.getChildren().addAll(
            titleLabel,
            new Label("Create Account"),
            usernameField,
            passwordField,
            confirmPasswordField,
            messageLabel,
            signupButton,
            backButton
        );

        Scene signupScene = new Scene(signupLayout, 300, 400);
        primaryStage.setTitle("TicTacTune - Sign Up");
        primaryStage.setScene(signupScene);
        primaryStage.getScene().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
    }

    private void showMainMenu() {
        VBox menuLayout = new VBox(20);
        menuLayout.setAlignment(Pos.CENTER);
        menuLayout.setPadding(new Insets(20));

        Button createRoomButton = new Button("Create Room");
        Button joinRoomButton = new Button("Enter Room ID");

        createRoomButton.setOnAction(e -> createRoom());
        joinRoomButton.setOnAction(e -> showJoinRoomDialog());

        menuLayout.getChildren().addAll(createRoomButton, joinRoomButton);

        Scene scene = new Scene(menuLayout, 300, 200);
        primaryStage.setTitle("Tic-Tac-Toe with Music");
        primaryStage.setScene(scene);
        primaryStage.getScene().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.show();
    }

    private VBox createMusicPlayerUI() {
        VBox musicPlayer = new VBox(10);
        musicPlayer.setAlignment(Pos.CENTER);
        musicPlayer.setPadding(new Insets(10));
        musicPlayer.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 5;");
    
        // Song List Section
        VBox songListSection = new VBox(5);
        songListSection.setAlignment(Pos.CENTER);
        Label songListLabel = new Label("Song List:");
        songListView = new ListView<>();
        songListView.setPrefHeight(150);
        songListView.setDisable(!hasWon);
        songListSection.getChildren().addAll(songListLabel, songListView);
    
        // Playlist Queue Section
        VBox queueSection = new VBox(5);
        queueSection.setAlignment(Pos.CENTER);
        Label queueLabel = new Label("Playlist Queue:");
        ListView<String> queueListView = new ListView<>();
        queueListView.setPrefHeight(150);
        queueListView.setDisable(!hasWon);
        queueSection.getChildren().addAll(queueLabel, queueListView);
    
        // Load songs from Firebase
        firebaseService.listenForSongs(songs -> {
            Platform.runLater(() -> {
                songsMap = songs;
                ObservableList<String> songTitles = FXCollections.observableArrayList();
                songs.values().forEach(song -> songTitles.add(song.getTitle()));
                songListView.setItems(songTitles);
            });
        });
    
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER);
    
        Button addQueueButton = new Button("Add to Playlist Queue");
        Button skipButton = new Button("Skip");
        
        addQueueButton.setDisable(!hasWon);
        skipButton.setDisable(!hasWon);
    
        addQueueButton.setOnAction(e -> {
            if (hasWon && songListView.getSelectionModel().getSelectedItem() != null) {
                String selectedTitle = songListView.getSelectionModel().getSelectedItem();
                FirebaseService.Song selectedSong = songsMap.values().stream()
                    .filter(song -> song.getTitle().equals(selectedTitle))
                    .findFirst()
                    .orElse(null);
        
                if (selectedSong != null) {
                    String songKey = songsMap.entrySet().stream()
                        .filter(entry -> entry.getValue().getTitle().equals(selectedTitle))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse("");
        
                    firebaseService.addToQueue(roomId, songKey, selectedTitle, selectedSong.getUrl());
                }
            }
        });
        
        skipButton.setOnAction(e -> {
            if (mediaPlayer != null) {
                // Update skip request in Firebase
                firebaseService.skipCurrentSong(roomId);
            }
        });
    
        volumeSlider = new Slider(0, 100, 50);
        volumeSlider.setShowTickMarks(true);
        volumeSlider.setShowTickLabels(true);
    
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(newVal.doubleValue() / 100);
                firebaseService.updateVolume(roomId, newVal.doubleValue());
            }
        });
    
        controls.getChildren().addAll(addQueueButton, skipButton);
        
        musicPlayer.getChildren().addAll(
            songListSection,
            queueSection,
            controls,
            new Label("Volume"),
            volumeSlider
        );
    
        // Initialize queue listener
        firebaseService.listenToQueue(roomId, new FirebaseService.QueueUpdateCallback() {
            @Override
            public void onSongAdded(String key, Map<String, Object> songData) {
                Platform.runLater(() -> {
                    // Add to queueMap and songQueue without duplicating
                    if (!queueMap.containsKey(key)) {
                        queueMap.put(key, songData);
                        songQueue.offer(new AbstractMap.SimpleEntry<>(key, songData));
                        updateQueueDisplay();
                        
                        // Only start playing if this is the first song in queue
                        if (songQueue.size() == 1 && mediaPlayer == null) {
                            Map.Entry<String, Map<String, Object>> firstSong = songQueue.peek();
                            loadMusic((String) firstSong.getValue().get("url"));
                        }
                    }
                });
            }
        
            @Override
            public void onSongRemoved(String key) {
                Platform.runLater(() -> {
                    queueMap.remove(key);
                    songQueue.removeIf(entry -> entry.getKey().equals(key));
                    updateQueueDisplay();
                });
            }
        });
    
        return musicPlayer;
    }


    private void createRoom() {
        // Generate a random room ID
        String newRoomId = UUID.randomUUID().toString().substring(0, 6);
        this.roomId = newRoomId;
        this.playerId = "player1";
        this.isPlayerX = true;
        this.isMyTurn = true;
    
        firebaseService.createRoom(newRoomId, () -> {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Room Created");
                alert.setHeaderText(null);
                alert.setContentText("Your room ID is: " + newRoomId);
                alert.showAndWait();
                
                initializeGame();
            });
        });
    }
    
    private void showJoinRoomDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Join Room");
        dialog.setHeaderText(null);
        dialog.setContentText("Enter Room ID:");
    
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(roomId -> {
            firebaseService.joinRoom(roomId, exists -> {
                if (exists) {
                    this.roomId = roomId;
                    this.playerId = "player2";
                    this.isPlayerX = false;
                    this.isMyTurn = false;
                    
                    Platform.runLater(this::initializeGame);
                } else {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText(null);
                        alert.setContentText("Room not found!");
                        alert.showAndWait();
                    });
                }
            });
        });
    }

    private void loadMusic(String url) {
        if (mediaPlayer != null) {
            mediaPlayer.dispose();
        }
    
        try {
            Media media = new Media(url);
            mediaPlayer = new MediaPlayer(media);
            
            mediaPlayer.setOnEndOfMedia(() -> {
                Platform.runLater(() -> {
                    // Remove the current song from queue first
                    if (!songQueue.isEmpty()) {
                        Map.Entry<String, Map<String, Object>> currentSong = songQueue.poll();
                        String queueItemKey = currentSong.getKey();
                        firebaseService.removeFromQueue(roomId, queueItemKey);
                    }
                    
                    // Then play the next song if available
                    if (!songQueue.isEmpty()) {
                        Map.Entry<String, Map<String, Object>> nextSong = songQueue.peek();
                        Map<String, Object> songData = nextSong.getValue();
                        loadMusic((String) songData.get("url"));
                    }
                    
                    updateQueueDisplay();
                });
            });
    
            mediaPlayer.setVolume(volumeSlider.getValue() / 100);
            mediaPlayer.play();
    
        } catch (Exception e) {
            System.err.println("Error loading music: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeMusicListeners() {
        firebaseService.listenForMusicUpdates(roomId, new FirebaseService.MusicUpdateCallback() {
            @Override
            public void onMusicStateUpdate(String state) {
                Platform.runLater(() -> {
                    if (mediaPlayer != null) {
                        switch (state) {
                            case "PAUSED":
                                mediaPlayer.pause();
                                break;
                            case "NEXT":
                                // Skip to next song when state is NEXT
                                if (!songQueue.isEmpty()) {
                                    Map.Entry<String, Map<String, Object>> nextSong = songQueue.poll();
                                    String queueItemKey = nextSong.getKey();
                                    Map<String, Object> songData = nextSong.getValue();
                                    
                                    loadMusic((String) songData.get("url"));
                                    firebaseService.removeFromQueue(roomId, queueItemKey);
                                    
                                    updateQueueDisplay();
                                }
                                break;
                        }
                    }
                });
            }
    
            @Override
            public void onVolumeUpdate(double volume) {
                Platform.runLater(() -> {
                    volumeSlider.setValue(volume);
                    if (mediaPlayer != null) {
                        mediaPlayer.setVolume(volume / 100);
                    }
                });
            }
    
            @Override
            public void onMusicUrlUpdate(String url) {
                Platform.runLater(() -> {
                    if (url != null && !url.isEmpty()) {
                        loadMusic(url);
                    }
                });
            }
        });
    }

    private Label turnLabel; // Add this field at class level
    
    private void initializeGame() {
        boardButtons = new Button[3][3];
        
        // Create main layout
        HBox mainLayout = new HBox(20);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setPadding(new Insets(20));
    
        // Game board section
        VBox gameSection = new VBox(20);
        gameSection.setAlignment(Pos.CENTER);
    
        // Add Room ID Label at the top
        Label roomIdLabel = new Label("Room ID: " + roomId);
        roomIdLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
    
        GridPane boardGrid = new GridPane();
        boardGrid.setAlignment(Pos.CENTER);
        boardGrid.setHgap(10);
        boardGrid.setVgap(10);
    
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Button button = new Button();
                button.setPrefSize(100, 100);
                button.setStyle("-fx-font-size: 24px;");
                final int row = i;
                final int col = j;
                button.setOnAction(e -> makeMove(row, col));
                boardButtons[i][j] = button;
                boardGrid.add(button, j, i);
            }
        }
    
        VBox musicPlayer = createMusicPlayerUI();
    
        Button resetButton = new Button("Reset Board");
        resetButton.setOnAction(e -> resetBoard());
        gameSection.getChildren().add(resetButton);
    
        Scene gameScene = new Scene(mainLayout, 800, 450);
        primaryStage.setScene(gameScene);
        primaryStage.getScene().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
    
        turnLabel = new Label(isMyTurn ? "Your turn (" + (isPlayerX ? "X" : "O") + ")" : 
                                       "Opponent's turn (" + (!isPlayerX ? "X" : "O") + ")");
        
        // Add roomIdLabel first, then other components
        gameSection.getChildren().addAll(roomIdLabel, turnLabel, boardGrid);
    
        // Add both sections to main layout
        mainLayout.getChildren().addAll(gameSection, musicPlayer);
    
        // Listen for game updates with improved turn handling
        firebaseService.listenForMoves(roomId, (row, col, value) -> {
            Platform.runLater(() -> {
                // Only process the move if it's not our turn and the cell is empty
                if (!isMyTurn && boardButtons[row][col].getText().isEmpty()) {
                    boardButtons[row][col].setText(value);
                    
                    // Check if opponent won
                    if (checkWin(value)) {
                        showGameOverDialog("You lose!");
                        return;
                    }
                    
                    // Check for draw
                    if (checkDraw()) {
                        showGameOverDialog("Game ended in a draw!");
                        return;
                    }
                    
                    // Switch turns
                    isMyTurn = true;
                    turnLabel.setText("Your turn (" + (isPlayerX ? "X" : "O") + ")");
                }
            });
        });
    }

    private void resetBoard() {
        // Clear all the board buttons and ensure they're enabled
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                boardButtons[i][j].setText("");
                boardButtons[i][j].setDisable(false);
            }
        }
    
        // Reset the turn indicator
        isMyTurn = isPlayerX;
        turnLabel.setText(isMyTurn ? "Your turn (" + (isPlayerX ? "X" : "O") + ")" : 
                         "Opponent's turn (" + (!isPlayerX ? "X" : "O") + ")");
    
        // Reset the game state
        hasWon = false;
    
        // Make sure music controls are in correct state
        if (hasWon) {
            enableMusicControls();
            if (songListView != null) {
                songListView.setDisable(false);
            }
        } else {
            disableMusicControls();
            if (songListView != null) {
                songListView.setDisable(true);
            }
        }
    }

    private void selectSong() {
        // Disable the "Select Song" button
        Scene scene = primaryStage.getScene();
        for (javafx.scene.Node node : ((HBox) scene.getRoot()).getChildren()) {
            if (node instanceof VBox) {
                VBox musicPlayerVBox = (VBox) node;
                for (javafx.scene.Node child : musicPlayerVBox.getChildren()) {
                    if (child instanceof HBox) {
                        for (javafx.scene.Node control : ((HBox) child).getChildren()) {
                            if (control instanceof Button && control.getId() != null && control.getId().equals("selectSongButton")) {
                                control.setDisable(true);
                            }
                        }
                    }
                }
            }
        }

        // Get the selected song from the song list
        String selectedTitle = songListView.getSelectionModel().getSelectedItem();
        FirebaseService.Song selectedSong = songsMap.values().stream()
            .filter(song -> song.getTitle().equals(selectedTitle))
            .findFirst()
            .orElse(null);

        if (selectedSong != null) {
            String songKey = songsMap.entrySet().stream()
                .filter(entry -> entry.getValue().getTitle().equals(selectedTitle))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("");

            // Add the selected song to the queue
            firebaseService.addToQueue(roomId, songKey, selectedTitle, selectedSong.getUrl());

            // Reset the board
            resetBoard();
        }
    }



    private void makeMove(int row, int col) {
        // Only allow moves if it's the player's turn and the cell is empty
        if (!isMyTurn || !boardButtons[row][col].getText().isEmpty()) {
            return;
        }
    
        String symbol = isPlayerX ? "X" : "O";
        boardButtons[row][col].setText(symbol);
        firebaseService.makeMove(roomId, row, col, symbol);
        
        // Switch turns immediately after making a move
        isMyTurn = false;
        turnLabel.setText("Opponent's turn (" + (!isPlayerX ? "X" : "O") + ")");
    
        // Check for win first
        if (checkWin(symbol)) {
            showGameOverDialog("You win!");
            return;
        }
    
        // Only check for draw if there's no win
        if (!checkDraw()) {
            // Game continues if it's not a win or draw
            turnLabel.setText("Opponent's turn (" + (!isPlayerX ? "X" : "O") + ")");
        }
    }
    
    private boolean checkWin(String symbol) {
        // Check rows
        for (int i = 0; i < 3; i++) {
            if (boardButtons[i][0].getText().equals(symbol) &&
                boardButtons[i][1].getText().equals(symbol) &&
                boardButtons[i][2].getText().equals(symbol)) {
                return true;
            }
        }
    
        // Check columns
        for (int i = 0; i < 3; i++) {
            if (boardButtons[0][i].getText().equals(symbol) &&
                boardButtons[1][i].getText().equals(symbol) &&
                boardButtons[2][i].getText().equals(symbol)) {
                return true;
            }
        }
    
        // Check diagonals
        if (boardButtons[0][0].getText().equals(symbol) &&
            boardButtons[1][1].getText().equals(symbol) &&
            boardButtons[2][2].getText().equals(symbol)) {
            return true;
        }
    
        if (boardButtons[0][2].getText().equals(symbol) &&
            boardButtons[1][1].getText().equals(symbol) &&
            boardButtons[2][0].getText().equals(symbol)) {
            return true;
        }
    
        return false;
    }
    
    private boolean checkDraw() {
        // First check if the board is full
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (boardButtons[i][j].getText().isEmpty()) {
                    return false;
                }
            }
        }
        
        // If we get here, it's a draw
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Over");
            alert.setHeaderText(null);
            alert.setContentText("Game ended in a draw! The board will reset automatically.");
            alert.show();
            
            // Dismiss the alert after 2 seconds
            PauseTransition delay = new PauseTransition(Duration.seconds(2));
            delay.setOnFinished(e -> {
                alert.close();
                
                // Reset all game states
                hasWon = false;
                isMyTurn = isPlayerX;
                
                // Enable all board buttons for both players
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        boardButtons[i][j].setDisable(false);
                        boardButtons[i][j].setText("");
                    }
                }
                
                // Reset turn label
                turnLabel.setText(isMyTurn ? "Your turn (" + (isPlayerX ? "X" : "O") + ")" : 
                                "Opponent's turn (" + (!isPlayerX ? "X" : "O") + ")");
                
                // Make sure music controls are disabled for both players
                disableMusicControls();
                if (songListView != null) {
                    songListView.setDisable(true);
                }
            });
            delay.play();
        });
        
        return true;
    }

    private void showGameOverDialog(String message) {
        hasWon = message.contains("You win");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(null);
        alert.setContentText(message + (hasWon ? "\nYou can now select a song to add to the queue!" : "\nWaiting for winner to select music..."));

        alert.showAndWait().ifPresent(response -> {
            // Disable all board buttons after game over
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    boardButtons[i][j].setDisable(true);
                }
            }

            turnLabel.setText("Game Over - " + message);

            initializeMusicListeners();

            if (hasWon) {
                enableMusicControls();
                songListView.setDisable(false);
            } else {
                disableMusicControls();
            }
        });
    }
    private void disableMusicControls() {
        // Disable all music controls for the losing player
        Scene scene = primaryStage.getScene();
        for (javafx.scene.Node node : ((HBox) scene.getRoot()).getChildren()) {
            if (node instanceof VBox) {
                VBox musicPlayerVBox = (VBox) node;
                for (javafx.scene.Node child : musicPlayerVBox.getChildren()) {
                    if (child instanceof HBox) { // Find the controls HBox
                        for (javafx.scene.Node control : ((HBox) child).getChildren()) {
                            control.setDisable(true); // Disable all controls
                        }
                    }
                }
            }
        }
    }

    private void enableMusicControls() {
        // Find the music player VBox and enable its controls
        Scene scene = primaryStage.getScene();
        for (javafx.scene.Node node : ((HBox) scene.getRoot()).getChildren()) {
            if (node instanceof VBox) {
                VBox musicPlayerVBox = (VBox) node;
                for (javafx.scene.Node child : musicPlayerVBox.getChildren()) {
                    if (child instanceof HBox) { // Find the controls HBox
                        for (javafx.scene.Node control : ((HBox) child).getChildren()) {
                            control.setDisable(false); // Enable all controls
                        }
                    }
                }
            }
        }
    }
    private void updateQueueDisplay() {
        Scene scene = primaryStage.getScene();
        ListView<String> queueListView = null;
        
        // Find the queue ListView in the scene
        for (javafx.scene.Node node : ((HBox) scene.getRoot()).getChildren()) {
            if (node instanceof VBox) {
                for (javafx.scene.Node child : ((VBox) node).getChildren()) {
                    if (child instanceof VBox) {
                        for (javafx.scene.Node grandChild : ((VBox) child).getChildren()) {
                            if (grandChild instanceof ListView) {
                                queueListView = (ListView<String>) grandChild;
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        if (queueListView != null) {
            ObservableList<String> queueTitles = FXCollections.observableArrayList();
            songQueue.forEach(entry -> queueTitles.add((String) entry.getValue().get("title")));
            queueListView.setItems(queueTitles);
        }
    }


    @Override
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.dispose();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}