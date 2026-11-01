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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import musicmanager.app.database.JpaSession;
import musicmanager.app.models.MusicBand;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class Bands implements Initializable {
    @FXML private GridPane cardsContainer;
    @FXML private TextField searchField;
    @FXML private Label emptyLabel;

    private final List<MusicBand> bands = new ArrayList<>();

    @FXML
    private void handleAddBand() {
        openBandForm(null);
    }

    private void editBand(MusicBand band) {
        openBandForm(band);
    }

    private void openBandForm(MusicBand band) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("band_form.fxml"));
            AnchorPane pane = loader.load();
            ((BandForm) loader.getController()).setBand(band);
            AnchorPane contentArea = (AnchorPane) cardsContainer.getScene().lookup("#contentArea");
            if (contentArea == null) throw new IOException("Не найдена область отображения экранов.");
            contentArea.getChildren().setAll(pane);
            AnchorPane.setTopAnchor(pane, 0.0);
            AnchorPane.setBottomAnchor(pane, 0.0);
            AnchorPane.setLeftAnchor(pane, 0.0);
            AnchorPane.setRightAnchor(pane, 0.0);
        } catch (Exception e) {
            showDatabaseError("Не удалось открыть форму группы", e);
        }
    }

    private void deleteBand(MusicBand band) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(cardsContainer.getScene().getWindow());
        confirm.setTitle("Удаление группы");
        confirm.setHeaderText("Удалить группу «" + band.getName() + "»?");
        confirm.setContentText("Вместе с группой будут удалены связанные музыканты, треки и туры.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        EntityManager manager = null;
        try {
            manager = JpaSession.createEntityManager();
            manager.getTransaction().begin();
            MusicBand managedBand = manager.find(MusicBand.class, band.getId());
            if (managedBand != null) manager.remove(managedBand);
            manager.getTransaction().commit();
            loadBands();
        } catch (Exception e) {
            rollback(manager);
            showDatabaseError("Не удалось удалить группу", e);
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
        cardsContainer.setMinWidth(0);
        cardsContainer.widthProperty().addListener((obs, oldVal, newVal) -> layoutCards(newVal.doubleValue()));
        searchField.textProperty().addListener((obs, oldVal, newVal) -> renderCards());
        loadBands();
    }

    private void loadBands() {
        EntityManager manager = null;
        try {
            manager = JpaSession.createEntityManager();
            List<MusicBand> result = manager.createQuery(
                    "SELECT b FROM MusicBand b ORDER BY b.name", MusicBand.class).getResultList();
            // Initialize lazy relationships while the EntityManager is still open.
            for (MusicBand band : result) {
                band.getMusicians().size();
                band.getTracks().size();
                band.getTours().size();
                band.getTours().forEach(tour -> tour.getCities().size());
            }
            bands.clear();
            bands.addAll(result);
            renderCards();
        } catch (Exception e) {
            showDatabaseError("Не удалось загрузить группы из базы данных", e);
        } finally {
            if (manager != null && manager.isOpen()) manager.close();
        }
    }

    private void renderCards() {
        if (cardsContainer == null) return;
        cardsContainer.getChildren().clear();
        String query = searchField == null || searchField.getText() == null
                ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);

        int index = 0;
        try {
            for (MusicBand band : bands) {
                if (!query.isEmpty() && !band.getName().toLowerCase(Locale.ROOT).contains(query)) continue;
                FXMLLoader loader = new FXMLLoader(getClass().getResource("band_card.fxml"));
                Node card = loader.load();
                BandCard controller = loader.getController();
                controller.setBand(band);
                controller.setActions(this::editBand, this::deleteBand);
                cardsContainer.add(card, index % 3, index / 3);
                index++;
            }
        } catch (IOException e) {
            showDatabaseError("Не удалось сформировать карточки групп", e);
        }
        if (emptyLabel != null) {
            emptyLabel.setText(bands.isEmpty() ? "В базе данных пока нет групп." : "По вашему запросу группы не найдены.");
            emptyLabel.setVisible(index == 0);
            emptyLabel.setManaged(index == 0);
            if (index == 0) cardsContainer.add(emptyLabel, 0, 0);
        }
        layoutCards(cardsContainer.getWidth());
    }

    private void layoutCards(double width) {
        if (cardsContainer == null || width < 100) return;
        int columns = width > 750 ? 3 : 1;
        double cardWidth = (width - cardsContainer.getHgap() * (columns - 1)) / columns;
        int index = 0;
        for (Node node : cardsContainer.getChildren()) {
            GridPane.setColumnIndex(node, index % columns);
            GridPane.setRowIndex(node, index / columns);
            if (node instanceof Region region && node != emptyLabel) {
                region.setMinWidth(cardWidth);
                region.setPrefWidth(cardWidth);
                region.setMaxWidth(cardWidth);
            }
            index++;
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
