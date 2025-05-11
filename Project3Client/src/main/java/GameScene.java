// https://docs.oracle.com/javase/8/javafx/api/javafx/animation/Animation.html
// https://www.tutorialspoint.com/javafx/javafx_images.htm
// https://motleybytes.com/w/JavaFxFonts

import java.util.Objects;
import java.util.function.Consumer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

// Manages the UI of the game , the single player against AI and
// the multiplayer functionality with another user
public class GameScene {
    private final Connect4 game;
    public final Client client;
    private final Stage stage;
    private final Scene menuScene;

    //Game state
    private boolean myTurn;
    private boolean gameStarted = false;
    private boolean gameEnded = false;

    //UI components
    private Label statusLabel;
    private Label playsLabel;
    private Button playAgainBtn;
    public Button returnButton;
    private Button centerMenuBtn;
    private Pane animationLayer;
    private final int CELL_SIZE = 65;

    // Chat components
    private TextArea chatArea;
    private TextField messageField;
    private VBox chatBox;
    private boolean chatVisible = true;

    //board components
    private final StackPane[][] cells = new StackPane[Connect4.numRows][Connect4.numCols];
    private final Pane[][] clickDetectors = new Pane[Connect4.numCols][1];

    // Background animation
    private final Image[] bgFrames = new Image[9];
    private int currentBg = 0;

    // game images
    private Image boardImg;
    private Image redImg;
    private Image yellowImg;
    private Image buttonBg;
    private Background buttonBackground;

    private final Scene scene;

    // Constructor
    public GameScene(Connect4 game, Client client, Stage stage, Scene menuScene) {
        this.game = game;
        this.client = client;
        this.stage = stage;
        this.menuScene = menuScene;
        this.myTurn = (client == null);

        loadImages();
        initButtonBackground();

        if (client != null) {
            client.setCallback(this::onReceive);
        }

        scene = buildScene();
        updateStatus();
        updatePlays();
    }

    public Scene getScene() {
        return scene;
    }

