package musicmanager.app.views;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import musicmanager.app.models.Tour;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.stream.Collectors;

public class TourInfo {
    @FXML private Label tourNameLabel;
    @FXML private Label bandNameLabel;
    @FXML private Label statusLabel;
    @FXML private Label citiesLabel;
    @FXML private Label revenueLabel;
    @FXML private Label citiesDetailLabel;

    public void setTour(Tour tour) {
        tourNameLabel.setText(tour.getStatus());
        bandNameLabel.setText("Группа: " + tour.getMusicBand().getName());
        statusLabel.setText("● " + tour.getStatus());
        String cityNames = tour.getCities().isEmpty() ? "Города не указаны" :
                tour.getCities().stream().map(city -> city.getName()).collect(Collectors.joining(", "));
        citiesLabel.setText(tour.getCities().size() + (tour.getCities().size() == 1 ? " город" : " городов"));
        citiesDetailLabel.setText(cityNames);
        revenueLabel.setText(NumberFormat.getIntegerInstance(Locale.forLanguageTag("ru-RU")).format(tour.getRevenue()) + " ₽");
    }

    @FXML
    private void handleBack(javafx.event.ActionEvent event) {
        try {
            AnchorPane pane = FXMLLoader.load(getClass().getResource("tours.fxml"));
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
            alert.setHeaderText("Не удалось вернуться к списку туров");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
