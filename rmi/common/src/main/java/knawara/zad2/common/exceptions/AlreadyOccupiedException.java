package knawara.zad2.common.exceptions;

/**
 * Created by blueeyedhush on 21.03.16.
 */
public class AlreadyOccupiedException extends RuntimeException {
    public AlreadyOccupiedException() {
    }

    public AlreadyOccupiedException(String message) {
        super(message);
    }

    public AlreadyOccupiedException(String message, Throwable cause) {
        super(message, cause);
    }

    public AlreadyOccupiedException(Throwable cause) {
        super(cause);
    }

    public AlreadyOccupiedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
