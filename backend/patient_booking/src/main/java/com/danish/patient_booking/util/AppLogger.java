package com.danish.patient_booking.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class AppLogger {

    private final Logger logger;

    private AppLogger(Class<?> sourceClass) {
        this.logger = Logger.getLogger(sourceClass.getName());
    }

    public static AppLogger getLogger(Class<?> sourceClass) {
        return new AppLogger(sourceClass);
    }

    public void debug(String message, Object... args) {
        log(Level.FINE, message, args);
    }

    public void info(String message, Object... args) {
        log(Level.INFO, message, args);
    }

    public void warn(String message, Object... args) {
        log(Level.WARNING, message, args);
    }

    public void error(String message, Object... args) {
        log(Level.SEVERE, message, args);
    }

    private void log(Level level, String message, Object... args) {
        if (!logger.isLoggable(level)) {
            return;
        }

        Throwable throwable = extractThrowable(args);
        Object[] formatArgs = throwable == null ? args : trimLast(args);
        String formattedMessage = format(message, formatArgs);

        if (throwable == null) {
            logger.log(level, formattedMessage);
        } else {
            logger.log(level, formattedMessage, throwable);
        }
    }

    private static Throwable extractThrowable(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        Object last = args[args.length - 1];
        return last instanceof Throwable throwable ? throwable : null;
    }

    private static Object[] trimLast(Object[] args) {
        Object[] trimmed = new Object[args.length - 1];
        System.arraycopy(args, 0, trimmed, 0, trimmed.length);
        return trimmed;
    }

    private static String format(String message, Object[] args) {
        if (message == null || args == null || args.length == 0) {
            return message;
        }

        StringBuilder result = new StringBuilder();
        int searchFrom = 0;
        int argIndex = 0;

        while (argIndex < args.length) {
            int placeholder = message.indexOf("{}", searchFrom);
            if (placeholder < 0) {
                break;
            }
            result.append(message, searchFrom, placeholder);
            result.append(args[argIndex]);
            searchFrom = placeholder + 2;
            argIndex++;
        }

        result.append(message.substring(searchFrom));
        return result.toString();
    }
}
