package knawara.zad2.common.exceptions;

/**
 * Created by blueeyedhush on 21.03.16.
 */
public class OutOfBoardException extends RuntimeException {
    public OutOfBoardException() {
    }

    public OutOfBoardException(String message) {
        super(message);
    }

    public OutOfBoardException(String message, Throwable cause) {
        super(message, cause);
    }

    public OutOfBoardException(Throwable cause) {
        super(cause);
    }

    public OutOfBoardException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
