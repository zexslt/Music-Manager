package musicmanager.app.database;

import musicmanager.app.models.City;
import musicmanager.app.models.MusicBand;
import musicmanager.app.models.Musician;
import musicmanager.app.models.Track;
import musicmanager.app.models.Tour;
import org.hibernate.type.StringType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.persistence.EntityManager;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Exports and imports a versioned XML snapshot of the music manager data. */
public final class XmlBackupService {
    private XmlBackupService() { }

    public static ExportCounts exportToXml(EntityManager manager, File file) throws Exception {
        List<MusicBand> bands = manager.createQuery("SELECT b FROM MusicBand b ORDER BY b.id", MusicBand.class).getResultList();
        List<City> cities = manager.createQuery("SELECT c FROM City c ORDER BY c.id", City.class).getResultList();
        for (MusicBand band : bands) {
            band.getMusicians().size();
            band.getTracks().size();
            band.getTours().size();
            band.getTours().forEach(tour -> tour.getCities().size());
        }

        Document document = secureDocumentFactory().newDocumentBuilder().newDocument();
        Element root = document.createElement("musicManagerData");
        root.setAttribute("version", "1");
        document.appendChild(root);

        Element bandsElement = append(document, root, "musicBands");
        int musicianCount = 0;
        int trackCount = 0;
        int tourCount = 0;
        for (MusicBand band : bands) {
            Element bandElement = append(document, bandsElement, "musicBand");
            bandElement.setAttribute("id", Integer.toString(band.getId()));
            text(document, bandElement, "name", band.getName());
            text(document, bandElement, "year", Integer.toString(band.getYear()));
            text(document, bandElement, "topPosition", Integer.toString(band.getTopPosition()));

            Element musicians = append(document, bandElement, "musicians");
            for (Musician musician : band.getMusicians()) {
                Element item = append(document, musicians, "musician");
                item.setAttribute("id", Integer.toString(musician.getId()));
                text(document, item, "name", musician.getName());
                text(document, item, "surname", musician.getSurname());
                text(document, item, "age", Integer.toString(musician.getAge()));
                text(document, item, "role", musician.getRole());
                musicianCount++;
            }

            Element tracks = append(document, bandElement, "tracks");
            for (Track track : band.getTracks()) {
                Element item = append(document, tracks, "track");
                item.setAttribute("id", Integer.toString(track.getTrackId()));
                text(document, item, "name", track.getName());
                text(document, item, "duration", track.getDuration());
                text(document, item, "genre", track.getGenre());
                trackCount++;
            }

            Element tours = append(document, bandElement, "tours");
            for (Tour tour : band.getTours()) {
                Element item = append(document, tours, "tour");
                item.setAttribute("id", Integer.toString(tour.getId()));
                text(document, item, "status", tour.getStatus());
                text(document, item, "revenue", Integer.toString(tour.getRevenue()));
                Element cityRefs = append(document, item, "cityRefs");
                for (City city : tour.getCities()) {
                    Element ref = append(document, cityRefs, "cityRef");
                    ref.setAttribute("id", Integer.toString(city.getId()));
                }
                tourCount++;
            }
        }

        Element citiesElement = append(document, root, "cities");
        for (City city : cities) {
            Element item = append(document, citiesElement, "city");
            item.setAttribute("id", Integer.toString(city.getId()));
            text(document, item, "name", city.getName());
        }

        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        var transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(new DOMSource(document), new StreamResult(file));
        return new ExportCounts(bands.size(), musicianCount, trackCount, tourCount, cities.size());
    }

