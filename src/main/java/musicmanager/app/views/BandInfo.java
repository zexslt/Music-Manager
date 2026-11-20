package musicmanager.app.views;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import musicmanager.app.models.MusicBand;
import musicmanager.app.models.Musician;
import musicmanager.app.models.Track;
import musicmanager.app.models.Tour;
import musicmanager.app.logging.AuditLog;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

public class BandInfo {
    @FXML private Label nameLabel;
    @FXML private Label chartLabel;
    @FXML private Label summaryLabel;
    @FXML private Label membersHeader;
    @FXML private Label toursHeader;
    @FXML private Label tracksHeader;
    @FXML private VBox musiciansContainer;
    @FXML private VBox toursContainer;
    @FXML private VBox tracksContainer;
    private MusicBand band;

    public void setBand(MusicBand band) {
        this.band = band;
        musiciansContainer.getChildren().clear();
        toursContainer.getChildren().clear();
        tracksContainer.getChildren().clear();
        nameLabel.setText(band.getName());
        chartLabel.setText("🏆 Позиция в чарте: #" + band.getTopPosition());
        summaryLabel.setText("Основана: " + band.getYear() + " г.");
        membersHeader.setText("Музыканты (" + band.getMusicians().size() + ")");
        toursHeader.setText("Туры (" + band.getTours().size() + ")");
        tracksHeader.setText("Треки (" + band.getTracks().size() + ")");

        for (Musician musician : band.getMusicians()) {
            VBox item = detailCard();
            item.getChildren().addAll(
                    detailLabel(musician.getName() + " " + musician.getSurname(), "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;"),
                    detailLabel("Возраст: " + musician.getAge(), "-fx-text-fill: #8a99ad; -fx-font-size: 12px;"),
                    detailLabel("Роль: " + (musician.getRole() == null ? "не указана" : musician.getRole()), "-fx-text-fill: #00e5ff; -fx-font-size: 12px;"));
            musiciansContainer.getChildren().add(item);
        }
        if (band.getMusicians().isEmpty()) musiciansContainer.getChildren().add(emptyMessage("Для этой группы музыканты не указаны."));

        for (Tour tour : band.getTours()) {
            String cities = tour.getCities().isEmpty() ? "Города не указаны" :
                    tour.getCities().stream().map(city -> city.getName()).reduce((a, b) -> a + ", " + b).orElse("");
            VBox item = detailCard();
            item.getChildren().addAll(
                    detailLabel(tour.getStatus(), "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;"),
                    detailLabel("Города: " + cities, "-fx-text-fill: #8a99ad; -fx-font-size: 12px;"),
                    detailLabel("Выручка: " + NumberFormat.getIntegerInstance(Locale.forLanguageTag("ru-RU")).format(tour.getRevenue()) + " ₽", "-fx-text-fill: #00e5ff; -fx-font-size: 12px;"));
            toursContainer.getChildren().add(item);
        }
        if (band.getTours().isEmpty()) toursContainer.getChildren().add(emptyMessage("Для этой группы туры не указаны."));

        for (Track track : band.getTracks()) {
            Label item = detailLabel(track.getName() + " · " + track.getGenre() + " · " + track.getDuration(),
                    "-fx-text-fill: white; -fx-font-size: 13px; -fx-background-color: #161b2e; -fx-background-radius: 6; -fx-padding: 10 15 10 15;");
            item.setWrapText(true);
            tracksContainer.getChildren().add(item);
        }
        if (band.getTracks().isEmpty()) tracksContainer.getChildren().add(emptyMessage("Для этой группы треки не указаны."));
    }

    @FXML
    private void handleEditBand() {
        if (band == null) return;
        AuditLog.event("Открытие формы редактирования группы из карточки", java.util.Map.of("bandId", band.getId(), "name", band.getName()));
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("band_form.fxml"));
            AnchorPane pane = loader.load();
            ((BandForm) loader.getController()).setBand(band);
            AnchorPane contentArea = (AnchorPane) nameLabel.getScene().lookup("#contentArea");
            if (contentArea == null) throw new IOException("Не найдена область отображения экранов.");
            contentArea.getChildren().setAll(pane);
            AnchorPane.setTopAnchor(pane, 0.0);
            AnchorPane.setBottomAnchor(pane, 0.0);
            AnchorPane.setLeftAnchor(pane, 0.0);
            AnchorPane.setRightAnchor(pane, 0.0);
        } catch (IOException e) {
            AuditLog.failure("Ошибка открытия формы редактирования группы", java.util.Map.of("bandId", band.getId()), e);
            showError("Не удалось открыть форму группы", e);
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            AnchorPane pane = FXMLLoader.load(getClass().getResource("bands.fxml"));
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
            showError("Не удалось вернуться к списку групп", e);
        }
    }

    private VBox detailCard() {
        VBox box = new VBox(4);
        box.setStyle("-fx-background-color: #161b2e; -fx-background-radius: 8; -fx-border-color: #232a42; -fx-border-radius: 8; -fx-padding: 15;");
        return box;
    }

    private Label detailLabel(String text, String style) {
        Label label = new Label(text);
        label.setStyle(style);
        return label;
    }

    private Label emptyMessage(String text) {
        return detailLabel(text, "-fx-text-fill: #8a99ad; -fx-font-size: 12px;");
    }

    private void showError(String header, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка навигации");
        alert.setHeaderText(header);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }
}
