package musicmanager.app.views;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import musicmanager.app.database.JpaSession;
import musicmanager.app.models.MusicBand;
import musicmanager.app.logging.AuditLog;
import musicmanager.app.models.Musician;
import musicmanager.app.models.Track;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

public class BandForm {
    @FXML private AnchorPane rootPane;
    @FXML private Label titleLabel;
    @FXML private TextField nameField;
    @FXML private TextField yearField;
    @FXML private TextField topPositionField;
    @FXML private VBox musiciansContainer;
    @FXML private VBox tracksContainer;

    private final List<MusicianRow> musicianRows = new ArrayList<>();
    private final List<TrackRow> trackRows = new ArrayList<>();
    private MusicBand originalBand;

    @FXML
    private void initialize() {
        String inputStyle = "-fx-background-color: #0f111a; -fx-text-fill: white; -fx-prompt-text-fill: #8a99ad; -fx-border-color: #232a42; -fx-border-radius: 5; -fx-background-radius: 5;";
        nameField.setStyle(inputStyle);
        yearField.setStyle(inputStyle);
        topPositionField.setStyle(inputStyle);
        nameField.setTextFormatter(new TextFormatter<>(maxLength(45, null)));
        yearField.setTextFormatter(new TextFormatter<>(maxLength(4, "\\d*")));
        topPositionField.setTextFormatter(new TextFormatter<>(maxLength(10, "\\d*")));
        addMusicianRow(null);
    }

    public void setBand(MusicBand band) {
        originalBand = band;
        titleLabel.setText(band == null ? "Добавить группу" : "Редактировать группу");
        if (band == null) return;
        nameField.setText(band.getName());
        yearField.setText(Integer.toString(band.getYear()));
        topPositionField.setText(Integer.toString(band.getTopPosition()));
        musicianRows.clear();
        musiciansContainer.getChildren().clear();
        for (Musician musician : band.getMusicians()) addMusicianRow(musician);
        if (musicianRows.isEmpty()) addMusicianRow(null);
        trackRows.clear();
        tracksContainer.getChildren().clear();
        for (Track track : band.getTracks()) addTrackRow(track);
    }

    @FXML private void handleAddMusician() { addMusicianRow(null); }
    @FXML private void handleAddTrack() { addTrackRow(null); }

    private void addMusicianRow(Musician musician) {
        MusicianRow row = new MusicianRow(musician == null ? 0 : musician.getId());
        row.name.setPromptText("Имя *");
        row.surname.setPromptText("Фамилия *");
        row.age.setPromptText("Возраст *");
        row.role.setPromptText("Роль (необязательно)");
        if (musician != null) {
            row.name.setText(musician.getName());
            row.surname.setText(musician.getSurname());
            row.age.setText(Integer.toString(musician.getAge()));
            row.role.setText(musician.getRole() == null ? "" : musician.getRole());
        }
        HBox.setHgrow(row.name, Priority.ALWAYS);
        HBox.setHgrow(row.surname, Priority.ALWAYS);
        HBox.setHgrow(row.role, Priority.ALWAYS);
        row.remove.setOnAction(event -> {
            musicianRows.remove(row);
            musiciansContainer.getChildren().remove(row.box);
            updateMusicianRemoveButtons();
        });
        row.box.getChildren().addAll(row.name, row.surname, row.age, row.role, row.remove);
        musicianRows.add(row);
        musiciansContainer.getChildren().add(row.box);
        updateMusicianRemoveButtons();
    }

    private void addTrackRow(Track track) {
        TrackRow row = new TrackRow(track == null ? 0 : track.getTrackId());
        row.name.setPromptText("Название трека *");
        row.duration.setPromptText("Длительность *");
        row.genre.setPromptText("Жанр *");
        if (track != null) {
            row.name.setText(track.getName());
            row.duration.setText(track.getDuration());
            row.genre.setText(track.getGenre());
        }
        HBox.setHgrow(row.name, Priority.ALWAYS);
        HBox.setHgrow(row.genre, Priority.ALWAYS);
        row.remove.setOnAction(event -> {
            trackRows.remove(row);
            tracksContainer.getChildren().remove(row.box);
        });
        row.box.getChildren().addAll(row.name, row.duration, row.genre, row.remove);
        trackRows.add(row);
        tracksContainer.getChildren().add(row.box);
    }

    private void updateMusicianRemoveButtons() {
        musicianRows.forEach(row -> row.remove.setDisable(musicianRows.size() <= 1));
    }