    public static ImportCounts importFromXml(EntityManager manager, File file) throws Exception {
        Snapshot snapshot = parse(file);
        validate(snapshot);

        Map<Integer, Integer> cityIdMap = new HashMap<>();
        for (CityData cityData : snapshot.cities()) {
            City existingByName = findCityByName(manager, cityData.name());
            if (existingByName != null) {
                cityIdMap.put(cityData.id(), existingByName.getId());
                continue;
            }

            City occupiedId = manager.find(City.class, cityData.id());
            if (occupiedId == null) {
                City city = new City();
                city.setId(cityData.id());
                city.setName(cityData.name());
                manager.persist(city);
                manager.flush();
                cityIdMap.put(cityData.id(), cityData.id());
            } else {
                City resolved = CityService.findOrCreate(manager, cityData.name());
                cityIdMap.put(cityData.id(), resolved.getId());
            }
        }

        for (BandData band : snapshot.bands()) {
            upsertBand(manager, band);
        }
        for (BandData band : snapshot.bands()) {
            for (MusicianData musician : band.musicians()) upsertMusician(manager, band.id(), musician);
            for (TrackData track : band.tracks()) upsertTrack(manager, band.id(), track);
            for (TourData tour : band.tours()) {
                upsertTour(manager, band.id(), tour);
                manager.createNativeQuery("DELETE FROM tour_city WHERE tour_id = :tourId")
                        .setParameter("tourId", tour.id()).executeUpdate();
                Set<Integer> linkedIds = new LinkedHashSet<>();
                for (int sourceCityId : tour.cityIds()) {
                    Integer actualId = cityIdMap.get(sourceCityId);
                    if (actualId != null) linkedIds.add(actualId);
                }
                for (int cityId : linkedIds) {
                    manager.createNativeQuery("INSERT INTO tour_city (tour_id, city_id) VALUES (:tourId, :cityId)")
                            .setParameter("tourId", tour.id())
                            .setParameter("cityId", cityId)
                            .executeUpdate();
                }
            }
        }
        return new ImportCounts(snapshot.bands().size(),
                snapshot.bands().stream().mapToInt(b -> b.musicians().size()).sum(),
                snapshot.bands().stream().mapToInt(b -> b.tracks().size()).sum(),
                snapshot.bands().stream().mapToInt(b -> b.tours().size()).sum(), snapshot.cities().size());
    }

    private static void upsertBand(EntityManager manager, BandData band) {
        manager.createNativeQuery("INSERT INTO musicband (musicband_id, musicband_name, musicband_year, musicband_topposition) " +
                        "VALUES (:id, :name, :year, :topPosition) ON DUPLICATE KEY UPDATE " +
                        "musicband_name = VALUES(musicband_name), musicband_year = VALUES(musicband_year), " +
                        "musicband_topposition = VALUES(musicband_topposition)")
                .setParameter("id", band.id()).setParameter("name", band.name()).setParameter("year", band.year())
                .setParameter("topPosition", band.topPosition()).executeUpdate();
    }

    private static void upsertMusician(EntityManager manager, int bandId, MusicianData musician) {
        org.hibernate.query.NativeQuery<?> query = manager.createNativeQuery("INSERT INTO musician " +
                        "(musician_id, musician_name, musician_surname, musician_age, musician_role, musicband_id) " +
                        "VALUES (:id, :name, :surname, :age, :role, :bandId) ON DUPLICATE KEY UPDATE " +
                        "musician_name = VALUES(musician_name), musician_surname = VALUES(musician_surname), " +
                        "musician_age = VALUES(musician_age), musician_role = VALUES(musician_role), musicband_id = VALUES(musicband_id)")
                .unwrap(org.hibernate.query.NativeQuery.class);
        query.setParameter("id", musician.id()).setParameter("name", musician.name())
                .setParameter("surname", musician.surname()).setParameter("age", musician.age())
                .setParameter("role", musician.role(), StringType.INSTANCE).setParameter("bandId", bandId).executeUpdate();
    }

    private static void upsertTrack(EntityManager manager, int bandId, TrackData track) {
        manager.createNativeQuery("INSERT INTO track (track_id, track_name, track_duration, track_genre, musicband_id) " +
                        "VALUES (:id, :name, :duration, :genre, :bandId) ON DUPLICATE KEY UPDATE " +
                        "track_name = VALUES(track_name), track_duration = VALUES(track_duration), " +
                        "track_genre = VALUES(track_genre), musicband_id = VALUES(musicband_id)")
                .setParameter("id", track.id()).setParameter("name", track.name()).setParameter("duration", track.duration())
                .setParameter("genre", track.genre()).setParameter("bandId", bandId).executeUpdate();
    }

    private static void upsertTour(EntityManager manager, int bandId, TourData tour) {
        manager.createNativeQuery("INSERT INTO tour (tour_id, tour_status, tour_revenue, musicband_id) " +
                        "VALUES (:id, :status, :revenue, :bandId) ON DUPLICATE KEY UPDATE " +
                        "tour_status = VALUES(tour_status), tour_revenue = VALUES(tour_revenue), musicband_id = VALUES(musicband_id)")
                .setParameter("id", tour.id()).setParameter("status", tour.status()).setParameter("revenue", tour.revenue())
                .setParameter("bandId", bandId).executeUpdate();
    }

    private static City findCityByName(EntityManager manager, String name) {
        List<City> cities = manager.createQuery("SELECT c FROM City c WHERE c.name = :name", City.class)
                .setParameter("name", name).setMaxResults(1).getResultList();
        return cities.isEmpty() ? null : cities.get(0);
    }

