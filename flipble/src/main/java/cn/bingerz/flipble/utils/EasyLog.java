package cn.bingerz.flipble.utils;

import android.os.Build;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class EasyLog {

    private static final int MAX_LOG_LENGTH = 4000;
    private static final int MAX_TAG_LENGTH = 23;
    private static final int CALL_STACK_INDEX = 4;
    private static final Pattern ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$");

    private static String explicitTag;
    private static boolean isLoggable = true;

    public static void setExplicitTag(String tag) {
        explicitTag = tag;
    }

    public static String getExplicitTag() {
        return explicitTag;
    }

    public static void setLoggable(boolean enable) {
        isLoggable = enable;
    }

    public static boolean isLoggable() {
        return isLoggable;
    }

    /** Log a verbose message with optional format args. */
    public static void v(String message, Object... args) {
        prepareLog(Log.VERBOSE, null, message, args);
    }

    /** Log a verbose exception and a message with optional format args. */
    public static void v(Throwable t, String message, Object... args) {
        prepareLog(Log.VERBOSE, t, message, args);
    }

    /** Log a verbose exception. */
    public static void v(Throwable t) {
        prepareLog(Log.VERBOSE, t, null);
    }

    /** Log a debug message with optional format args. */
    public static void d(String message, Object... args) {
        prepareLog(Log.DEBUG, null, message, args);
    }

    /** Log a debug exception and a message with optional format args. */
    public static void d(Throwable t, String message, Object... args) {
        prepareLog(Log.DEBUG, t, message, args);
    }

    /** Log a debug exception. */
    public static void d(Throwable t) {
        prepareLog(Log.DEBUG, t, null);
    }

    /** Log an info message with optional format args. */
    public static void i(String message, Object... args) {
        prepareLog(Log.INFO, null, message, args);
    }

    /** Log an info exception and a message with optional format args. */
    public static void i(Throwable t, String message, Object... args) {
        prepareLog(Log.INFO, t, message, args);
    }

    /** Log an info exception. */
    public static void i(Throwable t) {
        prepareLog(Log.INFO, t, null);
    }

    /** Log a warning message with optional format args. */
    public static void w(String message, Object... args) {
        prepareLog(Log.WARN, null, message, args);
    }

    /** Log a warning exception and a message with optional format args. */
    public static void w(Throwable t, String message, Object... args) {
        prepareLog(Log.WARN, t, message, args);
    }

    /** Log a warning exception. */
    public static void w(Throwable t) {
        prepareLog(Log.WARN, t, null);
    }

    /** Log an error message with optional format args. */
    public static void e(String message, Object... args) {
        prepareLog(Log.ERROR, null, message, args);
    }

    /** Log an error exception and a message with optional format args. */
    public static void e(Throwable t, String message, Object... args) {
        prepareLog(Log.ERROR, t, message, args);
    }

    /** Log an error exception. */
    public static void e(Throwable t) {
        prepareLog(Log.ERROR, t, null);
    }

    /** Log an assert message with optional format args. */
    public static void wtf(String message, Object... args) {
        prepareLog(Log.ASSERT, null, message, args);
    }

    /** Log an assert exception and a message with optional format args. */
    public static void wtf(Throwable t, String message, Object... args) {
        prepareLog(Log.ASSERT, t, message, args);
    }

    /** Log an assert exception. */
    public static void wtf(Throwable t) {
        prepareLog(Log.ASSERT, t, null);
    }

    /** Log at {@code priority} a message with optional format args. */
    public static void log(int priority, String message, Object... args) {
        prepareLog(priority, null, message, args);
    }

    /** Log at {@code priority} an exception and a message with optional format args. */
    public static void log(int priority, Throwable t, String message, Object... args) {
        prepareLog(priority, t, message, args);
    }

    /** Log at {@code priority} an exception. */
    public static void log(int priority, Throwable t) {
        prepareLog(priority, t, null);
    }

    private static void prepareLog(int priority, Throwable t, String message, Object... args) {
        if (!isLoggable) {
            return;
        }

        String tag = getTag();

        if (message != null && message.length() == 0) {
            message = null;
        }
        if (message == null) {
            if (t == null) {
                return; // Swallow message if it's null and there's no throwable.
            }
            message = getStackTraceString(t);
        } else {
            if (args != null && args.length > 0) {
                message = formatMessage(message, args);
            }
            if (t != null) {
                message += "\n" + getStackTraceString(t);
            }
        }

        logPrint(priority, tag, message, t);
    }

    /**
     * Formats a log message with optional arguments.
     */
    private static String formatMessage(String message, Object[] args) {
        return String.format(message, args);
    }

    private static String getStackTraceString(Throwable t) {
        // Don't replace this with Log.getStackTraceString() - it hides
        // UnknownHostException, which is not what we want.
        StringWriter sw = new StringWriter(256);
        PrintWriter pw = new PrintWriter(sw, false);
        t.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    private static StackTraceElement getStackTraceElement() {
        // DO NOT switch this to Thread.getCurrentThread().getStackTrace(). The test will pass
        // because Robolectric runs them on the JVM but on Android the elements are different.
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        if (stackTrace.length <= CALL_STACK_INDEX) {
            throw new IllegalStateException(
                    "Synthetic stacktrace didn't have enough elements, Are you using proguard?");
        }
        return stackTrace[CALL_STACK_INDEX];
    }

    private static String createStackElementTag(StackTraceElement element) {
        String tag = element.getClassName();
        Matcher m = ANONYMOUS_CLASS.matcher(tag);
        if (m.find()) {
            tag = m.replaceAll("");
        }
        return tag.substring(tag.lastIndexOf('.') + 1);
    }

    private static String getTag() {
        String tag;
        if (explicitTag != null && explicitTag.length() > 0) {
            tag = explicitTag;
        } else {
            tag = createStackElementTag(getStackTraceElement());
        }
        // Tag length limit was removed in API 24.
        if (tag.length() <= MAX_TAG_LENGTH || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return tag;
        }
        return tag.substring(0, MAX_TAG_LENGTH);
    }

    @Deprecated
    private static String getAppendTag() {
        StackTraceElement element = getStackTraceElement();
        int lineNumber = element.getLineNumber();
        String methodName = element.getMethodName();
        return String.format("#%s:%d ", methodName, lineNumber);
    }

    private static void logPrint(int priority, String tag, String message, Throwable t) {
        if (message.length() < MAX_LOG_LENGTH) {
            if (priority == Log.ASSERT) {
                Log.wtf(tag, message);
            } else {
                Log.println(priority, tag, message);
            }
            return;
        }

        // Split by line, then ensure each line can fit into Log's maximum length.
        for (int i = 0, length = message.length(); i < length; i++) {
            int newline = message.indexOf('\n', i);
            newline = newline != -1 ? newline : length;
            do {
                int end = Math.min(newline, i + MAX_LOG_LENGTH);
                String part = message.substring(i, end);
                if (priority == Log.ASSERT) {
                    Log.wtf(tag, part);
                } else {
                    Log.println(priority, tag, part);
                }
                i = end;
            } while (i < newline);
        }
    }
}