    @FXML
    private void handleSave() {
        BandValues values;
        try {
            values = readValues();
        } catch (IllegalArgumentException e) {
            AuditLog.failure("Некорректные данные музыкальной группы", java.util.Map.of("name", nameField.getText()), e);
            showError("Проверьте заполнение формы", e.getMessage());
            return;
        }

        EntityManager manager = null;
        boolean saved = false;
        MusicBand savedBand = null;
        try {
            manager = JpaSession.createEntityManager();
            manager.getTransaction().begin();
            MusicBand band = originalBand == null ? new MusicBand() : manager.find(MusicBand.class, originalBand.getId());
            if (band == null) throw new IllegalStateException("Группа уже удалена из базы данных.");
            band.setName(values.name);
            band.setYear(values.year);
            band.setTopPosition(values.topPosition);

            if (originalBand == null) {
                List<Musician> musicians = new ArrayList<>();
                for (MusicianValues value : values.musicians) musicians.add(value.toEntity(band));
                List<Track> tracks = new ArrayList<>();
                for (TrackValues value : values.tracks) tracks.add(value.toEntity(band));
                band.setMusicians(musicians);
                band.setTracks(tracks);
                manager.persist(band);
            } else {
                synchronizeMusicians(band, values.musicians);
                synchronizeTracks(band, values.tracks);
            }
            manager.getTransaction().commit();
            savedBand = band;
            saved = true;
        } catch (Exception e) {
            AuditLog.failure("Ошибка сохранения музыкальной группы", java.util.Map.of("name", nameField.getText()), e);
            rollback(manager);
            showError("Не удалось сохранить группу", e.getMessage() == null ? e.toString() : e.getMessage());
        } finally {
            if (manager != null && manager.isOpen()) manager.close();
        }
        if (saved) {
            AuditLog.event(originalBand == null ? "Добавлена музыкальная группа" : "Отредактирована музыкальная группа",
                    java.util.Map.of("bandId", savedBand.getId(), "name", values.name,
                            "year", values.year, "topPosition", values.topPosition));
            logMusicianChanges(savedBand, values.musicians);
            logTrackChanges(savedBand, values.tracks);
            navigateBack();
        }
    }

    private void logMusicianChanges(MusicBand band, List<MusicianValues> values) {
        Map<Integer, Musician> old = new HashMap<>();
        if (originalBand != null) originalBand.getMusicians().forEach(item -> old.put(item.getId(), item));
        Set<Integer> kept = new HashSet<>();
        for (MusicianValues value : values) {
            if (value.id == 0) {
                Musician added = band.getMusicians().stream()
                        .filter(item -> item.getName().equals(value.name) && item.getSurname().equals(value.surname))
                        .findFirst().orElse(null);
                AuditLog.event("Добавлен музыкант", java.util.Map.of("bandId", band.getId(), "name", value.name,
                        "surname", value.surname, "age", value.age, "role", String.valueOf(value.role),
                        "musicianId", added == null ? "unknown" : added.getId()));
            } else {
                kept.add(value.id);
                Musician previous = old.get(value.id);
                if (previous != null && (!previous.getName().equals(value.name) || !previous.getSurname().equals(value.surname)
                        || previous.getAge() != value.age || !java.util.Objects.equals(previous.getRole(), value.role))) {
                    AuditLog.event("Отредактирован музыкант", java.util.Map.of("musicianId", value.id,
                            "oldName", previous.getName() + " " + previous.getSurname(), "newName", value.name + " " + value.surname,
                            "age", value.age, "role", String.valueOf(value.role), "bandId", band.getId()));
                }
            }
        }
        old.values().stream().filter(item -> !kept.contains(item.getId())).forEach(item ->
                AuditLog.event("Удален музыкант", java.util.Map.of("musicianId", item.getId(), "name",
                        item.getName() + " " + item.getSurname(), "bandId", band.getId())));
    }