    private static Snapshot parse(File file) throws Exception {
        Document document = secureDocumentFactory().newDocumentBuilder().parse(file);
        Element root = document.getDocumentElement();
        if (!"musicManagerData".equals(root.getTagName()) || !"1".equals(root.getAttribute("version"))) {
            throw new IllegalArgumentException("Ожидается файл <musicManagerData version=\"1\">.");
        }
        requireOnlyElementChildren(root, Set.of("musicBands", "cities"));
        Element bandsElement = oneChild(root, "musicBands", true);
        Element citiesElement = oneChild(root, "cities", true);

        List<BandData> bands = new ArrayList<>();
        for (Element band : elementChildren(bandsElement, "musicBand")) bands.add(parseBand(band));
        List<CityData> cities = new ArrayList<>();
        for (Element city : elementChildren(citiesElement, "city")) {
            requireOnlyElementChildren(city, Set.of("name"));
            cities.add(new CityData(idAttribute(city), requiredText(city, "name")));
        }
        return new Snapshot(bands, cities);
    }

    private static BandData parseBand(Element element) {
        requireOnlyElementChildren(element, Set.of("name", "year", "topPosition", "musicians", "tracks", "tours"));
        int id = idAttribute(element);
        String name = requiredText(element, "name");
        int year = nonNegativeInt(requiredText(element, "year"), "year");
        int topPosition = nonNegativeInt(requiredText(element, "topPosition"), "topPosition");

        Element musiciansElement = oneChild(element, "musicians", true);
        List<MusicianData> musicians = new ArrayList<>();
        for (Element musician : elementChildren(musiciansElement, "musician")) {
            requireOnlyElementChildren(musician, Set.of("name", "surname", "age", "role"));
            String role = optionalText(musician, "role");
            musicians.add(new MusicianData(idAttribute(musician), requiredText(musician, "name"),
                    requiredText(musician, "surname"), nonNegativeInt(requiredText(musician, "age"), "age"), role));
        }

        Element tracksElement = oneChild(element, "tracks", true);
        List<TrackData> tracks = new ArrayList<>();
        for (Element track : elementChildren(tracksElement, "track")) {
            requireOnlyElementChildren(track, Set.of("name", "duration", "genre"));
            tracks.add(new TrackData(idAttribute(track), requiredText(track, "name"),
                    requiredText(track, "duration"), requiredText(track, "genre")));
        }

        Element toursElement = oneChild(element, "tours", true);
        List<TourData> tours = new ArrayList<>();
        for (Element tour : elementChildren(toursElement, "tour")) {
            requireOnlyElementChildren(tour, Set.of("status", "revenue", "cityRefs"));
            Element refs = oneChild(tour, "cityRefs", true);
            List<Integer> cityIds = new ArrayList<>();
            for (Element ref : elementChildren(refs, "cityRef")) {
                requireOnlyElementChildren(ref, Set.of());
                cityIds.add(idAttribute(ref));
            }
            tours.add(new TourData(idAttribute(tour), requiredText(tour, "status"),
                    nonNegativeInt(requiredText(tour, "revenue"), "revenue"), cityIds));
        }
        return new BandData(id, name, year, topPosition, musicians, tracks, tours);
    }

    private static void validate(Snapshot snapshot) {
        Set<Integer> bandIds = new HashSet<>();
        Set<Integer> musicianIds = new HashSet<>();
        Set<Integer> trackIds = new HashSet<>();
        Set<Integer> tourIds = new HashSet<>();
        Set<Integer> cityIds = new HashSet<>();
        Set<String> cityNames = new HashSet<>();

        for (BandData band : snapshot.bands()) {
            unique(bandIds, band.id(), "musicBand");
            length(band.name(), 45, "Название группы");
            for (MusicianData musician : band.musicians()) {
                unique(musicianIds, musician.id(), "musician");
                length(musician.name(), 45, "Имя музыканта");
                length(musician.surname(), 100, "Фамилия музыканта");
                if (musician.age() > 120) throw new IllegalArgumentException("Возраст музыканта должен быть от 0 до 120 лет.");
                if (musician.role() != null) length(musician.role(), 45, "Роль музыканта");
            }
            for (TrackData track : band.tracks()) {
                unique(trackIds, track.id(), "track");
                length(track.name(), 45, "Название трека");
                length(track.duration(), 10, "Длительность трека");
                length(track.genre(), 45, "Жанр трека");
            }
            for (TourData tour : band.tours()) {
                unique(tourIds, tour.id(), "tour");
                length(tour.status(), 45, "Статус тура");
                if (new HashSet<>(tour.cityIds()).size() != tour.cityIds().size()) {
                    throw new IllegalArgumentException("В туре с ID " + tour.id() + " повторяется ссылка на город.");
                }
            }
        }
        for (CityData city : snapshot.cities()) {
            unique(cityIds, city.id(), "city");
            length(city.name(), 45, "Название города");
            if (!cityNames.add(city.name().toLowerCase(java.util.Locale.ROOT))) {
                throw new IllegalArgumentException("В XML повторяется название города: " + city.name());
            }
        }
        for (BandData band : snapshot.bands()) {
            for (TourData tour : band.tours()) {
                for (int cityId : tour.cityIds()) {
                    if (!cityIds.contains(cityId)) throw new IllegalArgumentException("В туре " + tour.id() + " указан отсутствующий city_id=" + cityId + ".");
                }
            }
        }
    }

