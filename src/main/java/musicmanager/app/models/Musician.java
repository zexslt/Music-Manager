package musicmanager.app.models;

import musicmanager.app.exceptions.InvalidMusicDataException;

public class Musician {
    private String name;
    private String surname;
    private int age;
    private String role;

    public Musician(String name, String surname, int age, String role) throws InvalidMusicDataException {
        this.name = name;
        this.surname = surname;
        this.age = age;
        this.role = role;

        validate();
    }

    //Getters
    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public int getAge() {
        return age;
    }

    public String getRole() {
        return role;
    }

    //Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Проверяет корректность данных музыканта.
     * @throws InvalidMusicDataException если обязательные поля пусты или возраст некорректен
     */
    public void validate() throws InvalidMusicDataException {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidMusicDataException("Не указано имя музыканта.");
        }
        if (surname == null || surname.trim().isEmpty()) {
            throw new InvalidMusicDataException("Не указана фамилия музыканта.");
        }
        if (role == null || role.trim().isEmpty()) {
            throw new InvalidMusicDataException("Не указана роль музыканта.");
        }
        if (age < 0 || age > 120) {
            throw new InvalidMusicDataException("Возраст музыканта должен быть в диапазоне от 0 до 120 лет.");
        }
    }

    // Print Info
    @Override
    public String toString() {
        return name + " " + surname + " (" + role + ", " + age + " лет)";
    }
}
