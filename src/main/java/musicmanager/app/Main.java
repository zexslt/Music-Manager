package musicmanager.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            URL fxmlUrl = getClass().getResource("/musicmanager/app/views/main_window.fxml");

            if (fxmlUrl == null) {
                throw new RuntimeException("Не найден FXML файл главного окна!");
            }

            Parent root = FXMLLoader.load(fxmlUrl);
            Scene scene = new Scene(root, 1024, 640);

            // Подключаем стили программно
            URL cssUrl = getClass().getResource("/musicmanager/app/styles/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.out.println("Внимание: Не удалось найти файл стилей style.css!");
            }

            primaryStage.setTitle("Music Band Manager");
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}