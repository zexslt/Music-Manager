package musicmanager.app.views;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import musicmanager.app.database.CityService;
import musicmanager.app.database.JpaSession;
import musicmanager.app.models.City;
import musicmanager.app.models.MusicBand;
import musicmanager.app.models.Tour;
import musicmanager.app.logging.AuditLog;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

public class TourForm {
    @FXML private AnchorPane rootPane;
    @FXML private Label titleLabel;
    @FXML private TextField statusField;
    @FXML private TextField revenueField;
    @FXML private ComboBox<MusicBand> bandBox;
    @FXML private VBox citiesContainer;

    private final List<CityRow> cityRows = new ArrayList<>();
    private Tour originalTour;

    @FXML
    private void initialize() {
        String inputStyle = "-fx-background-color: #0f111a; -fx-text-fill: white; -fx-prompt-text-fill: #8a99ad; -fx-border-color: #232a42; -fx-border-radius: 5; -fx-background-radius: 5;";
        statusField.setStyle(inputStyle);
        revenueField.setStyle(inputStyle);
        bandBox.setStyle(inputStyle);
        statusField.setTextFormatter(new TextFormatter<>(maxLength(45, null)));
        revenueField.setTextFormatter(new TextFormatter<>(maxLength(10, "\\d*")));
        bandBox.setItems(FXCollections.observableArrayList());
        bandBox.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(MusicBand band) { return band == null ? "" : band.getName(); }
            @Override public MusicBand fromString(String value) { return null; }
        });
        loadBands();
        addCityRow("");
    }

    public void setTour(Tour tour) {
        originalTour = tour;
        titleLabel.setText(tour == null ? "Добавить тур" : "Редактировать тур");
        if (tour == null) return;
        statusField.setText(tour.getStatus());
        revenueField.setText(Integer.toString(tour.getRevenue()));
        bandBox.getItems().stream().filter(item -> item.getId() == tour.getMusicBand().getId())
                .findFirst().ifPresent(bandBox.getSelectionModel()::select);
        cityRows.clear();
        citiesContainer.getChildren().clear();
        for (City city : tour.getCities()) addCityRow(city.getName());
        if (cityRows.isEmpty()) addCityRow("");
    }

    private void loadBands() {
        EntityManager manager = null;
        try {
            manager = JpaSession.createEntityManager();
            List<MusicBand> bands = manager.createQuery("SELECT b FROM MusicBand b ORDER BY b.name", MusicBand.class).getResultList();
            bandBox.setItems(FXCollections.observableArrayList(bands));
            if (!bands.isEmpty()) bandBox.getSelectionModel().selectFirst();
        } catch (Exception e) {
            showError("Не удалось загрузить список групп", errorMessage(e));
        } finally {
            if (manager != null && manager.isOpen()) manager.close();
        }
    }

    @FXML private void handleAddCityRow() { addCityRow(""); }

    private void addCityRow(String name) {
        CityRow row = new CityRow();
        row.field.setText(name);
        HBox.setHgrow(row.field, Priority.ALWAYS);
        row.remove.setOnAction(event -> {
            cityRows.remove(row);
            citiesContainer.getChildren().remove(row.box);
        });
        row.box.getChildren().addAll(row.field, row.remove);
        cityRows.add(row);
        citiesContainer.getChildren().add(row.box);
    }

    @FXML
    private void handleOpenCityWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("city_form.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = new Stage();
            stage.setTitle("Города справочника");
            stage.initOwner(rootPane.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setScene(scene);
            stage.setMinWidth(520);
            stage.setMinHeight(500);
            stage.showAndWait();
        } catch (IOException e) {
            showError("Не удалось открыть окно городов", errorMessage(e));
        }
    }

    @FXML
    private void handleSave() {
        String status = statusField.getText().trim();
        if (status.isEmpty()) {
            AuditLog.failure("Некорректные данные тура", java.util.Map.of("field", "status"), new IllegalArgumentException("Укажите название или статус тура."));
            showError("Проверьте заполнение формы", "Укажите название или статус тура."); return;
        }
        if (bandBox.getValue() == null) {
            AuditLog.failure("Некорректные данные тура", java.util.Map.of("field", "band"), new IllegalArgumentException("Для тура нужно выбрать группу."));
            showError("Проверьте заполнение формы", "Добавьте группу и выберите её для тура."); return;
        }
        int revenue;
        try {
            revenue = Integer.parseInt(revenueField.getText().trim());
            if (revenue < 0) throw new NumberFormatException();
        } catch (RuntimeException e) {
            AuditLog.failure("Некорректная выручка тура", java.util.Map.of("value", revenueField.getText()), e);
            showError("Проверьте заполнение формы", "Укажите неотрицательную выручку целым числом.");
            return;
        }

        Map<String, String> uniqueCityNames = new LinkedHashMap<>();
        for (CityRow row : cityRows) {
            String name = row.field.getText().trim();
            if (name.isEmpty()) continue;
            if (name.length() > 45) { showError("Проверьте заполнение формы", "Название города не должно превышать 45 символов."); return; }
            uniqueCityNames.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
        }

        EntityManager manager = null;
        boolean saved = false;
        Tour savedTour = null;
        try {
            manager = JpaSession.createEntityManager();
            manager.getTransaction().begin();
            MusicBand band = manager.find(MusicBand.class, bandBox.getValue().getId());
            if (band == null) throw new IllegalStateException("Выбранная группа не найдена в базе данных.");
            Tour tour = originalTour == null ? new Tour() : manager.find(Tour.class, originalTour.getId());
            if (tour == null) throw new IllegalStateException("Тур уже удален из базы данных.");

            List<City> cities = new ArrayList<>();
            Set<Integer> cityIds = new HashSet<>();
            for (String cityName : uniqueCityNames.values()) {
                City city = CityService.findOrCreate(manager, cityName);
                if (cityIds.add(city.getId())) cities.add(city);
            }

            tour.setStatus(status);
            tour.setRevenue(revenue);
            tour.setMusicBand(band);
            if (originalTour == null) {
                tour.setCities(cities);
                manager.persist(tour);
            } else {
                tour.getCities().clear();
                tour.getCities().addAll(cities);
            }
            manager.getTransaction().commit();
            savedTour = tour;
            saved = true;
        } catch (Exception e) {
            AuditLog.failure("Ошибка сохранения тура", java.util.Map.of("status", statusField.getText()), e);
            rollback(manager);
            showError("Не удалось сохранить тур", errorMessage(e));
        } finally {
            if (manager != null && manager.isOpen()) manager.close();
        }
        if (saved) {
            int savedTourId = savedTour.getId();
            AuditLog.event(originalTour == null ? "Добавлен тур" : "Отредактирован тур",
                    java.util.Map.of("tourId", savedTourId, "status", status, "revenue", revenue,
                            "bandId", bandBox.getValue().getId(), "cities", uniqueCityNames.values().toString()));
            Set<String> oldCities = new java.util.HashSet<>();
            if (originalTour != null) originalTour.getCities().forEach(city -> oldCities.add(city.getName()));
            Set<String> newCities = new java.util.HashSet<>(uniqueCityNames.values());
            newCities.stream().filter(city -> !oldCities.contains(city)).forEach(city ->
                    AuditLog.event("Город добавлен в тур", java.util.Map.of("tourId", savedTourId, "city", city)));
            oldCities.stream().filter(city -> !newCities.contains(city)).forEach(city ->
                    AuditLog.event("Город удален из тура", java.util.Map.of("tourId", savedTourId, "city", city)));
            navigateBack();
        }
    }

    @FXML private void handleCancel() { navigateBack(); }

    private void navigateBack() {
        try { ViewNavigator.show(rootPane, "tours.fxml"); }
        catch (IOException e) { showError("Не удалось открыть список туров", errorMessage(e)); }
    }

    private void rollback(EntityManager manager) {
        if (manager != null && manager.isOpen() && manager.getTransaction().isActive()) manager.getTransaction().rollback();
    }

    private TextField textField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setStyle("-fx-background-color: #0f111a; -fx-text-fill: white; -fx-prompt-text-fill: #8a99ad; -fx-border-color: #232a42; -fx-border-radius: 5; -fx-background-radius: 5;");
        field.setTextFormatter(new TextFormatter<>(maxLength(45, null)));
        return field;
    }

    private Button removeButton(Runnable action) {
        Button button = new Button("−");
        button.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
        button.setOnAction(event -> action.run());
        return button;
    }

    private UnaryOperator<TextFormatter.Change> maxLength(int max, String pattern) {
        return change -> {
            String value = change.getControlNewText();
            return value.length() <= max && (pattern == null || value.matches(pattern)) ? change : null;
        };
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        if (rootPane.getScene() != null) alert.initOwner(rootPane.getScene().getWindow());
        alert.setTitle("Туры");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private String errorMessage(Exception e) { return e.getMessage() == null ? e.toString() : e.getMessage(); }

    private static final class CityRow {
        final HBox box = new HBox(8);
        final TextField field = new TextField();
        final Button remove = new Button("−");
        CityRow() {
            field.setPromptText("Название города");
            field.setStyle("-fx-background-color: #0f111a; -fx-text-fill: white; -fx-prompt-text-fill: #8a99ad; -fx-border-color: #232a42; -fx-border-radius: 5; -fx-background-radius: 5;");
            field.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().length() <= 45 ? change : null));
            remove.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
        }
    }
}
