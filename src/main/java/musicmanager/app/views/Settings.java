package musicmanager.app.views;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import musicmanager.app.database.JpaSession;
import musicmanager.app.database.XmlBackupService;

import javax.persistence.EntityManager;
import java.io.File;
import java.util.Locale;

public class Settings {
    @FXML private Label statusLabel;

    @FXML
    private void handleExportXml() {
        FileChooser chooser = xmlChooser("Экспортировать данные в XML");
        chooser.setInitialFileName("music-manager-backup.xml");
        File file = chooser.showSaveDialog(owner());
        if (file == null) return;
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".xml")) {
            file = new File(file.getParentFile(), file.getName() + ".xml");
        }

        EntityManager manager = null;
        try {
            manager = JpaSession.createEntityManager();
            XmlBackupService.ExportCounts counts = XmlBackupService.exportToXml(manager, file);
            statusLabel.setText("Экспорт завершён: " + counts.bands() + " групп, " + counts.musicians()
                    + " музыкантов, " + counts.tracks() + " треков, " + counts.tours() + " туров, "
                    + counts.cities() + " городов.\nФайл: " + file.getAbsolutePath());
        } catch (Exception e) {
            showError("Не удалось экспортировать данные", e);
        } finally {
            if (manager != null && manager.isOpen()) manager.close();
        }
    }

    @FXML
    private void handleImportXml() {
        File file = xmlChooser("Импортировать данные из XML").showOpenDialog(owner());
        if (file == null) return;

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.initOwner(owner());
        confirmation.setTitle("Импорт XML");
        confirmation.setHeaderText("Загрузить данные из выбранного XML-файла?");
        confirmation.setContentText("Записи с совпадающими ID будут обновлены!");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        EntityManager manager = null;
        try {
            manager = JpaSession.createEntityManager();
            manager.getTransaction().begin();
            XmlBackupService.ImportCounts counts = XmlBackupService.importFromXml(manager, file);
            manager.getTransaction().commit();
            statusLabel.setText("Импорт завершён: " + counts.bands() + " групп, " + counts.musicians()
                    + " музыкантов, " + counts.tracks() + " треков, " + counts.tours() + " туров, "
                    + counts.cities() + " городов.");
        } catch (Exception e) {
            if (manager != null && manager.isOpen() && manager.getTransaction().isActive()) {
                manager.getTransaction().rollback();
            }
            showError("Не удалось импортировать данные", e);
        } finally {
            if (manager != null && manager.isOpen()) manager.close();
        }
    }

    private FileChooser xmlChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML-файлы (*.xml)", "*.xml"));
        return chooser;
    }

    private Window owner() {
        return statusLabel.getScene().getWindow();
    }

    private void showError(String header, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(owner());
        alert.setTitle("Импорт и экспорт XML");
        alert.setHeaderText(header);
        alert.setContentText(exception.getMessage() == null ? exception.toString() : exception.getMessage());
        alert.showAndWait();
    }
}