    private static DocumentBuilderFactory secureDocumentFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }

    private static Element append(Document document, Element parent, String name) {
        Element child = document.createElement(name);
        parent.appendChild(child);
        return child;
    }

    private static void text(Document document, Element parent, String name, String value) {
        Element child = append(document, parent, name);
        child.setTextContent(value == null ? "" : value);
    }

    private static List<Element> elementChildren(Element parent, String expectedName) {
        List<Element> result = new ArrayList<>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if (!expectedName.equals(node.getNodeName())) throw new IllegalArgumentException("Неожиданный элемент <" + node.getNodeName() + ">.");
                result.add((Element) node);
            }
        }
        return result;
    }

    private static void requireOnlyElementChildren(Element parent, Set<String> allowedNames) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() == Node.ELEMENT_NODE && !allowedNames.contains(node.getNodeName())) {
                throw new IllegalArgumentException("Неожиданный элемент <" + node.getNodeName() + "> внутри <" + parent.getTagName() + ">.");
            }
        }
    }

    private static Element oneChild(Element parent, String name, boolean required) {
        Element result = null;
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() == Node.ELEMENT_NODE && name.equals(node.getNodeName())) {
                if (result != null) throw new IllegalArgumentException("Элемент <" + name + "> указан больше одного раза.");
                result = (Element) node;
            }
        }
        if (required && result == null) throw new IllegalArgumentException("Отсутствует обязательный элемент <" + name + ">.");
        return result;
    }

    private static String requiredText(Element parent, String name) {
        Element child = oneChild(parent, name, true);
        requireOnlyElementChildren(child, Set.of());
        String value = child.getTextContent().trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Поле <" + name + "> не может быть пустым.");
        return value;
    }

    private static String optionalText(Element parent, String name) {
        Element child = oneChild(parent, name, false);
        if (child == null) return null;
        requireOnlyElementChildren(child, Set.of());
        String value = child.getTextContent().trim();
        return value.isEmpty() ? null : value;
    }

    private static int idAttribute(Element element) {
        if (!element.hasAttribute("id")) throw new IllegalArgumentException("У элемента <" + element.getTagName() + "> отсутствует атрибут id.");
        return positiveInt(element.getAttribute("id"), "id");
    }

    private static int positiveInt(String text, String field) {
        int value = nonNegativeInt(text, field);
        if (value == 0) throw new IllegalArgumentException("Поле " + field + " должно быть больше нуля.");
        return value;
    }

    private static int nonNegativeInt(String text, String field) {
        try {
            int value = Integer.parseInt(text.trim());
            if (value < 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Поле " + field + " должно быть целым неотрицательным числом.");
        }
    }

    private static void unique(Set<Integer> values, int id, String entity) {
        if (!values.add(id)) throw new IllegalArgumentException("В XML повторяется ID " + id + " для " + entity + ".");
    }

    private static void length(String value, int max, String field) {
        if (value == null || value.isEmpty() || value.length() > max) {
            throw new IllegalArgumentException(field + " должно содержать от 1 до " + max + " символов.");
        }
    }

    public record ExportCounts(int bands, int musicians, int tracks, int tours, int cities) { }
    public record ImportCounts(int bands, int musicians, int tracks, int tours, int cities) { }
    private record Snapshot(List<BandData> bands, List<CityData> cities) { }
    private record BandData(int id, String name, int year, int topPosition, List<MusicianData> musicians, List<TrackData> tracks, List<TourData> tours) { }
    private record MusicianData(int id, String name, String surname, int age, String role) { }
    private record TrackData(int id, String name, String duration, String genre) { }
    private record TourData(int id, String status, int revenue, List<Integer> cityIds) { }
    private record CityData(int id, String name) { }
}
