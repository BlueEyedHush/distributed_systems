package knawara.zad2.server.events;

/**
 * Created by blueeyedhush on 20.03.16.
 */
public class MoveEvent implements GameEvent {
    private final int x;
    private final int y;

    public MoveEvent(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
