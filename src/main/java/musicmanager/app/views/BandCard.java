package musicmanager.app.views;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class BandCard implements Initializable {

    @FXML
    private AnchorPane rootPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (rootPane != null) {
            rootPane.setOnMouseClicked(event -> {
                openBandInfo(event.getSource());
            });
        }
    }

    private void openBandInfo(Object sourceNode) {
        try {
            URL fxmlUrl = getClass().getResource("band_info.fxml");
            if (fxmlUrl == null) {
                throw new RuntimeException("Не удалось найти файл BandInfo.fxml!");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            AnchorPane pane = loader.load();

            Node source = (Node) sourceNode;
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
            alert.setHeaderText("Не удалось открыть информацию о группе");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}