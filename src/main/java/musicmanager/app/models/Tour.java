package musicmanager.app.models;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "tour")
public class Tour {
    private int id;
    private List<City> cities = new ArrayList<>();
    private String status;
    private int revenue;
    private MusicBand musicBand;

    public Tour() { }

    public Tour(List<City> cities, String status, int revenue) {
        setCities(cities);
        setStatus(status);
        setRevenue(revenue);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tour_id")
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "tour_city",
            joinColumns = @JoinColumn(name = "tour_id"),
            inverseJoinColumns = @JoinColumn(name = "city_id"))
    public List<City> getCities() { return cities; }
    public void setCities(List<City> cities) { this.cities = cities == null ? new ArrayList<>() : cities; }

    @Column(name = "tour_status", nullable = false, length = 45)
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Column(name = "tour_revenue", nullable = false)
    public int getRevenue() { return revenue; }
    public void setRevenue(int revenue) { this.revenue = revenue; }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "musicband_id", nullable = false)
    public MusicBand getMusicBand() { return musicBand; }
    public void setMusicBand(MusicBand musicBand) { this.musicBand = musicBand; }

    @Override
    public String toString() {
        String cityNames = cities == null || cities.isEmpty()
                ? "список городов пуст"
                : cities.stream().map(City::getName).collect(Collectors.joining(", "));
        return "Статус тура: " + (status == null ? "не указан" : status)
                + "\nВыручка: " + revenue + " $.\nМаршрут (города): " + cityNames;
    }
}
