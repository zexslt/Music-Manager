package musicmanager.app.views;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;
import musicmanager.app.database.CityService;
import musicmanager.app.database.JpaSession;
import musicmanager.app.models.City;

import javax.persistence.EntityManager;
import java.util.List;

public class CityForm {
    @FXML private TextField cityNameField;
    @FXML private Label statusLabel;
    @FXML private ListView<City> citiesList;

    @FXML
    private void initialize() {
        cityNameField.setStyle("-fx-background-color: #161b2e; -fx-text-fill: white; -fx-prompt-text-fill: #8a99ad; -fx-border-color: #232a42; -fx-border-radius: 5; -fx-background-radius: 5;");
        cityNameField.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().length() <= 45 ? change : null));
        loadCities();
    }

    @FXML
    private void handleAddCity() {
        String name = cityNameField.getText().trim();
        if (name.isEmpty()) {
            showError("Укажите название города.");
            return;
        }

        EntityManager manager = null;
        boolean saved = false;
        boolean reused = false;
        City city = null;
        try {
            manager = JpaSession.createEntityManager();
            manager.getTransaction().begin();
            List<City> existing = manager.createQuery("SELECT c FROM City c WHERE c.name = :name", City.class)
                    .setParameter("name", name).setMaxResults(1).getResultList();
            reused = !existing.isEmpty();
            city = CityService.findOrCreate(manager, name);
            manager.getTransaction().commit();
            saved = true;
        } catch (Exception e) {
            if (manager != null && manager.isOpen() && manager.getTransaction().isActive()) manager.getTransaction().rollback();
            showError(e.getMessage() == null ? e.toString() : e.getMessage());
        } finally {
            if (manager != null && manager.isOpen()) manager.close();
        }

        if (saved) {
            statusLabel.setText(reused
                    ? "Город уже существует. Используется запись с ID " + city.getId() + "."
                    : "Город добавлен, ID: " + city.getId() + ".");
            cityNameField.clear();
            loadCities();
        }
    }

    private void loadCities() {
        EntityManager manager = null;
        try {
            manager = JpaSession.createEntityManager();
            List<City> cities = manager.createQuery("SELECT c FROM City c ORDER BY c.name", City.class).getResultList();
            citiesList.setItems(FXCollections.observableArrayList(cities));
        } catch (Exception e) {
            showError(e.getMessage() == null ? e.toString() : e.getMessage());
        } finally {
            if (manager != null && manager.isOpen()) manager.close();
        }
    }

    @FXML
    private void handleClose() {
        ((Stage) cityNameField.getScene().getWindow()).close();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        if (cityNameField.getScene() != null) alert.initOwner(cityNameField.getScene().getWindow());
        alert.setTitle("Города");
        alert.setHeaderText("Не удалось сохранить город");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