    private void logTrackChanges(MusicBand band, List<TrackValues> values) {
        Map<Integer, Track> old = new HashMap<>();
        if (originalBand != null) originalBand.getTracks().forEach(item -> old.put(item.getTrackId(), item));
        Set<Integer> kept = new HashSet<>();
        for (TrackValues value : values) {
            if (value.id == 0) {
                Track added = band.getTracks().stream().filter(item -> item.getName().equals(value.name))
                        .findFirst().orElse(null);
                AuditLog.event("Добавлен трек", java.util.Map.of("bandId", band.getId(), "name", value.name,
                        "duration", value.duration, "genre", value.genre, "trackId", added == null ? "unknown" : added.getTrackId()));
            } else {
                kept.add(value.id);
                Track previous = old.get(value.id);
                if (previous != null && (!previous.getName().equals(value.name) || !previous.getDuration().equals(value.duration)
                        || !previous.getGenre().equals(value.genre))) {
                    AuditLog.event("Отредактирован трек", java.util.Map.of("trackId", value.id, "oldName", previous.getName(),
                            "newName", value.name, "duration", value.duration, "genre", value.genre, "bandId", band.getId()));
                }
            }
        }
        old.values().stream().filter(item -> !kept.contains(item.getTrackId())).forEach(item ->
                AuditLog.event("Удален трек", java.util.Map.of("trackId", item.getTrackId(), "name", item.getName(), "bandId", band.getId())));
    }

