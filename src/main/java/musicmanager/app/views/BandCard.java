package musicmanager.app.views;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import musicmanager.app.models.MusicBand;
import musicmanager.app.models.Tour;

import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class BandCard implements Initializable {
    @FXML private AnchorPane rootPane;
    @FXML private Label initialsLabel;
    @FXML private Label nameLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label tourStatusLabel;
    @FXML private Label membersLabel;
    @FXML private Label averageRevenueLabel;
    @FXML private Label nextTourLabel;

    private MusicBand band;
    private Consumer<MusicBand> editAction;
    private Consumer<MusicBand> deleteAction;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        rootPane.setOnMouseClicked(event -> {
            if (!(event.getTarget() instanceof Button)) openBandInfo();
        });
    }

    public void setBand(MusicBand band) {
        this.band = band;
        initialsLabel.setText(initials(band.getName()));
        nameLabel.setText(band.getName());
        subtitleLabel.setText("Основана: " + band.getYear() + " г. · Позиция в чарте: #" + band.getTopPosition());
        membersLabel.setText(band.getMusicians().size() + " участников");

        if (band.getTours().isEmpty()) {
            tourStatusLabel.setText("● Нет туров");
            tourStatusLabel.setStyle("-fx-background-color: #262b3a; -fx-background-radius: 15; -fx-padding: 4 10 4 10;");
            tourStatusLabel.setTextFill(javafx.scene.paint.Color.web("#aab1c5"));
            averageRevenueLabel.setText("—");
            nextTourLabel.setText("Туры не запланированы");
        } else {
            long average = Math.round(band.getTours().stream().mapToLong(Tour::getRevenue).average().orElse(0));
            averageRevenueLabel.setText(NumberFormat.getIntegerInstance(Locale.forLanguageTag("ru-RU")).format(average) + " ₽");
            Tour tour = band.getTours().get(0);
            tourStatusLabel.setText("● " + tour.getStatus());
            nextTourLabel.setText(tour.getCities().isEmpty()
                    ? "Ближайший тур: " + tour.getStatus()
                    : "📅 " + tour.getStatus() + " • " + tour.getCities().get(0).getName());
        }
    }

    public void setActions(Consumer<MusicBand> editAction, Consumer<MusicBand> deleteAction) {
        this.editAction = editAction;
        this.deleteAction = deleteAction;
    }

    @FXML
    private void handleEditBand(ActionEvent event) {
        event.consume();
        if (band != null && editAction != null) editAction.accept(band);
    }

    @FXML
    private void handleDeleteBand(ActionEvent event) {
        event.consume();
        if (band != null && deleteAction != null) deleteAction.accept(band);
    }

    private String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] words = name.trim().split("\\s+");
        return words.length == 1 ? words[0].substring(0, 1).toUpperCase(Locale.ROOT)
                : (words[0].substring(0, 1) + words[1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private void openBandInfo() {
        if (band == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("band_info.fxml"));
            AnchorPane pane = loader.load();
            BandInfo controller = loader.getController();
            controller.setBand(band);
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
            showError("Не удалось открыть информацию о группе", e);
        }
    }

    private void showError(String header, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка навигации");
        alert.setHeaderText(header);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }
}
