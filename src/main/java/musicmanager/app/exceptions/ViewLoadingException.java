package musicmanager.app.exceptions;

/**
 * Исключение возникает, если экран приложения не удалось загрузить.
 * Используется для передачи ошибки загрузки представления обработчику интерфейса.
 */
public class ViewLoadingException extends Exception {
    /** Создаёт исключение с описанием причины ошибки.
     * @param message описание ошибки
     * @param cause исходное исключение
     */
    public ViewLoadingException(String message, Throwable cause) { super(message, cause); }
}
