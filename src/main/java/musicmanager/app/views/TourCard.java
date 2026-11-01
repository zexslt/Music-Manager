package musicmanager.app.views;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class TourCard implements Initializable {

    @FXML
    private VBox rootPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (rootPane != null) {
            rootPane.setOnMouseClicked(this::openTourInfo);
        }
    }

    private void openTourInfo(MouseEvent event) {
        try {
            URL fxmlUrl = getClass().getResource("tour_info.fxml");
            if (fxmlUrl == null) {
                System.err.println("Не удалось найти файл tour_info.fxml!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            AnchorPane pane = loader.load();

            Node source = (Node) event.getSource();
            AnchorPane contentArea = (AnchorPane) source.getScene().lookup("#contentArea");

            if (contentArea != null) {
                contentArea.getChildren().setAll(pane);
                AnchorPane.setTopAnchor(pane, 0.0);
                AnchorPane.setBottomAnchor(pane, 0.0);
                AnchorPane.setLeftAnchor(pane, 0.0);
                AnchorPane.setRightAnchor(pane, 0.0);
            }
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка навигации");
            alert.setHeaderText("Не удалось открыть информацию о туре");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleEditTour(javafx.event.ActionEvent event) {
        event.consume();
        System.out.println("Редактировать тур");
    }

    @FXML
    private void handleDeleteTour(javafx.event.ActionEvent event) {
        event.consume();
        System.out.println("Удалить тур");
    }
}