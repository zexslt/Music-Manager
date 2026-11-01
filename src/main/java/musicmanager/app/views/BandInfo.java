package musicmanager.app.views;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import java.io.IOException;

public class BandInfo {

    @FXML
    private void handleBack(ActionEvent event) {
        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("bands.fxml"));
            AnchorPane pane = loader.load();

            Node source = (Node) event.getSource();
            AnchorPane contentArea = (AnchorPane) source.getScene().lookup("#contentArea");

            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(pane);

                AnchorPane.setTopAnchor(pane, 0.0);
                AnchorPane.setBottomAnchor(pane, 0.0);
                AnchorPane.setLeftAnchor(pane, 0.0);
                AnchorPane.setRightAnchor(pane, 0.0);
            }
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка навигации");
            alert.setHeaderText("Не удалось вернуться к списку групп");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}