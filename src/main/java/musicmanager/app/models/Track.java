package musicmanager.app.models;
import javax.persistence.*;

public class Track {
    private int track_id;
    private String name;
    private String duration;
    private String genre;


    public int getTrackId() {
        return track_id;
    }
    public void setTrackId(int track_id){
        this.track_id = track_id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name){
        this.name = name;
    }

    public String getDuration() {
        return duration;
    }
    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getGenre() {
        return genre;
    }
    public void setGenre(String genre) {
        this.genre = genre;
    }

    // Print Info
    @Override
    public String toString() {
        return name + " (" + genre + ") — " + duration;
    }
}


