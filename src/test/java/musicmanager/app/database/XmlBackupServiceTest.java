package musicmanager.app.database;

import musicmanager.app.models.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class XmlBackupServiceTest extends ServiceTestSupport {
    @TempDir Path temp;
    private void sample() {
        City c = new City(); c.setId(1); c.setName("Moscow"); em.persist(c);
        MusicBand b = band("Backup Band");
        Musician m = new Musician(); m.setName("Nina"); m.setSurname("Singer"); m.setAge(28); m.setRole("Voice"); m.setMusicBand(b);
        Track t = new Track(); t.setName("Song"); t.setDuration("2:30"); t.setGenre("Pop"); t.setMusicBand(b);
        Tour tour = new Tour(List.of(c), "Done", 500); tour.setMusicBand(b);
        b.setMusicians(List.of(m)); b.setTracks(List.of(t)); b.setTours(List.of(tour)); em.persist(b); em.flush();
    }
    @Test void exportsAndRestoresRelationsAndCounts() throws Exception {
        sample(); var file = temp.resolve("backup.xml").toFile();
        assertEquals(new XmlBackupService.ExportCounts(1,1,1,1,1), XmlBackupService.exportToXml(em, file));
        em.createNativeQuery("DELETE FROM tour_city").executeUpdate();
        em.createQuery("delete from Tour").executeUpdate(); em.createQuery("delete from Musician").executeUpdate();
        em.createQuery("delete from Track").executeUpdate(); em.createQuery("delete from MusicBand").executeUpdate(); em.createQuery("delete from City").executeUpdate(); em.flush(); em.clear();
        assertEquals(new XmlBackupService.ImportCounts(1,1,1,1,1), XmlBackupService.importFromXml(em, file));
        em.clear();
        MusicBand restored = em.createQuery("select b from MusicBand b", MusicBand.class).getSingleResult();
        assertEquals("Backup Band", restored.getName()); assertEquals("Nina", restored.getMusicians().get(0).getName());
        assertEquals("Song", restored.getTracks().get(0).getName());
        assertEquals(List.of("Moscow"), restored.getTours().get(0).getCities().stream().map(City::getName).toList());
    }
    @Test void emptyAndMalformedOrMissingXml() throws Exception {
        var empty = temp.resolve("empty.xml").toFile();
        assertEquals(new XmlBackupService.ExportCounts(0,0,0,0,0), XmlBackupService.exportToXml(em, empty));
        assertEquals(new XmlBackupService.ImportCounts(0,0,0,0,0), XmlBackupService.importFromXml(em, empty));
        var malformed = temp.resolve("bad.xml"); java.nio.file.Files.writeString(malformed, "<musicManagerData version=\"2\"><musicBands/><cities/></musicManagerData>");
        assertThrows(Exception.class, () -> XmlBackupService.importFromXml(em, malformed.toFile()));
        assertThrows(Exception.class, () -> XmlBackupService.importFromXml(em, temp.resolve("missing.xml").toFile()));
    }
    @Test void rejectsXmlValidationViolations() throws Exception {
        String[] invalid = {
          "<musicManagerData version=\"1\"><musicBands><musicBand id=\"0\"/></musicBands><cities/></musicManagerData>",
          "<musicManagerData version=\"1\"><musicBands><musicBand id=\"1\"><name>B</name><year>-1</year><topPosition>0</topPosition><musicians/><tracks/><tours/></musicBand></musicBands><cities/></musicManagerData>",
          "<musicManagerData version=\"1\"><musicBands><musicBand id=\"1\"><name>B</name><year>1</year><topPosition>1</topPosition><musicians><musician id=\"1\"><name>N</name><surname>S</surname><age>121</age></musician></musicians><tracks/><tours/></musicBand></musicBands><cities/></musicManagerData>"
        };
        for (int i=0;i<invalid.length;i++) { var f=temp.resolve("invalid"+i+".xml"); java.nio.file.Files.writeString(f, invalid[i]); assertThrows(Exception.class, () -> XmlBackupService.importFromXml(em, f.toFile())); }
    }
}

