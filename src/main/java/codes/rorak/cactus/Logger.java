package codes.rorak.cactus;

import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    /*
    Log levels:
    1 - time
    2 - date
    4 - thread
    8 - method
    16 - class
    32 - line number
    */
    public static final PrintStream stdout_old = System.out;
    public static final PrintStream stderr_old = System.err;
    public static void init() {
        System.setOut(new PrintStream(OutputStream.nullOutputStream()){
            @Override
            public void println(String message) {
                log(message);
            }
            @Override
            public void print(String message) {
                note("[INFO] " + message, 5, stdout_old, false);
            }
        });
        System.setErr(new PrintStream(OutputStream.nullOutputStream()){
            @Override
            public void println(String message) {
                err(message);
            }
            @Override
            public void print(String message) {
                note("[ERROR] " + message, 5, stderr_old, false);
            }
        });
    }
    public static void note(String message, int logLevel, PrintStream wr, boolean line) {
        String datetime = bit(logLevel, 0) || bit(logLevel, 1) ?
                "[" + (bit(logLevel, 0) ? time() : "") + (bit(logLevel, 1) ? (bit(logLevel, 0) ? " " : "") + date() : "") + "] " : "";
        String thread = bit(logLevel, 2) ? "[THREAD#" + Thread.currentThread().getName() + "] " : "";
        String debugInfo =
                bit(logLevel, 3) || bit(logLevel, 4) || bit(logLevel, 5) ? "[" +
                        (bit(logLevel, 4) ? clazz() + ".java" : "") +
                        (bit(logLevel, 4) && bit(logLevel, 5) ? ":" : "") +
                        (bit(logLevel, 5) ? line() : "") +
                        ((bit(logLevel, 5) && bit(logLevel, 3)) || (bit(logLevel, 4) && bit(logLevel, 3)) ? " " : "") +
                        (bit(logLevel, 3) ? "in " + method() + "()" : "")
                        + "] " : "";

        wr.print(datetime + thread + debugInfo + message + (line ? "\n" : ""));
    }

    public static void log(String message) {
        log(message, 5);
    }
    public static void log(String message, int logLevel) {
        note("[INFO] " + message, logLevel, stdout_old, true);
    }
    public static void err(String message) {
        err(message, 61);
    }
    public static void err(String message, int logLevel) {
        note("[ERROR] " + message, logLevel, stderr_old, true);
    }
    public static void err(String message, Throwable e) {
        err(message, e, 61);
    }
    public static void err(String message, Throwable e, int logLevel) {
        err(message, logLevel);
        System.err.print("|||| More info: " );
        e.printStackTrace(System.err);
    }

    private static boolean bit(int n, int k) {
        return ((n >> k) & 1)==1;
    }
    private static @NotNull String date() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("d.M.yy"));
    }
    private static @NotNull String time() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    private static String clazz() {
        return Thread.currentThread().getStackTrace()[1].getClassName();
    }
    private static String line() {
        return Integer.toString(Thread.currentThread().getStackTrace()[1].getLineNumber());
    }
    private static String method() {
        return Thread.currentThread().getStackTrace()[1].getMethodName();
    }

    @Deprecated
    private static void just_to_annoy_you_lmao(String message, int logLevel) {
        System.out.println((((logLevel&1)==1)||(((logLevel>>1)&1)==1)?"["+(((logLevel&1)==1)?LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")):"")+((((logLevel>>1)&1)==1)?(((logLevel&1)==1)?" ":"")+LocalDateTime.now().format(DateTimeFormatter.ofPattern("d.M.yy")):"")+"] ":"")+((((logLevel>>2)&1)==1)?" [THREAD#"+Thread.currentThread().getName()+"] ":"")+((((logLevel>> 3)&1)==1)||(((logLevel>>4)&1)==1)||(((logLevel>>5)&1)==1)?"["+((((logLevel>>4)&1)==1)?Thread.currentThread().getStackTrace()[1].getClassName()+".java":"")+((((logLevel>>4)&1)==1)&&(((logLevel>>5)&1)==1)?":":"")+((((logLevel>>5)&1)==1)?Integer.toString(Thread.currentThread().getStackTrace()[1].getLineNumber()):"")+(((((logLevel>>5)&1)==1)&&(((logLevel>>3)&1)==1))||((((logLevel>>4)&1)==1)&&(((logLevel>>3)&1)==1))?" ":"")+(((((logLevel>>3)&1)==1))?"in "+Thread.currentThread().getStackTrace()[1].getMethodName():"")+"] ":"")+(message));
    }
}
