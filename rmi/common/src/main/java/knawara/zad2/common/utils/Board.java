package knawara.zad2.common.utils;

import java.util.Arrays;
import java.util.Optional;

/**
 * Created by blueeyedhush on 21.03.16.
 */
public class Board {
    private static final char UNOCCUPIED = '.';

    private char[][] board;
    private long unoccupied;

    public Board(int dimmension) {
        assert dimmension > 0;

        board = new char[dimmension][dimmension];
        unoccupied = dimmension*dimmension;
        zeroBoard();
    }

    public boolean isOccupied(int x, int y) {
        assert isWithinBounds(x,y);
        return board[y][x] != UNOCCUPIED;
    }

    public void occupy(int x, int y, char symbol) {
        if(symbol == UNOCCUPIED) throw new IllegalArgumentException();
        if(!isOccupied(x,y)) unoccupied--;
        board[y][x] = symbol;
    }

    public void unoccupy(int x, int y) {
        if(isOccupied(x,y)) unoccupied++;
        board[y][x] = UNOCCUPIED;
    }

    public Optional<Character> at(int x, int y) {
        if(isOccupied(x,y)) {
            return Optional.of(board[y][x]);
        } else {
            return Optional.empty();
        }
    }

    public boolean isWithinBounds(int x, int y) {
        return x >= 0 && x < board.length && y >= 0 && y < board.length;
    }

    public long howMuchUnoccupied() {
        return unoccupied;
    }

    private void zeroBoard() {
        for(char[] row: board) {
            Arrays.fill(row, UNOCCUPIED);
        }
    }
}
