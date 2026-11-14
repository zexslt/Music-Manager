package musicmanager.app.views;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import musicmanager.app.database.JpaSession;
import musicmanager.app.database.XmlBackupService;
import musicmanager.app.database.PdfExportService;
import musicmanager.app.models.MusicBand;

import javax.persistence.EntityManager;
import java.io.File;
import java.util.Locale;
import java.util.IdentityHashMap;
import java.util.Map;

public class Settings {
    @FXML private Label statusLabel;
    @FXML private TreeView<String> pdfSelectionTree;
    private final Map<TreeItem<String>, BandTreeSelection> bandTreeItems = new IdentityHashMap<>();
    private record BandTreeSelection(int bandId, PdfExportService.Selection section) { }

    @FXML
    private void initialize() {
        TreeItem<String> root = new TreeItem<>("Экспорт данных");
        TreeItem<String> all = new TreeItem<>("Все таблицы базы данных");
        TreeItem<String> band = new TreeItem<>("Музыкальная группа (все группы)");
        band.getChildren().addAll(new TreeItem<>("Туры"), new TreeItem<>("Музыканты"), new TreeItem<>("Треки"));
        EntityManager manager = null;
        try {
            manager = JpaSession.createEntityManager();
            for (MusicBand musicBand : manager.createQuery("select b from MusicBand b order by b.name", MusicBand.class).getResultList()) {
                TreeItem<String> bandItem = new TreeItem<>(musicBand.getName() + " (ID: " + musicBand.getId() + ")");
                bandTreeItems.put(bandItem, new BandTreeSelection(musicBand.getId(), null));
                TreeItem<String> tours = new TreeItem<>("Туры");
                TreeItem<String> musicians = new TreeItem<>("Музыканты");
                TreeItem<String> tracks = new TreeItem<>("Треки");
                bandTreeItems.put(tours, new BandTreeSelection(musicBand.getId(), PdfExportService.Selection.TOURS));
                bandTreeItems.put(musicians, new BandTreeSelection(musicBand.getId(), PdfExportService.Selection.MUSICIANS));
                bandTreeItems.put(tracks, new BandTreeSelection(musicBand.getId(), PdfExportService.Selection.TRACKS));
                bandItem.getChildren().addAll(tours, musicians, tracks);
                bandItem.setExpanded(true);
                band.getChildren().add(bandItem);
            }
        } catch (Exception e) {
            statusLabel.setText("Не удалось загрузить список групп: " + e.getMessage());
        } finally {
            if (manager != null && manager.isOpen()) manager.close();
        }
        all.getChildren().addAll(band, new TreeItem<>("Города"));
        root.getChildren().add(all);
        root.setExpanded(true); all.setExpanded(true); band.setExpanded(true);
        pdfSelectionTree.setRoot(root);
        pdfSelectionTree.setShowRoot(false);
        pdfSelectionTree.getSelectionModel().select(all);
    }

    @FXML
    private void handleExportPdf() {
        TreeItem<String> selected = pdfSelectionTree.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        BandTreeSelection bandSelection = bandTreeItems.get(selected);
        if (bandSelection != null) {
            exportPdf(file -> {
                EntityManager manager = JpaSession.createEntityManager();
                try { return PdfExportService.exportBand(manager, bandSelection.bandId(), bandSelection.section(), file); }
                finally { if (manager.isOpen()) manager.close(); }
            });
            return;
        }
        PdfExportService.Selection selection = switch (selected.getValue()) {
            case "Все таблицы базы данных" -> PdfExportService.Selection.ALL;
            case "Музыкальная группа (все группы)" -> PdfExportService.Selection.BANDS;
            case "Туры" -> PdfExportService.Selection.TOURS;
            case "Музыканты" -> PdfExportService.Selection.MUSICIANS;
            case "Треки" -> PdfExportService.Selection.TRACKS;
            case "Города" -> PdfExportService.Selection.CITIES;
            default -> null;
        };
        if (selection == null) return;
        exportPdf(file -> {
            EntityManager manager = JpaSession.createEntityManager();
            try { return PdfExportService.export(manager, selection, file); }
            finally { if (manager.isOpen()) manager.close(); }
        });
    }

    @FunctionalInterface
    private interface PdfWriter { int write(File file) throws Exception; }

    private void exportPdf(PdfWriter writer) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить данные в PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF-файлы (*.pdf)", "*.pdf"));
        chooser.setInitialFileName("music-manager-report.pdf");
        File file = chooser.showSaveDialog(owner());
        if (file == null) return;
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            file = new File(file.getParentFile(), file.getName() + ".pdf");
        }

        try {
            int records = writer.write(file);
            statusLabel.setText("PDF создан: " + records + " записей.\nФайл: " + file.getAbsolutePath());
        } catch (Exception e) {
            showError("Не удалось экспортировать данные в PDF", e);
        }
    }

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
