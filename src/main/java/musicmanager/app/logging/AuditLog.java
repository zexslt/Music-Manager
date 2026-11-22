package musicmanager.app.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public final class AuditLog {
    private static final String USER_ID = System.getProperty("musicmanager.userId",
            System.getenv().getOrDefault("MUSIC_MANAGER_USER_ID", "anonymous"));

    private AuditLog() { }

    public static void event(String name, Map<String, ?> parameters) {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        StackTraceElement caller = Arrays.stream(stack)
                .filter(frame -> !frame.getClassName().equals(Thread.class.getName()))
                .filter(frame -> !frame.getClassName().equals(AuditLog.class.getName()))
                .findFirst().orElse(stack[stack.length - 1]);
        String values = parameters == null ? "{}" : parameters.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + safeValue(entry.getValue()))
                .collect(Collectors.joining(", ", "{", "}"));
        String callStack = Arrays.stream(stack)
                .filter(frame -> !frame.getClassName().equals(Thread.class.getName()))
                .filter(frame -> !frame.getClassName().equals(AuditLog.class.getName()))
                .map(StackTraceElement::toString)
                .collect(Collectors.joining(" <- "));
        String message = String.format("time=%s event=%s module=%s method=%s userId=%s parameters=%s callStack=%s",
                Instant.now(), name, caller.getClassName(), caller.getMethodName(), USER_ID, values, callStack);
        try {
            Logger logger = LogManager.getLogger("UserAudit");
            if (logger.isInfoEnabled()) logger.info(message);
            else System.out.println("INFO UserAudit - " + message);
        } catch (Throwable loggingFailure) {
            System.out.println("INFO UserAudit - " + message);
            System.err.println("Log4j output failed: " + loggingFailure);
        }
    }

    public static void failure(String name, Map<String, ?> parameters, Throwable error) {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        StackTraceElement caller = Arrays.stream(stack)
                .filter(frame -> !frame.getClassName().equals(Thread.class.getName()))
                .filter(frame -> !frame.getClassName().equals(AuditLog.class.getName()))
                .findFirst().orElse(stack[stack.length - 1]);
        String values = parameters == null ? "{}" : parameters.toString();
        String message = String.format("time=%s event=%s module=%s method=%s userId=%s parameters=%s exception=%s",
                Instant.now(), name, caller.getClassName(), caller.getMethodName(), USER_ID,
                values, error == null ? "unknown" : error.toString());
        try {
            Logger logger = LogManager.getLogger("UserAudit");
            if (logger.isErrorEnabled()) logger.error(message, error);
            else {
                System.err.println("ERROR UserAudit - " + message);
                if (error != null) error.printStackTrace(System.err);
            }
        } catch (Throwable loggingFailure) {
            System.err.println("ERROR UserAudit - " + message);
            System.err.println("Audit logging failed for event '" + name + "': " + loggingFailure);
            if (error != null) error.printStackTrace(System.err);
        }
    }

    private static String safeValue(Object value) {
        if (value == null) return "null";
        String text = String.valueOf(value).replaceAll("[\\r\\n]", " ");
        return text.length() > 500 ? text.substring(0, 500) + "…" : text;
    }
}
