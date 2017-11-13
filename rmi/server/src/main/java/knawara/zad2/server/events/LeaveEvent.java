package knawara.zad2.server.events;

/**
 * Created by blueeyedhush on 20.03.16.
 */
public class LeaveEvent implements GameEvent {
    private final char playerId;

    public LeaveEvent(char playerId) {

        this.playerId = playerId;
    }

    public char getPlayerId() {
        return playerId;
    }
}
