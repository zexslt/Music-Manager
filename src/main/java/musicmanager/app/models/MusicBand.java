package musicmanager.app.models;
import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "musicband")
public class MusicBand {
    private int id;
    private String name;
    private int year;
    private int topPosition;

    private List<Musician> musicians;
    private List<Track> tracks;
    private List<Tour> tours;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "musicband_id")
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "musicband_name")
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "musicband_year")
    public int getYear() {
        return year;
    }
    public void setYear(int year) {
        this.year = year;
    }

    @Column(name = "musicband_topposition")
    public int getTopPosition() {
        return topPosition;
    }
    public void setTopPosition(int topPosition) {
        this.topPosition = topPosition;
    }



    @Transient
    public List<Track> getTracks() {
        return tracks;
    }
    public void setTracks(List<Track> tracks) {
        this.tracks = tracks;
    }

    @Transient
    public List<Musician> getMusicians() {
        return musicians;
    }
    public void setMusicians(List<Musician> musicians) {
        this.musicians = musicians;
    }

    @Transient
    public List<Tour> getTours() {
        return tours;
    }
    public void setTours(List<Tour> tours) {
        this.tours = tours;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Группа: ").append(name).append("\n");
        sb.append("Год основания: ").append(year).append("\n");
        sb.append("Позиция в чарте: ").append(topPosition).append("\n");

        sb.append("Участники (").append(musicians != null ? musicians.size() : 0).append("):\n");
        if (musicians != null && !musicians.isEmpty()) {
            for (Musician m : musicians) {
                sb.append("  - ").append(m).append("\n");
            }
        } else {
            sb.append("  (список пуст)\n");
        }

        sb.append("Треки (").append(tracks != null ? tracks.size() : 0).append("):\n");
        if (tracks != null && !tracks.isEmpty()) {
            for (Track t : tracks) {
                sb.append("  - ").append(t).append("\n");
            }
        } else {
            sb.append("  (список пуст)\n");
        }

        sb.append("Туры (").append(tours != null ? tours.size() : 0).append("):\n");
        if (tours != null && !tours.isEmpty()) {
            for (Tour tour : tours) {
                sb.append("  - ").append(tour).append("\n");
            }
        } else {
            sb.append("  (список пуст)\n");
        }

        return sb.toString();
    }
}

