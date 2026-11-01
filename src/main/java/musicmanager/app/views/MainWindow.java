package musicmanager.app.views;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import musicmanager.app.exceptions.ViewLoadingException;
import javafx.scene.layout.AnchorPane;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainWindow implements Initializable {
    @FXML
    private AnchorPane contentArea;

    @FXML private Button btnOverview;
    @FXML private Button btnBands;
    @FXML private Button btnTours;
    @FXML private Button btnSettings;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        handleShowOverview();
    }

    @FXML
    private void handleShowOverview() {
        setActiveButton(btnOverview);
        try { loadView("Overview.fxml"); } catch (ViewLoadingException e) { showLoadingError(e); }
    }

    @FXML
    private void handleShowBands() {
        setActiveButton(btnBands);
        try { loadView("Bands.fxml"); } catch (ViewLoadingException e) { showLoadingError(e); }
    }

    @FXML
    private void handleShowTours() {
        setActiveButton(btnTours);
        try { loadView("tours.fxml"); } catch (ViewLoadingException e) { showLoadingError(e); }
    }

    @FXML
    private void handleShowSettings() {
        setActiveButton(btnSettings);
        try { loadView("settings.fxml"); } catch (ViewLoadingException e) { showLoadingError(e); }
    }

    @FXML
    public void handleShowBandInfo() {
        try { loadView("band_info.fxml"); } catch (ViewLoadingException e) { showLoadingError(e); }
    }

    private void setActiveButton(Button selectedButton) {
        Button[] buttons = {btnOverview, btnBands, btnTours, btnSettings};
        for (Button btn : buttons) {
            if (btn != null) {
                btn.getStyleClass().remove("sidebar-button-active");
                if (!btn.getStyleClass().contains("sidebar-button")) {
                    btn.getStyleClass().add("sidebar-button");
                }
            }
        }
        if (selectedButton != null) {
            selectedButton.getStyleClass().remove("sidebar-button");
            selectedButton.getStyleClass().add("sidebar-button-active");
        }
    }

    /** Загружает FXML-представление и преобразует ошибки ввода-вывода в прикладное исключение.
     * @param fxmlFileName имя FXML-файла
     * @throws ViewLoadingException если файл не найден или не удалось построить экран
     */
    private void loadView(String fxmlFileName) throws ViewLoadingException {
        try {
            URL resource = getClass().getResource(fxmlFileName);
            if (resource == null) {
                throw new ViewLoadingException("Не найден файл представления: " + fxmlFileName, null);
            }
            FXMLLoader loader = new FXMLLoader(resource);
            AnchorPane pane = loader.load();

            if (contentArea == null) {
                throw new ViewLoadingException("Область отображения экранов не инициализирована.", null);
            }
            contentArea.getChildren().setAll(pane);
            AnchorPane.setTopAnchor(pane, 0.0);
            AnchorPane.setBottomAnchor(pane, 0.0);
            AnchorPane.setLeftAnchor(pane, 0.0);
            AnchorPane.setRightAnchor(pane, 0.0);
        } catch (IOException e) {
            throw new ViewLoadingException("Не удалось загрузить экран: " + fxmlFileName, e);
        }
    }

    /** Показывает пользователю понятное сообщение о проблеме загрузки экрана. */
    private void showLoadingError(ViewLoadingException exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка приложения");
        alert.setHeaderText("Не удалось открыть раздел");
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }
}