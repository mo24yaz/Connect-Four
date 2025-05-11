import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.util.HashMap;

public class GuiServer extends Application {
	private Server serverConnection;
	private Label lblSessions, lblClients, lblWaiting;
	private ListView<String> connList, gameList;

    public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		// Stats labels
		lblSessions = new Label("Games: 0");
		lblClients  = new Label("Clients: 0");
		lblWaiting  = new Label("Waiting: 0");

		// Connection vs. Game event logs
		connList = new ListView<>();
		gameList = new ListView<>();
		connList.setPrefHeight(300);
		gameList.setPrefHeight(300);

		// Wire up server callbacks
		serverConnection = new Server(data -> {
			Platform.runLater(() -> {
				String s = data.toString();
				if (s.startsWith("CONN:")) {
					connList.getItems().add(s.substring(5));
				} else if (s.startsWith("GAME:")) {
					gameList.getItems().add(s.substring(5));
				}
				updateStats();
			});
		});

        HashMap<String, Scene> sceneMap = new HashMap<>();
		sceneMap.put("server", createServerGui());

		primaryStage.setScene(sceneMap.get("server"));
		primaryStage.setTitle("Connect4 Server Monitor");
		primaryStage.setOnCloseRequest(e -> {
			Platform.exit();
			System.exit(0);
		});
		primaryStage.show();
	}

	private void updateStats() {
		lblSessions.setText("Games: " + serverConnection.getSessionCount());
		lblClients.setText("Clients: " + serverConnection.getCurrentClients());
		lblWaiting.setText("Waiting: " + serverConnection.getWaitingCount());
	}

	public Scene createServerGui() {
		VBox root = new VBox(10);
		root.setPadding(new Insets(20));

		HBox statsBox = new HBox(20, lblSessions, lblClients, lblWaiting);
		VBox connBox = new VBox(5, new Label("Connections"), connList);
		VBox gameBox = new VBox(5, new Label("Game Events"), gameList);
		HBox logsBox = new HBox(10, connBox, gameBox);
		logsBox.setPadding(new Insets(10));

		root.getChildren().addAll(statsBox, logsBox);
		return new Scene(root, 550, 400);
	}
}