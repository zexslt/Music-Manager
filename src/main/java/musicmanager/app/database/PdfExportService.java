package musicmanager.app.database;

import musicmanager.app.models.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import javax.persistence.EntityManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Создаёт PDF-отчёты с данными из таблиц приложения. Вызывается с открытым EntityManager. */
public final class PdfExportService {
    public enum Selection {
        ALL("Все данные"), BANDS("Музыкальные группы"), TOURS("Туры"),
        MUSICIANS("Музыканты"), TRACKS("Треки"), CITIES("Города");
        private final String title;
        Selection(String title) { this.title = title; }
        public String title() { return title; }
    }

    private record PdfTable(String title, List<String> headers, List<List<String>> rows) { }
    private record Report(List<PdfTable> tables, int recordCount) { }

    private PdfExportService() { }

    public static int export(EntityManager em, Selection selection, File destination) throws IOException {
        Report report = switch (selection) {
            case ALL -> {
                List<PdfTable> tables = new ArrayList<>();
                tables.add(bandsTable(allBands(em)));
                tables.add(musiciansTable(allMusicians(em)));
                tables.add(tracksTable(allTracks(em)));
                tables.add(toursTable(allTours(em)));
                tables.add(citiesTable(allCities(em)));
                yield report(tables);
            }
            case BANDS -> {
                List<MusicBand> bands = allBands(em);
                List<PdfTable> tables = new ArrayList<>();
                tables.add(bandsTable(bands));
                tables.add(musiciansTable(allMusicians(em)));
                tables.add(tracksTable(allTracks(em)));
                tables.add(toursTable(allTours(em)));
                yield report(tables);
            }
            case TOURS -> report(List.of(toursTable(allTours(em))));
            case MUSICIANS -> report(List.of(musiciansTable(allMusicians(em))));
            case TRACKS -> report(List.of(tracksTable(allTracks(em))));
            case CITIES -> report(List.of(citiesTable(allCities(em))));
        };
        writePdf(selection.title(), report.tables(), destination.toPath());
        return report.recordCount();
    }

    /** Группа целиком или одна из её вложенных таблиц. */
    public static int exportBand(EntityManager em, int bandId, File destination) throws IOException {
        return exportBand(em, bandId, null, destination);
    }

    public static int exportBand(EntityManager em, int bandId, Selection section, File destination) throws IOException {
        MusicBand band = em.find(MusicBand.class, bandId);
        if (band == null) throw new IllegalArgumentException("Музыкальная группа с ID " + bandId + " не найдена.");
        List<PdfTable> tables = new ArrayList<>();
        String title;
        if (section == null) {
            tables.add(bandsTable(List.of(band)));
            tables.add(musiciansTable(safe(band.getMusicians())));
            tables.add(tracksTable(safe(band.getTracks())));
            tables.add(toursTable(safe(band.getTours())));
            title = "Группа — " + band.getName();
        } else {
            switch (section) {
                case TOURS -> { tables.add(toursTable(safe(band.getTours()))); title = "Туры — " + band.getName(); }
                case MUSICIANS -> { tables.add(musiciansTable(safe(band.getMusicians()))); title = "Музыканты — " + band.getName(); }
                case TRACKS -> { tables.add(tracksTable(safe(band.getTracks()))); title = "Треки — " + band.getName(); }
                default -> throw new IllegalArgumentException("Для группы можно выбрать только туры, музыкантов или треки.");
            }
        }
        Report report = report(tables);
        writePdf(title, report.tables(), destination.toPath());
        return report.recordCount();
    }

    private static Report report(List<PdfTable> tables) {
        return new Report(tables, tables.stream().mapToInt(t -> t.rows().size()).sum());
    }

    private static List<MusicBand> allBands(EntityManager em) {
        return em.createQuery("select b from MusicBand b order by b.name", MusicBand.class).getResultList();
    }
    private static List<Musician> allMusicians(EntityManager em) {
        return em.createQuery("select m from Musician m join fetch m.musicBand order by m.musicBand.name, m.surname, m.name", Musician.class).getResultList();
    }
    private static List<Track> allTracks(EntityManager em) {
        return em.createQuery("select t from Track t join fetch t.musicBand order by t.musicBand.name, t.name", Track.class).getResultList();
    }
    private static List<Tour> allTours(EntityManager em) {
        return em.createQuery("select distinct t from Tour t left join fetch t.cities join fetch t.musicBand order by t.id", Tour.class).getResultList();
    }
    private static List<City> allCities(EntityManager em) {
        return em.createQuery("select c from City c order by c.name", City.class).getResultList();
    }

