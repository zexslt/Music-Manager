package musicmanager.app.views;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;
import java.net.URL;

final class ViewNavigator {
    private ViewNavigator() { }

    static void show(Node source, String fxmlName) throws IOException {
        URL resource = ViewNavigator.class.getResource(fxmlName);
        if (resource == null) throw new IOException("Не найден экран: " + fxmlName);
        AnchorPane pane = new FXMLLoader(resource).load();
        AnchorPane contentArea = (AnchorPane) source.getScene().lookup("#contentArea");
        if (contentArea == null) throw new IOException("Не найдена область отображения экранов.");
        contentArea.getChildren().setAll(pane);
        AnchorPane.setTopAnchor(pane, 0.0);
        AnchorPane.setBottomAnchor(pane, 0.0);
        AnchorPane.setLeftAnchor(pane, 0.0);
        AnchorPane.setRightAnchor(pane, 0.0);
    }
}
