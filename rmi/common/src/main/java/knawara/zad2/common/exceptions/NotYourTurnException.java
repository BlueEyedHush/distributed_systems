package knawara.zad2.common.exceptions;

/**
 * Created by blueeyedhush on 21.03.16.
 */
public class NotYourTurnException extends RuntimeException {
    public NotYourTurnException() {
    }

    public NotYourTurnException(String message) {
        super(message);
    }

    public NotYourTurnException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotYourTurnException(Throwable cause) {
        super(cause);
    }

    public NotYourTurnException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
