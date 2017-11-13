package knawara.zad2.common.requests;

/**
 * Created by blueeyedhush on 21.03.16.
 */
public final class Won implements Request {
    private final char playerId;

    public Won(char playerId) {

        this.playerId = playerId;
    }

    public char getPlayerId() {
        return playerId;
    }
}
