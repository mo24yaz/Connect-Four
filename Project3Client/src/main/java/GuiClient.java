import javafx.application.Application;
import javafx.stage.Stage;

public class GuiClient extends Application {
	@Override
	public void start(Stage primaryStage) {
		MainMenu menu = new MainMenu(primaryStage);
		primaryStage.setScene(menu.getScene());
		primaryStage.setTitle("Connect-4");
		primaryStage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}
}