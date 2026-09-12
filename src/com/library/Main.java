package com.library;

import com.library.app.DemoRunner;
import com.library.app.LibraryApplication;
import com.library.app.LibraryConsole;
import com.library.test.LibraryTestRunner;
import com.library.util.LoggingConfig;

import java.util.logging.Level;

/**
 * Entry point.
 *
 * <p>Chooses a mode, wires the application once through
 * {@link LibraryApplication}, and hands the wired services to whichever runner
 * was asked for. Nothing else happens here, so the entry point never becomes the
 * place where behaviour quietly accumulates.</p>
 *
 * <ul>
 *   <li>{@code --demo} run the scripted walkthrough and exit</li>
 *   <li>{@code --test} run the test suite and exit</li>
 *   <li>{@code --quiet} keep log output off the console</li>
 *   <li>{@code --verbose} show FINE level logging</li>
 *   <li>{@code --help} print usage and exit</li>
 * </ul>
 */
public final class Main {

    private Main() {
    }

    /**
     * @param args optional mode flags
     */
    public static void main(String[] args) {
        if (hasFlag(args, "--help")) {
            printUsage();
            return;
        }

        LoggingConfig.configure(resolveConsoleLevel(args));

        if (hasFlag(args, "--test")) {
            boolean passed = new LibraryTestRunner().runAll();
            System.exit(passed ? 0 : 1);
        }

        LibraryApplication application = new LibraryApplication();

        if (hasFlag(args, "--demo")) {
            new DemoRunner(application).run();
            return;
        }

        DemoRunner.SampleData.load(application);
        new LibraryConsole(application).start();
    }

    private static Level resolveConsoleLevel(String[] args) {
        if (hasFlag(args, "--quiet")) {
            return Level.OFF;
        }
        if (hasFlag(args, "--verbose")) {
            return Level.FINE;
        }
        return Level.INFO;
    }

    private static boolean hasFlag(String[] args, String flag) {
        if (args == null) {
            return false;
        }
        for (String arg : args) {
            if (flag.equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }

    private static void printUsage() {
        System.out.println("Library Management System");
        System.out.println("Usage: java -cp out com.library.Main [options]");
        System.out.println("  (no options)  start the interactive console with sample data");
        System.out.println("  --demo        run the guided demonstration and exit");
        System.out.println("  --test        run the test suite and exit");
        System.out.println("  --quiet       suppress console logging");
        System.out.println("  --verbose     show FINE level logging");
        System.out.println("  --help        show this message");
    }
}
