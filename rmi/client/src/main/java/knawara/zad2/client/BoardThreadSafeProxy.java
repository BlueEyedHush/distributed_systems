package knawara.zad2.client;

import knawara.zad2.common.utils.Board;

import java.util.Optional;

/**
 * Created by blueeyedhush on 21.03.16.
 */
public class BoardThreadSafeProxy {
    private Board board;
    private int dimmension;

    public BoardThreadSafeProxy(int dimmension) {
        board = new Board(dimmension);
    }

    public synchronized void occupy(int x, int y, char symbol) {
        board.occupy(x, y, symbol);
    }

    public synchronized String printExcerpt(int x, int y, int size) {
        /* clamp coords */
        if(!board.isWithinBounds(x,y)) {
            return "Lower corner out of bounds";
        } else {
            int maxX = Math.min(x + size, dimmension-1);
            int maxY = Math.min(y + size, dimmension-1);

            StringBuilder builder = new StringBuilder();
            for(int lY = maxY; lY >= x; lY--) {
                for(int lX = x; lX <= maxX; lX++) {
                    Optional<Character> val = board.at(lX, lY);
                    if(val.isPresent()) {
                        builder.append(val.get());
                    } else {
                        builder.append('.');
                    }
                }
                builder.append(System.lineSeparator());
            }

            return builder.toString();
        }
    }
}
