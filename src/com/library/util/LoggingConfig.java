package com.library.util;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Configures java.util.logging for the whole application.
 *
 * <p>The JDK's own logging framework is used deliberately: the brief rules out
 * external dependencies, and the project must compile with plain {@code javac}
 * without a build tool fetching jars. The API is the same shape as SLF4J or
 * Log4j — named loggers, levels, handlers, formatters — so swapping in either
 * would mean changing this class and the import lines, nothing else.</p>
 *
 * <p>Two handlers are installed: a console handler at INFO for the operator, and
 * a file handler at ALL writing {@code logs/library.log} for the audit trail.</p>
 */
public final class LoggingConfig {

    private static final String ROOT_PACKAGE = "com.library";
    private static final String LOG_DIRECTORY = "logs";
    private static final String LOG_FILE = LOG_DIRECTORY + "/library.log";
    private static boolean configured;

    private LoggingConfig() {
    }

    /**
     * Installs the handlers. Safe to call more than once; later calls do nothing.
     *
     * @param consoleLevel the lowest level shown on the console
     */
    public static synchronized void configure(Level consoleLevel) {
        if (configured) {
            return;
        }
        Logger root = Logger.getLogger(ROOT_PACKAGE);
        root.setUseParentHandlers(false);
        root.setLevel(Level.ALL);

        for (Handler existing : root.getHandlers()) {
            root.removeHandler(existing);
        }

        ConsoleHandler console = new ConsoleHandler();
        console.setLevel(consoleLevel);
        console.setFormatter(new CompactFormatter());
        root.addHandler(console);

        try {
            new java.io.File(LOG_DIRECTORY).mkdirs();
            FileHandler file = new FileHandler(LOG_FILE, true);
            file.setLevel(Level.ALL);
            file.setFormatter(new CompactFormatter());
            root.addHandler(file);
        } catch (IOException e) {
            root.log(Level.WARNING, "File logging is unavailable; continuing with console only", e);
        }

        configured = true;
    }

    /**
     * Convenience for callers that want the default console level.
     */
    public static void configure() {
        configure(Level.INFO);
    }

    /**
     * @param type the class requesting a logger
     * @return a logger named after that class
     */
    public static Logger getLogger(Class<?> type) {
        return Logger.getLogger(type.getName());
    }

    /**
     * Raises or lowers how much reaches the console at runtime.
     *
     * @param level the new console level
     */
    public static void setConsoleLevel(Level level) {
        for (Handler handler : Logger.getLogger(ROOT_PACKAGE).getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                handler.setLevel(level);
            }
        }
    }

    /**
     * One line per record, which keeps the console readable next to menu output.
     */
    private static class CompactFormatter extends Formatter {

        private static final DateTimeFormatter TIMESTAMP =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public String format(LogRecord record) {
            String source = record.getLoggerName() == null ? "?" : record.getLoggerName();
            int lastDot = source.lastIndexOf('.');
            String shortName = lastDot < 0 ? source : source.substring(lastDot + 1);
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("  %s  %-7s %-22s %s%n",
                    LocalDateTime.now().format(TIMESTAMP),
                    record.getLevel().getName(),
                    shortName,
                    formatMessage(record)));
            if (record.getThrown() != null) {
                builder.append("      cause: ").append(record.getThrown()).append(System.lineSeparator());
            }
            return builder.toString();
        }
    }
}
