package musicmanager.app.database;

import musicmanager.app.models.City;

import javax.persistence.EntityManager;
import java.util.List;

/** Helpers for reusing cities and assigning IDs in the existing non-AUTO_INCREMENT schema. */
public final class CityService {
    private CityService() { }

    public static City findOrCreate(EntityManager manager, String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) throw new IllegalArgumentException("Название города не может быть пустым.");
        if (name.length() > 45) throw new IllegalArgumentException("Название города не должно превышать 45 символов.");

        List<City> found = manager.createQuery("SELECT c FROM City c WHERE c.name = :name", City.class)
                .setParameter("name", name)
                .setMaxResults(1)
                .getResultList();
        if (!found.isEmpty()) return found.get(0);

        Number nextId = (Number) manager.createNativeQuery("SELECT COALESCE(MAX(city_id), 0) + 1 FROM city")
                .getSingleResult();
        City city = new City();
        city.setId(nextId.intValue());
        city.setName(name);
        manager.persist(city);
        manager.flush();
        return city;
    }
}
