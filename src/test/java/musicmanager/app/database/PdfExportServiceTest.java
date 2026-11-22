package musicmanager.app.database;

import musicmanager.app.models.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfExportServiceTest extends ServiceTestSupport {
    @TempDir Path temp;
    private void sample() {
        MusicBand b = band("The Testers");
        Musician m = new Musician(); m.setName("Ada"); m.setSurname("Lovelace"); m.setAge(36); m.setRole("Keys"); m.setMusicBand(b);
        Track t = new Track(); t.setName("Analytical Engine"); t.setDuration("3:10"); t.setGenre("Rock"); t.setMusicBand(b);
        City c = new City(); c.setId(1); c.setName("London"); em.persist(c);
        Tour tour = new Tour(List.of(c), "Planned", 100); tour.setMusicBand(b);
        b.setMusicians(List.of(m)); b.setTracks(List.of(t)); b.setTours(List.of(tour)); em.persist(b); em.flush();
    }
    private String pdf(File f) throws Exception { try (var doc = Loader.loadPDF(f)) { assertTrue(doc.getNumberOfPages() > 0); return new PDFTextStripper().getText(doc); } }
    @Test void exportsAllSectionsAndCounts() throws Exception {
        sample(); File f = temp.resolve("all.pdf").toFile();
        assertEquals(5, PdfExportService.export(em, PdfExportService.Selection.ALL, f));
        String text = pdf(f); for (String x : List.of("The Testers", "Ada", "Analytical Engine", "Planned", "London")) assertTrue(text.contains(x), x);
        assertEquals(4, PdfExportService.export(em, PdfExportService.Selection.BANDS, temp.resolve("bands.pdf").toFile()));
        for (var s : List.of(PdfExportService.Selection.TOURS, PdfExportService.Selection.MUSICIANS, PdfExportService.Selection.TRACKS, PdfExportService.Selection.CITIES))
            assertEquals(1, PdfExportService.export(em, s, temp.resolve(s.name()+".pdf").toFile()));
    }
    @Test void exportsBandAndIndividualSections() throws Exception {
        sample(); int id = em.createQuery("select b.id from MusicBand b", Integer.class).getSingleResult();
        assertEquals(4, PdfExportService.exportBand(em, id, temp.resolve("band.pdf").toFile()));
        assertEquals(1, PdfExportService.exportBand(em, id, PdfExportService.Selection.MUSICIANS, temp.resolve("mus.pdf").toFile()));
        assertTrue(pdf(temp.resolve("mus.pdf").toFile()).contains("Ada"));
        assertEquals(1, PdfExportService.exportBand(em, id, PdfExportService.Selection.TRACKS, temp.resolve("track.pdf").toFile()));
        assertEquals(1, PdfExportService.exportBand(em, id, PdfExportService.Selection.TOURS, temp.resolve("tour.pdf").toFile()));
        assertThrows(IllegalArgumentException.class, () -> PdfExportService.exportBand(em, id, PdfExportService.Selection.CITIES, temp.resolve("bad.pdf").toFile()));
        assertThrows(IllegalArgumentException.class, () -> PdfExportService.exportBand(em, id + 100, temp.resolve("missing.pdf").toFile()));
    }
    @Test void emptyAndUnwritableDestinations() throws Exception {
        assertEquals(0, PdfExportService.export(em, PdfExportService.Selection.ALL, temp.resolve("empty.pdf").toFile()));
        assertTrue(pdf(temp.resolve("empty.pdf").toFile()).contains("Записей нет"));
        assertThrows(Exception.class, () -> PdfExportService.export(em, PdfExportService.Selection.ALL, temp.resolve("absent").resolve("x.pdf").toFile()));
    }
}



