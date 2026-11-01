package musicmanager.app.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "city")
public class City {
    private int id;
    private String name;

    public City() { }

    @Id
    @Column(name = "city_id")
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    @Column(name = "city_name", nullable = false, unique = true, length = 45)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() { return name; }
}
