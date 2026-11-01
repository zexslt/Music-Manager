package musicmanager.app.models;

import javax.persistence.*;

@Entity
@Table(name = "track")
public class Track {
    private int trackId;
    private String name;
    private String duration;
    private String genre;
    private MusicBand musicBand;

    public Track() { }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "track_id")
    public int getTrackId() { return trackId; }
    public void setTrackId(int trackId) { this.trackId = trackId; }

    @Column(name = "track_name", nullable = false, length = 45)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Column(name = "track_duration", nullable = false, length = 10)
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    @Column(name = "track_genre", nullable = false, length = 45)
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "musicband_id", nullable = false)
    public MusicBand getMusicBand() { return musicBand; }
    public void setMusicBand(MusicBand musicBand) { this.musicBand = musicBand; }

    @Override
    public String toString() { return name + " (" + genre + ") — " + duration; }
}