    private BandValues readValues() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) throw new IllegalArgumentException("Укажите название группы.");
        int year = parseNonNegativeInt(yearField.getText(), "Укажите год основания целым числом.");
        if (year == 0) throw new IllegalArgumentException("Год основания должен быть больше нуля.");
        int topPosition = parseNonNegativeInt(topPositionField.getText(), "Укажите место в чарте целым числом.");
        if (topPosition == 0) throw new IllegalArgumentException("Место в чарте должно быть больше нуля.");

        if (musicianRows.isEmpty()) throw new IllegalArgumentException("Добавьте хотя бы одного музыканта.");
        List<MusicianValues> musicians = new ArrayList<>();
        for (MusicianRow row : musicianRows) {
            String musicianName = row.name.getText().trim();
            String surname = row.surname.getText().trim();
            String ageText = row.age.getText().trim();
            String role = row.role.getText().trim();
            if (musicianName.isEmpty() || surname.isEmpty() || ageText.isEmpty()) {
                throw new IllegalArgumentException("Для каждого музыканта заполните имя, фамилию и возраст.");
            }
            int age = parseNonNegativeInt(ageText, "Возраст музыканта должен быть целым числом от 0 до 120.");
            if (age > 120) throw new IllegalArgumentException("Возраст музыканта должен быть от 0 до 120 лет.");
            musicians.add(new MusicianValues(row.id, musicianName, surname, age, role.isEmpty() ? null : role));
        }

        List<TrackValues> tracks = new ArrayList<>();
        for (TrackRow row : trackRows) {
            String trackName = row.name.getText().trim();
            String duration = row.duration.getText().trim();
            String genre = row.genre.getText().trim();
            if (trackName.isEmpty() && duration.isEmpty() && genre.isEmpty()) continue;
            if (trackName.isEmpty() || duration.isEmpty() || genre.isEmpty()) {
                throw new IllegalArgumentException("Заполните название, длительность и жанр каждого трека или удалите пустую строку.");
            }
            tracks.add(new TrackValues(row.id, trackName, duration, genre));
        }
        return new BandValues(name, year, topPosition, musicians, tracks);
    }

    private int parseNonNegativeInt(String text, String errorMessage) {
        try {
            int value = Integer.parseInt(text.trim());
            if (value < 0) throw new NumberFormatException();
            return value;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private void synchronizeMusicians(MusicBand band, List<MusicianValues> values) {
        List<Musician> current = band.getMusicians();
        Map<Integer, Musician> existing = new HashMap<>();
        current.forEach(musician -> existing.put(musician.getId(), musician));
        Set<Integer> keepIds = new HashSet<>();
        values.stream().filter(value -> value.id != 0).forEach(value -> keepIds.add(value.id));
        current.removeIf(musician -> !keepIds.contains(musician.getId()));
        for (MusicianValues value : values) {
            Musician musician = value.id == 0 ? value.toEntity(band) : existing.get(value.id);
            if (musician == null) throw new IllegalStateException("Не удалось найти музыканта для изменения.");
            musician.setName(value.name);
            musician.setSurname(value.surname);
            musician.setAge(value.age);
            musician.setRole(value.role);
            musician.setMusicBand(band);
            if (value.id == 0) current.add(musician);
        }
    }

    private void synchronizeTracks(MusicBand band, List<TrackValues> values) {
        List<Track> current = band.getTracks();
        Map<Integer, Track> existing = new HashMap<>();
        current.forEach(track -> existing.put(track.getTrackId(), track));
        Set<Integer> keepIds = new HashSet<>();
        values.stream().filter(value -> value.id != 0).forEach(value -> keepIds.add(value.id));
        current.removeIf(track -> !keepIds.contains(track.getTrackId()));
        for (TrackValues value : values) {
            Track track = value.id == 0 ? value.toEntity(band) : existing.get(value.id);
            if (track == null) throw new IllegalStateException("Не удалось найти трек для изменения.");
            track.setName(value.name);
            track.setDuration(value.duration);
            track.setGenre(value.genre);
            track.setMusicBand(band);
            if (value.id == 0) current.add(track);
        }
    }

    @FXML
    private void handleCancel() { navigateBack(); }

    private void navigateBack() {
        try {
            ViewNavigator.show(rootPane, "bands.fxml");
        } catch (IOException e) {
            showError("Не удалось открыть список групп", e.getMessage());
        }
    }

    private void rollback(EntityManager manager) {
        if (manager != null && manager.isOpen() && manager.getTransaction().isActive()) manager.getTransaction().rollback();
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(rootPane.getScene().getWindow());
        alert.setTitle("Группы");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static TextField textField(String prompt, int maxLength, String pattern) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setStyle("-fx-background-color: #0f111a; -fx-text-fill: white; -fx-prompt-text-fill: #8a99ad; -fx-border-color: #232a42; -fx-border-radius: 5; -fx-background-radius: 5;");
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String next = change.getControlNewText();
            return next.length() <= maxLength && (pattern == null || next.matches(pattern)) ? change : null;
        };
        field.setTextFormatter(new TextFormatter<>(filter));
        return field;
    }

    private static UnaryOperator<TextFormatter.Change> maxLength(int maxLength, String pattern) {
        return change -> {
            String value = change.getControlNewText();
            return value.length() <= maxLength && (pattern == null || value.matches(pattern)) ? change : null;
        };
    }

    private static Button removeButton(Runnable action) {
        Button button = new Button("−");
        button.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
        button.setOnAction(event -> action.run());
        return button;
    }

    private static final class MusicianRow {
        final int id;
        final HBox box = new HBox(7);
        final TextField name;
        final TextField surname;
        final TextField age;
        final TextField role;
        final Button remove;
        MusicianRow(int id) {
            this.id = id;
            name = BandForm.textField("Имя *", 45, null);
            surname = BandForm.textField("Фамилия *", 100, null);
            age = BandForm.textField("Возраст *", 3, "\\d*");
            role = BandForm.textField("Роль", 45, null);
            remove = BandForm.removeButton(() -> { });
            box.setAlignment(Pos.CENTER_LEFT);
        }
    }

    private static final class TrackRow {
        final int id;
        final HBox box = new HBox(7);
        final TextField name;
        final TextField duration;
        final TextField genre;
        final Button remove;
        TrackRow(int id) {
            this.id = id;
            name = BandForm.textField("Название трека *", 45, null);
            duration = BandForm.textField("Длительность *", 10, null);
            genre = BandForm.textField("Жанр *", 45, null);
            remove = BandForm.removeButton(() -> { });
            box.setAlignment(Pos.CENTER_LEFT);
        }
    }

    private static final class BandValues {
        final String name; final int year; final int topPosition;
        final List<MusicianValues> musicians; final List<TrackValues> tracks;
        BandValues(String name, int year, int topPosition, List<MusicianValues> musicians, List<TrackValues> tracks) {
            this.name = name; this.year = year; this.topPosition = topPosition; this.musicians = musicians; this.tracks = tracks;
        }
    }
    private static final class MusicianValues {
        final int id; final String name; final String surname; final int age; final String role;
        MusicianValues(int id, String name, String surname, int age, String role) { this.id = id; this.name = name; this.surname = surname; this.age = age; this.role = role; }
        Musician toEntity(MusicBand band) {
            Musician musician = new Musician(); musician.setName(name); musician.setSurname(surname); musician.setAge(age); musician.setRole(role); musician.setMusicBand(band); return musician;
        }
    }
    private static final class TrackValues {
        final int id; final String name; final String duration; final String genre;
        TrackValues(int id, String name, String duration, String genre) { this.id = id; this.name = name; this.duration = duration; this.genre = genre; }
        Track toEntity(MusicBand band) {
            Track track = new Track(); track.setName(name); track.setDuration(duration); track.setGenre(genre); track.setMusicBand(band); return track;
        }
    }
}
