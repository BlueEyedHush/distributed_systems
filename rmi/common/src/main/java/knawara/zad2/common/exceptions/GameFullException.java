package knawara.zad2.common.exceptions;

import java.io.Serializable;

/**
 * Created by blueeyedhush on 20.03.16.
 */
public class GameFullException extends RuntimeException implements Serializable {
    public GameFullException() {
    }

    public GameFullException(String message) {
        super(message);
    }

    public GameFullException(String message, Throwable cause) {
        super(message, cause);
    }

    public GameFullException(Throwable cause) {
        super(cause);
    }

    public GameFullException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
