package musicmanager.app.models;

import musicmanager.app.exceptions.InvalidMusicDataException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MusicianValidationTest {
    @Test void acceptsBoundaryAgesAndNormalizesBlankRole() throws Exception {
        assertDoesNotThrow(() -> new Musician("A", "B", 0, null));
        assertDoesNotThrow(() -> new Musician("A", "B", 120, " keys "));
        Musician m = new Musician("A", "B", 20, "  "); assertNull(m.getRole());
    }
    @Test void validatesEachRequiredFieldAndAge() {
        assertInvalid(null,"B",20,"Не указано имя музыканта.");
        assertInvalid(" ","B",20,"Не указано имя музыканта.");
        assertInvalid("A",null,20,"Не указана фамилия музыканта.");
        assertInvalid("A","\t",20,"Не указана фамилия музыканта.");
        assertInvalid("A","B",-1,"Возраст музыканта должен быть в диапазоне от 0 до 120 лет.");
        assertInvalid("A","B",121,"Возраст музыканта должен быть в диапазоне от 0 до 120 лет.");
    }
    private void assertInvalid(String name,String surname,int age,String message) {
        var ex=assertThrows(InvalidMusicDataException.class,()->new Musician(name,surname,age,null)); assertEquals(message,ex.getMessage());
    }
}
