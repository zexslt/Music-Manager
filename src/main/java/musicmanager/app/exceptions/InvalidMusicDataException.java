package musicmanager.app.exceptions;

/** Исключение для некорректных данных о музыканте.
 */
public class InvalidMusicDataException extends Exception {
    /** Создаёт исключение с сообщением об ошибке.
     * @param message описание некорректных данных
     */
    public InvalidMusicDataException(String message) { super(message); }
}