    private static PdfTable bandsTable(List<MusicBand> items) {
        return table("Музыкальные группы", List.of("ID", "Название", "Год основания", "Позиция в чарте"), items,
                b -> List.of(s(b.getId()), s(b.getName()), s(b.getYear()), s(b.getTopPosition())));
    }
    private static PdfTable musiciansTable(List<Musician> items) {
        return table("Музыканты", List.of("ID", "Имя", "Фамилия", "Возраст", "Роль", "Группа"), items,
                m -> List.of(s(m.getId()), s(m.getName()), s(m.getSurname()), s(m.getAge()), value(m.getRole()), m.getMusicBand().getName()));
    }
    private static PdfTable tracksTable(List<Track> items) {
        return table("Треки", List.of("ID", "Название", "Длительность", "Жанр", "Группа"), items,
                t -> List.of(s(t.getTrackId()), s(t.getName()), s(t.getDuration()), s(t.getGenre()), t.getMusicBand().getName()));
    }
    private static PdfTable toursTable(List<Tour> items) {
        return table("Туры", List.of("ID", "Статус", "Выручка", "Группа", "Города"), items,
                t -> List.of(s(t.getId()), s(t.getStatus()), s(t.getRevenue()), t.getMusicBand().getName(), cityNames(t)));
    }
    private static PdfTable citiesTable(List<City> items) {
        return table("Города", List.of("ID", "Название"), items, c -> List.of(s(c.getId()), s(c.getName())));
    }
    private static <T> PdfTable table(String title, List<String> headers, List<T> items,
                                      Function<T, List<String>> rowMapper) {
        return new PdfTable(title, headers, items.stream().map(rowMapper).toList());
    }
    private static String cityNames(Tour tour) {
        String names = safe(tour.getCities()).stream().map(City::getName).collect(Collectors.joining(", "));
        return names.isBlank() ? "не указаны" : names;
    }
    private static String value(String value) { return value == null || value.isBlank() ? "не указана" : value; }
    private static String s(Object value) { return String.valueOf(value); }
    private static <T> List<T> safe(List<T> value) { return value == null ? List.of() : value; }

