package musicmanager.app.models;

import musicmanager.app.exceptions.InvalidMusicDataException;

import javax.persistence.*;

@Entity
@Table(name = "musician")
public class Musician {
    private int id;
    private String name;
    private String surname;
    private int age;
    private String role;
    private MusicBand musicBand;

    public Musician() { }

    public Musician(String name, String surname, int age, String role) throws InvalidMusicDataException {
        this.name = name;
        this.surname = surname;
        this.age = age;
        this.role = role;
        validate();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "musician_id")
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    @Column(name = "musician_name", nullable = false, length = 45)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Column(name = "musician_surname", nullable = false, length = 100)
    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    @Column(name = "musician_age", nullable = false)
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    @Column(name = "musician_role", length = 45)
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "musicband_id", nullable = false)
    public MusicBand getMusicBand() { return musicBand; }
    public void setMusicBand(MusicBand musicBand) { this.musicBand = musicBand; }

    public void validate() throws InvalidMusicDataException {
        if (name == null || name.trim().isEmpty()) throw new InvalidMusicDataException("Не указано имя музыканта.");
        if (surname == null || surname.trim().isEmpty()) throw new InvalidMusicDataException("Не указана фамилия музыканта.");
        if (role != null && role.trim().isEmpty()) role = null;
        if (age < 0 || age > 120) throw new InvalidMusicDataException("Возраст музыканта должен быть в диапазоне от 0 до 120 лет.");
    }

    @Override
    public String toString() { return name + " " + surname + " (" + (role == null ? "роль не указана" : role) + ", " + age + " лет)"; }
}
