package musicmanager.app.database;

import musicmanager.app.models.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import javax.persistence.*;
import java.util.Properties;

class ServiceTestSupport {
    static EntityManagerFactory factory;
    EntityManager em;
    @BeforeEach void openDb() {
        if (factory == null || !factory.isOpen()) {
            Properties p = new Properties();
            p.setProperty("javax.persistence.jdbc.driver", "org.h2.Driver");
            p.setProperty("javax.persistence.jdbc.url", "jdbc:h2:mem:mmtest;MODE=MySQL;DB_CLOSE_DELAY=-1");
            p.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
            p.setProperty("hibernate.hbm2ddl.auto", "create-drop");
            p.setProperty("hibernate.show_sql", "false");
            factory = Persistence.createEntityManagerFactory("test_persistence", p);
        }
        em = factory.createEntityManager(); em.getTransaction().begin();
    }
    @AfterEach void closeDb() { if (em != null && em.isOpen()) { if (em.getTransaction().isActive()) em.getTransaction().rollback(); em.close(); } }
    static MusicBand band(String name) {
        MusicBand b = new MusicBand(); b.setName(name); b.setYear(2000); b.setTopPosition(1); return b;
    }
    static int count(Class<?> type) { return factory.createEntityManager().createQuery("select count(x) from " + type.getSimpleName() + " x", Long.class).getSingleResult().intValue(); }
}


