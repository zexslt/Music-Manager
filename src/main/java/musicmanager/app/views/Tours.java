package musicmanager.app.views;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import musicmanager.app.database.JpaSession;
import musicmanager.app.models.MusicBand;
import musicmanager.app.models.Tour;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class Tours implements Initializable {
    @FXML private VBox cardsContainer;
    @FXML private TextField searchField;
    @FXML private Label emptyLabel;

    private final List<Tour> tours = new ArrayList<>();

    @FXML
    private void handleAddTour() {
        showTourDialog(null);
    }

    private void editTour(Tour tour) {
        showTourDialog(tour);
    }

    private void showTourDialog(Tour tour) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("tour_form.fxml"));
            AnchorPane pane = loader.load();
            ((TourForm) loader.getController()).setTour(tour);
            AnchorPane contentArea = (AnchorPane) cardsContainer.getScene().lookup("#contentArea");
            if (contentArea == null) throw new IOException("Не найдена область отображения экранов.");
            contentArea.getChildren().setAll(pane);
            AnchorPane.setTopAnchor(pane, 0.0);
            AnchorPane.setBottomAnchor(pane, 0.0);
            AnchorPane.setLeftAnchor(pane, 0.0);
            AnchorPane.setRightAnchor(pane, 0.0);
        } catch (Exception e) {
            showDatabaseError("Не удалось открыть форму тура", e);
        }
    }

    private void deleteTour(Tour tour) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(cardsContainer.getScene().getWindow());
        confirm.setTitle("Удаление тура");
        confirm.setHeaderText("Удалить тур «" + tour.getStatus() + "»?");
        confirm.setContentText("Запись тура и связи с городами будут удалены.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        EntityManager manager = null;
        try {
            manager = JpaSession.createEntityManager();
            manager.getTransaction().begin();
            Tour managedTour = manager.find(Tour.class, tour.getId());
            if (managedTour != null) manager.remove(managedTour);
            manager.getTransaction().commit();
            loadTours();
        } catch (Exception e) {
            rollback(manager);
            showDatabaseError("Не удалось удалить тур", e);
        } finally {
            if (manager != null && manager.isOpen()) manager.close();
        }
    }

    private void rollback(EntityManager manager) {
        if (manager != null && manager.isOpen() && manager.getTransaction().isActive()) {
            manager.getTransaction().rollback();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> renderTours());
        loadTours();
    }

    private void loadTours() {
        EntityManager manager = null;
        try {
            manager = JpaSession.createEntityManager();
            List<Tour> result = manager.createQuery("SELECT t FROM Tour t ORDER BY t.id DESC", Tour.class).getResultList();
            for (Tour tour : result) {
                tour.getMusicBand().getName();
                tour.getCities().size();
            }
            tours.clear();
            tours.addAll(result);
            renderTours();
        } catch (Exception e) {
            showDatabaseError("Не удалось загрузить туры из базы данных", e);
        } finally {
            if (manager != null && manager.isOpen()) manager.close();
        }
    }

    private void renderTours() {
        if (cardsContainer == null) return;
        cardsContainer.getChildren().clear();
        String query = searchField == null || searchField.getText() == null
                ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        int count = 0;
        try {
            for (Tour tour : tours) {
                String bandName = tour.getMusicBand().getName();
                String status = tour.getStatus() == null ? "" : tour.getStatus();
                if (!query.isEmpty() && !bandName.toLowerCase(Locale.ROOT).contains(query)
                        && !status.toLowerCase(Locale.ROOT).contains(query)) continue;
                FXMLLoader loader = new FXMLLoader(getClass().getResource("tour_card.fxml"));
                Node card = loader.load();
                TourCard controller = loader.getController();
                controller.setTour(tour);
                controller.setActions(this::editTour, this::deleteTour);
                cardsContainer.getChildren().add(card);
                count++;
            }
        } catch (IOException e) {
            showDatabaseError("Не удалось сформировать карточки туров", e);
        }
        if (emptyLabel != null && count == 0) {
            emptyLabel.setText(tours.isEmpty() ? "В базе данных пока нет туров." : "По вашему запросу туры не найдены.");
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);
            cardsContainer.getChildren().add(emptyLabel);
        }
    }

    private void showDatabaseError(String header, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка базы данных");
        alert.setHeaderText(header);
        alert.setContentText(exception.getMessage() == null ? exception.toString() : exception.getMessage());
        alert.showAndWait();
    }
}
