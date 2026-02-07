package exception;

import static java.lang.System.err;

/**
 * This should be used in a execpt where Python is involved.
 * */
public class UhOhPythonDied extends RuntimeException {
    public UhOhPythonDied(String message, Throwable err) {
        super(message, err);

    }
}
