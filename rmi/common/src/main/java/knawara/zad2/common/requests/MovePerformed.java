package knawara.zad2.common.requests;

/**
 * Created by blueeyedhush on 21.03.16.
 */
public class MovePerformed implements Request {
    private final char who;
    private final int x;
    private final int y;

    public MovePerformed(char who, int x, int y) {
        this.who = who;
        this.x = x;
        this.y = y;
    }

    public char getWho() {
        return who;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
