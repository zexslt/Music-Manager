package musicmanager.app.database;

import musicmanager.app.models.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CityServiceTest extends ServiceTestSupport {
    @Test void trimsFindsAndReusesByName() {
        City c = CityService.findOrCreate(em, "  Paris  ");
        assertEquals("Paris", c.getName());
        assertSame(c, CityService.findOrCreate(em, "Paris"));
        assertSame(c, CityService.findOrCreate(em, " Paris "));
    }
    @Test void rejectsNullBlankAndTooLong() {
        var nullError = assertThrows(IllegalArgumentException.class, () -> CityService.findOrCreate(em, null));
        assertEquals("Название города не может быть пустым.", nullError.getMessage());
        var blankError = assertThrows(IllegalArgumentException.class, () -> CityService.findOrCreate(em, " \t "));
        assertEquals("Название города не может быть пустым.", blankError.getMessage());
        assertDoesNotThrow(() -> CityService.findOrCreate(em, "x".repeat(45)));
        var lengthError = assertThrows(IllegalArgumentException.class, () -> CityService.findOrCreate(em, "x".repeat(46)));
        assertEquals("Название города не должно превышать 45 символов.", lengthError.getMessage());
    }
}


