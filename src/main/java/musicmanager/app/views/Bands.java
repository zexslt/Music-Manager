package musicmanager.app.views;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import java.net.URL;
import java.util.ResourceBundle;

public class Bands implements Initializable {

    @FXML
    private GridPane cardsContainer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (cardsContainer != null) {
            cardsContainer.setMinWidth(0);
            cardsContainer.setPrefWidth(0);

            cardsContainer.widthProperty().addListener((obs, oldVal, newVal) -> {
                double containerWidth = newVal.doubleValue();
                if (containerWidth < 100) return;

                double hgap = cardsContainer.getHgap();
                int columns = (containerWidth > 750) ? 3 : 1;

                double cardWidth;
                if (columns == 3) {
                    cardWidth = (containerWidth - (hgap * 2)) / 3;
                } else {
                    cardWidth = containerWidth;
                }

                int index = 0;
                for (Node node : cardsContainer.getChildren()) {
                    int col = index % columns;
                    int row = index / columns;

                    GridPane.setColumnIndex(node, col);
                    GridPane.setRowIndex(node, row);

                    if (node instanceof Region) {
                        Region card = (Region) node;
                        card.setPrefWidth(cardWidth);
                        card.setMaxWidth(cardWidth);
                        card.setMinWidth(cardWidth);
                    }
                    index++;
                }
            });
        }
    }
}