package musicmanager.app.views;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class TourInfo implements Initializable {

    @FXML private Label tourNameLabel;
    @FXML private Label bandNameLabel;
    @FXML private Label statusLabel;
    @FXML private Label citiesLabel;
    @FXML private Label revenueLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    @FXML
    private void handleBack(javafx.event.ActionEvent event) {
        try {
            URL fxmlUrl = getClass().getResource("tours.fxml");
            if (fxmlUrl != null) {
                AnchorPane pane = FXMLLoader.load(fxmlUrl);
                Node source = (Node) event.getSource();
                AnchorPane contentArea = (AnchorPane) source.getScene().lookup("#contentArea");

                if (contentArea != null) {
                    contentArea.getChildren().setAll(pane);
                    AnchorPane.setTopAnchor(pane, 0.0);
                    AnchorPane.setBottomAnchor(pane, 0.0);
                    AnchorPane.setLeftAnchor(pane, 0.0);
                    AnchorPane.setRightAnchor(pane, 0.0);
                }
            }
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка навигации");
            alert.setHeaderText("Не удалось вернуться к списку туров");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}