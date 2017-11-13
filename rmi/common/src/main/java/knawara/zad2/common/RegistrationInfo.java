package knawara.zad2.common;

import java.io.Serializable;

/**
 * Created by blueeyedhush on 20.03.16.
 */
public class RegistrationInfo implements Serializable {
    private final char id;
    private final Player player;
    private final int boardDim;

    public RegistrationInfo(char id, Player player, int boardDim) {
        this.id = id;
        this.player = player;

        this.boardDim = boardDim;
    }

    public Player getPlayer() {
        return player;
    }

    public char getId() {
        return id;
    }

    public int getBoardDim() {
        return boardDim;
    }
}
