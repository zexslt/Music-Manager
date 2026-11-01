package musicmanager.app.database;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/** Provides EntityManagers backed by the application's configured persistence unit. */
public final class JpaSession {
    private static final String PERSISTENCE_UNIT = "test_persistence";
    private static EntityManagerFactory factory;

    private JpaSession() { }

    public static synchronized EntityManager createEntityManager() {
        if (factory == null || !factory.isOpen()) {
            factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
        }
        return factory.createEntityManager();
    }

    public static synchronized void close() {
        if (factory != null && factory.isOpen()) {
            factory.close();
            factory = null;
        }
    }
}
