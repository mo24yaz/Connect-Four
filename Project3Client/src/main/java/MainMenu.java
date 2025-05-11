import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.Objects;


public class MainMenu {
    private final Stage stage;
    private TextArea chatArea;
    private TextField messageField;
    private TextField usernameField;
    private String username = "User";
    private Client client;

    // We'll rely on the server to validate username uniqueness
    public MainMenu(Stage stage) {
        this.stage = stage;
        stage.setResizable(false);
    }

    public Scene getScene() {
        // Set up the background image
        Image backgroundImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("background.png")));
        BackgroundImage background = new BackgroundImage(
                backgroundImage,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(100, 100, true, true, true, true)
        );

        // Set up custom button image
        Image buttonImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("button.png")));

        Label title = new Label("Connect-4");
        title.setFont(Font.loadFont(getClass().getResourceAsStream("ka1.ttf"), 30));

        // Create custom buttons using the button image
        Button single = createCustomButton("Single Player", buttonImg);
        single.setOnAction(this::startSingle);

        Button multi = createCustomButton("Multiplayer", buttonImg);
        multi.setOnAction(this::startMultiplayer);

        Button connectBtn = createCustomButton("Connect to Server", buttonImg);
        connectBtn.setOnAction(e -> connectToServer());

        // Create chat components
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setPrefHeight(400);
        chatArea.setWrapText(true);
        chatArea.setStyle("-fx-font-size: 14px; -fx-control-inner-background: rgba(255, 255, 255, 0.8);");

        // Message input at the bottom
        messageField = new TextField();
        messageField.setPromptText("Type a message...");
        messageField.setOnAction(e -> sendChatMessage());

        Button sendBtn = createCustomButton("Send", buttonImg);
        sendBtn.setOnAction(e -> sendChatMessage());

        HBox messageBox = new HBox(10, messageField, sendBtn);
        messageBox.setAlignment(Pos.CENTER);
        messageBox.setPadding(new Insets(10));

        // Username field
        usernameField = new TextField(username);
        usernameField.setPromptText("Your username");

        Button setUsernameBtn = createCustomButton("Set Username", buttonImg);
        setUsernameBtn.setOnAction(e -> setUsername());

        HBox usernameBox = new HBox(10,
                new Label("Username:"), usernameField, setUsernameBtn, connectBtn
        );
        usernameBox.setAlignment(Pos.CENTER);
        usernameBox.setPadding(new Insets(10));

        // Create the main layout
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setBackground(new Background(background));

        // Top section with title and game buttons
        VBox gameButtons = new VBox(20, title, single, multi);
        gameButtons.setAlignment(Pos.CENTER);
        gameButtons.setPadding(new Insets(20));
        root.setTop(gameButtons);

        // Chat section
        Label chatLabel = new Label("Public Chat");
        chatLabel.setFont(Font.loadFont(getClass().getResourceAsStream("ka1.ttf"), 12));
        chatLabel.setTextFill(Color.WHITE);

        VBox chatBox = new VBox(10,
                chatLabel, chatArea, messageBox, usernameBox
        );
        chatBox.setPadding(new Insets(20));
        chatBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5); -fx-background-radius: 10;");
        root.setCenter(chatBox);

        return new Scene(root, 600, 600);
    }

    private Button createCustomButton(String text, Image buttonImage) {
        // Create a button with text
        Button button = new Button(text);

        // Create an ImageView with the button image
        ImageView imageView = new ImageView(buttonImage);
        imageView.setFitHeight(40);
        imageView.setPreserveRatio(true);

        // Set the graphic for the button
        button.setGraphic(imageView);
        button.setContentDisplay(javafx.scene.control.ContentDisplay.CENTER);

        // Style the button text
        button.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-color: transparent; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 0);"
        );

        return button;
    }

    private void setUsername() {
        String newUsername = usernameField.getText().trim();

        // Basic validation
        if (newUsername.isEmpty()) {
            showAlert();
            return;
        }

        // If we're connected to server, send username change request
        if (client != null) {
            // The server will validate uniqueness and respond with USERNAME_ACCEPTED or USERNAME_TAKEN
            client.send("SET_USERNAME:" + newUsername);
            chatArea.appendText("Requesting username change to: " + newUsername + "...\n");
        } else {
            // If not connected, just update locally
            username = newUsername;
            chatArea.appendText("Username set to: " + username + " (not connected to server)\n");
        }
    }

    private void showAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Username Error");
        alert.setHeaderText(null);
        alert.setContentText("Username cannot be empty.");
        alert.showAndWait();
    }

    private void connectToServer() {
        // Check if already connected
        if (client != null) {
            chatArea.appendText("Already connected to server.\n");
            return;
        }

        // Get username before connecting
        String newUsername = usernameField.getText().trim();
        if (newUsername.isEmpty()) {
            newUsername = "User" + System.currentTimeMillis() % 1000; // Generate random username if empty
            usernameField.setText(newUsername);
        }

        // Store the desired username
        username = newUsername;

        // Connect to localhost:5555
        client = new Client("localhost", 5555, this::handleServerMessage);

        // Now start the networking thread
        client.start();

        // Server will send list of active usernames upon connection
        chatArea.appendText("Connecting to server...\n");
    }

    private void startSingle(ActionEvent e) {
        Connect4 logic = new Connect4();
        GameScene gs = new GameScene(logic, null, stage, getScene());

        stage.setScene(gs.getScene());
    }

    private void startMultiplayer(ActionEvent e) {
        // Get username before starting multiplayer
        String newUsername = usernameField.getText().trim();
        if (newUsername.isEmpty()) {
            newUsername = "User" + System.currentTimeMillis() % 1000;
            usernameField.setText(newUsername);
        }

        // Store the desired username - the server will validate
        username = newUsername;

        Connect4 logic = new Connect4();
        Client gameClient = new Client("localhost", 5555, null);
        GameScene gs = new GameScene(logic, gameClient, stage, getScene());

        // wire callback after construction
        gs.client.setCallback(gs::onReceive);

        stage.setScene(gs.getScene());
        gs.client.start();

        // Set username for this client - the server will validate
        gameClient.setUsername(username);
    }

    private void sendChatMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty() || client == null) return;

        // Send public chat message
        client.sendPublicChat(username + ": " + message);
        messageField.clear();
    }

    private void handleServerMessage(String message) {
        javafx.application.Platform.runLater(() -> {
            // Public chat from server: protocol → "PUBLIC_CHAT:user:hello there"
            if (message.startsWith("PUBLIC_CHAT:")) {
                String payload = message.substring("PUBLIC_CHAT:".length());
                String[] parts = payload.split(":", 2);
                if (parts.length == 2) {
                    String fromUser = parts[0];
                    String text     = parts[1];
                    chatArea.appendText(fromUser + ": " + text + "\n");
                } else {
                    // fallback if server sent just "PUBLIC_CHAT:hello"
                    chatArea.appendText(payload + "\n");
                }
            }
            // User join/leave/disconnect
            else if (message.startsWith("USER_JOINED:")) {
                chatArea.appendText(message.substring("USER_JOINED:".length()) + " joined.\n");
            }
            else if (message.startsWith("USER_LEFT:")) {
                chatArea.appendText(message.substring("USER_LEFT:".length()) + " left.\n");
            }
            else if (message.startsWith("DISCONNECTED:")) {
                chatArea.appendText("Disconnected: " + message.substring("DISCONNECTED:".length()) + "\n");
                client = null;
            }
            // Username feedback
            else if (message.startsWith("USERNAME_TAKEN:")) {
                String name = message.substring("USERNAME_TAKEN:".length());
                chatArea.appendText("Username '" + name + "' is taken.\n");
                usernameField.setText(name + System.currentTimeMillis() % 1000);
            }
            else if (message.startsWith("USERNAME_ACCEPTED:")) {
                username = message.substring("USERNAME_ACCEPTED:".length());
                chatArea.appendText("Username set to " + username + "\n");
            }
        });
    }
}