    private static void writePdf(String reportTitle, List<PdfTable> tables, Path destination) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDType0Font font;
            try (var input = Files.newInputStream(findCyrillicFont())) { font = PDType0Font.load(document, input); }
            TableCanvas canvas = new TableCanvas(document, font, reportTitle);
            canvas.startPage();
            for (PdfTable table : tables) canvas.drawTable(table);
            canvas.close();
            document.save(destination.toFile());
        }
    }

    /** Рисует сетку таблиц, переносит текст в ячейках и повторяет заголовок таблицы на новой странице. */
    private static final class TableCanvas {
        private static final float MARGIN = 34, FONT_SIZE = 8.5f, HEADER_SIZE = 9f, LEADING = 11f, PAD_X = 5f, PAD_Y = 4f;
        private final PDDocument document;
        private final PDType0Font font;
        private final String reportTitle;
        private PDPage page;
        private PDPageContentStream stream;
        private float y;
        private final float tableWidth = PDRectangle.A4.getHeight() - MARGIN * 2;
        private String activeTableTitle;
        private List<String> activeHeaders;
        private float[] activeWidths;
        private boolean repeatingHeader;

        private TableCanvas(PDDocument document, PDType0Font font, String reportTitle) {
            this.document = document; this.font = font; this.reportTitle = reportTitle;
        }

        private void startPage() throws IOException {
            page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - MARGIN;
            text("Music Manager — " + reportTitle, MARGIN, y, 15, 0.05f, 0.07f, 0.13f);
            y -= 27;
        }

        private void drawTable(PdfTable table) throws IOException {
            float[] widths = columnWidths(table);
            activeTableTitle = table.title();
            activeHeaders = table.headers();
            activeWidths = widths;
            drawSectionTitle(table.title());
            drawRow(table.headers(), widths, true, HEADER_SIZE);
            if (table.rows().isEmpty()) drawRow(emptyRow(table.headers().size()), widths, false, FONT_SIZE);
            else for (List<String> row : table.rows()) drawRow(row, widths, false, FONT_SIZE);
            y -= 17;
        }

        private List<String> emptyRow(int columnCount) {
            List<String> values = new ArrayList<>();
            values.add("Записей нет");
            while (values.size() < columnCount) values.add("");
            return values;
        }

        private void drawSectionTitle(String title) throws IOException {
            if (y < MARGIN + 38) newPage();
            y -= 3;
            text(title, MARGIN, y, 11, 0.35f, 0.12f, 0.60f);
            y -= 15;
        }

        private void drawRow(List<String> values, float[] widths, boolean header, float fontSize) throws IOException {
            List<List<String>> wrapped = new ArrayList<>();
            float height = 0;
            for (int i = 0; i < widths.length; i++) {
                String value = i < values.size() ? values.get(i) : "";
                List<String> cellLines = wrapCell(value, widths[i] - PAD_X * 2, fontSize);
                wrapped.add(cellLines);
                height = Math.max(height, cellLines.size() * LEADING + PAD_Y * 2);
            }
            if (y - height < MARGIN) {
                newPage();
                if (!header && !repeatingHeader) {
                    drawSectionTitle(activeTableTitle);
                    repeatingHeader = true;
                    drawRow(activeHeaders, activeWidths, true, HEADER_SIZE);
                    repeatingHeader = false;
                }
            }
            float x = MARGIN;
            for (int i = 0; i < widths.length; i++) {
                float width = widths[i];
                stream.setNonStrokingColor(header ? 0.25f : 0.96f, header ? 0.12f : 0.97f, header ? 0.43f : 0.98f);
                stream.addRect(x, y - height, width, height); stream.fill();
                stream.setStrokingColor(0.72f, 0.74f, 0.80f); stream.setLineWidth(0.45f);
                stream.addRect(x, y - height, width, height); stream.stroke();
                float textY = y - PAD_Y - fontSize;
                for (String line : wrapped.get(i)) {
                    text(line, x + PAD_X, textY, fontSize, header ? 1f : 0.10f, header ? 1f : 0.12f, header ? 1f : 0.16f);
                    textY -= LEADING;
                }
                x += width;
            }
            y -= height;
        }

        private void newPage() throws IOException {
            stream.close();
            startPage();
        }

        private void text(String value, float x, float baseline, float size, float r, float g, float b) throws IOException {
            stream.beginText(); stream.setFont(font, size); stream.setNonStrokingColor(r, g, b);
            stream.newLineAtOffset(x, baseline); stream.showText(value); stream.endText();
        }

        private float[] columnWidths(PdfTable table) throws IOException {
            float[] weights = new float[table.headers().size()];
            float sum = 0;
            for (int col = 0; col < weights.length; col++) {
                float max = Math.min(27, font.getStringWidth(table.headers().get(col)) / 1000f);
                for (List<String> row : table.rows()) {
                    if (col < row.size()) max = Math.max(max, Math.min(27, font.getStringWidth(row.get(col)) / 1000f));
                }
                weights[col] = Math.max(8, max + 3);
                sum += weights[col];
            }
            for (int col = 0; col < weights.length; col++) weights[col] = tableWidth * weights[col] / sum;
            return weights;
        }

        private List<String> wrapCell(String value, float maxWidth, float fontSize) throws IOException {
            List<String> result = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String word : value.split(" ")) {
                if (font.getStringWidth(word) * fontSize / 1000f > maxWidth) {
                    if (line.length() > 0) { result.add(line.toString()); line.setLength(0); }
                    StringBuilder fragment = new StringBuilder();
                    for (int offset = 0; offset < word.length(); offset++) {
                        String candidate = fragment.toString() + word.charAt(offset);
                        if (fragment.length() > 0 && font.getStringWidth(candidate) * fontSize / 1000f > maxWidth) {
                            result.add(fragment.toString()); fragment.setLength(0);
                        }
                        fragment.append(word.charAt(offset));
                    }
                    if (fragment.length() > 0) result.add(fragment.toString());
                    continue;
                }
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (line.length() > 0 && font.getStringWidth(candidate) * fontSize / 1000f > maxWidth) {
                    result.add(line.toString()); line.setLength(0); line.append(word);
                } else {
                    line.setLength(0); line.append(candidate);
                }
            }
            if (line.length() > 0) result.add(line.toString());
            return result.isEmpty() ? List.of("") : result;
        }

        private void close() throws IOException { if (stream != null) stream.close(); }
    }

    private static Path findCyrillicFont() throws IOException {
        List<Path> candidates = List.of(Path.of("C:/Windows/Fonts/arial.ttf"), Path.of("C:/Windows/Fonts/calibri.ttf"),
                Path.of("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"), Path.of("/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf"));
        return candidates.stream().filter(Files::isRegularFile).findFirst()
                .orElseThrow(() -> new IOException("Не найден шрифт с поддержкой кириллицы (например, Arial)."));
    }
}
