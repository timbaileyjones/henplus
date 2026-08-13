package henplus.logging;

import henplus.HenPlus;

public class Logger {

    /*
     * All methods here tolerate HenPlus.getInstance() being null: callers
     * like SQLStatementSeparator log unconditionally, so this also gets hit
     * before a HenPlus instance exists yet - e.g. from unit tests, or any
     * future headless/embedded use - and used to NPE in that case.
     */

    public static void debug(final String message, final Object... args) {
        final HenPlus instance = HenPlus.getInstance();
        if (instance != null && instance.isVerbose() && !instance.isQuiet()) {
            HenPlus.msg().println(String.format(message, args));
        }
    }

    public static void debug(final String message, final Throwable t, final Object... args) {
        final HenPlus instance = HenPlus.getInstance();
        if (instance != null && instance.isVerbose() && !instance.isQuiet()) {
            HenPlus.msg().println(String.format(message, args));
            t.printStackTrace();
        }
    }

    public static void info(final String message, final Object... args) {
        final HenPlus instance = HenPlus.getInstance();
        if (instance == null || !instance.isQuiet()) {
            print(instance, String.format(message, args));
        }
    }

    public static void error(final String message, final Object... args) {
        print(HenPlus.getInstance(), String.format(message, args));
    }

    public static void error(final String message, final Throwable t, final Object... args) {
        print(HenPlus.getInstance(), String.format(message, args));
        t.printStackTrace();
    }

    private static void print(final HenPlus instance, final String formatted) {
        if (instance == null) {
            System.err.println(formatted);
        } else {
            HenPlus.msg().println(formatted);
        }
    }
}
