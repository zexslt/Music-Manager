package musicmanager.app.views;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import musicmanager.app.models.Tour;

import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class TourCard implements Initializable {
    @FXML private VBox rootPane;
    @FXML private Label bandNameLabel;
    @FXML private Label tourNameLabel;
    @FXML private Label statusLabel;
    @FXML private Label cityCountLabel;
    @FXML private Label revenueLabel;

    private Tour tour;
    private Consumer<Tour> editAction;
    private Consumer<Tour> deleteAction;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        rootPane.setOnMouseClicked(this::openTourInfo);
    }

    public void setTour(Tour tour) {
        this.tour = tour;
        bandNameLabel.setText(tour.getMusicBand().getName());
        tourNameLabel.setText(tour.getStatus());
        statusLabel.setText("● " + tour.getStatus());
        int cityCount = tour.getCities().size();
        int lastTwoDigits = cityCount % 100;
        String cityWord = lastTwoDigits >= 11 && lastTwoDigits <= 14 ? "городов"
                : switch (cityCount % 10) {
                    case 1 -> "город";
                    case 2, 3, 4 -> "города";
                    default -> "городов";
                };
        cityCountLabel.setText(cityCount + " " + cityWord);
        revenueLabel.setText(NumberFormat.getIntegerInstance(Locale.forLanguageTag("ru-RU")).format(tour.getRevenue()) + " ₽");
    }

    public void setActions(Consumer<Tour> editAction, Consumer<Tour> deleteAction) {
        this.editAction = editAction;
        this.deleteAction = deleteAction;
    }

    private void openTourInfo(MouseEvent event) {
        if (tour == null || event.getTarget() instanceof javafx.scene.control.Button) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("tour_info.fxml"));
            AnchorPane pane = loader.load();
            ((TourInfo) loader.getController()).setTour(tour);
            Node source = rootPane;
            AnchorPane contentArea = (AnchorPane) source.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(pane);
                AnchorPane.setTopAnchor(pane, 0.0);
                AnchorPane.setBottomAnchor(pane, 0.0);
                AnchorPane.setLeftAnchor(pane, 0.0);
                AnchorPane.setRightAnchor(pane, 0.0);
            }
        } catch (IOException e) {
            showError("Не удалось открыть информацию о туре", e);
        }
    }

    @FXML
    private void handleEditTour(javafx.event.ActionEvent event) {
        event.consume();
        if (tour != null && editAction != null) editAction.accept(tour);
    }

    @FXML
    private void handleDeleteTour(javafx.event.ActionEvent event) {
        event.consume();
        if (tour != null && deleteAction != null) deleteAction.accept(tour);
    }

    private void showError(String header, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка навигации");
        alert.setHeaderText(header);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }
}