    // loads the images of background, board, red coin, yellow coin, and the button
    private void loadImages() {
        for (int i = 0; i < bgFrames.length; i++) {
            String name = String.format("bg%d.png", i + 1);
            bgFrames[i] = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream(name)
            ));
        }
        boardImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("board.png")));
        redImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("red_coin.png")));
        yellowImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("yellow_coin.png")));
        buttonBg = new Image(
                Objects.requireNonNull(getClass().getResourceAsStream("Button.png")),
                150,
                40,
                false,
                true
        );
    }

    // creates the button with the image background
    private void initButtonBackground() {
        double w = buttonBg.getWidth();
        double h = buttonBg.getHeight();
        BackgroundSize size = new BackgroundSize(w, h, false, false, false, false);
        BackgroundImage bi = new BackgroundImage(
                buttonBg,
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                size
        );
        buttonBackground = new Background(bi);
    }

    // build the complete scene of the game
    private Scene buildScene() {
        BorderPane root = new BorderPane();
        setBackground(root, bgFrames[0]);
        startBackgroundAnimation(root);

        // Main Menu button
        returnButton = new Button("Main Menu");
        returnButton.setFont(Font.loadFont(getClass().getResourceAsStream("ka1.ttf"), 12));
        returnButton.setTextFill(Color.WHITE);
        returnButton.setBackground(buttonBackground);
        returnButton.setPrefSize(buttonBg.getWidth(), buttonBg.getHeight());
        returnButton.setOnAction(e -> {
            if (client != null) {
                client.disconnect();
            }
            stage.setScene(menuScene);
        });

        // chat toggle button
        Button toggleChatBtn = new Button("Toggle Chat");
        toggleChatBtn.setFont(Font.loadFont(getClass().getResourceAsStream("ka1.ttf"), 12));
        toggleChatBtn.setTextFill(Color.WHITE);
        toggleChatBtn.setBackground(buttonBackground);
        toggleChatBtn.setPrefSize(buttonBg.getWidth(), buttonBg.getHeight());
        toggleChatBtn.setOnAction(e -> toggleChat());
        toggleChatBtn.setVisible(client != null); // Only show toggle button in multiplayer mode

        VBox topButtons = new VBox(10, returnButton, toggleChatBtn);
        topButtons.setAlignment(Pos.TOP_LEFT);
        topButtons.setPadding(new Insets(20, 0, 0, 20));

        // shows the number of moves played by the user on top right
        playsLabel = new Label("Plays: 0");
        playsLabel.setFont(Font.loadFont(getClass().getResourceAsStream("ka1.ttf"), 18));
        playsLabel.setTextFill(Color.LIGHTGRAY);
        DropShadow outline = new DropShadow(0, 0, 0, Color.BLACK);
        outline.setRadius(3);
        playsLabel.setEffect(outline);

        HBox playsBox = new HBox(playsLabel);
        playsBox.setAlignment(Pos.TOP_RIGHT);
        playsBox.setPadding(new Insets(20, 20, 0, 0));

        BorderPane topContainer = new BorderPane();
        topContainer.setLeft(topButtons);
        topContainer.setRight(playsBox);
        root.setTop(topContainer);

        // Initialize chat components
        initChatComponents();

        playAgainBtn = new Button("Play Again");
        playAgainBtn.setFont(Font.loadFont(getClass().getResourceAsStream("ka1.ttf"), 12));
        playAgainBtn.setTextFill(Color.WHITE);
        playAgainBtn.setBackground(buttonBackground);
        playAgainBtn.setPrefSize(buttonBg.getWidth(), buttonBg.getHeight());
        playAgainBtn.setVisible(false);
        playAgainBtn.setManaged(false);
        playAgainBtn.setOnAction(e -> {
            game.reset();
            gameStarted = (client == null);
            gameEnded = false;
            myTurn = (client == null);
            redraw();
            updateStatus();
            updatePlays();
            enableBoard();

            returnButton.setVisible(true);
            returnButton.setManaged(true);
            centerMenuBtn.setVisible(false);
            centerMenuBtn.setManaged(false);
            playAgainBtn.setVisible(false);
            playAgainBtn.setManaged(false);

            if (client != null) {
                client.send("RESET");
            }
        });

        // creates a main menu button after the game ends
        centerMenuBtn = new Button("Main Menu");
        centerMenuBtn.setFont(Font.loadFont(getClass().getResourceAsStream("ka1.ttf"), 12));
        centerMenuBtn.setTextFill(Color.WHITE);
        centerMenuBtn.setBackground(buttonBackground);
        centerMenuBtn.setPrefSize(buttonBg.getWidth(), buttonBg.getHeight());
        centerMenuBtn.setVisible(false);
        centerMenuBtn.setManaged(false);
        centerMenuBtn.setOnAction(e -> {
            if (client != null) {
                client.disconnect();
            }
            stage.setScene(menuScene);
        });

        // label for the game messages
        statusLabel = new Label();
        statusLabel.setFont(Font.loadFont(getClass().getResourceAsStream("ka1.ttf"), 24));
        statusLabel.setTextFill(Color.web("#4B0082"));
        StackPane statusBox = new StackPane(statusLabel);
        statusBox.setAlignment(Pos.CENTER);
        statusBox.setTranslateY(100);
        statusBox.setPadding(new Insets(2, 0, 2, 0));

        //Create game board
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(8);
        grid.setVgap(-5);
        grid.setTranslateY(-8);
        for (int r = 0; r < Connect4.numRows; r++) {
            for (int c = 0; c < Connect4.numCols; c++) {
                StackPane cell = new StackPane();
                cell.setPrefSize(CELL_SIZE, CELL_SIZE);
                final int col = c;
                cell.setOnMouseClicked(e -> handleLocalMove(col));
                cells[r][c] = cell;
                grid.add(cell, c, r);
            }
        }

        // click feature to drop the coin
        GridPane detectors = new GridPane();
        detectors.setAlignment(Pos.TOP_CENTER);
        detectors.setTranslateY(-18);
        detectors.setHgap(8);
        for (int c = 0; c < Connect4.numCols; c++) {
            Pane p = new Pane();
            p.setPrefSize(CELL_SIZE, CELL_SIZE * Connect4.numRows);
            p.setStyle("-fx-cursor: hand;");

            final int col = c;
            p.setOnMouseClicked(e -> handleLocalMove(col));
            detectors.add(p, c, 0);
            clickDetectors[c][0] = p;
        }

        // game board overlay
        ImageView boardView = new ImageView(boardImg);
        boardView.setFitWidth(CELL_SIZE * Connect4.numCols * 1.7);
        boardView.setFitHeight(CELL_SIZE * Connect4.numRows * 1.7);
        boardView.setPreserveRatio(false);
        boardView.setTranslateX(9);

        // creates an animation of the coin dropping
        animationLayer = new Pane();
        animationLayer.setPickOnBounds(false);

        StackPane boardStack = new StackPane(grid, boardView, detectors, animationLayer);

        VBox centerContainer = new VBox(8, statusBox, playAgainBtn, centerMenuBtn, boardStack);
        centerContainer.setAlignment(Pos.CENTER);
        root.setCenter(centerContainer);

        // Add chat to the right side
        if (client != null) {
            root.setRight(chatBox);
        }

        Scene s = new Scene(root, 1050, 1000);

        s.widthProperty().addListener((obs, oldVal, newVal) -> updateBackground(root));
        s.heightProperty().addListener((obs, oldVal, newVal) -> updateBackground(root));

        redraw();
        return s;
    }

    private void updateBackground(Region pane) {
        setBackground(pane, bgFrames[currentBg]);
    }

    private void initChatComponents() {
        // Create chat components
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setPrefHeight(400);
        chatArea.setPrefWidth(250);
        chatArea.setWrapText(true);
        chatArea.setStyle("-fx-font-size: 12px;");

        messageField = new TextField();
        messageField.setPrefWidth(180);
        messageField.setPromptText("Type a message...");
        messageField.setOnAction(e -> sendChatMessage());

        Button sendBtn = new Button("Send");
        sendBtn.setBackground(buttonBackground);
        sendBtn.setTextFill(Color.WHITE);
        sendBtn.setFont(Font.loadFont(getClass().getResourceAsStream("ka1.ttf"), 12));
        sendBtn.setOnAction(e -> sendChatMessage());

        HBox messageBox = new HBox(5, messageField, sendBtn);
        messageBox.setAlignment(Pos.CENTER);

        Label chatLabel = new Label("Private Chat");
        chatLabel.setFont(Font.loadFont(getClass().getResourceAsStream("ka1.ttf"), 16));
        chatLabel.setTextFill(Color.WHITE);

        // Create a semi-transparent background for chat
        chatBox = new VBox(10, chatLabel, chatArea, messageBox);
        chatBox.setPadding(new Insets(15));
        chatBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6); -fx-background-radius: 10;");
        chatBox.setPrefWidth(250);
        chatBox.setAlignment(Pos.TOP_CENTER);
        chatBox.setPadding(new Insets(20, 10, 20, 10));

        // Only show chat in multiplayer mode
        chatBox.setVisible(client != null);
        chatBox.setManaged(client != null);

        // Add initial message
        if (client != null) {
            chatArea.appendText("Private chat with opponent.\n");
        }
    }

    private void toggleChat() {
        chatVisible = !chatVisible;
        chatBox.setVisible(chatVisible);
        chatBox.setManaged(chatVisible);
    }

    private void sendChatMessage() {
        if (client == null || messageField.getText().trim().isEmpty()) {
            return;
        }

        String message = messageField.getText().trim();
        client.send("PRIVATE_CHAT:" + message);
        chatArea.appendText("You: " + message + "\n");
        messageField.clear();
    }

    private void animateDrop(int col, char player, Consumer<Integer> onFinished) {
        Image img = player == 'X' ? redImg : yellowImg;
        ImageView coin = new ImageView(img);
        coin.setFitWidth(CELL_SIZE);
        coin.setFitHeight(CELL_SIZE);

        double totalWidth = Connect4.numCols * CELL_SIZE + (Connect4.numCols - 1) * 8;
        double startX = (animationLayer.getWidth() - totalWidth) / 2 + col * (CELL_SIZE + 8);
        coin.setLayoutX(startX);
        coin.setLayoutY(-CELL_SIZE);
        animationLayer.getChildren().add(coin);

        int targetRow = game.getNextOpenRow(col);

        double totalHeight = Connect4.numRows * CELL_SIZE + (Connect4.numRows - 1) * -5;
        double finalY = (animationLayer.getHeight() - totalHeight) / 2 + targetRow * (CELL_SIZE - 5);

        TranslateTransition tt = new TranslateTransition(Duration.seconds(0.4), coin);
        tt.setToY(finalY + CELL_SIZE);
        tt.setOnFinished(e -> {
            animationLayer.getChildren().remove(coin);
            onFinished.accept(targetRow);
        });
        tt.play();
    }

    private void handleLocalMove(int col) {
        if (!myTurn || gameEnded || (client != null && !gameStarted) || game.isColumnFull(col)) {
            return;
        }
        disableBoard();
        animateDrop(col, Connect4.currPlayer, row -> {
        game.drop(Connect4.currPlayer, col);
        redraw();
        updatePlays();

        if (client != null) {
            client.send("MOVE:" + col);
        }
        else {
            handleAIMove();
        }

        if (Connect4.checkForWin()) {
            gameEnded = true;
            statusLabel.setText("You win!");
            showNewGame();
        }
        else if (game.isBoardFull()) {
            gameEnded = true;
            statusLabel.setText("Draw!");
            showNewGame();
        }
        else {
            game.switchPlayer();
            myTurn = false;
            updateStatus();
        }

        if (!gameEnded) {
            enableBoard();
        }
        });
    }

    private void handleAIMove() {
        game.switchPlayer();
        myTurn = false;
        updateStatus();

        int col = game.AI_chooseCol();
        disableBoard();
        animateDrop(col, Connect4.currPlayer, row -> {
            game.drop(Connect4.currPlayer, col);
            redraw();
            updatePlays();

            if (Connect4.checkForWin()) {
                gameEnded = true;
                statusLabel.setText("AI wins!");
                showNewGame();
            } else {
                game.switchPlayer();
                myTurn = true;
                updateStatus();
                enableBoard();
            }
        });
    }

    private void onOpponentLeave() {
        gameEnded = true;
        statusLabel.setText("Opponent left.");
        disableBoard();
        returnButton.setVisible(false);
        returnButton.setManaged(false);
        centerMenuBtn.setVisible(true);
        centerMenuBtn.setManaged(true);
    }

    public void onReceive(String msg) {
        Platform.runLater(() -> {
            if (msg.equals("DISCONNECT:server")) {
                gameEnded = true;
                statusLabel.setText("Server closed.");
                disableBoard();
                return;
            }
            if (msg.equals("DISCONNECT:opponent")) {
                onOpponentLeave();
                return;
            }
            if (msg.startsWith("TURN:")) {
                gameStarted = true;
                myTurn = Boolean.parseBoolean(msg.substring(5));
                updateStatus();
            } else if (msg.startsWith("MOVE:")) {
                int c = Integer.parseInt(msg.substring(5));
                disableBoard();
                animateDrop(c, Connect4.currPlayer, row -> {
                    game.drop(Connect4.currPlayer, c);
                    redraw();
                    updatePlays();
                    if (Connect4.checkForWin()) {
                        gameEnded = true;
                        statusLabel.setText("You lose!");
                        showNewGame();
                    } else {
                        game.switchPlayer();
                        myTurn = true;
                        updateStatus();
                        enableBoard();
                    }
                });
            } else if (msg.startsWith("PRIVATE_CHAT:")) {
                // Handle private chat messages from opponent
                chatArea.appendText("Opponent: " + msg.substring(13) + "\n");
            }
        });
    }

    private void showNewGame() {
        playAgainBtn.setVisible(true);
        playAgainBtn.setManaged(true);
    }

    private void disableBoard() {
        for (Pane[] col : clickDetectors) {
            for (Pane p : col) {
                if (p != null) {
                    p.setDisable(true);
                }
            }
        }
    }

    private void enableBoard() {
        for (Pane[] col : clickDetectors) {
            for (Pane p : col) {
                if (p != null) {
                    p.setDisable(false);
                }
            }
        }
    }

    private void redraw() {
        for (int r = 0; r < Connect4.numRows; r++) {
            for (int c = 0; c < Connect4.numCols; c++) {
                cells[r][c].getChildren().clear();
                char m = Connect4.board.get(r).get(c);
                if (m == 'X' || m == 'O') {
                    ImageView iv = new ImageView(m == 'X' ? redImg : yellowImg);
                    iv.setFitWidth(CELL_SIZE);
                    iv.setFitHeight(CELL_SIZE);
                    cells[r][c].getChildren().add(iv);
                }
            }
        }
    }

    private void updateStatus() {
        if (!gameStarted && client != null) {
            statusLabel.setText("Waiting for opponent to join...");
        } else if (!gameEnded) {
            statusLabel.setText(
                    myTurn ? "Your turn! Drop a piece."
                            : (client == null ? "AI's turn..." : "Opponent's turn...")
            );
        }
    }

    private void updatePlays() {
        playsLabel.setText("Plays: " + Connect4.numPlays);
    }

    private void setBackground(Region pane, Image img) {

        BackgroundSize bs = new BackgroundSize(
                BackgroundSize.AUTO,
                BackgroundSize.AUTO,
                false,
                false,
                true,
                true
        );

        BackgroundImage bgImage = new BackgroundImage(
                img,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                bs
        );

        pane.setBackground(new Background(bgImage));
    }

    private void startBackgroundAnimation(Region pane) {
        Timeline timeline = new Timeline(new KeyFrame(
                Duration.seconds(1),
                e -> {
                    currentBg = (currentBg + 1) % bgFrames.length;
                    setBackground(pane, bgFrames[currentBg]);
                }
        ));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }
}